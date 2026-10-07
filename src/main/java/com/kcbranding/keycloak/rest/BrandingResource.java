package com.kcbranding.keycloak.rest;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import com.kcbranding.keycloak.config.BrandingConfig;
import com.kcbranding.keycloak.config.BrandingField;
import com.kcbranding.keycloak.config.BrandingValidator;
import com.kcbranding.keycloak.css.CssGenerator;
import com.kcbranding.keycloak.spi.BrandingProvider;
import com.kcbranding.keycloak.theme.BrandingBean;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

/**
 * Public, unauthenticated endpoints under {@code /realms/{realm}/branding}. Only serves presentation
 * data that is visible on the login page anyway.
 */
public class BrandingResource {

    private final KeycloakSession session;
    private final RealmModel realm;
    private final BrandingProvider provider;

    public BrandingResource(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.provider = session.getProvider(BrandingProvider.class);
    }

    @GET
    @Path("theme.css")
    @Produces("text/css")
    public Response css(@QueryParam("scope") String scope, @Context Request request) {
        BrandingConfig config = provider.getConfig(realm);
        CssGenerator.Scope cssScope = "console".equalsIgnoreCase(scope) ? CssGenerator.Scope.CONSOLE : CssGenerator.Scope.LOGIN;
        EntityTag etag = new EntityTag(config.version() + "-" + cssScope.name().toLowerCase(Locale.ROOT));
        Response.ResponseBuilder notModified = request.evaluatePreconditions(etag);
        if (notModified != null) {
            return notModified.build();
        }
        String css = CssGenerator.generate(config, cssScope, value -> provider.resolveUrl(realm, value));
        return Response.ok(css, "text/css; charset=utf-8").tag(etag).cacheControl(Responses.cache(60)).build();
    }

    /** Small public document used by the account/admin console loader script. */
    @GET
    @Path("config.json")
    @Produces(MediaType.APPLICATION_JSON)
    public Response config() {
        BrandingConfig config = provider.getConfig(realm);
        BrandingBean bean = new BrandingBean(config, provider, realm);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("realm", realm.getName());
        body.put("version", config.version());
        body.put("enabled", config.is(BrandingField.ENABLED));
        body.put("applyToAccount", config.is(BrandingField.APPLY_TO_ACCOUNT));
        body.put("applyToAdmin", config.is(BrandingField.APPLY_TO_ADMIN));
        body.put("cssUrl", provider.endpointUrl(realm) + "/theme.css?scope=console&v=" + config.version());
        body.put("fontCssUrl", bean.getFontCssUrl());
        body.put("logoUrl", bean.getLogoUrl());
        body.put("faviconUrl", bean.getFaviconUrl());
        body.put("faviconType", bean.getFaviconType());
        body.put("pageTitle", bean.getPageTitle());
        body.put("colorScheme", bean.getColorScheme());
        return Response.ok(body).cacheControl(Responses.cache(30)).build();
    }

    @GET
    @Path("logo")
    public Response logo(@Context Request request) {
        return configured(request, BrandingField.LOGO_URL);
    }

    @GET
    @Path("favicon")
    public Response favicon(@Context Request request) {
        return configured(request, BrandingField.FAVICON_URL);
    }

    @GET
    @Path("background")
    public Response background(@Context Request request) {
        return configured(request, BrandingField.BACKGROUND_IMAGE_URL);
    }

    @GET
    @Path("assets/{name}")
    public Response asset(@PathParam("name") String name, @Context Request request) {
        return provider.findAsset(realm, name)
                .map(asset -> Responses.asset(request, asset))
                .orElseThrow(NotFoundException::new);
    }

    private Response configured(Request request, BrandingField field) {
        String value = provider.getConfig(realm).get(field);
        if (value == null || value.isEmpty()) {
            throw new NotFoundException();
        }
        if (value.startsWith("data:")) {
            return Responses.dataUri(request, field.key(), value);
        }
        if (BrandingValidator.isExternalUrl(value)) {
            return Response.temporaryRedirect(URI.create(value)).build();
        }
        return asset(value, request);
    }
}
