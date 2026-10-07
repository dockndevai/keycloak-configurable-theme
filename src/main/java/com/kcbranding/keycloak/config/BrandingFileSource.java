package com.kcbranding.keycloak.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.jboss.logging.Logger;
import org.keycloak.util.JsonSerialization;

/**
 * Global branding file shared by all realms:
 *
 * <pre>
 * {
 *   "defaults": { "primaryColor": "#0f766e", "layout": "split-left" },
 *   "realms":   { "acme": { "logoUrl": "acme-logo.svg" } }
 * }
 * </pre>
 *
 * The file is re-read when its modification time changes (checked at most once per reload interval),
 * so edits take effect without restarting Keycloak. A broken file keeps the last good version.
 */
public class BrandingFileSource {

    private static final Logger LOG = Logger.getLogger(BrandingFileSource.class);

    /** Parsed file content. */
    public static final class FileModel {
        public Map<String, Object> defaults = Map.of();
        public Map<String, Map<String, Object>> realms = Map.of();
    }

    private final Path file;
    private final long reloadIntervalMillis;

    private volatile FileModel model = new FileModel();
    private volatile long lastModified = Long.MIN_VALUE;
    private volatile long lastCheck = Long.MIN_VALUE;

    public BrandingFileSource(Path file, long reloadIntervalMillis) {
        this.file = file;
        this.reloadIntervalMillis = reloadIntervalMillis;
        reloadIfChanged(true);
    }

    /** Returns the layers for a realm, lowest priority first: file defaults, then the realm section. */
    public List<Map<String, Object>> layersFor(String realmName) {
        reloadIfChanged(false);
        FileModel current = model;
        Map<String, Object> realm = current.realms == null ? null : current.realms.get(realmName);
        return List.of(current.defaults == null ? Map.of() : current.defaults, realm == null ? Map.of() : realm);
    }

    public Path file() {
        return file;
    }

    private void reloadIfChanged(boolean force) {
        if (file == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (!force && now - lastCheck < reloadIntervalMillis) {
            return;
        }
        synchronized (this) {
            lastCheck = now;
            try {
                if (!Files.isRegularFile(file)) {
                    if (lastModified != -1) {
                        LOG.infof("Branding file %s not found; using built-in defaults and realm settings", file);
                    }
                    model = new FileModel();
                    lastModified = -1;
                    return;
                }
                long modified = Files.getLastModifiedTime(file).toMillis();
                if (modified == lastModified) {
                    return;
                }
                try (InputStream in = Files.newInputStream(file)) {
                    FileModel parsed = JsonSerialization.readValue(in, FileModel.class);
                    logInvalidEntries(parsed);
                    model = parsed;
                    lastModified = modified;
                    LOG.infof("Loaded branding file %s", file);
                }
            } catch (IOException | RuntimeException e) {
                LOG.errorf(e, "Could not read branding file %s; keeping the previous configuration", file);
            }
        }
    }

    private static void logInvalidEntries(FileModel parsed) {
        if (parsed.defaults != null) {
            BrandingValidator.validate(parsed.defaults).errors()
                    .forEach(e -> LOG.warnf("Ignoring invalid branding default: %s", e));
        }
        if (parsed.realms != null) {
            parsed.realms.forEach((realm, values) -> BrandingValidator.validate(values).errors()
                    .forEach(e -> LOG.warnf("Ignoring invalid branding value for realm %s: %s", realm, e)));
        }
    }
}
