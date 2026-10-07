package com.kcbranding.keycloak.spi;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

/** Declares the {@code branding} SPI. Configure it with {@code --spi-branding--default--<option>}. */
public class BrandingSpi implements Spi {

    public static final String NAME = "branding";

    @Override
    public boolean isInternal() {
        return false;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return BrandingProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return BrandingProviderFactory.class;
    }
}
