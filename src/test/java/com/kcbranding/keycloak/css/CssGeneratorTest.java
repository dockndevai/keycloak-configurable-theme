package com.kcbranding.keycloak.css;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import com.kcbranding.keycloak.config.BrandingConfig;
import org.junit.jupiter.api.Test;

class CssGeneratorTest {

    private static String css(Map<String, String> values) {
        return CssGenerator.generate(BrandingConfig.layered(List.of(values)), CssGenerator.Scope.LOGIN,
                v -> v.startsWith("http") ? v : "https://kc.example/realms/r/branding/assets/" + v);
    }

    @Test
    void declaresBrandVariablesAndPatternFlyTokens() {
        String css = css(Map.of("primaryColor", "#0f766e", "borderRadius", "12px"));
        assertTrue(css.contains("--cfg-primary: #0f766e;"));
        assertTrue(css.contains("--cfg-radius: 12px;"));
        assertTrue(css.contains("--pf-v5-global--primary-color--100: var(--cfg-primary);"));
        assertTrue(css.contains("--cfg-accent: #0f766e;"), "accent defaults to primary");
    }

    @Test
    void resolvesAssetUrlsAndOmitsUnsetImages() {
        String withLogo = css(Map.of("logoUrl", "logo.svg"));
        assertTrue(withLogo.contains("--cfg-logo-url: url(\"https://kc.example/realms/r/branding/assets/logo.svg\");"));
        assertTrue(withLogo.contains("--cfg-console-logo-url: url(\"https://kc.example/realms/r/branding/assets/logo.svg\");"));
        assertFalse(withLogo.contains("--cfg-bg-image"));

        String withDark = css(Map.of("logoUrl", "logo.svg", "logoDarkUrl", "logo-dark.svg"));
        assertTrue(withDark.contains("--cfg-console-logo-url: url(\"https://kc.example/realms/r/branding/assets/logo-dark.svg\");"));
    }

    @Test
    void fontOverridesOnlyWhenConfigured() {
        assertFalse(css(Map.of()).contains("FontFamily"));
        assertTrue(css(Map.of("fontFamily", "'Inter', sans-serif"))
                .contains("--pf-v5-global--FontFamily--text: 'Inter', sans-serif;"));
    }

    @Test
    void appendsCustomCss() {
        String css = css(Map.of("customCss", ".pf-v5-c-login__main { box-shadow: none; }"));
        assertTrue(css.endsWith(".pf-v5-c-login__main { box-shadow: none; }\n"));
    }

    @Test
    void injectionAttemptsNeverReachTheStylesheet() {
        String css = css(Map.of("primaryColor", "red;}body{display:none", "logoUrl", "x\");}*{color:red"));
        assertFalse(css.contains("display:none"));
        assertFalse(css.contains("color:red"));
    }
}
