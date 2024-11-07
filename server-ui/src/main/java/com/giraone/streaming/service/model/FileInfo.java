package com.giraone.streaming.service.model;

import com.giraone.imaging.ImagingProvider;
import com.giraone.imaging.java2.ProviderJava2D;
import org.springframework.http.MediaType;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public record FileInfo(String fileName, long sizeInBytes, String mediaType, LocalDateTime lastModified, String resolution) {

    private static final ImagingProvider imagingProvider = new ProviderJava2D();

    public String toDisplayShort() {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(lastModified);
    }

    public boolean isVideo() {
        return mediaType.startsWith("video");
    }

    public boolean isImage() {
        return mediaType.startsWith("image");
    }

    public static FileInfo fromFile(File file) {
        final String fileName = file.getName();
        final String mediaType = mediaTypeFromFileName(fileName);
        return new FileInfo(file.getName(), file.length(), mediaType,
            ofEpochSecond(file.lastModified() / 1000), mediaType.startsWith("image") ? fetchImageResolution(file) : "Video");
    }

    public static String mediaTypeFromFileName(String filename) {

        if (filename.endsWith(".jpg") || filename.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG_VALUE;
        } else if (filename.endsWith(".png")) {
            return MediaType.IMAGE_PNG_VALUE;
        } else if (filename.endsWith(".mp4")) {
            return "video/mp4";
        } else {
            return MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
    }

    public static String fetchImageResolution(File file) {
        try {
            com.giraone.imaging.FileInfo imagingFileInfo = imagingProvider.fetchFileInfo(file);
            return imagingFileInfo.getWidth() + "x" + imagingFileInfo.getHeight();
        } catch (Exception e) {
            return "?";
        }
    }

    public static LocalDateTime ofEpochSecond(long epochSecond) {
        final Instant instant = Instant.now();
        final ZoneId systemZone = ZoneId.systemDefault();
        final ZoneOffset currentOffsetForMyZone = systemZone.getRules().getOffset(instant);
        return LocalDateTime.ofEpochSecond(epochSecond, 0, currentOffsetForMyZone);
    }
}
