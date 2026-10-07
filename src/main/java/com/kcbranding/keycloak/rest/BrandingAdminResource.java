package com.kcbranding.keycloak.rest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.kcbranding.keycloak.config.BrandingField;
import com.kcbranding.keycloak.config.BrandingLayout;
import com.kcbranding.keycloak.config.BrandingValidator;
import com.kcbranding.keycloak.spi.BrandingProvider;
import com.kcbranding.keycloak.ui.BrandingUiTabProviderFactory;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PATCH;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.services.resources.admin.AdminEventBuilder;
import org.keycloak.services.resources.admin.fgap.AdminPermissionEvaluator;

/**
 * Admin API under {@code /admin/realms/{realm}/branding}. Reads need view-realm, writes need
 * manage-realm, exactly like the built-in realm settings.
 */
public class BrandingAdminResource {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final AdminPermissionEvaluator auth;
    private final AdminEventBuilder adminEvent;
    private final BrandingProvider provider;

    public BrandingAdminResource(KeycloakSession session, RealmModel realm, AdminPermissionEvaluator auth,
                                 AdminEventBuilder adminEvent) {
        this.session = session;
        this.realm = realm;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.REALM);
        this.provider = session.getProvider(BrandingProvider.class);
    }

    /** Realm overrides and the effective result. */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> get() {
        auth.realm().requireViewRealm();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("realm", realm.getName());
        body.put("overrides", provider.getRealmOverrides(realm));
        body.put("effective", provider.getConfig(realm).asMap());
        body.put("version", provider.getConfig(realm).version());
        return body;
    }

    /** Field catalogue, useful for building forms or documentation. */
    @GET
    @Path("schema")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> schema() {
        auth.realm().requireViewRealm();
        return java.util.Arrays.stream(BrandingField.values()).map(f -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("key", f.key());
            m.put("type", f.type().name().toLowerCase(java.util.Locale.ROOT));
            m.put("default", f.defaultValue());
            m.put("label", f.label());
            if (f.help() != null) {
                m.put("help", f.help());
            }
            if (!f.options().isEmpty()) {
                m.put("options", f.options());
            }
            return m;
        }).toList();
    }

    /** Replaces all realm overrides. */
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> replace(Map<String, Object> body) {
        auth.realm().requireManageRealm();
        store(validated(body));
        return get();
    }

    /** Merges the given keys into the realm overrides; blank values remove a key. */
    @PATCH
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> patch(Map<String, Object> body) {
        auth.realm().requireManageRealm();
        Map<String, String> merged = new LinkedHashMap<>(provider.getRealmOverrides(realm));
        validated(body).forEach((k, v) -> {
            if (v.isEmpty()) {
                merged.remove(k);
            } else {
                merged.put(k, v);
            }
        });
        store(merged);
        return get();
    }

    /** Removes all realm overrides; the global file and defaults apply again. */
    @DELETE
    public Response reset() {
        auth.realm().requireManageRealm();
        store(Map.of());
        return Response.noContent().build();
    }

    @GET
    @Path("assets")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, Object> listAssets() {
        auth.realm().requireViewRealm();
        return Map.of("assets", provider.listRealmAssets(realm), "maxBytes", provider.maxAssetBytes());
    }

    @GET
    @Path("assets/{name}")
    public Response getAsset(@PathParam("name") String name, @Context Request request) {
        auth.realm().requireViewRealm();
        return provider.findAsset(realm, name).map(a -> Responses.asset(request, a)).orElseThrow(NotFoundException::new);
    }

    /** Uploads a binary asset; reference it from settings by its name, e.g. {@code "logoUrl": "logo.svg"}. */
    @PUT
    @Path("assets/{name}")
    @Consumes(MediaType.WILDCARD)
    public Response uploadAsset(@PathParam("name") String name, byte[] content) {
        auth.realm().requireManageRealm();
        if (!BrandingValidator.ASSET_NAME.matcher(name).matches()) {
            throw new BadRequestException(error("Asset names must be simple file names with an image or font extension"));
        }
        if (content == null || content.length == 0) {
            throw new BadRequestException(error("Empty upload"));
        }
        if (content.length > provider.maxAssetBytes()) {
            return Response.status(Response.Status.REQUEST_ENTITY_TOO_LARGE)
                    .entity(Map.of("error", "Asset exceeds " + provider.maxAssetBytes() / 1024 + " KiB"))
                    .type(MediaType.APPLICATION_JSON).build();
        }
        provider.saveRealmAsset(realm, name, content);
        adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri()).success();
        return Response.noContent().build();
    }

    @DELETE
    @Path("assets/{name}")
    public Response deleteAsset(@PathParam("name") String name) {
        auth.realm().requireManageRealm();
        if (!provider.deleteRealmAsset(realm, name)) {
            throw new NotFoundException();
        }
        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();
        return Response.noContent().build();
    }

    /** Built-in and custom layouts available to this realm. */
    @GET
    @Path("layouts")
    @Produces(MediaType.APPLICATION_JSON)
    public List<Map<String, Object>> listLayouts() {
        auth.realm().requireViewRealm();
        return provider.listLayouts(realm).stream().map(l -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("name", l.getName());
            m.put("source", l.getSource().name().toLowerCase(java.util.Locale.ROOT));
            m.put("extends", l.getBase());
            return m;
        }).toList();
    }

    @GET
    @Path("layouts/{name}")
    @Produces("text/css")
    public String getLayout(@PathParam("name") String name) {
        auth.realm().requireViewRealm();
        return provider.findLayout(realm, name).filter(BrandingLayout::isCustom)
                .map(BrandingLayout::getCss).orElseThrow(NotFoundException::new);
    }

    /**
     * Uploads (or replaces) a custom layout for this realm. Start the CSS with
     * {@code /* extends: <built-in> *&#47;} to build on a built-in layout; then select it with
     * {@code "layout": "<name>"}.
     */
    @PUT
    @Path("layouts/{name}")
    @Consumes({"text/css", MediaType.TEXT_PLAIN, MediaType.WILDCARD})
    public Response uploadLayout(@PathParam("name") String name, String css) {
        auth.realm().requireManageRealm();
        if (!BrandingLayout.isValidName(name) || BrandingLayout.isBuiltIn(name)) {
            throw new BadRequestException(error("Layout names use lowercase letters, digits and dashes, "
                    + "and cannot replace a built-in layout " + BrandingLayout.BUILT_IN));
        }
        if (!BrandingLayout.isValidCss(css)) {
            throw new BadRequestException(error("Layout CSS must be non-empty and at most "
                    + BrandingLayout.MAX_CSS_BYTES / 1024 + " KiB"));
        }
        provider.saveRealmLayout(realm, name, css);
        adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri()).success();
        return Response.noContent().build();
    }

    @DELETE
    @Path("layouts/{name}")
    public Response deleteLayout(@PathParam("name") String name) {
        auth.realm().requireManageRealm();
        if (!provider.deleteRealmLayout(realm, name)) {
            throw new NotFoundException();
        }
        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();
        return Response.noContent().build();
    }

    private Map<String, String> validated(Map<String, Object> body) {
        BrandingValidator.Result result = BrandingValidator.validate(body);
        String layout = result.values().get(BrandingField.LAYOUT.key());
        if (result.isValid() && layout != null && !layout.isEmpty() && provider.findLayout(realm, layout).isEmpty()) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid branding configuration",
                            "details", List.of("layout: unknown layout '" + layout + "'; see GET .../branding/layouts")))
                    .type(MediaType.APPLICATION_JSON).build());
        }
        if (!result.isValid()) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Invalid branding configuration", "details", result.errors()))
                    .type(MediaType.APPLICATION_JSON).build());
        }
        return result.values();
    }

    private void store(Map<String, String> overrides) {
        provider.setRealmOverrides(realm, overrides);
        BrandingUiTabProviderFactory.syncComponent(session, realm, provider.getRealmOverrides(realm));
        adminEvent.operation(OperationType.UPDATE).resourcePath(session.getContext().getUri())
                .representation(overrides).success();
    }

    private static Response error(String message) {
        return Response.status(Response.Status.BAD_REQUEST).entity(Map.of("error", message))
                .type(MediaType.APPLICATION_JSON).build();
    }
}
