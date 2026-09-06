package com.giraone.camera.service.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import tools.jackson.databind.annotation.JsonSerialize;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record CameraStatus(String cameraName,
                           int rssi,
                           int imageCounter,
                           int imageErrors,
                           int cameraInitCounter,
                           int cameraInitErrors,
                           int uploadImageErrors,
                           int uploadStatusErrors
                           ) {
}
