package com.giraone.streaming.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.giraone.streaming.service.FileService;
import com.giraone.streaming.service.model.FileInfo;
import com.giraone.streaming.service.model.FileInfoAndContent;
import com.giraone.streaming.service.model.FileInfoOrder;
import com.giraone.streaming.service.model.FileInfoQuery;
import com.giraone.streaming.service.video.model.TimelapseCommand;
import com.giraone.streaming.service.video.model.TimelapseResult;
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

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.NoSuchFileException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
public class FileStorageController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileStorageController.class);

    private static final String X_HEADER_ERROR = "X-Files-Error";
    private static final int RESTART_EVERY_PHOTO = 25;

    private final AtomicInteger photosStored = new AtomicInteger(0);
    private long lastSettingsChange = CameraSettingsController.getLastModified();

    private final FileService fileService;

    public FileStorageController(FileService fileService) {
        this.fileService = fileService;
    }

    @SuppressWarnings("unused")
    @PostMapping(value = "images/{filename}", consumes = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    Mono<ResponseEntity<UploadStatus>> uploadImage(@PathVariable String filename,
                                                   @RequestBody Flux<ByteBuffer> content,
                                                   @RequestHeader("Content-Length") Optional<String> contentLengthString) {

        return uploadFile(FileService.Media.IMAGES, filename, content, contentLengthString);
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
    ResponseEntity<FileService.Status> renameImage(@PathVariable String filename, @RequestBody String newName) {
        LOGGER.debug("renameImage {} {}", filename, newName);
        FileService.Status ret = fileService.rename(FileService.Media.IMAGES, filename, newName);
        return ret.success()
            ? ResponseEntity.ok(ret)
            : ResponseEntity.unprocessableEntity().body(ret);
    }

    @SuppressWarnings("unused")
    @DeleteMapping("images/{filename}")
    ResponseEntity<FileService.Status> deleteImage(@PathVariable String filename) {
        LOGGER.debug("deleteImage {}", filename);
        FileService.Status ret = fileService.delete(FileService.Media.IMAGES, filename);
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

    //------------------------------------------------------------------------------------------------------------------

    @SuppressWarnings("unused")
    @PostMapping(value = "videos/{filename}", consumes = {"video/mp4"})
    Mono<ResponseEntity<UploadStatus>> uploadVideo(@PathVariable String filename,
                                                   @RequestBody Flux<ByteBuffer> content,
                                                   @RequestHeader("Content-Length") Optional<String> contentLengthString) {

        return uploadFile(FileService.Media.VIDEOS, filename, content, contentLengthString);
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
    ResponseEntity<FileService.Status> renameVideo(@PathVariable String filename, @RequestBody String newName) {
        LOGGER.debug("renameVideo {} {}", filename, newName);
        FileService.Status ret = fileService.rename(FileService.Media.VIDEOS, filename, newName);
        return ret.success()
            ? ResponseEntity.ok(ret)
            : ResponseEntity.unprocessableEntity().body(ret);
    }

    @SuppressWarnings("unused")
    @DeleteMapping("videos/{filename}")
    ResponseEntity<FileService.Status> deleteVideo(@PathVariable String filename) {
        LOGGER.debug("deleteVideo {}", filename);
        FileService.Status ret = fileService.delete(FileService.Media.VIDEOS, filename);
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

    private Mono<ResponseEntity<UploadStatus>> uploadFile(FileService.Media type,
                                                          String filename,
                                                          Flux<ByteBuffer> content,
                                                          Optional<String> contentLengthString) {

        final long contentLength = contentLengthString.orElse("-1").transform(Long::parseLong);
        return fileService.storeFile(type, filename, content, contentLength)
            .map(fileInfo -> {
                boolean restartNow = false;
                boolean readSettings = false;
                if (type == FileService.Media.IMAGES) {
                    long newLastSettingsChange;
                    final int newNumberStored = photosStored.getAndIncrement();
                    if (newNumberStored >= (RESTART_EVERY_PHOTO - 1)) {
                        restartNow = true;
                        LOGGER.info("Forcing restart after {} photos.", RESTART_EVERY_PHOTO);
                        photosStored.set(0);
                    } else if ((newLastSettingsChange = CameraSettingsController.getLastModified()) > lastSettingsChange) {
                        LOGGER.info("Forcing to read settings.");
                        lastSettingsChange = newLastSettingsChange;
                        readSettings = true;
                    }
                }
                return ResponseEntity.ok(new UploadStatus(true, contentLength, restartNow, readSettings, null));
            })
            .onErrorResume(IllegalArgumentException.class, exc -> Mono.just(ResponseEntity.badRequest()
                .body(new UploadStatus(false, contentLength, true, false, exc.getMessage()))))
            .onErrorResume(Exception.class, exc -> Mono.just(ResponseEntity.status(HttpStatusCode.valueOf(503))
                .body(new UploadStatus(false, contentLength, true, false, exc.getMessage()))));
    }

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

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UploadStatus(boolean success, long size, boolean restartNow, boolean readSettings, String error) {
    }

}
