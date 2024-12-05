package com.giraone.camera.service.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giraone.camera.service.video.VideoService;
import com.giraone.camera.util.ObjectMapperBuilder;
import com.giraone.imaging.ImagingProvider;
import com.giraone.imaging.java2.ProviderJava2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;

import static com.giraone.camera.service.FileService.DIR_NAME_META;

public record FileInfo(String fileName, long sizeInBytes, String mediaType, LocalDateTime lastModified, String infos) {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileInfo.class);

    private static final ImagingProvider imagingProvider = new ProviderJava2D();
    private static final VideoService videoService = new VideoService();
    private static final ObjectMapper objectMapper = ObjectMapperBuilder.build();

    public static FileInfo fromFile(Path file) {
        return FileInfo.fromFile(file, size(file));
    }

    public static FileInfo fromFile(Path file, long size) {
        return FileInfo.fromFile(file, size, lastModified(file));
    }

    public static FileInfo fromFile(Path file, long size, LocalDateTime lastModified) {
        final String fileName = file.getFileName().toString();
        final String mediaType = mediaTypeFromFileName(fileName);
        return new FileInfo(file.getFileName().toString(), size, mediaType,
            lastModified,
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

    public static String fetchImageInfos(Path file) {
        try {
            com.giraone.imaging.FileInfo imagingFileInfo = imagingProvider.fetchFileInfo(file.toFile());
            return imagingFileInfo.getWidth() + "x" + imagingFileInfo.getHeight();
        } catch (Exception e) {
            return "?";
        }
    }

    private static String fetchVideoInfos(Path file) {

        final String filename = file.getFileName().toString();
        final String baseName = filename.substring(0, filename.lastIndexOf('.'));
        final Path metaFile = file.getParent().resolve(DIR_NAME_META).resolve(baseName + ".json");
        if (!Files.exists(metaFile)) {
            return fetchVideoInfosFresh(file, metaFile);
        }
        VideoMetaInfo videoMetaInfo = null;
        try {
            videoMetaInfo = objectMapper.readValue(metaFile.toFile(), VideoMetaInfo.class);
        } catch (IOException e) {
            LOGGER.warn("Cannot parse {}", metaFile, e);
        }
        return videoMetaInfo != null ? videoMetaInfo.toString() : "JSON-Error";
    }

    private static String fetchVideoInfosFresh(Path file, Path metaFile) {
        VideoMetaInfo videoMetaInfo;
        try {
             videoMetaInfo = videoService.extractVideoMetaInfo(file);
        } catch (IOException e) {
            LOGGER.warn("Cannot extract video meta from " + file, e);
            return "ERROR (write)";
        }
        try {
            objectMapper.writeValue(metaFile.toFile(), videoMetaInfo);
        } catch (IOException e) {
            LOGGER.warn("Cannot write video meta to " + metaFile, e);
            return "ERROR (write)";
        }
        return videoMetaInfo.toString();
    }

    public static int size(Path file) {
        try {
            return (int) Files.size(file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static LocalDateTime lastModified(Path file) {
        try {
            return ofEpochSecond(Files.getLastModifiedTime(file).to(TimeUnit.SECONDS));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static LocalDateTime ofEpochSecond(long epochSecond) {
        final Instant instant = Instant.now();
        final ZoneId systemZone = ZoneId.systemDefault();
        final ZoneOffset currentOffsetForMyZone = systemZone.getRules().getOffset(instant);
        return LocalDateTime.ofEpochSecond(epochSecond, 0, currentOffsetForMyZone);
    }
}
