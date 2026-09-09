package com.giraone.camera.service.api;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpHeaders;
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

    /** Prefix of the HTTP headers, with which the camera passes its status on every upload */
    public static final String HEADER_PREFIX = "cam-status-";
    public static final String HEADER_CAMERA_NAME = HEADER_PREFIX + "camera-name";
    public static final String HEADER_RSSI = HEADER_PREFIX + "rssi";
    public static final String HEADER_IMAGE_COUNTER = HEADER_PREFIX + "image-counter";
    public static final String HEADER_IMAGE_ERRORS = HEADER_PREFIX + "image-errors";
    public static final String HEADER_CAMERA_INIT_COUNTER = HEADER_PREFIX + "camera-init-counter";
    public static final String HEADER_CAMERA_INIT_ERRORS = HEADER_PREFIX + "camera-init-errors";
    public static final String HEADER_UPLOAD_IMAGE_ERRORS = HEADER_PREFIX + "upload-image-errors";
    public static final String HEADER_UPLOAD_STATUS_ERRORS = HEADER_PREFIX + "upload-status-errors";

    /**
     * Build the camera status from the request headers of an upload.
     * @param headers the headers of the request
     * @return the status or null, if the request carries no camera name, e.g. because it
     *         does not come from a camera at all. Missing numeric headers count as 0.
     */
    public static @Nullable CameraStatus fromHeaders(HttpHeaders headers) {

        final String cameraName = headers.getFirst(HEADER_CAMERA_NAME);
        if (cameraName == null || cameraName.isBlank()) {
            return null;
        }
        return new CameraStatus(
            cameraName,
            intHeader(headers, HEADER_RSSI),
            intHeader(headers, HEADER_IMAGE_COUNTER),
            intHeader(headers, HEADER_IMAGE_ERRORS),
            intHeader(headers, HEADER_CAMERA_INIT_COUNTER),
            intHeader(headers, HEADER_CAMERA_INIT_ERRORS),
            intHeader(headers, HEADER_UPLOAD_IMAGE_ERRORS),
            intHeader(headers, HEADER_UPLOAD_STATUS_ERRORS)
        );
    }

    private static int intHeader(HttpHeaders headers, String name) {

        final String value = headers.getFirst(name);
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException nfe) {
            return 0;
        }
    }
}
