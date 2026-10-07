package com.kcbranding.keycloak.spi;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.kcbranding.keycloak.config.BrandingFileSource;
import org.jboss.logging.Logger;
import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;

/**
 * Options (CLI {@code --spi-branding--default--<name>}, env {@code KC_SPI_BRANDING__DEFAULT__<NAME>}):
 * <ul>
 *   <li>{@code config-file} - global JSON file, default {@code <kc.home>/conf/branding.json}</li>
 *   <li>{@code assets-dir} - directory with shared assets, default {@code <kc.home>/branding}</li>
 *   <li>{@code reload-interval} - seconds between checks of the config file, default 10</li>
 *   <li>{@code max-asset-size} - maximum upload size in KiB, default 512</li>
 * </ul>
 */
public class DefaultBrandingProviderFactory implements BrandingProviderFactory {

    private static final Logger LOG = Logger.getLogger(DefaultBrandingProviderFactory.class);

    public static final String ID = "default";

    private BrandingFileSource fileSource;
    private Path assetsDir;
    private int maxAssetBytes;
    /** Parsed realm overrides keyed by realm id, invalidated by comparing the raw attribute value. */
    private final Map<String, DefaultBrandingProvider.ParsedOverrides> overrideCache = new ConcurrentHashMap<>();

    @Override
    public BrandingProvider create(KeycloakSession session) {
        return new DefaultBrandingProvider(session, fileSource, assetsDir, maxAssetBytes, overrideCache);
    }

    @Override
    public void init(Config.Scope config) {
        String home = System.getProperty("kc.home.dir", ".");
        Path file = Path.of(config.get("config-file", Path.of(home, "conf", "branding.json").toString()));
        long reloadSeconds = config.getLong("reload-interval", 10L);
        fileSource = new BrandingFileSource(file, Math.max(0, reloadSeconds) * 1000);
        assetsDir = Path.of(config.get("assets-dir", Path.of(home, "branding").toString())).toAbsolutePath().normalize();
        maxAssetBytes = config.getInt("max-asset-size", 512) * 1024;
        LOG.infof("Configurable theme branding: config file %s, assets dir %s", file, assetsDir);
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property().name("config-file").type("string")
                .helpText("Global branding JSON file").add()
                .property().name("assets-dir").type("string")
                .helpText("Directory with shared branding assets").add()
                .property().name("reload-interval").type("int").defaultValue(10)
                .helpText("Seconds between checks for changes to the config file").add()
                .property().name("max-asset-size").type("int").defaultValue(512)
                .helpText("Maximum uploaded asset size in KiB").add()
                .build();
    }
}
