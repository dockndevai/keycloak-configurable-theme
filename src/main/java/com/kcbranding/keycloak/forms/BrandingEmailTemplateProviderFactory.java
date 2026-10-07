package com.kcbranding.keycloak.forms;

import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.email.freemarker.FreeMarkerEmailTemplateProviderFactory;
import org.keycloak.models.KeycloakSession;

/** Replaces the default email template provider (positive order) to expose {@code branding} to emails. */
public class BrandingEmailTemplateProviderFactory extends FreeMarkerEmailTemplateProviderFactory {

    public static final String ID = "configurable-branding";

    @Override
    public EmailTemplateProvider create(KeycloakSession session) {
        return new BrandingEmailTemplateProvider(session);
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
