package com.kcbranding.keycloak.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BrandingValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"#fff", "#0066cc", "#0066ccff", "rgb(10, 20, 30)", "rgba(0,0,0,0.4)", "hsl(200deg 50% 40%)", "transparent"})
    void acceptsColors(String color) {
        assertTrue(BrandingValidator.validate(Map.of("primaryColor", color)).isValid(), color);
    }

    @ParameterizedTest
    @ValueSource(strings = {"red;}body{display:none", "#12", "url(x)", "rgb(1,2,3);color:red", "expression(alert(1))", "blue"})
    void rejectsColorsThatCouldBreakOutOfCss(String color) {
        assertFalse(BrandingValidator.validate(Map.of("primaryColor", color)).isValid(), color);
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://cdn.example.com/logo.svg", "/static/logo.png", "logo.svg", "acme-logo_v2.PNG",
            "data:image/png;base64,iVBORw0KGgo="})
    void acceptsUrlsAndAssetNames(String url) {
        assertTrue(BrandingValidator.validate(Map.of("logoUrl", url)).isValid(), url);
    }

    @ParameterizedTest
    @ValueSource(strings = {"javascript:alert(1)", "//evil.example/x.png", "https://x/a\")}", "../secret.png", "logo.html",
            "data:text/html;base64,PHNjcmlwdD4=", "https://x/a b.png"})
    void rejectsDangerousUrls(String url) {
        assertFalse(BrandingValidator.validate(Map.of("logoUrl", url)).isValid(), url);
    }

    @Test
    void validatesSizesFontsAndEnums() {
        assertTrue(BrandingValidator.validate(Map.of("borderRadius", "0", "cardWidth", "34.5rem")).isValid());
        assertFalse(BrandingValidator.validate(Map.of("borderRadius", "8px; color: red")).isValid());
        assertTrue(BrandingValidator.validate(Map.of("fontFamily", "'Inter', \"Open Sans\", sans-serif")).isValid());
        assertFalse(BrandingValidator.validate(Map.of("fontFamily", "Inter; } body { x")).isValid());
        assertFalse(BrandingValidator.validate(Map.of("layout", "sideways")).isValid());
    }

    @Test
    void normalisesBooleansAndEnumsAndAcceptsNonStringJsonValues() {
        var result = BrandingValidator.validate(Map.of("layout", "SPLIT-LEFT", "showRealmName", false));
        assertTrue(result.isValid());
        assertEquals("split-left", result.values().get("layout"));
        assertEquals("false", result.values().get("showRealmName"));
    }

    @Test
    void reportsUnknownKeys() {
        var result = BrandingValidator.validate(Map.of("primaryColour", "#fff"));
        assertEquals(1, result.errors().size());
        assertTrue(result.errors().get(0).contains("primaryColour"));
    }

    @Test
    void blankValuesAreKeptAsInherit() {
        var result = BrandingValidator.validate(Map.of("primaryColor", "  "));
        assertTrue(result.isValid());
        assertEquals("", result.values().get("primaryColor"));
    }

    @Test
    void validatesFooterLinks() {
        assertTrue(BrandingValidator.validate(Map.of("footerLinks",
                "Privacy|https://example.com/privacy; Help|mailto:help@example.com\nHome|/")).isValid());
        assertFalse(BrandingValidator.validate(Map.of("footerLinks", "Bad|javascript:alert(1)")).isValid());
        assertFalse(BrandingValidator.validate(Map.of("footerLinks", "|https://example.com")).isValid());
    }

    @Test
    void parsesFooterLinks() {
        var links = FooterLink.parseLenient("A|https://a.example;B|/b\n\n");
        assertEquals(2, links.size());
        assertEquals("A", links.get(0).getLabel());
        assertEquals("/b", links.get(1).getUrl());
    }
}
