package com.kcbranding.keycloak.config;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Every configurable branding setting. This enum is the single source of truth for keys, defaults,
 * validation and the admin UI form, so adding a setting means adding a constant here.
 */
public enum BrandingField {

    // --- General -------------------------------------------------------------------------------
    ENABLED("enabled", Type.BOOLEAN, "true", "Enabled",
            "Apply the configurable theme to this realm. When off, the realm's own theme settings are used."),
    LAYOUT("layout", Type.ENUM, "centered", "Login layout",
            "Arrangement of the login pages.", "centered", "split-left", "split-right", "minimal"),
    COLOR_SCHEME("colorScheme", Type.ENUM, "auto", "Color scheme",
            "auto follows the browser preference; light/dark force one scheme.", "auto", "light", "dark"),

    // --- Colours -------------------------------------------------------------------------------
    PRIMARY_COLOR("primaryColor", Type.COLOR, "#0066cc", "Primary color", "Buttons and highlights."),
    PRIMARY_HOVER_COLOR("primaryHoverColor", Type.COLOR, "", "Primary hover color",
            "Leave empty to derive it from the primary color."),
    ACCENT_COLOR("accentColor", Type.COLOR, "", "Accent color",
            "Top border of the login card. Defaults to the primary color."),
    LINK_COLOR("linkColor", Type.COLOR, "", "Link color", "Defaults to the primary color."),
    BACKGROUND_COLOR("backgroundColor", Type.COLOR, "#eef1f6", "Page background color", null),
    BACKGROUND_IMAGE_URL("backgroundImageUrl", Type.URL, "", "Page background image",
            "URL, data URI or uploaded asset name."),
    BACKGROUND_OVERLAY("backgroundOverlay", Type.COLOR, "", "Background overlay",
            "Translucent color laid over the background image, e.g. rgba(0,0,0,0.4)."),
    CARD_BACKGROUND_COLOR("cardBackgroundColor", Type.COLOR, "#ffffff", "Card background color",
            "Login card background in light mode."),
    TEXT_COLOR("textColor", Type.COLOR, "#151515", "Text color", "Body text in light mode."),
    HEADER_TEXT_COLOR("headerTextColor", Type.COLOR, "#1f2937", "Header text color",
            "Realm name shown above the card."),

    // --- Typography & shape ---------------------------------------------------------------------
    FONT_FAMILY("fontFamily", Type.FONT, "", "Font family", "CSS font-family list, e.g. 'Inter', sans-serif."),
    FONT_CSS_URL("fontCssUrl", Type.URL, "", "Font stylesheet URL",
            "Stylesheet that defines the font, e.g. a Google Fonts URL."),
    BORDER_RADIUS("borderRadius", Type.SIZE, "8px", "Corner radius", "Buttons, inputs and card."),
    CARD_WIDTH("cardWidth", Type.SIZE, "34rem", "Card width", "Maximum width of the login card."),

    // --- Assets ---------------------------------------------------------------------------------
    LOGO_URL("logoUrl", Type.URL, "", "Logo", "URL, data URI or uploaded asset name."),
    LOGO_DARK_URL("logoDarkUrl", Type.URL, "", "Logo for dark backgrounds",
            "Used in dark mode, on the split-layout side panel and in the console header. Defaults to the logo."),
    LOGO_HEIGHT("logoHeight", Type.SIZE, "56px", "Logo height", null),
    FAVICON_URL("faviconUrl", Type.URL, "", "Favicon", "URL, data URI or uploaded asset name."),

    // --- Texts ----------------------------------------------------------------------------------
    PAGE_TITLE("pageTitle", Type.TEXT, "", "Browser title", "Replaces the browser tab title."),
    HEADER_TEXT("headerText", Type.TEXT, "", "Header text", "Replaces the realm display name above the card."),
    SHOW_REALM_NAME("showRealmName", Type.BOOLEAN, "true", "Show realm name",
            "Show the header text next to / below the logo."),
    HERO_TITLE("heroTitle", Type.TEXT, "", "Hero title", "Headline of the side panel in split layouts."),
    HERO_TEXT("heroText", Type.TEXT, "", "Hero text", "Paragraph of the side panel in split layouts."),
    HERO_BACKGROUND_COLOR("heroBackgroundColor", Type.COLOR, "", "Hero background color",
            "Side panel color in split layouts. Defaults to the primary color."),
    INFO_BANNER("infoBanner", Type.TEXT, "", "Announcement banner", "Message shown at the top of every login page."),
    INFO_BANNER_TYPE("infoBannerType", Type.ENUM, "info", "Banner type", null,
            "info", "success", "warning", "danger"),
    FOOTER_TEXT("footerText", Type.TEXT, "", "Footer text", "Shown below the login card and in emails."),
    FOOTER_LINKS("footerLinks", Type.LINKS, "", "Footer links",
            "Entries as Label|https://url separated by ';' or new lines."),

    // --- Consoles & email -------------------------------------------------------------------------
    APPLY_TO_ACCOUNT("applyToAccount", Type.BOOLEAN, "true", "Brand the account console", null),
    APPLY_TO_ADMIN("applyToAdmin", Type.BOOLEAN, "true", "Brand the admin console",
            "Takes effect on the realm that hosts the admin console (normally master)."),
    APPLY_TO_EMAIL("applyToEmail", Type.BOOLEAN, "true", "Brand emails", null),
    CONSOLE_HEADER_COLOR("consoleHeaderColor", Type.COLOR, "", "Console header color",
            "Masthead color of the account and admin consoles."),

    // --- Advanced -------------------------------------------------------------------------------
    CUSTOM_CSS("customCss", Type.CSS, "", "Custom CSS", "Appended to the generated stylesheet for every page.");

    public enum Type { BOOLEAN, ENUM, COLOR, URL, SIZE, FONT, TEXT, LINKS, CSS }

    private static final Map<String, BrandingField> BY_KEY = Arrays.stream(values())
            .collect(Collectors.toUnmodifiableMap(BrandingField::key, Function.identity()));

    private final String key;
    private final Type type;
    private final String defaultValue;
    private final String label;
    private final String help;
    private final List<String> options;

    BrandingField(String key, Type type, String defaultValue, String label, String help, String... options) {
        this.key = key;
        this.type = type;
        this.defaultValue = defaultValue;
        this.label = label;
        this.help = help;
        this.options = List.of(options);
    }

    public String key() {
        return key;
    }

    public Type type() {
        return type;
    }

    public String defaultValue() {
        return defaultValue;
    }

    public String label() {
        return label;
    }

    public String help() {
        return help;
    }

    public List<String> options() {
        return options;
    }

    public static Optional<BrandingField> byKey(String key) {
        return Optional.ofNullable(BY_KEY.get(key));
    }
}
