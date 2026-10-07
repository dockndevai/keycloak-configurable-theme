package com.kcbranding.keycloak.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.util.JsonSerialization;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.MountableFile;

/**
 * Boots the latest Keycloak with the packaged provider jar and checks the theme end to end:
 * enforcement across realms, the layered configuration, admin API, assets, consoles and email.
 */
class ConfigurableThemeIT {

    private static final String CODE_CHALLENGE = "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM";

    private static final Network NETWORK = Network.newNetwork();

    private static final GenericContainer<?> MAILPIT = new GenericContainer<>("axllent/mailpit:latest")
            .withNetwork(NETWORK)
            .withNetworkAliases("mailpit")
            .withExposedPorts(8025, 1025)
            .waitingFor(Wait.forHttp("/livez").forPort(8025));

    private static final KeycloakContainer KEYCLOAK =
            new KeycloakContainer("quay.io/keycloak/keycloak:" + System.getProperty("keycloak.version", "26.8.0"))
                    .withNetwork(NETWORK)
                    .withProviderLibsFrom(List.of(new File(
                            System.getProperty("provider.jar", "target/keycloak-configurable-theme.jar"))))
                    .withRealmImportFile("it/it-realm.json")
                    .withCopyFileToContainer(MountableFile.forClasspathResource("it/branding.json"),
                            "/opt/keycloak/conf/branding.json")
                    .withCopyFileToContainer(MountableFile.forClasspathResource("it/it-logo.svg"),
                            "/opt/keycloak/branding/it-logo.svg")
                    .withEnv("KC_SPI_BRANDING__DEFAULT__RELOAD_INTERVAL", "1");

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private static String base;
    private static Keycloak admin;

    @BeforeAll
    static void start() {
        MAILPIT.start();
        KEYCLOAK.start();
        base = KEYCLOAK.getAuthServerUrl().replaceAll("/$", "");
        admin = KEYCLOAK.getKeycloakAdminClient();
    }

    @AfterAll
    static void stop() {
        KEYCLOAK.stop();
        MAILPIT.stop();
        NETWORK.close();
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    void newRealmsUseTheConfigurableThemeEvenWhenTheyPickAnotherOne() throws Exception {
        createRealm("fresh", "keycloak");

        String html = loginPage("fresh").body();
        assertTrue(html.contains("/login/configurable/css/configurable.css"), "configurable login theme is used");
        assertTrue(html.contains("cfg-layout-centered"));
        assertTrue(html.contains("/realms/fresh/branding/theme.css?scope=login"));
        assertTrue(html.contains("/realms/fresh/branding/assets/it-logo.svg"), "logo from file defaults");
        assertTrue(html.contains("IT footer"));
        assertTrue(html.contains("href=\"https://example.com/privacy\""));

        String css = get("/realms/fresh/branding/theme.css").body();
        assertTrue(css.contains("--cfg-primary: #123abc;"), "file default primary color");
    }

    @Test
    void realmSectionOfTheFileOverridesDefaults() throws Exception {
        String html = loginPage("it").body();
        assertTrue(html.contains("cfg-layout-split-left"));
        assertTrue(html.contains("Hello from the file"));
        assertTrue(get("/realms/it/branding/theme.css").body().contains("--cfg-primary: #0f766e;"));
    }

    @Test
    void adminApiChangesTheLoginPage() throws Exception {
        createRealm("api", null);

        HttpResponse<String> put = send("PUT", "/admin/realms/api/branding", token(),
                "{\"layout\":\"minimal\",\"infoBanner\":\"Hello IT\",\"showRealmName\":false}");
        assertEquals(200, put.statusCode(), put.body());

        Map<?, ?> body = JsonSerialization.readValue(put.body(), Map.class);
        assertEquals("minimal", ((Map<?, ?>) body.get("overrides")).get("layout"));
        assertEquals("#123abc", ((Map<?, ?>) body.get("effective")).get("primaryColor"), "inherits the file");

        String html = loginPage("api").body();
        assertTrue(html.contains("cfg-layout-minimal"));
        assertTrue(html.contains("Hello IT"));
        assertFalse(html.contains("cfg-brand__name"), "realm name hidden");

        HttpResponse<String> patch = send("PATCH", "/admin/realms/api/branding", token(), "{\"infoBanner\":\"\"}");
        assertEquals(200, patch.statusCode());
        assertFalse(loginPage("api").body().contains("Hello IT"), "blank value removes the override");
    }

    @Test
    void adminApiValidatesInputAndRequiresAdminRights() throws Exception {
        HttpResponse<String> bad = send("PUT", "/admin/realms/it/branding", token(),
                "{\"primaryColor\":\"red;}body{display:none\"}");
        assertEquals(400, bad.statusCode());
        assertTrue(bad.body().contains("primaryColor"));

        assertEquals(401, send("PUT", "/admin/realms/it/branding", null, "{}").statusCode());
        assertEquals(401, send("GET", "/admin/realms/it/branding", null, null).statusCode());
    }

    @Test
    void uploadedAssetsAreServedPublicly() throws Exception {
        String svg = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"8\" height=\"8\"/>";
        assertEquals(204, send("PUT", "/admin/realms/it/branding/assets/uploaded.svg", token(), svg).statusCode());

        HttpResponse<String> asset = get("/realms/it/branding/assets/uploaded.svg");
        assertEquals(200, asset.statusCode());
        assertTrue(asset.headers().firstValue("Content-Type").orElse("").startsWith("image/svg+xml"));
        assertEquals("nosniff", asset.headers().firstValue("X-Content-Type-Options").orElse(""));
        assertEquals(svg, asset.body());

        assertEquals(400, send("PUT", "/admin/realms/it/branding/assets/page.html", token(), "<b>x</b>").statusCode());
        assertEquals(404, get("/realms/it/branding/assets/..%2F..%2Fconf%2Fbranding.json").statusCode());

        // File-system asset from the assets directory
        assertEquals(200, get("/realms/it/branding/assets/it-logo.svg").statusCode());
        assertEquals(204, send("DELETE", "/admin/realms/it/branding/assets/uploaded.svg", token(), null).statusCode());
        assertEquals(404, get("/realms/it/branding/assets/uploaded.svg").statusCode());
    }

    @Test
    void stylesheetSupportsConditionalRequests() throws Exception {
        HttpResponse<String> first = get("/realms/it/branding/theme.css");
        String etag = first.headers().firstValue("ETag").orElseThrow();
        HttpResponse<String> second = HTTP.send(HttpRequest.newBuilder(URI.create(base + "/realms/it/branding/theme.css"))
                .header("If-None-Match", etag).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(304, second.statusCode());
    }

    @Test
    void accountConsoleLoadsTheBrandingLoaderUnlessDisabled() throws Exception {
        createRealm("consoles", null);
        String account = get("/realms/consoles/account/").body();
        assertTrue(account.contains("/account/configurable/js/branding-loader.js"), account);

        assertEquals(200, send("PATCH", "/admin/realms/consoles/branding", token(),
                "{\"applyToAccount\":false}").statusCode());
        assertFalse(get("/realms/consoles/account/").body().contains("branding-loader.js"));

        String config = get("/realms/consoles/branding/config.json").body();
        assertTrue(config.contains("\"applyToAccount\":false"));
    }

    @Test
    void disablingBrandingRestoresTheRealmsOwnTheme() throws Exception {
        createRealm("optout", "keycloak.v2");
        assertEquals(200, send("PUT", "/admin/realms/optout/branding", token(), "{\"enabled\":false}").statusCode());

        String html = loginPage("optout").body();
        assertTrue(html.contains("/login/keycloak.v2/"), "realm's own theme is used");
        assertFalse(html.contains("cfg-layout"));
    }

    @Test
    void htmlEmailsUseTheBrandedLayout() throws Exception {
        UserRepresentation me = admin.realm("master").users().search("admin").get(0);
        me.setEmail("admin@example.test");
        admin.realm("master").users().get(me.getId()).update(me);
        send("PATCH", "/admin/realms/master/branding", token(), "{\"footerText\":\"Branded email footer\"}");

        HttpResponse<String> sent = send("POST", "/admin/realms/master/testSMTPConnection", token(),
                "{\"host\":\"mailpit\",\"port\":\"1025\",\"from\":\"keycloak@example.test\"}");
        assertEquals(204, sent.statusCode(), sent.body());

        String mailApi = "http://" + MAILPIT.getHost() + ":" + MAILPIT.getMappedPort(8025);
        HttpResponse<String> latest = HTTP.send(HttpRequest.newBuilder(URI.create(mailApi + "/api/v1/message/latest")).build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, latest.statusCode());
        String html = (String) JsonSerialization.readValue(latest.body(), Map.class).get("HTML");
        assertTrue(html.contains("border-top:4px solid #123abc"), html);
        assertTrue(html.contains("/realms/master/branding/assets/it-logo.svg"), html);
        assertTrue(html.contains("Branded email footer"), html);
    }

    // ---------------------------------------------------------------------------------------------

    private static void createRealm(String name, String loginTheme) {
        RealmRepresentation realm = new RealmRepresentation();
        realm.setRealm(name);
        realm.setEnabled(true);
        realm.setLoginTheme(loginTheme);
        admin.realms().create(realm);
    }

    private static HttpResponse<String> loginPage(String realm) throws Exception {
        String redirect = URLEncoder.encode(base + "/realms/" + realm + "/account/", StandardCharsets.UTF_8);
        HttpResponse<String> response = get("/realms/" + realm + "/protocol/openid-connect/auth?client_id=account-console"
                + "&redirect_uri=" + redirect + "&response_type=code&scope=openid"
                + "&code_challenge=" + CODE_CHALLENGE + "&code_challenge_method=S256");
        assertEquals(200, response.statusCode(), response.body());
        return response;
    }

    private static String token() {
        return admin.tokenManager().getAccessTokenString();
    }

    private static HttpResponse<String> get(String path) throws Exception {
        return send("GET", path, null, null);
    }

    private static HttpResponse<String> send(String method, String path, String bearer, String json) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(base + path))
                .method(method, json == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(json));
        if (json != null) {
            request.header("Content-Type", path.contains("/assets/") ? "application/octet-stream" : "application/json");
        }
        if (bearer != null) {
            request.header("Authorization", "Bearer " + bearer);
        }
        return HTTP.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }
}
