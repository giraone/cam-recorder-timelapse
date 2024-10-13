package com.giraone.streaming.controller;

import com.giraone.streaming.service.FileService;
import com.giraone.streaming.service.FluxUtil;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.util.ResourceUtils;
import org.springframework.web.reactive.function.BodyInserters;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.util.Map;
import java.util.UUID;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestMethodOrder(MethodOrderer.MethodName.class)
@AutoConfigureWebTestClient
class FileControllerIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileControllerIT.class);
    private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {
    };
    private static final long EXPECTED_FILE_SIZE = 60550;
    private static final String FILENAME = "file-" + UUID.randomUUID() + ".jpg";

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void test1_uploadFile() throws IOException {

        File file = ResourceUtils.getFile("classpath:testdata/small.jpg");
        assertThat(file).isNotNull();
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            var bodyInserter = BodyInserters.fromResource(new InputStreamResource(fileInputStream));
            LOGGER.info("Read upload image from {}", file.getAbsolutePath());
            webTestClient.post()
                .uri("/camera-images/{file}", FILENAME)
                .contentType(MediaType.IMAGE_JPEG)
                .body(bodyInserter)
                .exchange()
                .expectStatus().isOk()
                .expectBody(MAP)
                .value(value -> assertThat(value).containsExactlyInAnyOrderEntriesOf(Map.of(
                    "success", true,
                    "size", -1,
                    "restart", false
                )));
        }
        File uploadedFile = new File(FileService.getFileDir(), FILENAME);
        assertThat(uploadedFile).exists().hasSize(EXPECTED_FILE_SIZE);
        File thumbFile = new File(FileService.getThumbDir(), FILENAME);
        assertThat(thumbFile).exists();
        assertThat(thumbFile.length()).isGreaterThan(100L);
    }

    @Test
    void test2_downloadFile() throws IOException {

        Flux<ByteBuffer> content = webTestClient.get()
            .uri("/camera-images/{file}", FILENAME)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.IMAGE_JPEG)
            .expectHeader().contentLength(EXPECTED_FILE_SIZE)
            .returnResult(ByteBuffer.class)
            .getResponseBody();
        File downloadedFile = File.createTempFile("test-", ".jpg");
        LOGGER.info("Write download image to {}", downloadedFile);
        AsynchronousFileChannel channel = AsynchronousFileChannel.open(downloadedFile.toPath(), CREATE, WRITE);
        FluxUtil.writeFile(content, channel).block();
        assertThat(downloadedFile).exists().hasSize(EXPECTED_FILE_SIZE);
        // Now delete the uploaded file
        File uploadedFile = new File(FileService.getFileDir(), FILENAME);
        assertThat(uploadedFile.delete()).isTrue();
    }

    @Test
    void test3_listFiles() {

        // act/assert
        webTestClient.get().uri("/camera-image-infos?filter=0")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON) // Normally not necessary
            .expectBody()
            .jsonPath("$.[0].fileName").isEqualTo("0000-ferrari.jpg")
            .jsonPath("$.[0].sizeInBytes").isEqualTo(237365L)
            .jsonPath("$.[0].mediaType").isEqualTo("image/jpeg")
            .jsonPath("$.[1].fileName").isEqualTo("0001-porsche.jpg")
            .jsonPath("$.[1].sizeInBytes").isEqualTo(339894L)
            .jsonPath("$.[1].mediaType").isEqualTo("image/jpeg")
        ;
    }
}