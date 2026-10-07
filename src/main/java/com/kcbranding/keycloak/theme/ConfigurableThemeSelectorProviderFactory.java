package com.kcbranding.keycloak.theme;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.provider.ProviderConfigurationBuilder;
import org.keycloak.theme.Theme;
import org.keycloak.theme.ThemeSelectorProvider;
import org.keycloak.theme.ThemeSelectorProviderFactory;

/**
 * Makes every realm use the configurable theme. Because {@link #order()} is positive, Keycloak picks
 * this factory as the default theme selector without extra configuration.
 *
 * <p>Options: {@code --spi-theme-selector--configurable--theme-name} (default {@code configurable})
 * and {@code --spi-theme-selector--configurable--types} (default {@code login,account,admin,email}).
 */
public class ConfigurableThemeSelectorProviderFactory implements ThemeSelectorProviderFactory {

    public static final String ID = "configurable";
    public static final String DEFAULT_THEME = "configurable";

    private String themeName = DEFAULT_THEME;
    private Set<Theme.Type> types = EnumSet.of(Theme.Type.LOGIN, Theme.Type.ACCOUNT, Theme.Type.ADMIN, Theme.Type.EMAIL);

    @Override
    public ThemeSelectorProvider create(KeycloakSession session) {
        return new ConfigurableThemeSelectorProvider(session, themeName, types);
    }

    @Override
    public void init(Config.Scope config) {
        themeName = config.get("theme-name", DEFAULT_THEME);
        String configuredTypes = config.get("types");
        if (configuredTypes != null && !configuredTypes.isBlank()) {
            types = Arrays.stream(configuredTypes.split(","))
                    .map(String::strip)
                    .filter(s -> !s.isEmpty())
                    .map(s -> Theme.Type.valueOf(s.toUpperCase(Locale.ROOT)))
                    .collect(Collectors.toCollection(() -> EnumSet.noneOf(Theme.Type.class)));
        }
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
    public int order() {
        return 100;
    }

    @Override
    public List<ProviderConfigProperty> getConfigMetadata() {
        return ProviderConfigurationBuilder.create()
                .property().name("theme-name").type("string").defaultValue(DEFAULT_THEME)
                .helpText("Theme enforced for all realms").add()
                .property().name("types").type("string").defaultValue("login,account,admin,email")
                .helpText("Theme types to enforce").add()
                .build();
    }
}
