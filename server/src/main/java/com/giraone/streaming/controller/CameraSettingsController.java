package com.giraone.streaming.controller;

import com.giraone.streaming.service.CameraSettingsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@SuppressWarnings("unused")
@RestController
public class CameraSettingsController {

    private final CameraSettingsService cameraSettingsService;

    public CameraSettingsController(CameraSettingsService cameraSettingsService) {
        this.cameraSettingsService = cameraSettingsService;
    }

    @SuppressWarnings("unused")
    @GetMapping("camera/settings")
    ResponseEntity<Map<String, Object>> fetchSettings() {

        Map<String, Object> settings = cameraSettingsService.getSettings();
        return ResponseEntity.ok(settings);
    }
}
