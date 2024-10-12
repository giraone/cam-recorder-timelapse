package com.giraone.streaming.service.model;

import org.springframework.http.MediaType;

import java.io.File;

public record FileInfo(String fileName, long sizeInBytes, String mediaType) {

    public static FileInfo fromFile(File file) {
        final String fileName = file.getName();
        return new FileInfo(file.getName(), file.length(), mediaTypeFromFileName(fileName));
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
}
