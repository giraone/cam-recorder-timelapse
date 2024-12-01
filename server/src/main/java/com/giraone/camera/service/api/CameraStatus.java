package com.giraone.camera.service.api;

public record CameraStatus(int rssi,
                           String cameraName,
                           int imageCounter,
                           int imageErrors,
                           int cameraInitCounter,
                           int cameraInitErrors,
                           int uploadImageErrors,
                           int uploadStatusErrors
) {
}
