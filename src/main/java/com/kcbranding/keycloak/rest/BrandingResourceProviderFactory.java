package com.kcbranding.keycloak.rest;

import org.keycloak.Config;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.services.resource.RealmResourceProvider;
import org.keycloak.services.resource.RealmResourceProviderFactory;

/** Mounts {@link BrandingResource} at {@code /realms/{realm}/branding}. */
public class BrandingResourceProviderFactory implements RealmResourceProviderFactory {

    public static final String ID = "branding";

    @Override
    public RealmResourceProvider create(KeycloakSession session) {
        return new RealmResourceProvider() {
            @Override
            public Object getResource() {
                return new BrandingResource(session);
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
