package com.giraone.camera.service.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public record FileInfo(String fileName, Long sizeInBytes, String mediaType, LocalDateTime lastModified, String infos) {

    public String toDisplayShort() {
        return lastModified != null ? DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(lastModified) : "-";
    }

    public boolean isVideo() {
        return mediaType != null && mediaType.startsWith("video");
    }

    public boolean isImage() {
        return mediaType != null && mediaType.startsWith("image");
    }
}
