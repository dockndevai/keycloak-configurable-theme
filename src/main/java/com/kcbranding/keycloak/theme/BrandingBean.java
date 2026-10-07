package com.kcbranding.keycloak.theme;

import java.util.List;

import com.kcbranding.keycloak.config.BrandingConfig;
import com.kcbranding.keycloak.config.BrandingField;
import com.kcbranding.keycloak.config.FooterLink;
import com.kcbranding.keycloak.spi.BrandingProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

/**
 * Exposed to FreeMarker templates as {@code branding}. URLs are already resolved; text values are
 * escaped by FreeMarker's HTML output format.
 */
public class BrandingBean {

    public static final String ATTRIBUTE = "branding";

    private final BrandingConfig config;
    private final BrandingProvider provider;
    private final RealmModel realm;

    public BrandingBean(BrandingConfig config, BrandingProvider provider, RealmModel realm) {
        this.config = config;
        this.provider = provider;
        this.realm = realm;
    }

    /** Builds the bean, or returns null when branding is unavailable or disabled for the realm. */
    public static BrandingBean create(KeycloakSession session, RealmModel realm) {
        if (realm == null) {
            return null;
        }
        BrandingProvider provider = session.getProvider(BrandingProvider.class);
        if (provider == null) {
            return null;
        }
        BrandingConfig config = provider.getConfig(realm);
        return config.is(BrandingField.ENABLED) ? new BrandingBean(config, provider, realm) : null;
    }

    private String text(BrandingField field) {
        return config.get(field);
    }

    private String url(BrandingField field) {
        return provider.resolveUrl(realm, config.get(field));
    }

    public String getVersion() {
        return config.version();
    }

    public String getCssUrl() {
        return provider.endpointUrl(realm) + "/theme.css?scope=login&v=" + config.version();
    }

    public String getLayout() {
        return text(BrandingField.LAYOUT);
    }

    public boolean isSplitLayout() {
        return getLayout().startsWith("split");
    }

    public String getColorScheme() {
        return text(BrandingField.COLOR_SCHEME);
    }

    public String getLogoUrl() {
        return url(BrandingField.LOGO_URL);
    }

    public String getFaviconUrl() {
        return url(BrandingField.FAVICON_URL);
    }

    public String getFaviconType() {
        String favicon = text(BrandingField.FAVICON_URL);
        if (favicon.endsWith(".svg") || favicon.startsWith("data:image/svg")) {
            return "image/svg+xml";
        }
        if (favicon.endsWith(".png") || favicon.startsWith("data:image/png")) {
            return "image/png";
        }
        return "image/x-icon";
    }

    public String getFontCssUrl() {
        return url(BrandingField.FONT_CSS_URL);
    }

    public String getPageTitle() {
        return text(BrandingField.PAGE_TITLE);
    }

    public String getHeaderText() {
        return text(BrandingField.HEADER_TEXT);
    }

    public boolean isShowRealmName() {
        return config.is(BrandingField.SHOW_REALM_NAME);
    }

    public String getHeroTitle() {
        return text(BrandingField.HERO_TITLE);
    }

    public String getHeroText() {
        return text(BrandingField.HERO_TEXT);
    }

    public String getInfoBanner() {
        return text(BrandingField.INFO_BANNER);
    }

    public String getInfoBannerType() {
        return text(BrandingField.INFO_BANNER_TYPE);
    }

    public String getFooterText() {
        return text(BrandingField.FOOTER_TEXT);
    }

    public List<FooterLink> getFooterLinks() {
        return config.footerLinks();
    }

    // Values used by email templates, which need inline styles.
    public String getPrimaryColor() {
        return text(BrandingField.PRIMARY_COLOR);
    }

    public String getBackgroundColor() {
        return text(BrandingField.BACKGROUND_COLOR);
    }

    public String getCardBackgroundColor() {
        return text(BrandingField.CARD_BACKGROUND_COLOR);
    }

    public String getTextColor() {
        return text(BrandingField.TEXT_COLOR);
    }

    public String getLogoHeight() {
        return text(BrandingField.LOGO_HEIGHT);
    }

    public String getFontFamily() {
        return config.has(BrandingField.FONT_FAMILY) ? text(BrandingField.FONT_FAMILY) : "Arial, Helvetica, sans-serif";
    }
}
