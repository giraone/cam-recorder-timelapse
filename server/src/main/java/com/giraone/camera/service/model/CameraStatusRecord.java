package com.giraone.camera.service.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import tools.jackson.databind.annotation.JsonSerialize;

import java.time.LocalDateTime;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
@JsonSerialize
public record CameraStatusRecord(LocalDateTime timestamp,
                                 int rssi,
                                 int imageCounter,
                                 int imageErrors,
                                 int cameraInitCounter,
                                 int cameraInitErrors,
                                 int uploadImageErrors,
                                 int uploadStatusErrors) {
}
