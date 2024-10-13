package com.giraone.streaming.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@SuppressWarnings("unused")
@RestController
public class CameraSettingsController {

    private static final Logger LOGGER = LoggerFactory.getLogger(CameraSettingsController.class);
    private static final Path SETTINGS_FILE_PATH = new File("../camera-settings.json").toPath();

    @SuppressWarnings("unused")
    @GetMapping("camera-settings")
    ResponseEntity<String> fetchSettings() {

        try {
            final String content = Files.readString(SETTINGS_FILE_PATH);
            return ResponseEntity.ok(content);
        } catch (IOException e) {
            LOGGER.warn("Cannot read \"{}\"", SETTINGS_FILE_PATH, e);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

    }
}
