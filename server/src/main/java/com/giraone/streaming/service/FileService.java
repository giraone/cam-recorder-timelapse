package com.giraone.streaming.service;

import com.giraone.streaming.controller.FileController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.file.NoSuchFileException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import static java.nio.file.StandardOpenOption.READ;

@Service
public class FileService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileController.class);

    private static final File FILE_BASE = new File("FILES");
    private static final Pattern FILE_NAME_PATTERN = Pattern.compile("[a-zA-Z0-9-]+[.][a-z]{3,4}");

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

    //------------------------------------------------------------------------------------------------------------------

    private static boolean isFileNameInvalid(String filename) {
        return !FILE_NAME_PATTERN.matcher(filename).matches();
    }
}
