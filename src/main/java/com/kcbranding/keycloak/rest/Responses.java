package com.kcbranding.keycloak.rest;

import java.util.Base64;

import com.kcbranding.keycloak.spi.BrandingAsset;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;

final class Responses {

    private Responses() {
    }

    static CacheControl cache(int maxAgeSeconds) {
        CacheControl cc = new CacheControl();
        cc.setMaxAge(maxAgeSeconds);
        cc.setNoTransform(false);
        return cc;
    }

    /** Serves an asset with conditional-request support and a locked-down CSP for SVGs. */
    static Response asset(Request request, BrandingAsset asset) {
        EntityTag etag = new EntityTag(Integer.toHexString(java.util.Arrays.hashCode(asset.content())));
        Response.ResponseBuilder notModified = request.evaluatePreconditions(etag);
        if (notModified != null) {
            return notModified.cacheControl(cache(300)).build();
        }
        return Response.ok(asset.content(), asset.contentType())
                .tag(etag)
                .cacheControl(cache(300))
                .header("X-Content-Type-Options", "nosniff")
                .header("Content-Security-Policy", "default-src 'none'; style-src 'unsafe-inline'; img-src data:; font-src data:")
                .build();
    }

    /** Decodes a validated {@code data:image/...;base64,} URI. */
    static Response dataUri(Request request, String name, String dataUri) {
        int comma = dataUri.indexOf(',');
        String contentType = dataUri.substring("data:".length(), dataUri.indexOf(';'));
        byte[] bytes = Base64.getDecoder().decode(dataUri.substring(comma + 1));
        return asset(request, new BrandingAsset(name, contentType, bytes, BrandingAsset.Source.REALM));
    }
}
