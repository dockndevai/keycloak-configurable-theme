package com.kcbranding.keycloak.theme;

import java.util.Set;

import com.kcbranding.keycloak.config.BrandingConfig;
import com.kcbranding.keycloak.config.BrandingField;
import com.kcbranding.keycloak.spi.BrandingProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.theme.DefaultThemeSelectorProvider;
import org.keycloak.theme.Theme;
import org.keycloak.theme.ThemeSelectorProvider;

/**
 * Returns the configurable theme for every realm, unless the realm's branding disables it (globally or
 * per UI), in which case Keycloak's normal selection applies.
 */
public class ConfigurableThemeSelectorProvider implements ThemeSelectorProvider {

    private final KeycloakSession session;
    private final String themeName;
    private final Set<Theme.Type> types;
    private final ThemeSelectorProvider fallback;

    public ConfigurableThemeSelectorProvider(KeycloakSession session, String themeName, Set<Theme.Type> types) {
        this.session = session;
        this.themeName = themeName;
        this.types = types;
        this.fallback = new DefaultThemeSelectorProvider(session);
    }

    @Override
    public String getThemeName(Theme.Type type) {
        return applies(type) ? themeName : fallback.getThemeName(type);
    }

    private boolean applies(Theme.Type type) {
        if (!types.contains(type)) {
            return false;
        }
        RealmModel realm = session.getContext().getRealm();
        BrandingProvider branding = session.getProvider(BrandingProvider.class);
        if (realm == null || branding == null) {
            return true;
        }
        BrandingConfig config = branding.getConfig(realm);
        if (!config.is(BrandingField.ENABLED)) {
            return false;
        }
        return switch (type) {
            case ACCOUNT -> config.is(BrandingField.APPLY_TO_ACCOUNT);
            case ADMIN -> config.is(BrandingField.APPLY_TO_ADMIN);
            case EMAIL -> config.is(BrandingField.APPLY_TO_EMAIL);
            default -> true;
        };
    }

    @Override
    public void close() {
    }
}
