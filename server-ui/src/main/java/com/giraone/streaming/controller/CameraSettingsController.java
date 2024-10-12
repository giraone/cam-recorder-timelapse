package com.giraone.streaming.controller;

import com.giraone.streaming.service.CameraSettingsService;
import com.giraone.streaming.service.model.CameraSettings;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SuppressWarnings("unused")
@RestController
public class CameraSettingsController {

    private final CameraSettingsService cameraSettingsService;

    public CameraSettingsController(CameraSettingsService cameraSettingsService) {
        this.cameraSettingsService = cameraSettingsService;
    }

    @SuppressWarnings("unused")
    @GetMapping("camera-settings")
    ResponseEntity<CameraSettings> fetchSettings() {

        final CameraSettings settings = cameraSettingsService.getSettings();
        return ResponseEntity.ok(settings);
    }
}
