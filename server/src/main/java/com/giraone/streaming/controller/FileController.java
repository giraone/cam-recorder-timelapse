package com.giraone.streaming.controller;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.giraone.streaming.service.FileService;
import com.giraone.streaming.service.model.FileInfo;
import com.giraone.streaming.service.model.FileInfoAndContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
import java.util.regex.Pattern;

@RestController
public class FileController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileController.class);

    private static final String X_HEADER_ERROR = "X-Files-Error";
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9-]+[.][a-z]{3,4}");
    private static final int RESTART_EVERY_PHOTO = 10;

    private final AtomicInteger photosStored = new AtomicInteger(0);

    private final FileService fileService;

    public FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @SuppressWarnings("unused")
    @PostMapping(value = "files/images/{filename}", consumes = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    Mono<ResponseEntity<UploadStatus>> uploadFile(@PathVariable String filename,
                                                  @RequestBody Flux<ByteBuffer> content,
                                                  @RequestHeader("Content-Length") Optional<String> contentLengthString) {

        long contentLength = contentLengthString.orElse("-1").transform(Long::parseLong);
        return fileService.storeFile(filename, content, contentLength)
            .map(fileInfo -> {
                final int newNumberStored = photosStored.getAndIncrement();
                final boolean restartNow;
                if (newNumberStored >= (RESTART_EVERY_PHOTO - 1)) {
                    restartNow = true;
                    LOGGER.info("Forcing restart after {} photos.", RESTART_EVERY_PHOTO);
                    photosStored.set(0);
                } else {
                    restartNow = false;
                }
                return ResponseEntity.ok(new UploadStatus(true, contentLength, restartNow, null));
            })
            .onErrorResume(IllegalArgumentException.class, exc -> Mono.just(ResponseEntity.badRequest()
                .body(new UploadStatus(false, contentLength, true, exc.getMessage()))))
            .onErrorResume(Exception.class, exc -> Mono.just(ResponseEntity.status(HttpStatusCode.valueOf(503))
                .body(new UploadStatus(false, contentLength, true, exc.getMessage()))));
    }

    @SuppressWarnings("unused")
    @GetMapping("files/images/{filename}")
    ResponseEntity<Flux<ByteBuffer>> downloadFile(@PathVariable String filename) {

        final FileInfoAndContent fileInfoAndContent;
        try {
            fileInfoAndContent = fileService.downloadFile(filename);
        } catch (NoSuchFileException nsfe) {
            return ResponseEntity.notFound().header(X_HEADER_ERROR, nsfe.getMessage()).build();
        } catch (IOException ioe) {
            return ResponseEntity.badRequest().header(X_HEADER_ERROR, ioe.getMessage()).build();
        }
        final String mediaType = fileInfoAndContent.fileInfo().mediaType();
        final long contentLength = fileInfoAndContent.fileInfo().sizeInBytes();
        return streamToWebClient(fileInfoAndContent.content(), mediaType, contentLength);
    }

    @SuppressWarnings("unused")
    @GetMapping("files/infos")
    List<FileInfo> listFiles(@RequestParam String filter) {
        return fileService.listFileInfos(filter);
    }

    //------------------------------------------------------------------------------------------------------------------

    private static ResponseEntity<Flux<ByteBuffer>> streamToWebClient(Flux<ByteBuffer> content, String mediaType, long contentLength) {

        return ResponseEntity
            .ok()
            .header("Content-Type", mediaType)
            .header("Content-Length", Long.toString(contentLength))
            .body(content);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record UploadStatus(boolean success, long size, boolean restart, String error) {
    }
}
