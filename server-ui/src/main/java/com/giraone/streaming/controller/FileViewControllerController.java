package com.giraone.streaming.controller;

import com.giraone.streaming.service.FileViewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@SuppressWarnings("unused")
@RestController
public class FileViewControllerController {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileViewControllerController.class);

    private final FileViewService fileViewService;

    public FileViewControllerController(FileViewService fileViewService) {
        this.fileViewService = fileViewService;
    }

    @SuppressWarnings("unused")
    @GetMapping("api/thumbs/{filename}")
    ResponseEntity<byte[]> streamThumb(@PathVariable String filename) {

        final File file = new File(fileViewService.getThumbDir(), filename);
        try {
            final byte[] content = Files.readAllBytes(file.toPath());
            return ResponseEntity.ok(content);
        } catch (IOException e) {
            LOGGER.warn("Cannot read \"{}\"", file, e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
    }
}
