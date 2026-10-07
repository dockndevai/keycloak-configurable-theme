package com.kcbranding.keycloak.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A login layout. Built-in layouts are pure CSS in the theme; custom layouts are CSS files that are
 * plugged in at runtime (assets directory or admin API) and build on a built-in "base" layout, whose
 * markup they reuse. The base is declared in the first comment of the file:
 *
 * <pre>
 * /* extends: split-left *&#47;
 * body.cfg-layout-my-layout .cfg-hero { ... }
 * </pre>
 */
public final class BrandingLayout {

    public enum Source { BUILT_IN, FILE, REALM }

    public static final List<String> BUILT_IN = BrandingField.LAYOUT.options();
    public static final String DEFAULT_BASE = "centered";
    public static final int MAX_CSS_BYTES = 100 * 1024;

    private static final Pattern EXTENDS = Pattern.compile("^\\s*/\\*\\s*extends\\s*:\\s*([a-z0-9-]+)\\s*\\*/");

    private final String name;
    private final String base;
    private final String css;
    private final Source source;

    private BrandingLayout(String name, String base, String css, Source source) {
        this.name = name;
        this.base = base;
        this.css = css;
        this.source = source;
    }

    public static boolean isBuiltIn(String name) {
        return BUILT_IN.contains(name);
    }

    public static BrandingLayout builtIn(String name) {
        return new BrandingLayout(name, name, "", Source.BUILT_IN);
    }

    /** Parses a custom layout; an unknown or missing {@code extends} falls back to {@code centered}. */
    public static BrandingLayout custom(String name, String css, Source source) {
        Matcher m = EXTENDS.matcher(css);
        String declared = m.find() ? m.group(1).toLowerCase(Locale.ROOT) : DEFAULT_BASE;
        return new BrandingLayout(name, isBuiltIn(declared) ? declared : DEFAULT_BASE, css, source);
    }

    public static boolean isValidName(String name) {
        return name != null && BrandingValidator.LAYOUT_NAME.matcher(name).matches();
    }

    public static boolean isValidCss(String css) {
        return css != null && !css.isBlank() && css.getBytes(StandardCharsets.UTF_8).length <= MAX_CSS_BYTES;
    }

    public String getName() {
        return name;
    }

    /** Built-in layout whose markup and CSS this layout builds on. */
    public String getBase() {
        return base;
    }

    public String getCss() {
        return css;
    }

    public Source getSource() {
        return source;
    }

    public boolean isCustom() {
        return source != Source.BUILT_IN;
    }

    /** Short content hash for cache busting. */
    public String version() {
        return Integer.toHexString(css.hashCode());
    }
}
