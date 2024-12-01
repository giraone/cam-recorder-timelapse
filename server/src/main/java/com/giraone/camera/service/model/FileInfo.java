package com.giraone.camera.service.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giraone.imaging.ImagingProvider;
import com.giraone.imaging.java2.ProviderJava2D;
import com.giraone.camera.service.video.VideoService;
import com.giraone.camera.util.ObjectMapperBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static com.giraone.camera.service.FileService.DIR_NAME_META;

public record FileInfo(String fileName, long sizeInBytes, String mediaType, LocalDateTime lastModified, String infos) {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileInfo.class);

    private static final ImagingProvider imagingProvider = new ProviderJava2D();
    private static final VideoService videoService = new VideoService();
    private static final ObjectMapper objectMapper = ObjectMapperBuilder.build();

    public static FileInfo fromFile(File file) {
        final String fileName = file.getName();
        final String mediaType = mediaTypeFromFileName(fileName);
        return new FileInfo(file.getName(), file.length(), mediaType,
            ofEpochSecond(file.lastModified() / 1000),
            mediaType.startsWith("image")
                ? fetchImageInfos(file)
                : fetchVideoInfos(file)
        );
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

    public static String fetchImageInfos(File file) {
        try {
            com.giraone.imaging.FileInfo imagingFileInfo = imagingProvider.fetchFileInfo(file);
            return imagingFileInfo.getWidth() + "x" + imagingFileInfo.getHeight();
        } catch (Exception e) {
            return "?";
        }
    }

    private static String fetchVideoInfos(File file) {

        final String filename = file.getName();
        final String baseName = filename.substring(0, filename.lastIndexOf('.'));
        final File metaFile = new File(new File(file.getParentFile(), DIR_NAME_META), baseName + ".json");
        if (!metaFile.exists()) {
            return fetchVideoInfosFresh(file, metaFile);
        }
        VideoMetaInfo videoMetaInfo = null;
        try {
            videoMetaInfo = objectMapper.readValue(metaFile, VideoMetaInfo.class);
        } catch (IOException e) {
            LOGGER.warn("Cannot parse {}", metaFile, e);
        }
        return videoMetaInfo != null ? videoMetaInfo.toString() : "JSON-Error";
    }

    private static String fetchVideoInfosFresh(File file, File metaFile) {
        VideoMetaInfo videoMetaInfo;
        try {
             videoMetaInfo = videoService.extractVideoMetaInfo(file);
        } catch (IOException e) {
            LOGGER.warn("Cannot extract video meta from " + file, e);
            return "ERROR (write)";
        }
        try {
            objectMapper.writeValue(metaFile, videoMetaInfo);
        } catch (IOException e) {
            LOGGER.warn("Cannot write video meta to " + metaFile, e);
            return "ERROR (write)";
        }
        return videoMetaInfo.toString();
    }

    public static LocalDateTime ofEpochSecond(long epochSecond) {
        final Instant instant = Instant.now();
        final ZoneId systemZone = ZoneId.systemDefault();
        final ZoneOffset currentOffsetForMyZone = systemZone.getRules().getOffset(instant);
        return LocalDateTime.ofEpochSecond(epochSecond, 0, currentOffsetForMyZone);
    }
}