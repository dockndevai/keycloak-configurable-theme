package com.kcbranding.keycloak.rest;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProvider;
import org.keycloak.services.resources.admin.ext.AdminRealmResourceProviderFactory;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

/** Mounts {@link BrandingAdminResource} at {@code /admin/realms/{realm}/branding}. */
public class BrandingAdminResourceProviderFactory implements AdminRealmResourceProviderFactory {

    public static final String ID = "branding";

    @Override
    public AdminRealmResourceProvider create(KeycloakSession session) {
        return new AdminRealmResourceProvider() {
            @Override
            public Object getResource(KeycloakSession s, RealmModel realm, AdminPermissionEvaluator auth,
                                      AdminEventBuilder adminEvent) {
                return new BrandingAdminResource(s, realm, auth, adminEvent);
            }

            @Override
            public void close() {
            }
        };
    }

    @Override
    public void init(Config.Scope config) {
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
}
