package com.giraone.camera.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.vaadin.lineawesome.LineAwesomeIcon;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Vaadin 25 does not grant "anyRequest" to every authenticated user anymore, it derives the access rules from the
 * Flow route registry. Static resources of the views are no routes, so without an explicit rule in
 * {@link SecurityConfig} they are answered with 403 and the iframes of the views stay empty.
 * The rules are verified over real HTTP, because they only take effect in the complete filter chain.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StaticResourceAccessTest {

    private static final Pattern CSRF_PATTERN = Pattern.compile("name=\"_csrf\" content=\"([^\"]+)\"");

    private final HttpClient httpClient = HttpClient.newBuilder()
        .followRedirects(HttpClient.Redirect.NEVER)
        .connectTimeout(Duration.ofSeconds(10))
        .build();

    @Value("${local.server.port}")
    private int port;

    private String sessionCookie;

    @ParameterizedTest
    @ValueSource(strings = {
        "/components/image-viewer/image-viewer.html",
        "/components/video-viewer/video-viewer.html",
        "/components/image-viewer/js/imagecanvas-1.4.js",
        "/components/image-viewer/images/zoom_in.gif",
        "/js/global.js"
    })
    void authenticatedUserCanReadStaticViewResources(String path) throws Exception {
        assertThat(get(path, login()).statusCode()).isEqualTo(200);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "/components/image-viewer/image-viewer.html",
        "/js/global.js"
    })
    void anonymousUserIsSentToTheLoginPage(String path) throws Exception {
        HttpResponse<String> response = get(path, null);
        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("Location").orElseThrow()).endsWith("/login");
    }

    /**
     * The icons of the line-awesome add-on are single SVG files below "/line-awesome/svg/", which are no routes
     * either. The URLs are taken from the enum itself, so the test does not depend on the naming convention.
     */
    @ParameterizedTest
    @EnumSource(value = LineAwesomeIcon.class, names = {
        "BACKWARD_SOLID", "CARET_SQUARE_LEFT_SOLID", "CARET_SQUARE_RIGHT_SOLID", "CUT_SOLID", "DOWNLOAD_SOLID",
        "FORWARD_SOLID", "LAPTOP_SOLID", "PEN_SOLID", "SYNC_SOLID", "TIMES_CIRCLE_SOLID", "VIDEO_SOLID"})
    void iconsUsedByTheViewsAreServed(LineAwesomeIcon icon) throws Exception {
        HttpResponse<String> response = get("/" + icon.getSource(), login());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("<svg");
    }

    @Test
    void theViewerPagesAreReallyServed() throws Exception {
        assertThat(get("/components/image-viewer/image-viewer.html", login()).body())
            .contains("<title>ImageViewer</title>");
        assertThat(get("/components/video-viewer/video-viewer.html", login()).body())
            .contains("<title>VideoViewer</title>");
    }

    @Test
    void publicResourcesStayPublic() throws Exception {
        assertThat(get("/images/icon.png", null).statusCode()).isEqualTo(200);
    }

    private HttpResponse<String> get(String path, String cookie) throws IOException, InterruptedException {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (cookie != null) {
            request.header("Cookie", cookie);
        }
        return httpClient.send(request.GET().build(), HttpResponse.BodyHandlers.ofString());
    }

    /** Performs the real form login and returns the session cookie of the authenticated session. */
    private String login() throws IOException, InterruptedException {

        if (sessionCookie != null) {
            return sessionCookie;
        }
        HttpResponse<String> loginPage = get("/login", null);
        assertThat(loginPage.statusCode()).isEqualTo(200);
        Matcher matcher = CSRF_PATTERN.matcher(loginPage.body());
        assertThat(matcher.find()).as("CSRF token on the login page").isTrue();

        String form = "username=boss&password=" + URLEncoder("boss-secret") + "&_csrf=" + URLEncoder(matcher.group(1));
        HttpResponse<String> response = httpClient.send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Cookie", cookieOf(loginPage))
                .POST(HttpRequest.BodyPublishers.ofString(form, StandardCharsets.UTF_8))
                .build(),
            HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).as("login of user 'boss'").isEqualTo(302);
        assertThat(response.headers().firstValue("Location").orElseThrow()).doesNotContain("error");
        sessionCookie = cookieOf(response);
        return sessionCookie;
    }

    private static String cookieOf(HttpResponse<String> response) {
        return response.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
    }

    private static String URLEncoder(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
