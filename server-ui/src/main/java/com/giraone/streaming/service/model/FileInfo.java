package com.giraone.streaming.service.model;

import org.springframework.http.MediaType;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public record FileInfo(String fileName, long sizeInBytes, String mediaType, LocalDateTime lastModified) {

    public String toDisplayShort() {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(lastModified);
    }

    public static FileInfo fromFile(File file) {
        final String fileName = file.getName();
        return new FileInfo(file.getName(), file.length(), mediaTypeFromFileName(fileName), ofEpochSecond(file.lastModified() / 1000));
    }

    public static String mediaTypeFromFileName(String filename) {

        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG_VALUE;
        } else if (filename.endsWith(".png")) {
            return MediaType.IMAGE_PNG_VALUE;
        } else {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }

    public static LocalDateTime ofEpochSecond(long epochSecond) {
        final Instant instant = Instant.now();
        final ZoneId systemZone = ZoneId.systemDefault();
        final ZoneOffset currentOffsetForMyZone = systemZone.getRules().getOffset(instant);
        return LocalDateTime.ofEpochSecond(epochSecond, 0, currentOffsetForMyZone);
    }
}
