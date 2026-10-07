package com.kcbranding.keycloak.spi;

/** A binary branding asset (logo, favicon, background, font) and where it came from. */
public record BrandingAsset(String name, String contentType, byte[] content, Source source) {

    public enum Source { REALM, FILE }
}
