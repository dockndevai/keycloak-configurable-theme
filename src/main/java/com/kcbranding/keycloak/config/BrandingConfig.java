package com.kcbranding.keycloak.config;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Effective (fully resolved) branding for one realm. Built by layering partial maps on top of the
 * built-in defaults; later layers win, blank values inherit from the layer below.
 */
public final class BrandingConfig {

    private final Map<String, String> values;
    private final String version;

    private BrandingConfig(Map<String, String> values) {
        this.values = Collections.unmodifiableMap(values);
        this.version = hash(values);
    }

    public static BrandingConfig defaults() {
        return layered(List.of());
    }

    /**
     * Builds the effective configuration. Each layer is validated independently; invalid values are
     * dropped so that one bad entry (e.g. in a hand-edited file) cannot break the login page.
     */
    public static BrandingConfig layered(List<? extends Map<String, ?>> layers) {
        Map<String, String> merged = new LinkedHashMap<>();
        for (BrandingField field : BrandingField.values()) {
            merged.put(field.key(), field.defaultValue());
        }
        for (Map<String, ?> layer : layers) {
            BrandingValidator.validate(layer).values().forEach((key, value) -> {
                if (!value.isEmpty()) {
                    merged.put(key, value);
                }
            });
        }
        return new BrandingConfig(merged);
    }

    public String get(BrandingField field) {
        return values.get(field.key());
    }

    public boolean is(BrandingField field) {
        return Boolean.parseBoolean(get(field));
    }

    public boolean has(BrandingField field) {
        String value = get(field);
        return value != null && !value.isEmpty();
    }

    /** Returns the field value, or the value of {@code fallback} when the field is blank. */
    public String getOr(BrandingField field, BrandingField fallback) {
        return has(field) ? get(field) : get(fallback);
    }

    public String primaryHoverColor() {
        return has(BrandingField.PRIMARY_HOVER_COLOR)
                ? get(BrandingField.PRIMARY_HOVER_COLOR)
                : ColorUtil.darken(get(BrandingField.PRIMARY_COLOR), 0.18);
    }

    public List<FooterLink> footerLinks() {
        return FooterLink.parseLenient(get(BrandingField.FOOTER_LINKS));
    }

    /** All resolved values keyed by setting name. */
    public Map<String, String> asMap() {
        return values;
    }

    /** Short content hash, used for cache busting and ETags. */
    public String version() {
        return version;
    }

    private static String hash(Map<String, String> values) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            values.forEach((k, v) -> {
                digest.update(k.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 0);
                digest.update(v.getBytes(StandardCharsets.UTF_8));
                digest.update((byte) 1);
            });
            return HexFormat.of().formatHex(digest.digest(), 0, 6);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
