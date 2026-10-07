package com.kcbranding.keycloak.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BrandingFileSourceTest {

    @TempDir
    Path dir;

    @Test
    void returnsDefaultsAndRealmLayers() throws Exception {
        Path file = dir.resolve("branding.json");
        Files.writeString(file, """
                {"defaults": {"primaryColor": "#123456"},
                 "realms": {"acme": {"layout": "split-left"}}}
                """);
        BrandingFileSource source = new BrandingFileSource(file, 0);

        List<Map<String, Object>> acme = source.layersFor("acme");
        assertEquals("#123456", acme.get(0).get("primaryColor"));
        assertEquals("split-left", acme.get(1).get("layout"));
        assertTrue(source.layersFor("other").get(1).isEmpty());
    }

    @Test
    void reloadsChangedFileAndKeepsLastGoodVersionOnErrors() throws Exception {
        Path file = dir.resolve("branding.json");
        Files.writeString(file, "{\"defaults\": {\"primaryColor\": \"#111111\"}}");
        Files.setLastModifiedTime(file, FileTime.fromMillis(1_000_000));
        BrandingFileSource source = new BrandingFileSource(file, 0);
        assertEquals("#111111", source.layersFor("x").get(0).get("primaryColor"));

        Files.writeString(file, "{\"defaults\": {\"primaryColor\": \"#222222\"}}");
        Files.setLastModifiedTime(file, FileTime.fromMillis(2_000_000));
        assertEquals("#222222", source.layersFor("x").get(0).get("primaryColor"));

        Files.writeString(file, "{ not json");
        Files.setLastModifiedTime(file, FileTime.fromMillis(3_000_000));
        assertEquals("#222222", source.layersFor("x").get(0).get("primaryColor"));
    }

    @Test
    void missingFileMeansNoLayers() {
        BrandingFileSource source = new BrandingFileSource(dir.resolve("absent.json"), 0);
        assertTrue(source.layersFor("x").get(0).isEmpty());
        assertTrue(source.layersFor("x").get(1).isEmpty());
    }
}
