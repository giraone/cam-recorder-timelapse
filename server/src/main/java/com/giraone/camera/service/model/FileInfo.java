package com.giraone.camera.service.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.giraone.camera.service.FileService.DIR_NAME_META;

public class FileInfo {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileInfo.class);

    private static final ImagingProvider imagingProvider = new ProviderJava2D();
    private static final VideoService videoService = new VideoService();
    private static final ObjectMapper objectMapper = ObjectMapperBuilder.build();

    private static final Map<String,String> infoCacheImage = new HashMap<>();
    private static final Map<String,String> infoCacheVideo = new HashMap<>();

    @JsonIgnore
    private final Path path;
    private final String fileName;
    private final long sizeInBytes;
    private final String mediaType;
    private final LocalDateTime lastModified;
    private String infos;

    public FileInfo(Path path, String fileName, long sizeInBytes, String mediaType, LocalDateTime lastModified) {
        this.path = path;
        this.fileName = fileName;
        this.sizeInBytes = sizeInBytes;
        this.mediaType = mediaType;
        this.lastModified = lastModified;
        this.infos = "";
    }

    public String getFileName() {
        return fileName;
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }

    public String getMediaType() {
        return mediaType;
    }

    public LocalDateTime getLastModified() {
        return lastModified;
    }

    public String getInfos() {
        return infos;
    }

    public FileInfo buildInfos() {
        if (mediaType.startsWith("image")) {
            infos = fetchImageInfos(fileName, path);
        } else if (mediaType.startsWith("video")) {
            infos = fetchVideoInfos(fileName, path);
        }
        return this;
    }

    public static FileInfo fromFile(Path file) {
        return FileInfo.fromFile(file, size(file));
    }

    public static FileInfo fromFile(Path file, long size) {
        return FileInfo.fromFile(file, size, lastModified(file));
    }

    public static FileInfo fromFile(Path path, long size, LocalDateTime lastModified) {
        final String fileName = path.getFileName().toString();
        return new FileInfo(path, fileName, size, mediaTypeFromFileName(fileName), lastModified);
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

    public static String fetchImageInfos(String filename, Path file) {
        String ret = infoCacheImage.get(filename);
        if (ret != null) {
            return ret;
        }
        try {
            com.giraone.imaging.FileInfo imagingFileInfo = imagingProvider.fetchFileInfo(file.toFile());
            ret = imagingFileInfo.getWidth() + "x" + imagingFileInfo.getHeight();
            infoCacheImage.put(filename, ret);
            LOGGER.info("fetchImageInfos {}={}", filename, ret);
            return ret;
        } catch (Exception e) {
            return "?";
        }
    }

    private static String fetchVideoInfos(String filename, Path file) {

        String ret = infoCacheVideo.get(filename);
        if (ret != null) {
            return ret;
        }
        final String baseName = filename.substring(0, filename.lastIndexOf('.'));
        final Path metaFile = file.getParent().resolve(DIR_NAME_META).resolve(baseName + ".json");
        if (!Files.exists(metaFile)) {
            ret = fetchVideoInfosFresh(file, metaFile);
            infoCacheVideo.put(filename, ret);
            LOGGER.info("fetchVideoInfos {}={}", filename, ret);
            return ret;
        }
        VideoMetaInfo videoMetaInfo = null;
        try {
            videoMetaInfo = objectMapper.readValue(metaFile.toFile(), VideoMetaInfo.class);
        } catch (IOException e) {
            LOGGER.warn("Cannot parse {}", metaFile, e);
        }
        ret = videoMetaInfo != null ? videoMetaInfo.toString() : "JSON-Error";
        infoCacheVideo.put(filename, ret);
        LOGGER.info("fetchImageInfos {}={}", filename, ret);
        return ret;
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
