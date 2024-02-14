package com.giraone.streaming.controller;

import com.giraone.streaming.service.FluxUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.NoSuchFileException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import static java.nio.file.StandardOpenOption.*;

@RestController
public class FileController {

    public static final File FILE_BASE = new File("FILES");
    public static final String X_HEADER_ERROR = "X-Files-Error";

    private static final Logger LOGGER = LoggerFactory.getLogger(FileController.class);
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9-]+[.][a-z]{3,4}");
    private static final String ATTR_SUCCESS = "success";
    private static final String ATTR_SIZE = "size";
    private static final String ATTR_ERROR = "error";

    @SuppressWarnings("unused")
    @PostMapping(value = "files/images/{filename}", consumes = {MediaType.IMAGE_JPEG_VALUE, MediaType.IMAGE_PNG_VALUE})
    Mono<ResponseEntity<Map<String, Object>>> uploadFile(@PathVariable String filename,
                                                         @RequestBody Flux<ByteBuffer> content,
                                                         @RequestHeader("Content-Length") Optional<String> contentLength) {

        if (isFileNameInvalid(filename)) {
            return Mono.just(ResponseEntity.badRequest().body(Map.of(ATTR_SUCCESS, false, ATTR_ERROR, "Invalid target filename!")));
        }
        final File file = new File(FILE_BASE, filename);
        final AsynchronousFileChannel channel;
        try {
            channel = AsynchronousFileChannel.open(file.toPath(), CREATE, WRITE);
        } catch (IOException e) {
            LOGGER.warn("Cannot open file to write to \"{}\"!", file.getAbsolutePath(), e);
            return Mono.just(ResponseEntity.badRequest().body(Map.of(ATTR_SUCCESS, false, ATTR_ERROR, "Cannot store file!")));
        }
        AtomicLong writtenBytes = new AtomicLong(0L);
        return FluxUtil.writeFile(content, channel)
            .doOnSuccess(voidIgnore -> {
                try {
                    channel.close();
                    writtenBytes.set(file.length());
                    LOGGER.warn("File \"{}\" with {} bytes written.", file.getAbsolutePath(), writtenBytes.get());
                } catch (IOException e) {
                    LOGGER.warn("Cannot close file \"{}\"!", file.getAbsolutePath(), e);
                }
            })
            .thenReturn(ResponseEntity.ok(Map.of(
                ATTR_SUCCESS, true,
                ATTR_SIZE, contentLength.orElse("-1").transform(Long::parseLong)
            )));
    }

    @SuppressWarnings("unused")
    @GetMapping("files/images/{filename}")
    ResponseEntity<Flux<ByteBuffer>> downloadFile(@PathVariable String filename) {

        if (isFileNameInvalid(filename)) {
            return ResponseEntity.badRequest().header(X_HEADER_ERROR, "Invalid download filename!").build();
        }
        final File file = new File(FILE_BASE, filename);
        final MediaType mediaType = mediaTypeFromFileName(filename);
        final long contentLength = file.length();
        final AsynchronousFileChannel channel;
        try {
            channel = AsynchronousFileChannel.open(file.toPath(), READ);
        } catch (NoSuchFileException nsfe) {
            LOGGER.warn("File \"{}\" does not exist! {}", file.getAbsolutePath(), nsfe.getMessage());
            return ResponseEntity.notFound().header(X_HEADER_ERROR, "File does not exist!").build();
        } catch (IOException e) {
            LOGGER.warn("Cannot open file to read from \"{}\"! {}", file.getAbsolutePath(), e.getMessage());
            return ResponseEntity.badRequest().header(X_HEADER_ERROR, "Cannot read file!").build();
        }
        final Flux<ByteBuffer> content = FluxUtil.readFile(channel);
        return streamToWebClient(content, mediaType.toString(), contentLength);
    }

    //------------------------------------------------------------------------------------------------------------------

    private static boolean isFileNameInvalid(String filename) {
        return !FILE_NAME_PATTERN.matcher(filename).matches();
    }

    private static MediaType mediaTypeFromFileName(String filename) {

        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        } else  if (filename.endsWith(".png")) {
            return MediaType.IMAGE_JPEG;
        } else {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private static ResponseEntity<Flux<ByteBuffer>> streamToWebClient(Flux<ByteBuffer> content, String mediaType, long contentLength) {

        return ResponseEntity
            .ok()
            .header("Content-Type", mediaType)
            .header("Content-Length", Long.toString(contentLength))
            .body(content);
    }
}
