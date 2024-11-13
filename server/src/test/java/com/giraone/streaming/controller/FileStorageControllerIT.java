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
class FileStorageControllerIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileStorageControllerIT.class);
    private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {
    };

    private static final String FILENAME_IMAGE = "file-" + UUID.randomUUID() + ".jpg";
    private static final String FILENAME_VIDEO = "file-" + UUID.randomUUID() + ".mp4";
    private static final long EXPECTED_IMAGE_FILE_SIZE = 60550;
    private static final long EXPECTED_VIDEO_FILE_SIZE = 1057149;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void test1_uploadImageFile() throws IOException {

        File file = ResourceUtils.getFile("classpath:testdata/small.jpg");
        assertThat(file).isNotNull();
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            var bodyInserter = BodyInserters.fromResource(new InputStreamResource(fileInputStream));
            LOGGER.info("Read upload image from {}", file.getAbsolutePath());
            webTestClient.post()
                .uri("/images/{file}", FILENAME_IMAGE)
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
        File uploadedFile = new File(FileService.getFileDirImages(), FILENAME_IMAGE);
        assertThat(uploadedFile).exists().hasSize(EXPECTED_IMAGE_FILE_SIZE);
        File thumbFile = new File(FileService.getThumbDirImages(), FileService.buildThumbnailFileName(FILENAME_IMAGE));
        assertThat(thumbFile).exists();
        assertThat(thumbFile.length()).isGreaterThan(100L);
    }

    @Test
    void test2_downloadImage() throws IOException {

        Flux<ByteBuffer> content = webTestClient.get()
            .uri("/images/{file}", FILENAME_IMAGE)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.IMAGE_JPEG)
            .expectHeader().contentLength(EXPECTED_IMAGE_FILE_SIZE)
            .returnResult(ByteBuffer.class)
            .getResponseBody();
        File downloadedFile = File.createTempFile("test-", ".jpg");
        LOGGER.info("Write download image to {}", downloadedFile);
        AsynchronousFileChannel channel = AsynchronousFileChannel.open(downloadedFile.toPath(), CREATE, WRITE);
        FluxUtil.writeFile(content, channel).block();
        assertThat(downloadedFile).exists().hasSize(EXPECTED_IMAGE_FILE_SIZE);
        // Now delete the uploaded file and thumbnail
        File uploadedFile = new File(FileService.getFileDirImages(), FILENAME_IMAGE);
        assertThat(uploadedFile.delete()).isTrue();
        File thumbFile = new File(FileService.getThumbDirImages(), FileService.buildThumbnailFileName(FILENAME_IMAGE));
        assertThat(thumbFile.delete()).isTrue();
    }

    @Test
    void test3_listImageFiles() {

        // act/assert
        webTestClient.get().uri("/image-infos?filter=0")
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

    @Test
    void test4_uploadVideoFile() throws IOException {

        File file = ResourceUtils.getFile("classpath:testdata/video-720x480-1MB.mp4");
        assertThat(file).isNotNull();
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            var bodyInserter = BodyInserters.fromResource(new InputStreamResource(fileInputStream));
            LOGGER.info("Read upload image from {}", file.getAbsolutePath());
            webTestClient.post()
                .uri("/images/{file}", FILENAME_VIDEO)
                .contentType(MediaType.parseMediaType("video/mp4"))
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
        File uploadedFile = new File(FileService.getFileDirVideos(), FILENAME_VIDEO);
        assertThat(uploadedFile).exists().hasSize(EXPECTED_VIDEO_FILE_SIZE);
        File thumbFile = new File(FileService.getThumbDirVideos(), FileService.buildThumbnailFileName(FILENAME_VIDEO));
        assertThat(thumbFile).exists();
        assertThat(thumbFile.length()).isGreaterThan(100L);
    }

    @Test
    void test5_downloadVideo() throws IOException {

        Flux<ByteBuffer> content = webTestClient.get()
            .uri("/videos/{file}", FILENAME_VIDEO)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType("video/mp4")
            .expectHeader().contentLength(EXPECTED_VIDEO_FILE_SIZE)
            .returnResult(ByteBuffer.class)
            .getResponseBody();
        File downloadedFile = File.createTempFile("test-", ".jpg");
        LOGGER.info("Write download image to {}", downloadedFile);
        AsynchronousFileChannel channel = AsynchronousFileChannel.open(downloadedFile.toPath(), CREATE, WRITE);
        FluxUtil.writeFile(content, channel).block();
        assertThat(downloadedFile).exists().hasSize(EXPECTED_VIDEO_FILE_SIZE);
        // Now delete the uploaded file and thumbnail
        File uploadedFile = new File(FileService.getFileDirImages(), FILENAME_VIDEO);
        assertThat(uploadedFile.delete()).isTrue();
        File thumbFile = new File(FileService.getThumbDirImages(), FileService.buildThumbnailFileName(FILENAME_VIDEO));
        assertThat(thumbFile.delete()).isTrue();
    }

    @Test
    void test6_listVideoFiles() {

        // act/assert
        webTestClient.get().uri("/video-infos?filter=0")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON) // Normally not necessary
            .expectBody()
            .jsonPath("$.[0].fileName").isEqualTo("video-720x480-1MB.mp4")
            .jsonPath("$.[0].sizeInBytes").isEqualTo(1057149L)
            .jsonPath("$.[0].mediaType").isEqualTo("video/mp4")
        ;
    }
}