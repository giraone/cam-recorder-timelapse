package com.giraone.streaming.controller;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestMethodOrder(MethodOrderer.MethodName.class)
@AutoConfigureWebTestClient
class CamControllerIT {

    private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {
    };

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void testSettingsDownload() {

        webTestClient.get()
            .uri("/camera/settings")
            .exchange()
            .expectStatus().isOk()
            .expectBody(MAP)
            .value(value -> assertThat(value).containsAllEntriesOf(Map.of(
                "brightness", 0,
                "contrast", 0
            )));
    }
}