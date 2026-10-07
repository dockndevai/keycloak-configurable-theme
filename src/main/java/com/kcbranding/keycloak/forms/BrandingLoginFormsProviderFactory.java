package com.kcbranding.keycloak.forms;

import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.forms.login.freemarker.FreeMarkerLoginFormsProviderFactory;
import org.keycloak.models.KeycloakSession;

/** Replaces the default login forms provider (positive order) to expose {@code branding} to templates. */
public class BrandingLoginFormsProviderFactory extends FreeMarkerLoginFormsProviderFactory {

    public static final String ID = "configurable-branding";

    @Override
    public LoginFormsProvider create(KeycloakSession session) {
        return new BrandingLoginFormsProvider(session);
    }

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public int order() {
        return 100;
    }
}
