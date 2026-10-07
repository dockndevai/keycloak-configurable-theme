package com.kcbranding.keycloak.spi;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.kcbranding.keycloak.config.BrandingConfig;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;

/**
 * Resolves branding for a realm and manages the realm-level overrides and uploaded assets.
 *
 * <p>Precedence, lowest first: built-in defaults, global file {@code defaults}, global file
 * {@code realms.<name>}, realm overrides (admin REST API / admin console tab).
 */
public interface BrandingProvider extends Provider {

    /** Effective configuration for the realm. */
    BrandingConfig getConfig(RealmModel realm);

    /** Realm-level overrides only (what an admin changed for this realm). */
    Map<String, String> getRealmOverrides(RealmModel realm);

    /** Replaces the realm-level overrides. Values must already be validated. */
    void setRealmOverrides(RealmModel realm, Map<String, String> overrides);

    Optional<BrandingAsset> findAsset(RealmModel realm, String name);

    /** Names of assets uploaded to the realm (file-system assets are not listed). */
    List<String> listRealmAssets(RealmModel realm);

    void saveRealmAsset(RealmModel realm, String name, byte[] content);

    boolean deleteRealmAsset(RealmModel realm, String name);

    /** Maximum accepted upload size in bytes. */
    int maxAssetBytes();

    /**
     * Turns a URL/asset setting into a browser-usable URL: http(s), root-relative and data URIs are
     * returned as-is, anything else is treated as an asset name served by the branding endpoint.
     */
    String resolveUrl(RealmModel realm, String value);

    /** Absolute URL of the realm's public branding endpoint, without trailing slash. */
    String endpointUrl(RealmModel realm);

    @Override
    default void close() {
    }
}
