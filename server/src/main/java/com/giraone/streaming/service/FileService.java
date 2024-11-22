package com.giraone.streaming.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.giraone.imaging.ConversionCommand;
import com.giraone.imaging.ImagingProvider;
import com.giraone.imaging.java2.ProviderJava2D;
import com.giraone.streaming.config.ApplicationProperties;
import com.giraone.streaming.service.model.FileInfo;
import com.giraone.streaming.service.model.FileInfoAndContent;
import com.giraone.streaming.service.model.VideoMetaInfo;
import com.giraone.streaming.service.video.VideoService;
import com.giraone.streaming.service.video.model.TimelapseCommand;
import com.giraone.streaming.service.video.model.TimelapseResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import static java.nio.file.StandardOpenOption.*;

@Service
public class FileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileService.class);

    public static String DIR_NAME_THUMBS = ".thumbs";
    public static String DIR_NAME_META = ".meta";
    public static File STORAGE_BASE = new File("../STORAGE");
    public static File IMAGES_BASE = new File(STORAGE_BASE, "IMAGES");
    public static File IMAGES_THUMBS = new File(IMAGES_BASE, DIR_NAME_THUMBS);
    public static File IMAGES_META = new File(IMAGES_BASE, DIR_NAME_META);
    public static File VIDEOS_BASE = new File(STORAGE_BASE, "VIDEOS");
    public static File VIDEOS_THUMBS = new File(VIDEOS_BASE, DIR_NAME_THUMBS);
    public static File VIDEOS_META = new File(VIDEOS_BASE, DIR_NAME_META);

    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9-]+[.][a-z0-9]{3,4}");

    private final ImagingProvider imagingProvider = new ProviderJava2D();

    private final VideoService videoService;
    private final ApplicationProperties applicationProperties;

    static {
        createDirectory(IMAGES_THUMBS);
        createDirectory(VIDEOS_THUMBS);
        createDirectory(IMAGES_META);
        createDirectory(VIDEOS_META);
    }

    public FileService(VideoService videoService, ApplicationProperties applicationProperties) {
        this.videoService = videoService;
        this.applicationProperties = applicationProperties;
    }

    public Mono<FileInfo> storeFile(Media type, String filename, Flux<ByteBuffer> content, long contentLength) {

        if (isFileNameInvalid(filename)) {
            return Mono.error(new IllegalArgumentException("Invalid filename \"" + filename + "\"!"));
        }
        final File file = getFile(type, filename);
        final AsynchronousFileChannel channel;
        try {
            channel = AsynchronousFileChannel.open(file.toPath(), CREATE, WRITE);
        } catch (IOException ioe) {
            LOGGER.warn("Cannot open file to write to \"{}\"!", file.getAbsolutePath(), ioe);
            return Mono.error(ioe);
        }
        final AtomicLong writtenBytes = new AtomicLong(0L);
        return FluxUtil.writeFile(content, channel)
            .doOnSuccess(voidIgnore -> {
                try {
                    channel.close();
                    writtenBytes.set(file.length());
                    LOGGER.info("File \"{}\" with {} bytes written.", file.getName(), writtenBytes.get());
                    if (contentLength > 0 && contentLength != file.length()) {
                        LOGGER.warn("Content length and file length mismatch {} != {}!", contentLength, file.length());
                    }
                } catch (IOException e) {
                    LOGGER.warn("Cannot close file \"{}\"!", file.getAbsolutePath(), e);
                }
            })
            .doOnSuccess(unused -> {
                if (applicationProperties.isGenerateThumbnails()) {
                    createThumbnail(type, file);
                    LOGGER.debug("Thumbnail for \"{}\" stored.", filename);
                }
            })
            .thenReturn(new FileInfo(filename, writtenBytes.get(),
                FileInfo.mediaTypeFromFileName(filename), FileInfo.ofEpochSecond(file.lastModified()), "?"));
    }

    public FileInfoAndContent downloadFile(Media type, String filename) throws IOException {

        if (isFileNameInvalid(filename)) {
            throw new IllegalArgumentException("Invalid filename \"" + filename + "\"!");
        }
        final File file = new File(getBaseOf(type), filename);
        final AsynchronousFileChannel channel;
        try {
            channel = AsynchronousFileChannel.open(file.toPath(), READ);
        } catch (NoSuchFileException nsfe) {
            LOGGER.warn("File \"{}\" does not exist! {}", file.getAbsolutePath(), nsfe.getMessage());
            throw nsfe;
        } catch (IOException ioe) {
            LOGGER.warn("Cannot open file to read from \"{}\"! {}", file.getAbsolutePath(), ioe.getMessage());
            throw ioe;
        }
        final Flux<ByteBuffer> content = FluxUtil.readFile(channel);
        return new FileInfoAndContent(content, FileInfo.fromFile(file));
    }

    public List<FileInfo> listFileInfos(Media type, String prefixFilter) {
        File[] files = getBaseOf(type).listFiles((dir, name) -> !name.startsWith(".") && prefixFilter != null && name.startsWith(prefixFilter));
        if (files == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(files).map(FileInfo::fromFile).toList();
    }

    public Status rename(Media type, String filename, String newName) {
        if (isFileNameInvalid(filename)) {
            return new Status(false, "Invalid filename \"" + filename + "\"!");
        }
        final File oldFile = new File(getBaseOf(type), filename);
        final File newFile = new File(getBaseOf(type), newName);
        LOGGER.error("Rename \"{}\" to \"{}\"", oldFile, newFile);
        try {
            final boolean ret = oldFile.renameTo(newFile);
            if (ret) {
                final File oldThumbnailFile = buildThumbnailFile(type, filename);
                final File newThumbnailFile = buildThumbnailFile(type, newName);
                LOGGER.error("Rename \"{}\" to \"{}\"", oldThumbnailFile, newThumbnailFile);
                oldThumbnailFile.renameTo(newThumbnailFile);
            }
            return new Status(ret, null);
        } catch (Exception exc) {
            LOGGER.error("Failed to rename \"{}\" to \"{}\"", oldFile, newFile, exc);
            return new Status(false, exc.getMessage());
        }
    }

    public Status delete(Media type, String filename) {
        if (isFileNameInvalid(filename)) {
            return new Status(false, "Invalid filename \"" + filename + "\"!");
        }
        final File file = new File(getBaseOf(type), filename);
        LOGGER.error("Delete \"{}\"", file);
        try {
            final boolean ret = file.delete();
            if (ret) {
                final File thumbnailFile = buildThumbnailFile(type, filename);
                thumbnailFile.delete();
            }
            return new Status(ret, null);
        } catch (Exception exc) {
            LOGGER.error("Failed to delete \"{}\"", file, exc);
            return new Status(false, exc.getMessage());
        }
    }

    public int rebuildThumbnails(Media type) {
        File[] files = getBaseOf(type).listFiles((dir, name) -> !name.startsWith("."));
        if (files == null) {
            return 0;
        }
        AtomicInteger ret = new AtomicInteger(0);
        Arrays.stream(files).forEach(file -> {
            LOGGER.info("Creating thumbnail for {}", file.getAbsolutePath());
            if (createThumbnail(type, file)) {
                ret.getAndIncrement();
            }
        });
        return ret.get();
    }

    public int rebuildMeta(Media type) {
        File[] files = getBaseOf(type).listFiles((dir, name) -> !name.startsWith("."));
        if (files == null) {
            return 0;
        }
        AtomicInteger ret = new AtomicInteger(0);
        Arrays.stream(files).forEach(file -> {
            LOGGER.info("Creating meta data for {}", file.getAbsolutePath());
            if (createMetaData(type, file)) {
                ret.getAndIncrement();
            }
        });
        return ret.get();
    }

    //------------------------------------------------------------------------------------------------------------------

    public static File getFileDirImages() {
        return IMAGES_BASE;
    }

    public static File getThumbDirImages() {
        return IMAGES_THUMBS;
    }

    public static File getFileDirVideos() {
        return VIDEOS_BASE;
    }

    public static File getThumbDirVideos() {
        return VIDEOS_THUMBS;
    }

    public static File getFile(Media type, String filename) {
        return new File(getBaseOf(type), filename);
    }

    public static String buildThumbnailFileName(String fileName) {
        return replaceFileExtension(fileName, ".jpg");
    }

    public static String buildMetaFileName(String fileName) {
        return replaceFileExtension(fileName, ".json");
    }

    public Mono<TimelapseResult> createTimelapseVideo(TimelapseCommand timelapseCommand) {

        final File outputVideoFile;
        try {
            outputVideoFile = File.createTempFile("f2mp4-out-", ".mp4");
            videoService.createTimelapseVideo(timelapseCommand, outputVideoFile);
        } catch (IOException ioe) {
            LOGGER.error("createTimelapseVideo failed", ioe);
            return Mono.just(new TimelapseResult(false, null));
        }
        final long contentLength = outputVideoFile.length();
        final Flux<ByteBuffer> content = FluxUtil.readFile(outputVideoFile.toPath());
        return this.storeFile(Media.VIDEOS, timelapseCommand.outputFilename(), content, contentLength)
            .thenReturn(new TimelapseResult(true, timelapseCommand.outputFilename()));
    }

    // TODO: No zip - actually just a simple concat
    public Flux<ByteBuffer> downloadImagesAsZip(Flux<String> fileNames) {
        return fileNames.concatMap(fileName -> FluxUtil.readFile(getFile(Media.IMAGES, fileName)));
    }

    //------------------------------------------------------------------------------------------------------------------

    boolean createThumbnail(Media type, File originalFile) {
        final File thumbnailFile = buildThumbnailFile(type, originalFile.getName());
        if (type == Media.IMAGES) {
            return createThumbnailForImage(originalFile, thumbnailFile);
        } else {
            return createThumbnailForVideo(originalFile, thumbnailFile);
        }
    }

    boolean createThumbnailForImage(File originalFile, File thumbnailFile) {
        try {
            imagingProvider.createThumbNail(originalFile, thumbnailFile, MediaType.IMAGE_JPEG_VALUE,
                160, 120, ConversionCommand.CompressionQuality.LOSSY_BEST, ConversionCommand.SpeedHint.ULTRA_QUALITY);
        } catch (Exception exc) {
            LOGGER.warn("Cannot create thumbnail for image \"{}\"! {}", originalFile.getAbsolutePath(), exc.getMessage());
            return false;
        }
        return true;
    }

    boolean createThumbnailForVideo(File originalFile, File thumbnailFile) {
        try {
            videoService.videoToThumbnail(originalFile, thumbnailFile);
        } catch (Exception exc) {
            LOGGER.warn("Cannot create thumbnail for video \"{}\"! {}", originalFile.getAbsolutePath(), exc.getMessage());
            return false;
        }
        return true;
    }

    boolean createMetaData(Media type, File originalFile) {
        final File metaFile = buildMetaFile(type, originalFile.getName());
        try {
            if (type == Media.VIDEOS) {
                final VideoMetaInfo videoMetaInfo = videoService.extractVideoMetaInfo(originalFile);
                return videoService.storeVideoMetaInfo(videoMetaInfo, metaFile);
            } else {
                return false;
            }
        } catch (Exception exc) {
            LOGGER.warn("Cannot create meta data for \"{}\"! {}", originalFile.getAbsolutePath(), exc.getMessage());
            return false;
        }
    }

    private static boolean isFileNameInvalid(String filename) {
        return !FILE_NAME_PATTERN.matcher(filename).matches();
    }

    private static File getBaseOf(Media type) {
        return type == Media.IMAGES ? IMAGES_BASE : VIDEOS_BASE;
    }

    private static File buildThumbnailFile(Media type, String filename) {
        final String thumbnailFileName = buildThumbnailFileName(filename);
        return new File(getBaseOf(type) + File.separator + DIR_NAME_THUMBS, thumbnailFileName);
    }

    private static File buildMetaFile(Media type, String filename) {
        final String metaFileName = buildMetaFileName(filename);
        return new File(getBaseOf(type) + File.separator + DIR_NAME_META, metaFileName);
    }

    @SuppressWarnings("SameParameterValue")
    private static String replaceFileExtension(String fileName, String newExtension) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == 0) {
            return fileName + newExtension;
        }
        return fileName.substring(0, lastDotIndex) + newExtension;
    }

    private static void createDirectory(File directory) {
        if (!directory.isDirectory()) {
            if (directory.mkdirs()) {
                LOGGER.info("{} directory created.", directory);
            } else {
                LOGGER.error("Cannot create {} directory!", directory);
            }
        } else {
            LOGGER.info("{} directory already exists.", directory);
        }
    }

    public enum Media {
        IMAGES, VIDEOS
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Status(boolean success, String error) {
    }
}
