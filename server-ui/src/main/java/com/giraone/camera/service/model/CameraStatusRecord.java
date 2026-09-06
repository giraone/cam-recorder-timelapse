package com.giraone.camera.service.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;

/**
 * One status record of a camera, as it is returned by the backend endpoint {@code status/{cameraName}}.
 * The attributes mirror {@code com.giraone.camera.service.model.CameraStatusRecord} of the server module.
 *
 * @param timestamp          point in time, when the status was received by the server
 * @param rssi               WiFi signal strength of the camera in dBm
 * @param imageCounter       number of images taken by the camera since its last restart
 * @param imageErrors        number of failed attempts to take an image
 * @param cameraInitCounter  number of camera initializations since the last restart
 * @param cameraInitErrors   number of failed camera initializations
 * @param uploadImageErrors  number of failed image uploads
 * @param uploadStatusErrors number of failed status uploads
 */
public record CameraStatusRecord(LocalDateTime timestamp,
                                 int rssi,
                                 int imageCounter,
                                 int imageErrors,
                                 int cameraInitCounter,
                                 int cameraInitErrors,
                                 int uploadImageErrors,
                                 int uploadStatusErrors) {

    /**
     * Orders the records by {@link #timestamp} in descending order, so the most recent status comes first.
     * Records without a timestamp are placed at the end.
     */
    public static final Comparator<CameraStatusRecord> BY_TIMESTAMP_DESCENDING =
        Comparator.comparing(CameraStatusRecord::timestamp, Comparator.nullsLast(Comparator.reverseOrder()));

    /**
     * @return the timestamp as an ISO-8601 local date time or {@code "-"}, if there is no timestamp
     */
    public String timestampToDisplay() {
        return timestamp != null ? DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(timestamp) : "-";
    }
}
