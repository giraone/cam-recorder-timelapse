package com.giraone.camera.service;

import com.giraone.camera.config.ApplicationProperties;
import com.giraone.camera.service.model.CameraStatusRecord;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests of the camera status calls of {@link FileViewService} against a real HTTP server, which acts as a
 * test double of the backend server. Using real HTTP is essential here, because the JSON decoding of the
 * WebClient is part of the behaviour under test.
 */
class FileViewServiceTest {

    private final Map<String, String> responseByPath = new HashMap<>();

    private HttpServer httpServer;
    private FileViewService fileViewService;

    @BeforeEach
    void startServer() throws IOException {
        httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        httpServer.createContext("/", this::respond);
        httpServer.start();
        ApplicationProperties applicationProperties = new ApplicationProperties();
        applicationProperties.setHostUrl("http://localhost:" + httpServer.getAddress().getPort());
        fileViewService = new FileViewService(applicationProperties);
    }

    @AfterEach
    void stopServer() {
        httpServer.stop(0);
    }

    @Test
    void listCamerasDecodesTheJsonArrayOfCameraNames() {
        // arrange
        responseByPath.put("/cameras", "[\"cam-b\",\"cam-a\"]");
        // act
        List<String> result = fileViewService.listCameras();
        // assert
        assertThat(result).containsExactly("cam-a", "cam-b");
    }

    @Test
    void listCamerasReturnsAnEmptyListWhenNoCameraIsKnown() {
        // arrange
        responseByPath.put("/cameras", "[]");
        // act
        List<String> result = fileViewService.listCameras();
        // assert
        assertThat(result).isEmpty();
    }

    @Test
    void listCameraStatusDecodesAllAttributesOfTheStatusRecords() {
        // arrange
        responseByPath.put("/status/cam-a", """
            [{"timestamp":"2026-09-06T12:30:45","rssi":-61,"imageCounter":12,"imageErrors":1,
              "cameraInitCounter":2,"cameraInitErrors":3,"uploadImageErrors":4,"uploadStatusErrors":5},
             {"timestamp":"2026-09-06T12:31:45","rssi":-62,"imageCounter":13,"imageErrors":0,
              "cameraInitCounter":0,"cameraInitErrors":0,"uploadImageErrors":0,"uploadStatusErrors":0}]""");
        // act
        List<CameraStatusRecord> result = fileViewService.listCameraStatus("cam-a");
        // assert
        assertThat(result).containsExactly(
            new CameraStatusRecord(LocalDateTime.parse("2026-09-06T12:30:45"), -61, 12, 1, 2, 3, 4, 5),
            new CameraStatusRecord(LocalDateTime.parse("2026-09-06T12:31:45"), -62, 13, 0, 0, 0, 0, 0));
    }

    @Test
    void listCameraStatusEncodesTheCameraNameIntoThePath() {
        // arrange
        responseByPath.put("/status/cam%20a", "[]");
        // act
        List<CameraStatusRecord> result = fileViewService.listCameraStatus("cam a");
        // assert
        assertThat(result).isEmpty();
    }

    private void respond(HttpExchange exchange) throws IOException {
        final String path = exchange.getRequestURI().getRawPath();
        final String body = responseByPath.get(path);
        final byte[] content = (body != null ? body : "not found").getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(body != null ? 200 : 404, content.length);
        try (OutputStream outputStream = exchange.getResponseBody()) {
            outputStream.write(content);
        }
    }
}
