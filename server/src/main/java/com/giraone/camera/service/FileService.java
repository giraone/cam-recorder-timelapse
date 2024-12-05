package com.giraone.camera.service;

import com.giraone.camera.config.ApplicationProperties;
import com.giraone.camera.service.api.Status;
import com.giraone.camera.service.model.FileInfo;
import com.giraone.camera.service.model.FileInfoAndContent;
import com.giraone.camera.service.model.FileInfoQuery;
import com.giraone.camera.service.model.VideoMetaInfo;
import com.giraone.camera.service.video.VideoService;
import com.giraone.camera.service.video.model.TimelapseCommand;
import com.giraone.camera.service.video.model.TimelapseResult;
import com.giraone.imaging.ConversionCommand;
import com.giraone.imaging.ImagingProvider;
import com.giraone.imaging.java2.ProviderJava2D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.nio.file.StandardOpenOption.*;

@Service
public class FileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileService.class);

    public static final String DIR_NAME_THUMBS = ".thumbs";
    public static final String DIR_NAME_META = ".meta";
    public static final Path STORAGE_BASE = Path.of("../STORAGE");
    public static final Path IMAGES_BASE = STORAGE_BASE.resolve("IMAGES");
    public static final Path IMAGES_THUMBS = IMAGES_BASE.resolve(DIR_NAME_THUMBS);
    public static final Path IMAGES_META = IMAGES_BASE.resolve(DIR_NAME_META);
    public static final Path VIDEOS_BASE = STORAGE_BASE.resolve("VIDEOS");
    public static final Path VIDEOS_THUMBS = VIDEOS_BASE.resolve(DIR_NAME_THUMBS);
    public static final Path VIDEOS_META = VIDEOS_BASE.resolve(DIR_NAME_META);

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
            return returnErrorOnInvalidFileName(filename);
        }
        final Path file = getFile(type, filename);
        final AsynchronousFileChannel channel;
        try {
            channel = AsynchronousFileChannel.open(file, CREATE, WRITE);
        } catch (IOException ioe) {
            LOGGER.warn("Cannot open file to write to \"{}\"!", file, ioe);
            return Mono.error(ioe);
        }
        final AtomicLong writtenBytes = new AtomicLong(0L);
        return FluxUtil.writeFile(content, channel)
            .doOnSuccess(voidIgnore -> {
                try {
                    channel.close();
                    writtenBytes.set(Files.size(file));
                    LOGGER.info("File \"{}\" with {} bytes written.", file.getFileName(), writtenBytes.get());
                    if (contentLength > 0 && contentLength != Files.size(file)) {
                        LOGGER.warn("Content length and file length mismatch {} != {}!", contentLength, Files.size(file));
                    }
                } catch (IOException e) {
                    LOGGER.warn("Cannot close file \"{}\"!", file, e);
                }
            })
            .doOnSuccess(unused -> {
                if (applicationProperties.isGenerateThumbnails()) {
                    createThumbnail(type, file);
                    LOGGER.debug("Thumbnail for \"{}\" stored.", filename);
                }
            })
            .thenReturn(FileInfo.fromFile(file, writtenBytes.get()));
    }

    public FileInfoAndContent downloadFile(Media type, String filename) throws IOException {
        if (isFileNameInvalid(filename)) {
            throw errorOnInvalidFileName(filename);
        }
        final Path file = getBaseOf(type).resolve(filename);
        return downloadFile(file);
    }

    public FileInfoAndContent downloadThumb(Media type, String filename) throws IOException {
        if (isFileNameInvalid(filename)) {
            throw errorOnInvalidFileName(filename);
        }
        final Path file = getThumbOf(type).resolve(filename);
        return downloadFile(file);
    }

    public FileInfoAndContent downloadMeta(Media type, String filename) throws IOException {
        if (isFileNameInvalid(filename)) {
            throw errorOnInvalidFileName(filename);
        }
        final Path file = getMetaOf(type).resolve(filename);
        return downloadFile(file);
    }

    public List<FileInfo> listFileInfos(Media type, FileInfoQuery query) {
        final List<FileInfo> files = listFileInfosUsingFilter(getBaseOf(type), query);
        files.sort(query.order().getComparator());
        return files.stream()
            .skip(query.offset())
            .limit(query.limit())
            .toList();
    }

    public int countFileInfos(Media type, FileInfoQuery query) {
        final List<FileInfo> files = listFileInfosUsingFilter(getBaseOf(type), query);
        int ret = (int) files.stream()
            .skip(query.offset())
            .limit(query.limit())
            .count();
        LOGGER.info("countFileInfos {} {} = {}", type, query, ret);
        return ret;
    }

    public Status rename(Media type, String filename, String newName) {
        if (isFileNameInvalid(filename)) {
            return new Status(false, errorTextInvalidFileName(filename));
        }
        final Path dir = getBaseOf(type);
        final Path oldFile = dir.resolve(filename);
        final Path newFile = dir.resolve(newName);
        LOGGER.error("Rename \"{}\" to \"{}\"", oldFile, newFile);
        try {
            Files.move(oldFile,newFile);
            final Path oldThumbnailFile = buildThumbnailFile(type, filename);
            final Path newThumbnailFile = buildThumbnailFile(type, newName);
            if (Files.exists(oldThumbnailFile)) {
                Files.move(oldThumbnailFile, newThumbnailFile);
            }
            return new Status(true, null);
        } catch (Exception exc) {
            LOGGER.error("Failed to rename \"{}\" to \"{}\"", oldFile, newFile, exc);
            return new Status(false, exc.getMessage());
        }
    }

    public Status delete(Media type, String filename) {
        if (isFileNameInvalid(filename)) {
            return new Status(false, "Invalid filename \"" + filename + "\"!");
        }
        final Path file = getBaseOf(type).resolve(filename);
        LOGGER.error("Delete \"{}\"", file);
        try {
            Files.delete(file);
            final Path thumbnailFile = buildThumbnailFile(type, filename);
            try {
                Files.deleteIfExists(file);
            } catch (IOException e) {
                LOGGER.error("Failed to delete thumbnail \"{}\". Error ignored.", thumbnailFile);
            }
            return new Status(true, null);
        } catch (Exception exc) {
            LOGGER.error("Failed to delete \"{}\"", file, exc);
            return new Status(false, exc.getMessage());
        }
    }

    public int rebuildThumbnails(Media type) {
        AtomicInteger ret = new AtomicInteger(0);
        listPathsUsingFilter(getBaseOf(type), null).forEach(file -> {
            LOGGER.info("Creating thumbnail for {}", file);
            if (createThumbnail(type, file)) {
                ret.getAndIncrement();
            }
        });
        return ret.get();
    }

    public int rebuildMeta(Media type) {
        AtomicInteger ret = new AtomicInteger(0);
        listPathsUsingFilter(getBaseOf(type), null).forEach(file -> {
            LOGGER.info("Creating thumbnail for {}", file);
            if (createMetaData(type, file)) {
                ret.getAndIncrement();
            }
        });
        return ret.get();
    }

    //------------------------------------------------------------------------------------------------------------------

    public static Path getFileDirImages() {
        return IMAGES_BASE;
    }

    public static Path getThumbDirImages() {
        return IMAGES_THUMBS;
    }

    public static Path getFileDirVideos() {
        return VIDEOS_BASE;
    }

    public static Path getThumbDirVideos() {
        return VIDEOS_THUMBS;
    }

    public static Path getFile(Media type, String filename) {
        return getBaseOf(type).resolve(filename);
    }

    public static String buildThumbnailFileName(String fileName) {
        return replaceFileExtension(fileName, ".jpg");
    }

    public static String buildMetaFileName(String fileName) {
        return replaceFileExtension(fileName, ".json");
    }

    public Mono<TimelapseResult> createTimelapseVideo(TimelapseCommand timelapseCommand) {

        final Path outputVideoFile;
        try {
            outputVideoFile = Files.createTempFile("f2mp4-out-", ".mp4");
            videoService.createTimelapseVideo(timelapseCommand, outputVideoFile);
        } catch (IOException ioe) {
            LOGGER.error("createTimelapseVideo failed", ioe);
            return Mono.just(new TimelapseResult(false, null));
        }
        final long contentLength;
        try {
            contentLength = Files.size(outputVideoFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        final Flux<ByteBuffer> content = FluxUtil.readFile(outputVideoFile);
        return this.storeFile(Media.VIDEOS, timelapseCommand.outputFilename(), content, contentLength)
            .thenReturn(new TimelapseResult(true, timelapseCommand.outputFilename()));
    }

    // TODO: No zip - actually just a simple concat
    public Flux<ByteBuffer> downloadImagesAsZip(Flux<String> fileNames) {
        return fileNames.concatMap(fileName -> FluxUtil.readFile(getFile(Media.IMAGES, fileName)));
    }

    //------------------------------------------------------------------------------------------------------------------

    boolean createThumbnail(Media type, Path originalFile) {
        final Path thumbnailFile = buildThumbnailFile(type, originalFile.getFileName().toString());
        if (type == Media.IMAGES) {
            return createThumbnailForImage(originalFile, thumbnailFile);
        } else {
            return createThumbnailForVideo(originalFile, thumbnailFile);
        }
    }

    boolean createThumbnailForImage(Path originalFile, Path thumbnailFile) {
        try {
            imagingProvider.createThumbNail(originalFile.toFile(), thumbnailFile.toFile(), MediaType.IMAGE_JPEG_VALUE,
                160, 120, ConversionCommand.CompressionQuality.LOSSY_BEST, ConversionCommand.SpeedHint.ULTRA_QUALITY);
        } catch (Exception exc) {
            LOGGER.warn("Cannot create thumbnail for image \"{}\"! {}", originalFile, exc.getMessage());
            return false;
        }
        return true;
    }

    boolean createThumbnailForVideo(Path originalFile, Path thumbnailFile) {
        try {
            videoService.videoToThumbnail(originalFile, thumbnailFile);
        } catch (Exception exc) {
            LOGGER.warn("Cannot create thumbnail for video \"{}\"! {}", originalFile, exc.getMessage());
            return false;
        }
        return true;
    }

    boolean createMetaData(Media type, Path originalFile) {
        final Path metaFile = buildMetaFile(type, originalFile.getFileName().toString());
        try {
            if (type == Media.VIDEOS) {
                final VideoMetaInfo videoMetaInfo = videoService.extractVideoMetaInfo(originalFile);
                return videoService.storeVideoMetaInfo(videoMetaInfo, metaFile);
            } else {
                return false;
            }
        } catch (Exception exc) {
            LOGGER.warn("Cannot create meta data for \"{}\"! {}", originalFile, exc.getMessage());
            return false;
        }
    }

    private FileInfoAndContent downloadFile(Path file) throws IOException {

        final AsynchronousFileChannel channel;
        try {
            channel = AsynchronousFileChannel.open(file, READ);
        } catch (NoSuchFileException nsfe) {
            LOGGER.warn("File \"{}\" does not exist! {}", file, nsfe.getMessage());
            throw nsfe;
        } catch (IOException ioe) {
            LOGGER.warn("Cannot open file to read from \"{}\"! {}", file, ioe.getMessage());
            throw ioe;
        }
        final Flux<ByteBuffer> content = FluxUtil.readFile(channel);
        return new FileInfoAndContent(content, FileInfo.fromFile(file));
    }

    private static boolean isFileNameInvalid(String filename) {
        return !FILE_NAME_PATTERN.matcher(filename).matches();
    }

    private static Mono<FileInfo> returnErrorOnInvalidFileName(String filename) {
        return Mono.error(errorOnInvalidFileName(filename));
    }

    private static IllegalArgumentException errorOnInvalidFileName(String filename) {
        return new IllegalArgumentException(errorTextInvalidFileName(filename));
    }

    private static String errorTextInvalidFileName(String filename) {
        return "Invalid filename \"" + filename + "\"!";
    }

    private static Path getBaseOf(Media type) {
        return type == Media.IMAGES ? IMAGES_BASE : VIDEOS_BASE;
    }

    private static Path getThumbOf(Media type) {
        return type == Media.IMAGES ? IMAGES_THUMBS : VIDEOS_THUMBS;
    }

    private static Path getMetaOf(Media type) {
        return type == Media.IMAGES ? IMAGES_META : VIDEOS_META;
    }

    private static Path buildThumbnailFile(Media type, String filename) {
        final String thumbnailFileName = buildThumbnailFileName(filename);
        return getThumbOf(type).resolve(thumbnailFileName);
    }

    private static Path buildMetaFile(Media type, String filename) {
        final String metaFileName = buildMetaFileName(filename);
        return getMetaOf(type).resolve(metaFileName);
    }

    @SuppressWarnings("SameParameterValue")
    private static String replaceFileExtension(String fileName, String newExtension) {
        int lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == 0) {
            return fileName + newExtension;
        }
        return fileName.substring(0, lastDotIndex) + newExtension;
    }

    private static void createDirectory(Path directory) {
        if (!Files.isDirectory(directory)) {
            try {
               Files.createDirectories(directory);
            } catch (IOException e) {
                LOGGER.error("Cannot create directory \"{}\"!", directory);
            }
        } else {
            LOGGER.info("Directory \"{}\" already exists.", directory);
        }
    }

    private static List<Path> listPathsUsingFilter(Path dir, FileInfoQuery query) {
        final List<Path> files = new ArrayList<>(100);
        final DirectoryStream.Filter<? super Path> filter = path ->
            Files.isRegularFile(path)
                && !Files.isHidden(path)
                && (query.prefixFilter() == null || path.getFileName().toString().startsWith(query.prefixFilter()));
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, filter)) {
            for (Path path: stream) {
                files.add(path);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return files;
    }

    private static List<FileInfo> listFileInfosUsingFilter(Path dir, FileInfoQuery query) {
        return listPathsUsingFilter(dir, query)
            .stream()
            .map(FileInfo::fromFile)
            .collect(Collectors.toList());
    }

    public enum Media {
        IMAGES, VIDEOS
    }
}
