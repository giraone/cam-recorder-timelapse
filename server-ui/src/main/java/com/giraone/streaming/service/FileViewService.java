package com.giraone.streaming.service;

import com.giraone.streaming.service.model.FileInfo;
import com.sun.jna.platform.unix.X11;
import org.checkerframework.checker.units.qual.A;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class FileViewService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileViewService.class);

    public static File FILE_BASE = new File("../FILES");
    public static File FILE_THUMBS = new File(FILE_BASE, ".thumbs");

    static {
        try {
            FILE_BASE = FILE_BASE.getCanonicalFile();
        } catch (IOException ioe) {
            LOGGER.debug("Cannot canonicalize: {}", FILE_BASE, ioe);
        }
        try {
            FILE_THUMBS = FILE_THUMBS.getCanonicalFile();
        } catch (IOException ioe) {
            LOGGER.debug("Cannot canonicalize: {}", FILE_THUMBS, ioe);
        }
    }
    public FileViewService() {
    }

    public File getThumbDir() {
        return FILE_THUMBS;
    }

    public String getThumbUrl(String fileName) {
        return "api/thumbs/" + fileName;
    }

    public List<FileInfo> listFileInfos(String prefixFilter) {
        File[] files = FILE_BASE.listFiles((dir, name) -> !name.startsWith(".") && name.startsWith(prefixFilter));
        LOGGER.debug("Files: {}", files);
        if (files == null) {
            return Collections.emptyList();
        }
        AtomicInteger index = new AtomicInteger(0);
        return Arrays.stream(files)
            .map(file -> FileInfo.fromFile(file, index.getAndIncrement()))
            .toList();
    }

    public void deleteFile(FileInfo fileInfo) {
        new File(FILE_BASE, fileInfo.fileName()).delete();
        new File(FILE_THUMBS, fileInfo.fileName()).delete();
    }

    public void deleteFiles(Set<FileInfo> selectedItems) {
        selectedItems.forEach(fileInfo -> deleteFile(fileInfo));
    }
}
