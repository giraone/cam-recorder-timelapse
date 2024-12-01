package com.giraone.camera.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giraone.camera.service.FileService;
import com.giraone.camera.service.api.CameraStatus;
import com.giraone.camera.service.api.Settings;
import com.giraone.camera.service.api.Status;
import com.giraone.camera.service.model.FileInfo;
import com.giraone.camera.service.model.FileInfoAndContent;
import com.giraone.camera.service.model.FileInfoOrder;
import com.giraone.camera.service.model.FileInfoQuery;
import com.giraone.camera.service.video.model.TimelapseCommand;
import com.giraone.camera.service.video.model.TimelapseResult;
import com.giraone.camera.util.ObjectMapperBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@RestController
public class CameraController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CameraController.class);

    private static final File SETTINGS_FILE = new File("../camera-settings.json");
    private static final Path SETTINGS_FILE_PATH = SETTINGS_FILE.toPath();
    private static final String X_HEADER_ERROR = "X-Files-Error";

    private static final ObjectMapper objectMapper = ObjectMapperBuilder.build();

    // Cached settings
    private Settings currentSettings = new Settings();
    private String cameraSettingsJsonString;
    private boolean cameraSettingsChanged = false;

    private final FileService fileService;

    @SuppressWarnings("unused")
    public CameraController(FileService fileService) {
        this.fileService = fileService;
        try {
            final String content = Files.readString(SETTINGS_FILE_PATH);
            currentSettings = objectMapper.readValue(content, Settings.class);
            cameraSettingsJsonString = objectMapper.writeValueAsString(currentSettings.getCamera());
        } catch (Exception e) {
           LOGGER.error("Cannot read settings file \"{}\"! Using default", SETTINGS_FILE_PATH, e);
        }
        cameraSettingsJsonString = getCameraSettingsJson(currentSettings);
    }

    //-- SETTINGS / STATUS ---------------------------------------------------------------------------------------------

    @SuppressWarnings("unused")
    @GetMapping("settings")
    ResponseEntity<Settings> getSettings() {
        LOGGER.info("Passing settings: {}", currentSettings);
        return ResponseEntity.ok(currentSettings);
    }

    @PutMapping("settings")
    ResponseEntity<Status> storeSettings(@RequestBody Settings settings) {
        LOGGER.info("Storing settings: {}", settings);
        currentSettings = settings;
        final String newCameraSettings = getCameraSettingsJson(currentSettings);
        if (!cameraSettingsJsonString.equals(newCameraSettings)) {
            cameraSettingsChanged = true;
        }
        try {
            String content = objectMapper.writeValueAsString(settings);
            Files.writeString(SETTINGS_FILE_PATH, content);
        } catch (Exception e) {
            LOGGER.error("Cannot write settings file \"{}\"!", SETTINGS_FILE_PATH, e);
            return ResponseEntity.ok(new Status(false, e.getMessage()));
        }
        return ResponseEntity.ok(new Status(true, null));
    }

    @SuppressWarnings("unused")
    @PutMapping("status")
    ResponseEntity<Settings> uploadStatus(@RequestBody CameraStatus status) {
        LOGGER.info("Camera status = {}", status);
        final Settings settingsToReturn = new Settings(currentSettings.getStatus(), currentSettings.getWorkflow(), null);
        if (cameraSettingsChanged || status.imageCounter() == 0) {
            LOGGER.info("Forcing to re-initialize camera settings.");
            // return camera settings, when they were changed or no image was taken yet
            settingsToReturn.setCamera(currentSettings.getCamera());
            cameraSettingsChanged = false;
        }
        return ResponseEntity.ok(settingsToReturn);
    }

    //-- IMAGES --------------------------------------------------------------------------------------------------------

    @SuppressWarnings("unused")
    @PostMapping(value = "images/{filename}", consumes = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    Mono<ResponseEntity<Settings>> uploadImage(@PathVariable String filename,
                                                   @RequestBody Flux<ByteBuffer> content,
                                                   @RequestHeader("Content-Length") Optional<String> contentLengthString) {

        final Settings settingsToReturn = new Settings(currentSettings.getStatus(), currentSettings.getWorkflow(), null);
        final long contentLength = contentLengthString.orElse("-1").transform(Long::parseLong);
        return fileService.storeFile(FileService.Media.IMAGES, filename, content, contentLength)
            .map(fileInfo -> {
                    if (cameraSettingsChanged) {
                        LOGGER.info("Forcing to re-initialize camera settings.");
                        settingsToReturn.setCamera(currentSettings.getCamera()); // return camera settings, when they were changed
                        cameraSettingsChanged = false;
                    }
                return ResponseEntity.ok(settingsToReturn);
            })
            .onErrorResume(IllegalArgumentException.class, iae -> Mono.just(ResponseEntity.badRequest()
                .body(new Settings(new Status(false, iae.getMessage())))))
            .onErrorResume(Exception.class, exc -> Mono.just(ResponseEntity.status(HttpStatusCode.valueOf(503))
                .body(new Settings(new Status(false, exc.getMessage())))));
    }

    @SuppressWarnings("unused")
    @GetMapping("images/{filename}")
    ResponseEntity<Flux<ByteBuffer>> downloadImage(@PathVariable String filename) {
        return downloadOriginal(FileService.Media.IMAGES, filename);
    }

    @SuppressWarnings("unused")
    @GetMapping("image-thumbs/{filename}")
    ResponseEntity<Flux<ByteBuffer>> downloadImageThumb(@PathVariable String filename) {
        return downloadThumb(FileService.Media.IMAGES, filename);
    }

    @SuppressWarnings("unused")
    @PutMapping("images/{filename}")
    ResponseEntity<Status> renameImage(@PathVariable String filename, @RequestBody String newName) {
        LOGGER.debug("renameImage {} {}", filename, newName);
        Status ret = fileService.rename(FileService.Media.IMAGES, filename, newName);
        return ret.success()
            ? ResponseEntity.ok(ret)
            : ResponseEntity.unprocessableEntity().body(ret);
    }

    @SuppressWarnings("unused")
    @DeleteMapping("images/{filename}")
    ResponseEntity<Status> deleteImage(@PathVariable String filename) {
        LOGGER.debug("deleteImage {}", filename);
        Status ret = fileService.delete(FileService.Media.IMAGES, filename);
        return ret.success()
            ? ResponseEntity.ok(ret)
            : ResponseEntity.unprocessableEntity().body(ret);
    }

    @SuppressWarnings("unused")
    @GetMapping("image-infos")
    List<FileInfo> listImageFiles(
        @RequestParam(required = false) String prefixFilter,
        @RequestParam(required = false, defaultValue = "0") int offset,
        @RequestParam(required = false, defaultValue = "50") int limit,
        @RequestParam(required = false, defaultValue = "fileName") String orderAttribute,
        @RequestParam(required = false, defaultValue = "false") boolean orderDesc
    ) {
        return fileService.listFileInfos(FileService.Media.IMAGES,
            new FileInfoQuery(prefixFilter, offset, limit, new FileInfoOrder(orderAttribute, orderDesc)));
    }

    @SuppressWarnings("unused")
    @GetMapping("image-count")
    int countImageFiles(
        @RequestParam(required = false) String prefixFilter,
        @RequestParam(required = false, defaultValue = "0") int offset,
        @RequestParam(required = false, defaultValue = "50") int limit
    ) {
        return fileService.countFileInfos(FileService.Media.IMAGES,
            new FileInfoQuery(prefixFilter, offset, limit, null));
    }

    @SuppressWarnings("unused")
    @GetMapping("image/rebuild-thumbnails")
    int rebuildImageThumbnails() {
        return fileService.rebuildThumbnails(FileService.Media.IMAGES);
    }

    @SuppressWarnings("unused")
    @GetMapping("image/rebuild-meta")
    int rebuildImageMeta() {
        return fileService.rebuildMeta(FileService.Media.IMAGES);
    }

    @SuppressWarnings("unused")
    @PostMapping("image/download-as-zip")
    Flux<ByteBuffer> downloadImagesAsZip(@RequestBody Flux<String> fileNames) {
        return fileService.downloadImagesAsZip(fileNames);
    }

    //-- VIDEOS --------------------------------------------------------------------------------------------------------

    @SuppressWarnings("unused")
    @PostMapping(value = "videos/{filename}", consumes = {"video/mp4"})
    Mono<ResponseEntity<Status>> uploadVideo(@PathVariable String filename,
                                               @RequestBody Flux<ByteBuffer> content,
                                               @RequestHeader("Content-Length") Optional<String> contentLengthString) {

        final long contentLength = contentLengthString.orElse("-1").transform(Long::parseLong);
        return fileService.storeFile(FileService.Media.VIDEOS, filename, content, contentLength)
            .map(fileInfo -> ResponseEntity.ok(new Status(true, null)))
            .onErrorResume(IllegalArgumentException.class, iae -> Mono.just(ResponseEntity.badRequest()
                .body(new Status(false, iae.getMessage()))))
            .onErrorResume(Exception.class, exc -> Mono.just(ResponseEntity.status(HttpStatusCode.valueOf(503))
                .body(new Status(false, exc.getMessage()))));
    }

    @SuppressWarnings("unused")
    @GetMapping("videos/{filename}")
    ResponseEntity<Flux<ByteBuffer>> downloadVideo(@PathVariable String filename) {
        return downloadOriginal(FileService.Media.VIDEOS, filename);
    }

    @SuppressWarnings("unused")
    @GetMapping("video-thumbs/{filename}")
    ResponseEntity<Flux<ByteBuffer>> downloadVideoThumb(@PathVariable String filename) {
        return downloadThumb(FileService.Media.VIDEOS, filename);
    }

    @SuppressWarnings("unused")
    @PutMapping("videos/{filename}")
    ResponseEntity<Status> renameVideo(@PathVariable String filename, @RequestBody String newName) {
        LOGGER.debug("renameVideo {} {}", filename, newName);
        Status ret = fileService.rename(FileService.Media.VIDEOS, filename, newName);
        return ret.success()
            ? ResponseEntity.ok(ret)
            : ResponseEntity.unprocessableEntity().body(ret);
    }

    @SuppressWarnings("unused")
    @DeleteMapping("videos/{filename}")
    ResponseEntity<Status> deleteVideo(@PathVariable String filename) {
        LOGGER.debug("deleteVideo {}", filename);
        Status ret = fileService.delete(FileService.Media.VIDEOS, filename);
        return ret.success()
            ? ResponseEntity.ok(ret)
            : ResponseEntity.unprocessableEntity().body(ret);
    }

    @SuppressWarnings("unused")
    @GetMapping("video-infos")
    List<FileInfo> listVideoFiles(
        @RequestParam(required = false) String prefixFilter,
        @RequestParam(required = false, defaultValue = "0") int offset,
        @RequestParam(required = false, defaultValue = "50") int limit,
        @RequestParam(required = false, defaultValue = "fileName") String orderAttribute,
        @RequestParam(required = false, defaultValue = "false") boolean orderDesc
    ) {
        return fileService.listFileInfos(FileService.Media.VIDEOS,
            new FileInfoQuery(prefixFilter, offset, limit, new FileInfoOrder(orderAttribute, orderDesc)));
    }

    @SuppressWarnings("unused")
    @GetMapping("video-count")
    int countVideoFiles(
        @RequestParam(required = false) String prefixFilter,
        @RequestParam(required = false, defaultValue = "0") int offset,
        @RequestParam(required = false, defaultValue = "50") int limit
    ) {
        return fileService.countFileInfos(FileService.Media.VIDEOS,
            new FileInfoQuery(prefixFilter, offset, limit, null));
    }

    @SuppressWarnings("unused")
    @PostMapping(value = "video/create-timelapse")
    Mono<TimelapseResult> createTimelapseVideo(@RequestBody TimelapseCommand timelapseCommand) {
        return fileService.createTimelapseVideo(timelapseCommand);
    }

    @SuppressWarnings("unused")
    @GetMapping("video/rebuild-thumbnails")
    int rebuildVideoThumbnails() {
        return fileService.rebuildThumbnails(FileService.Media.VIDEOS);
    }

    @SuppressWarnings("unused")
    @GetMapping("video/rebuild-meta")
    int rebuildVideoMeta() {
        return fileService.rebuildMeta(FileService.Media.VIDEOS);
    }

    //------------------------------------------------------------------------------------------------------------------

    private ResponseEntity<Flux<ByteBuffer>> downloadOriginal(FileService.Media type, String filename) {

        final FileInfoAndContent fileInfoAndContent;
        try {
            fileInfoAndContent = fileService.downloadFile(type, filename);
        } catch (NoSuchFileException nsfe) {
            return ResponseEntity.notFound().header(X_HEADER_ERROR, nsfe.getMessage()).build();
        } catch (IOException ioe) {
            return ResponseEntity.badRequest().header(X_HEADER_ERROR, ioe.getMessage()).build();
        }
        final String mediaType = fileInfoAndContent.fileInfo().mediaType();
        final long contentLength = fileInfoAndContent.fileInfo().sizeInBytes();
        return streamToWebClient(fileInfoAndContent.content(), mediaType, contentLength);
    }

    private ResponseEntity<Flux<ByteBuffer>> downloadThumb(FileService.Media type, String filename) {

        final FileInfoAndContent fileInfoAndContent;
        try {
            fileInfoAndContent = fileService.downloadThumb(type, filename);
        } catch (NoSuchFileException nsfe) {
            return ResponseEntity.notFound().header(X_HEADER_ERROR, nsfe.getMessage()).build();
        } catch (IOException ioe) {
            return ResponseEntity.badRequest().header(X_HEADER_ERROR, ioe.getMessage()).build();
        }
        final String mediaType = fileInfoAndContent.fileInfo().mediaType();
        final long contentLength = fileInfoAndContent.fileInfo().sizeInBytes();
        return streamToWebClient(fileInfoAndContent.content(), mediaType, contentLength);
    }

    private static ResponseEntity<Flux<ByteBuffer>> streamToWebClient(Flux<ByteBuffer> content, String mediaType, long contentLength) {

        return ResponseEntity
            .ok()
            .header("Content-Type", mediaType)
            .header("Content-Length", Long.toString(contentLength))
            .body(content);
    }

    private static String getCameraSettingsJson(Settings settings) {
        try {
            return objectMapper.writeValueAsString(settings.getCamera());
        } catch (Exception e) {
            LOGGER.error("Cannot write camera settings!", e);
            return null;
        }
    }
}
