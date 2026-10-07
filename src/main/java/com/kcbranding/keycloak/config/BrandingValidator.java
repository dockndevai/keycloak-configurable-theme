package com.kcbranding.keycloak.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Validates and normalises raw branding values. Everything that ends up inside generated CSS is
 * restricted to a safe grammar here, so the CSS generator can concatenate values without escaping.
 */
public final class BrandingValidator {

    private static final Pattern HEX_COLOR = Pattern.compile("^#(?:[0-9a-fA-F]{3,4}|[0-9a-fA-F]{6}|[0-9a-fA-F]{8})$");
    private static final Pattern FUNC_COLOR = Pattern.compile("^(?:rgb|rgba|hsl|hsla)\\([0-9.,%\\s/deg]{1,60}\\)$");
    private static final Pattern NAMED_COLOR = Pattern.compile("^(?:transparent|white|black|currentColor)$");
    private static final Pattern SIZE = Pattern.compile("^(?:0|\\d{1,4}(?:\\.\\d{1,3})?(?:px|rem|em|%|vh|vw|ch))$");
    private static final Pattern FONT = Pattern.compile("^[A-Za-z0-9 ,'\"_-]{1,200}$");
    private static final Pattern ABSOLUTE_URL = Pattern.compile("^https?://[^\\s\"'\\\\()<>]{1,2000}$");
    private static final Pattern ROOT_RELATIVE_URL = Pattern.compile("^/(?!/)[^\\s\"'\\\\()<>]{0,2000}$");
    private static final Pattern DATA_URI = Pattern.compile(
            "^data:image/(?:png|jpeg|gif|webp|svg\\+xml|x-icon|vnd\\.microsoft\\.icon);base64,[A-Za-z0-9+/=]{1,700000}$");
    public static final Pattern ASSET_NAME = Pattern.compile(
            "^[A-Za-z0-9][A-Za-z0-9._-]{0,99}\\.(?:png|jpe?g|gif|webp|svg|ico|woff2?|ttf|otf)$", Pattern.CASE_INSENSITIVE);

    static final int MAX_TEXT = 1000;
    static final int MAX_CSS = 50_000;

    private BrandingValidator() {
    }

    /** Result of validating a map of raw values: cleaned values plus human readable errors. */
    public record Result(Map<String, String> values, List<String> errors) {
        public boolean isValid() {
            return errors.isEmpty();
        }
    }

    /**
     * Validates raw values. Unknown keys are reported as errors; blank values are kept as "" which
     * means "inherit from the lower configuration layer".
     */
    public static Result validate(Map<String, ?> raw) {
        Map<String, String> clean = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        if (raw == null) {
            return new Result(clean, errors);
        }
        raw.forEach((key, value) -> {
            var field = BrandingField.byKey(key);
            if (field.isEmpty()) {
                errors.add("Unknown branding setting '" + key + "'");
                return;
            }
            String text = value == null ? "" : String.valueOf(value).strip();
            String error = text.isEmpty() ? null : check(field.get(), text);
            if (error != null) {
                errors.add(key + ": " + error);
            } else {
                clean.put(key, normalise(field.get(), text));
            }
        });
        return new Result(clean, errors);
    }

    /** Returns an error message, or null when the value is acceptable for the field. */
    static String check(BrandingField field, String value) {
        return switch (field.type()) {
            case BOOLEAN -> value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false")
                    ? null : "must be true or false";
            case ENUM -> field.options().contains(value.toLowerCase(Locale.ROOT))
                    ? null : "must be one of " + field.options();
            case COLOR -> isColor(value) ? null : "must be a hex, rgb(a) or hsl(a) color";
            case SIZE -> SIZE.matcher(value).matches() ? null : "must be a CSS length such as 8px or 2rem";
            case FONT -> FONT.matcher(value).matches() ? null : "contains characters not allowed in a font list";
            case URL -> isUrlOrAsset(value) ? null
                    : "must be an http(s) URL, a root-relative path, an image data URI or an asset file name";
            case TEXT -> value.length() <= MAX_TEXT ? null : "must be at most " + MAX_TEXT + " characters";
            case LINKS -> checkLinks(value);
            case CSS -> value.length() <= MAX_CSS ? null : "must be at most " + MAX_CSS + " characters";
        };
    }

    private static String normalise(BrandingField field, String value) {
        return switch (field.type()) {
            case BOOLEAN, ENUM -> value.toLowerCase(Locale.ROOT);
            default -> value;
        };
    }

    public static boolean isColor(String value) {
        return HEX_COLOR.matcher(value).matches() || FUNC_COLOR.matcher(value).matches()
                || NAMED_COLOR.matcher(value).matches();
    }

    public static boolean isExternalUrl(String value) {
        return ABSOLUTE_URL.matcher(value).matches() || ROOT_RELATIVE_URL.matcher(value).matches()
                || DATA_URI.matcher(value).matches();
    }

    public static boolean isUrlOrAsset(String value) {
        return isExternalUrl(value) || ASSET_NAME.matcher(value).matches();
    }

    private static String checkLinks(String value) {
        if (value.length() > MAX_TEXT) {
            return "must be at most " + MAX_TEXT + " characters";
        }
        for (FooterLink link : FooterLink.parseLenient(value)) {
            if (link.getLabel().isEmpty()) {
                return "every link needs a label (Label|https://url)";
            }
            if (!ABSOLUTE_URL.matcher(link.getUrl()).matches() && !ROOT_RELATIVE_URL.matcher(link.getUrl()).matches()
                    && !link.getUrl().startsWith("mailto:")) {
                return "link '" + link.getLabel() + "' must use an http(s), mailto: or root-relative URL";
            }
        }
        return null;
    }
}
