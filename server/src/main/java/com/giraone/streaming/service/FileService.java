package com.giraone.streaming.service;

import com.giraone.imaging.ConversionCommand;
import com.giraone.imaging.ImagingProvider;
import com.giraone.imaging.java2.ProviderJava2D;
import com.giraone.streaming.config.ApplicationProperties;
import com.giraone.streaming.service.model.FileInfo;
import com.giraone.streaming.service.model.FileInfoAndContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import static java.nio.file.StandardOpenOption.*;

@Service
public class FileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileService.class);

    private static final File FILE_BASE = new File("../FILES");
    private static final File FILE_THUMBS = new File(FILE_BASE, ".thumbs");
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9-]+[.][a-z]{3,4}");

    private final ImagingProvider imagingProvider = new ProviderJava2D();
    private final ApplicationProperties applicationProperties;

    static {
        FILE_THUMBS.mkdirs();
    }

    public FileService(ApplicationProperties applicationProperties) {
        this.applicationProperties = applicationProperties;
    }

    public Mono<FileInfo> storeFile(String filename, Flux<ByteBuffer> content, long contentLength) {

        if (isFileNameInvalid(filename)) {
            return Mono.error(new IllegalArgumentException("Invalid target filename \"" + filename + "\"!"));
        }
        final File file = new File(FILE_BASE, filename);
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

                if (applicationProperties.isGenerateThumbnails() && filename.endsWith(".jpg")) {
                    createThumbnail(file);
                    LOGGER.debug("Thumbnail for \"{}\" stored.", filename);
                }
            })
            .thenReturn(new FileInfo(filename, writtenBytes.get(),
                FileInfo.mediaTypeFromFileName(filename), FileInfo.ofEpochSecond(file.lastModified())));
    }

    public FileInfoAndContent downloadFile(@PathVariable String filename) throws IOException {

        if (isFileNameInvalid(filename)) {
            throw new IllegalArgumentException("Invalid download filename \"" + filename + "\"!");
        }
        final File file = new File(FILE_BASE, filename);
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

    public List<FileInfo> listFileInfos(String prefixFilter) {
        File[] files = FILE_BASE.listFiles((dir, name) -> !name.startsWith(".") && name.startsWith(prefixFilter));
        if (files == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(files).map(FileInfo::fromFile).toList();
    }

    public static File getThumbDir() {
        return FILE_THUMBS;
    }

    public static File getFileDir() {
        return FILE_BASE;
    }

    //------------------------------------------------------------------------------------------------------------------

    void createThumbnail(File originalFile) {

        final File thumbnailFile = new File(FILE_THUMBS, originalFile.getName());
        try {
            imagingProvider.createThumbNail(originalFile, thumbnailFile, MediaType.IMAGE_JPEG_VALUE,
                160, 120, ConversionCommand.CompressionQuality.LOSSY_BEST, ConversionCommand.SpeedHint.ULTRA_QUALITY);
        } catch (Exception exc) {
            LOGGER.warn("Cannot create thumbnail for \"{}\"! {}", originalFile.getAbsolutePath(), exc.getMessage());
        }
    }

    private static boolean isFileNameInvalid(String filename) {
        return !FILE_NAME_PATTERN.matcher(filename).matches();
    }
}
