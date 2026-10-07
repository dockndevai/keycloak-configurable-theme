package com.kcbranding.keycloak.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BrandingLayoutTest {

    @Test
    void customLayoutReadsItsBaseFromTheExtendsComment() {
        BrandingLayout layout = BrandingLayout.custom("banner-top",
                "/* extends: split-left */\nbody.cfg-layout-banner-top {}", BrandingLayout.Source.FILE);
        assertEquals("split-left", layout.getBase());
        assertTrue(layout.isCustom());
    }

    @Test
    void missingOrUnknownBaseFallsBackToCentered() {
        assertEquals("centered", BrandingLayout.custom("a", "body {}", BrandingLayout.Source.REALM).getBase());
        assertEquals("centered", BrandingLayout.custom("b", "/* extends: nope */ body {}", BrandingLayout.Source.REALM).getBase());
        assertEquals("centered", BrandingLayout.custom("c", "/* extends: other-custom */", BrandingLayout.Source.REALM).getBase(),
                "custom layouts can only extend built-in ones");
    }

    @Test
    void extendsMustBeTheFirstComment() {
        BrandingLayout layout = BrandingLayout.custom("x", "body {}\n/* extends: minimal */", BrandingLayout.Source.FILE);
        assertEquals("centered", layout.getBase());
    }

    @Test
    void builtInLayoutsAreTheirOwnBase() {
        BrandingLayout layout = BrandingLayout.builtIn("split-right");
        assertEquals("split-right", layout.getBase());
        assertFalse(layout.isCustom());
        assertTrue(BrandingLayout.isBuiltIn("minimal"));
        assertFalse(BrandingLayout.isBuiltIn("card-left"));
    }

    @Test
    void validatesNamesAndCss() {
        assertTrue(BrandingLayout.isValidName("card-left-2"));
        assertFalse(BrandingLayout.isValidName("Card"));
        assertFalse(BrandingLayout.isValidName("../x"));
        assertFalse(BrandingLayout.isValidName("-x"));
        assertTrue(BrandingLayout.isValidCss("body {}"));
        assertFalse(BrandingLayout.isValidCss("  "));
        assertFalse(BrandingLayout.isValidCss("x".repeat(BrandingLayout.MAX_CSS_BYTES + 1)));
    }

    @Test
    void versionTracksContent() {
        String a = BrandingLayout.custom("a", "body{color:red}", BrandingLayout.Source.FILE).version();
        String b = BrandingLayout.custom("a", "body{color:blue}", BrandingLayout.Source.FILE).version();
        assertFalse(a.equals(b));
    }
}
