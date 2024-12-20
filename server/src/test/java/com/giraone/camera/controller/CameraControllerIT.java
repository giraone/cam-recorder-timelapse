package com.giraone.camera.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giraone.camera.service.FileService;
import com.giraone.camera.service.FluxUtil;
import com.giraone.camera.service.api.Settings;
import com.giraone.camera.service.api.Status;
import com.giraone.camera.service.video.model.TimelapseCommand;
import com.giraone.camera.service.video.model.TimelapseResult;
import com.giraone.camera.util.ObjectMapperBuilder;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.giraone.camera.service.FileService.buildThumbnailFileName;
import static com.giraone.camera.service.video.VideoServiceIT.buildInputFiles;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestMethodOrder(MethodOrderer.MethodName.class)
@AutoConfigureWebTestClient
class CameraControllerIT {

    private static final Logger LOGGER = LoggerFactory.getLogger(CameraControllerIT.class);
    private static final ObjectMapper MAPPER = ObjectMapperBuilder.build();
    private static final ParameterizedTypeReference<Map<String, Object>> MAP = new ParameterizedTypeReference<>() {
    };

    private static final String FILENAME_IMAGE = "file-" + UUID.randomUUID() + ".jpg";
    private static final String FILENAME_VIDEO = "file-" + UUID.randomUUID() + ".mp4";
    private static final long EXPECTED_IMAGE_FILE_SIZE = 60550;
    private static final long EXPECTED_VIDEO_FILE_SIZE = 1057149;

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void getSettings() {

        Settings settings = webTestClient.get()
            .uri("/settings")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .returnResult(Settings.class)
            .getResponseBody()
            .blockFirst();
        // assert
        assertThat(settings).isNotNull();
        assertThat(settings.getWorkflow()).isNotNull();
        assertThat(settings.getCamera()).isNotNull();
        assertThat(settings.getWorkflow().getDelayMs()).isEqualTo(20000);
        assertThat(settings.getCamera().getClockFrequencyHz()).isEqualTo(16000000);
    }

    @Test
    void storeSettings() {

        Settings settings = new Settings();
        Status status = webTestClient.put()
            .uri("/settings")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(settings)
            .exchange()
            .expectStatus().isOk()
            .returnResult(Status.class)
            .getResponseBody()
            .blockFirst();
        assertThat(status).isNotNull();
        assertThat(status.success()).isTrue();
    }

    @Test
    void uploadStatus() {

        Settings settings = webTestClient.put()
            .uri("/status")
            .contentType(MediaType.APPLICATION_JSON)
            .accept(MediaType.APPLICATION_JSON)
            .bodyValue(Map.of("rssi", -10))
            .exchange()
            .expectStatus().isOk()
            .returnResult(Settings.class)
            .getResponseBody()
            .blockFirst();
        assertThat(settings).isNotNull();
        assertThat(settings.getStatus()).isNotNull();
        assertThat(settings.getStatus().success()).isTrue();
    }

    @Test
    void test1_uploadImageFile() throws IOException {

        File file = ResourceUtils.getFile("classpath:testdata/small.jpg");
        assertThat(file).isNotNull();
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            var bodyInserter = BodyInserters.fromResource(new InputStreamResource(fileInputStream));
            LOGGER.info("Read upload image from {}", file.getAbsolutePath());
            Settings settings = webTestClient.post()
                .uri("/images/{file}", FILENAME_IMAGE)
                .contentType(MediaType.IMAGE_JPEG)
                .body(bodyInserter)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Settings.class)
                .returnResult()
                .getResponseBody();
            // assert
            assertThat(settings).isNotNull();
            assertThat(settings.getStatus()).isNotNull();
            assertThat(settings.getStatus().success()).isTrue();
        }
        Path uploadedFile = FileService.getFileDirImages().resolve(FILENAME_IMAGE);
        assertThat(uploadedFile).exists().hasSize(EXPECTED_IMAGE_FILE_SIZE);
        Path thumbFile = FileService.getThumbDirImages().resolve(FileService.buildThumbnailFileName(FILENAME_IMAGE));
        assertThat(thumbFile).exists();
        assertThat(Files.size(thumbFile)).isGreaterThan(100L);
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
        Path uploadedFile = FileService.getFileDirImages().resolve(FILENAME_IMAGE);
        Files.delete(uploadedFile);
        Path thumbFile = FileService.getThumbDirImages().resolve(FileService.buildThumbnailFileName(FILENAME_IMAGE));
        Files.delete(thumbFile);
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
            .jsonPath("$.[1].mediaType").isEqualTo("image/jpeg");
    }

    @Test
    void test4_uploadVideoFile() throws IOException {

        File file = ResourceUtils.getFile("classpath:testdata/video-640x480-1MB.mp4");
        assertThat(file).isNotNull();
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            var bodyInserter = BodyInserters.fromResource(new InputStreamResource(fileInputStream));
            LOGGER.info("Read upload image from {}", file.getAbsolutePath());
            webTestClient.post()
                .uri("/videos/{file}", FILENAME_VIDEO)
                .contentType(MediaType.parseMediaType("video/mp4"))
                .body(bodyInserter)
                .exchange()
                .expectStatus().isOk()
                .expectBody(MAP)
                .value(value -> assertThat(value).containsExactlyInAnyOrderEntriesOf(Map.of(
                    "success", true
                )));
        }
        Path uploadedFile = FileService.getFileDirVideos().resolve(FILENAME_VIDEO);
        assertThat(uploadedFile).exists().hasSize(EXPECTED_VIDEO_FILE_SIZE);
        Path thumbFile = FileService.getThumbDirVideos().resolve(buildThumbnailFileName(FILENAME_VIDEO));
        assertThat(thumbFile).exists();
        assertThat(Files.size(thumbFile)).isGreaterThan(100L);
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
    }

    @Test
    void test6_listVideoFiles() {

        // act/assert
        webTestClient.get().uri("/video-infos?prefixFilter=video")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(MediaType.APPLICATION_JSON)
            .expectBody()
            .jsonPath("$.[0].fileName").isEqualTo("video-640x480-1MB.mp4")
            .jsonPath("$.[0].sizeInBytes").isEqualTo(1057149L)
            .jsonPath("$.[0].mediaType").isEqualTo("video/mp4");
    }

    @Test
    void test7_createTimelapseVideo() throws JsonProcessingException {

        // arrange
        String wantedFilename = "test.mp4";
        List<String> imageFiles = buildInputFiles();
        TimelapseCommand timelapseCommand = new TimelapseCommand(wantedFilename, imageFiles, 2, 15);
        String jsonPostBody = MAPPER.writeValueAsString(timelapseCommand);
        // act/assert
        TimelapseResult result = webTestClient.post().uri("/video/create-timelapse")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(jsonPostBody)
            .exchange()
            .expectStatus().isOk()
            .expectBody(TimelapseResult.class)
            .returnResult()
            .getResponseBody();
        assertThat(result.success()).isTrue();
        // assert (stored)
        MediaType mp4 = MediaType.parseMediaType("video/mp4");
        webTestClient.get().uri("/videos/{filename}", wantedFilename)
            .accept(mp4)
            .exchange()
            .expectStatus().isOk()
            .expectHeader().contentType(mp4)
            .expectHeader().contentLength(111L);
    }

    @Test
    void test8_renameVideo() {
        String wantedFilename = "test.mp4";
        String newName = "new.mp4";
        webTestClient.put().uri("/videos/{filename}", wantedFilename)
            .bodyValue(newName)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$").isEqualTo("true");
        webTestClient.delete().uri("/videos/{filename}", newName)
            .exchange()
            .expectStatus().isOk()
            .expectBody()
            .jsonPath("$.success").isEqualTo("true");
    }
}