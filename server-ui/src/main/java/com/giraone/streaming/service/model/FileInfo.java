package com.giraone.streaming.service.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record FileInfo(String fileName, long sizeInBytes, String mediaType, LocalDateTime lastModified, String infos) {

    public String toDisplayShort() {
        return DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(lastModified);
    }

    public boolean isVideo() {
        return mediaType.startsWith("video");
    }

    public boolean isImage() {
        return mediaType.startsWith("image");
    }
}
