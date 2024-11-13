package com.giraone.streaming.service;

import com.giraone.imaging.ConversionCommand;
import com.giraone.imaging.ImagingProvider;
import com.giraone.imaging.java2.ProviderJava2D;
import com.giraone.streaming.config.ApplicationProperties;
import com.giraone.streaming.service.model.FileInfo;
import com.giraone.streaming.service.model.FileInfoAndContent;
import com.giraone.streaming.service.video.VideoService;
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

    private static final String THUMBS = ".thumbs";
    private static final File STORAGE_BASE = new File(".." + File.separator + "STORAGE");
    private static final File IMAGES_BASE = new File(STORAGE_BASE, "IMAGES");
    private static final File IMAGES_THUMBS = new File(IMAGES_BASE, THUMBS);
    private static final File VIDEOS_BASE = new File(STORAGE_BASE, "VIDEOS");
    private static final File VIDEOS_THUMBS = new File(VIDEOS_BASE, THUMBS);

    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9-]+[.][a-z0-9]{3,4}");

    private final ImagingProvider imagingProvider = new ProviderJava2D();

    private final VideoService videoService;
    private final ApplicationProperties applicationProperties;

    static {
        createDirectory(IMAGES_THUMBS);
        createDirectory(VIDEOS_THUMBS);
    }

    public FileService(VideoService videoService, ApplicationProperties applicationProperties) {
        this.videoService = videoService;
        this.applicationProperties = applicationProperties;
    }

    public Mono<FileInfo> storeFile(Media type, String filename, Flux<ByteBuffer> content, long contentLength) {

        if (isFileNameInvalid(filename)) {
            return Mono.error(new IllegalArgumentException("Invalid target filename \"" + filename + "\"!"));
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
                FileInfo.mediaTypeFromFileName(filename), FileInfo.ofEpochSecond(file.lastModified())));
    }

    public FileInfoAndContent downloadFile(Media type, String filename) throws IOException {

        if (isFileNameInvalid(filename)) {
            throw new IllegalArgumentException("Invalid download filename \"" + filename + "\"!");
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

    public String createTimelapseVideo(List<String> imageNames) {
        return "DONE!";
    }

    //------------------------------------------------------------------------------------------------------------------

    boolean createThumbnail(Media type, File originalFile) {

        final String thumbnailFileName = buildThumbnailFileName(originalFile.getName());
        final File thumbnailFile = new File(getBaseOf(type) + File.separator + THUMBS, thumbnailFileName);
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

    private static boolean isFileNameInvalid(String filename) {
        return !FILE_NAME_PATTERN.matcher(filename).matches();
    }

    private static File getBaseOf(Media type) {
        return type == Media.IMAGES ? IMAGES_BASE : VIDEOS_BASE;
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
}
