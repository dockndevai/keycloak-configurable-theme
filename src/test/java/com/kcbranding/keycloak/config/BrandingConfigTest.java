package com.kcbranding.keycloak.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class BrandingConfigTest {

    @Test
    void defaultsCoverEveryField() {
        BrandingConfig config = BrandingConfig.defaults();
        for (BrandingField field : BrandingField.values()) {
            assertEquals(field.defaultValue(), config.get(field), field.key());
        }
        assertTrue(config.is(BrandingField.ENABLED));
    }

    @Test
    void laterLayersWinAndBlankValuesInherit() {
        BrandingConfig config = BrandingConfig.layered(List.of(
                Map.of("primaryColor", "#111111", "layout", "minimal"),
                Map.of("primaryColor", "#222222"),
                Map.of("primaryColor", "", "footerText", "Realm footer")));

        assertEquals("#222222", config.get(BrandingField.PRIMARY_COLOR));
        assertEquals("minimal", config.get(BrandingField.LAYOUT));
        assertEquals("Realm footer", config.get(BrandingField.FOOTER_TEXT));
    }

    @Test
    void invalidValuesInALayerAreIgnoredButValidOnesApply() {
        BrandingConfig config = BrandingConfig.layered(List.of(
                Map.of("primaryColor", "red;}", "layout", "split-right", "nope", "x")));
        assertEquals(BrandingField.PRIMARY_COLOR.defaultValue(), config.get(BrandingField.PRIMARY_COLOR));
        assertEquals("split-right", config.get(BrandingField.LAYOUT));
    }

    @Test
    void derivesHoverColorAndFallbacks() {
        BrandingConfig config = BrandingConfig.layered(List.of(Map.of("primaryColor", "#ffffff")));
        assertEquals("#d1d1d1", config.primaryHoverColor());
        assertEquals("#ffffff", config.getOr(BrandingField.ACCENT_COLOR, BrandingField.PRIMARY_COLOR));

        BrandingConfig explicit = BrandingConfig.layered(List.of(Map.of("primaryColor", "rgb(1,2,3)")));
        assertEquals("rgb(1,2,3)", explicit.primaryHoverColor());
    }

    @Test
    void versionChangesWithContent() {
        String a = BrandingConfig.layered(List.of(Map.of("primaryColor", "#111111"))).version();
        String b = BrandingConfig.layered(List.of(Map.of("primaryColor", "#111112"))).version();
        String a2 = BrandingConfig.layered(List.of(Map.of("primaryColor", "#111111"))).version();
        assertNotEquals(a, b);
        assertEquals(a, a2);
        assertEquals(12, a.length());
    }

    @Test
    void booleansParse() {
        BrandingConfig config = BrandingConfig.layered(List.of(Map.of("applyToAdmin", "FALSE")));
        assertFalse(config.is(BrandingField.APPLY_TO_ADMIN));
        assertTrue(config.is(BrandingField.APPLY_TO_ACCOUNT));
    }
}
