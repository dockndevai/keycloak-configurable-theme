package com.kcbranding.keycloak.spi;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import com.kcbranding.keycloak.config.BrandingConfig;
import com.kcbranding.keycloak.config.BrandingField;
import com.kcbranding.keycloak.config.BrandingFileSource;
import com.kcbranding.keycloak.config.BrandingLayout;
import com.kcbranding.keycloak.config.BrandingValidator;
import jakarta.ws.rs.core.UriBuilder;
import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.urls.UrlType;
import org.keycloak.util.JsonSerialization;

public class DefaultBrandingProvider implements BrandingProvider {

    private static final Logger LOG = Logger.getLogger(DefaultBrandingProvider.class);

    public static final String CONFIG_ATTRIBUTE = "branding.config";
    public static final String ASSET_ATTRIBUTE_PREFIX = "branding.asset.";
    public static final String LAYOUT_ATTRIBUTE_PREFIX = "branding.layout.";
    private static final String LAYOUTS_DIR = "layouts";

    private static final Map<String, String> CONTENT_TYPES = Map.ofEntries(
            Map.entry("png", "image/png"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("gif", "image/gif"),
            Map.entry("webp", "image/webp"),
            Map.entry("svg", "image/svg+xml"),
            Map.entry("ico", "image/x-icon"),
            Map.entry("woff", "font/woff"),
            Map.entry("woff2", "font/woff2"),
            Map.entry("ttf", "font/ttf"),
            Map.entry("otf", "font/otf"));

    record ParsedOverrides(String raw, Map<String, String> values) {
    }

    private final KeycloakSession session;
    private final BrandingFileSource fileSource;
    private final Path assetsDir;
    private final int maxAssetBytes;
    private final Map<String, ParsedOverrides> overrideCache;

    DefaultBrandingProvider(KeycloakSession session, BrandingFileSource fileSource, Path assetsDir,
                            int maxAssetBytes, Map<String, ParsedOverrides> overrideCache) {
        this.session = session;
        this.fileSource = fileSource;
        this.assetsDir = assetsDir;
        this.maxAssetBytes = maxAssetBytes;
        this.overrideCache = overrideCache;
    }

    @Override
    public BrandingConfig getConfig(RealmModel realm) {
        List<Map<String, ?>> layers = new ArrayList<>(fileSource.layersFor(realm.getName()));
        layers.add(getRealmOverrides(realm));
        return BrandingConfig.layered(layers);
    }

    @Override
    public Map<String, String> getRealmOverrides(RealmModel realm) {
        String raw = realm.getAttribute(CONFIG_ATTRIBUTE);
        if (raw == null || raw.isBlank()) {
            return Map.of();
        }
        ParsedOverrides cached = overrideCache.get(realm.getId());
        if (cached != null && cached.raw().equals(raw)) {
            return cached.values();
        }
        Map<String, String> values;
        try {
            Map<String, Object> parsed = JsonSerialization.readValue(raw, new TypeReference<Map<String, Object>>() { });
            values = BrandingValidator.validate(parsed).values();
        } catch (IOException e) {
            LOG.errorf(e, "Ignoring unreadable branding overrides of realm %s", realm.getName());
            values = Map.of();
        }
        overrideCache.put(realm.getId(), new ParsedOverrides(raw, Map.copyOf(values)));
        return Map.copyOf(values);
    }

    @Override
    public void setRealmOverrides(RealmModel realm, Map<String, String> overrides) {
        Map<String, String> nonBlank = new LinkedHashMap<>();
        overrides.forEach((k, v) -> {
            if (v != null && !v.isBlank()) {
                nonBlank.put(k, v);
            }
        });
        if (nonBlank.isEmpty()) {
            realm.removeAttribute(CONFIG_ATTRIBUTE);
            return;
        }
        try {
            realm.setAttribute(CONFIG_ATTRIBUTE, JsonSerialization.writeValueAsString(nonBlank));
        } catch (IOException e) {
            throw new IllegalStateException("Could not serialise branding overrides", e);
        }
    }

    @Override
    public Optional<BrandingAsset> findAsset(RealmModel realm, String name) {
        if (!BrandingValidator.ASSET_NAME.matcher(name).matches()) {
            return Optional.empty();
        }
        String stored = realm.getAttribute(ASSET_ATTRIBUTE_PREFIX + name);
        if (stored != null) {
            try {
                return Optional.of(new BrandingAsset(name, contentType(name), Base64.getDecoder().decode(stored),
                        BrandingAsset.Source.REALM));
            } catch (IllegalArgumentException e) {
                LOG.warnf("Corrupt branding asset %s in realm %s", name, realm.getName());
            }
        }
        for (Path candidate : List.of(assetsDir.resolve(realm.getName()).resolve(name), assetsDir.resolve(name))) {
            Path normalised = candidate.normalize();
            if (normalised.startsWith(assetsDir) && Files.isRegularFile(normalised)) {
                try {
                    return Optional.of(new BrandingAsset(name, contentType(name), Files.readAllBytes(normalised),
                            BrandingAsset.Source.FILE));
                } catch (IOException e) {
                    LOG.warnf(e, "Could not read branding asset %s", normalised);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<String> listRealmAssets(RealmModel realm) {
        return realm.getAttributes().keySet().stream()
                .filter(k -> k.startsWith(ASSET_ATTRIBUTE_PREFIX))
                .map(k -> k.substring(ASSET_ATTRIBUTE_PREFIX.length()))
                .sorted()
                .toList();
    }

    @Override
    public void saveRealmAsset(RealmModel realm, String name, byte[] content) {
        if (!BrandingValidator.ASSET_NAME.matcher(name).matches()) {
            throw new IllegalArgumentException("Invalid asset name");
        }
        if (content.length > maxAssetBytes) {
            throw new IllegalArgumentException("Asset exceeds " + maxAssetBytes / 1024 + " KiB");
        }
        realm.setAttribute(ASSET_ATTRIBUTE_PREFIX + name, Base64.getEncoder().encodeToString(content));
    }

    @Override
    public boolean deleteRealmAsset(RealmModel realm, String name) {
        String key = ASSET_ATTRIBUTE_PREFIX + name;
        if (realm.getAttribute(key) == null) {
            return false;
        }
        realm.removeAttribute(key);
        return true;
    }

    @Override
    public Optional<BrandingLayout> findLayout(RealmModel realm, String name) {
        if (!BrandingLayout.isValidName(name)) {
            return Optional.empty();
        }
        if (BrandingLayout.isBuiltIn(name)) {
            return Optional.of(BrandingLayout.builtIn(name));
        }
        String stored = realm.getAttribute(LAYOUT_ATTRIBUTE_PREFIX + name);
        if (stored != null) {
            return Optional.of(BrandingLayout.custom(name, stored, BrandingLayout.Source.REALM));
        }
        for (Path dir : layoutDirs(realm)) {
            Path file = dir.resolve(name + ".css").normalize();
            if (file.startsWith(assetsDir) && Files.isRegularFile(file)) {
                try {
                    String css = Files.readString(file, StandardCharsets.UTF_8);
                    if (BrandingLayout.isValidCss(css)) {
                        return Optional.of(BrandingLayout.custom(name, css, BrandingLayout.Source.FILE));
                    }
                    LOG.warnf("Ignoring layout %s: empty or larger than %d bytes", file, BrandingLayout.MAX_CSS_BYTES);
                } catch (IOException e) {
                    LOG.warnf(e, "Could not read layout %s", file);
                }
            }
        }
        return Optional.empty();
    }

    @Override
    public List<BrandingLayout> listLayouts(RealmModel realm) {
        Map<String, BrandingLayout> custom = new TreeMap<>();
        // Lowest priority first so realm-specific sources overwrite global ones.
        List<Path> dirs = new ArrayList<>(layoutDirs(realm));
        java.util.Collections.reverse(dirs);
        for (Path dir : dirs) {
            if (!Files.isDirectory(dir)) {
                continue;
            }
            try (Stream<Path> files = Files.list(dir)) {
                files.map(f -> f.getFileName().toString())
                        .filter(f -> f.endsWith(".css"))
                        .map(f -> f.substring(0, f.length() - 4))
                        .filter(n -> BrandingLayout.isValidName(n) && !BrandingLayout.isBuiltIn(n))
                        .forEach(n -> findLayoutInDir(dir, n).ifPresent(l -> custom.put(n, l)));
            } catch (IOException e) {
                LOG.warnf(e, "Could not list layouts in %s", dir);
            }
        }
        realm.getAttributes().forEach((key, value) -> {
            if (key.startsWith(LAYOUT_ATTRIBUTE_PREFIX)) {
                String n = key.substring(LAYOUT_ATTRIBUTE_PREFIX.length());
                custom.put(n, BrandingLayout.custom(n, value, BrandingLayout.Source.REALM));
            }
        });
        List<BrandingLayout> all = new ArrayList<>();
        BrandingLayout.BUILT_IN.forEach(n -> all.add(BrandingLayout.builtIn(n)));
        all.addAll(custom.values());
        return all;
    }

    private Optional<BrandingLayout> findLayoutInDir(Path dir, String name) {
        try {
            String css = Files.readString(dir.resolve(name + ".css"), StandardCharsets.UTF_8);
            return BrandingLayout.isValidCss(css)
                    ? Optional.of(BrandingLayout.custom(name, css, BrandingLayout.Source.FILE))
                    : Optional.empty();
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    private List<Path> layoutDirs(RealmModel realm) {
        return List.of(assetsDir.resolve(realm.getName()).resolve(LAYOUTS_DIR), assetsDir.resolve(LAYOUTS_DIR));
    }

    @Override
    public void saveRealmLayout(RealmModel realm, String name, String css) {
        if (!BrandingLayout.isValidName(name) || BrandingLayout.isBuiltIn(name)) {
            throw new IllegalArgumentException("Invalid layout name");
        }
        if (!BrandingLayout.isValidCss(css)) {
            throw new IllegalArgumentException("Layout CSS must be non-empty and at most "
                    + BrandingLayout.MAX_CSS_BYTES / 1024 + " KiB");
        }
        realm.setAttribute(LAYOUT_ATTRIBUTE_PREFIX + name, css);
    }

    @Override
    public boolean deleteRealmLayout(RealmModel realm, String name) {
        String key = LAYOUT_ATTRIBUTE_PREFIX + name;
        if (realm.getAttribute(key) == null) {
            return false;
        }
        realm.removeAttribute(key);
        return true;
    }

    @Override
    public BrandingLayout resolveLayout(RealmModel realm, BrandingConfig config) {
        String name = config.get(BrandingField.LAYOUT);
        return findLayout(realm, name).orElseGet(() -> {
            LOG.warnf("Layout '%s' configured for realm %s was not found; using '%s'",
                    name, realm.getName(), BrandingLayout.DEFAULT_BASE);
            return BrandingLayout.builtIn(BrandingLayout.DEFAULT_BASE);
        });
    }

    @Override
    public int maxAssetBytes() {
        return maxAssetBytes;
    }

    @Override
    public String resolveUrl(RealmModel realm, String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        if (BrandingValidator.isExternalUrl(value)) {
            return value;
        }
        return endpointUrl(realm) + "/assets/" + value;
    }

    @Override
    public String endpointUrl(RealmModel realm) {
        UriBuilder base;
        try {
            base = session.getContext().getUri(UrlType.FRONTEND).getBaseUriBuilder();
        } catch (RuntimeException e) {
            // Outside an HTTP request (e.g. emails sent by background tasks): fall back to a relative URL.
            return "/realms/" + realm.getName() + "/branding";
        }
        return base.path("realms").path(realm.getName()).path("branding").build().toString();
    }

    public static String contentType(String name) {
        int dot = name.lastIndexOf('.');
        String ext = dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
        return CONTENT_TYPES.getOrDefault(ext, "application/octet-stream");
    }
}
