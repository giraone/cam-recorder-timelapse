package com.giraone.streaming.service;

import com.giraone.streaming.service.model.FileInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
public class FileViewService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileViewService.class);

    public static File STORAGE_BASE = new File("../STORAGE");
    public static File IMAGES_BASE = new File(STORAGE_BASE, "IMAGES");
    public static File IMAGES_THUMBS = new File(IMAGES_BASE, ".thumbs");
    public static File VIDEOS_BASE = new File(STORAGE_BASE, "VIDEOS");
    public static File VIDEOS_THUMBS = new File(VIDEOS_BASE, ".thumbs");

    static {
        try {
            IMAGES_BASE = IMAGES_BASE.getCanonicalFile();
        } catch (IOException ioe) {
            LOGGER.debug("Cannot canonicalize: {}", IMAGES_BASE, ioe);
        }
        try {
            VIDEOS_BASE = VIDEOS_BASE.getCanonicalFile();
        } catch (IOException ioe) {
            LOGGER.debug("Cannot canonicalize: {}", VIDEOS_BASE, ioe);
        }
        try {
            IMAGES_THUMBS = IMAGES_THUMBS.getCanonicalFile();
        } catch (IOException ioe) {
            LOGGER.debug("Cannot canonicalize: {}", IMAGES_THUMBS, ioe);
        }
        try {
            VIDEOS_THUMBS = VIDEOS_THUMBS.getCanonicalFile();
        } catch (IOException ioe) {
            LOGGER.debug("Cannot canonicalize: {}", VIDEOS_THUMBS, ioe);
        }
    }
    public FileViewService() {
    }

    public File getImagesThumbDir() {
        return IMAGES_THUMBS;
    }

    public File getVideosThumbDir() {
        return VIDEOS_THUMBS;
    }

    public String getThumbUrl(FileInfo fileInfo) {
        return fileInfo.isVideo()
            ? "api/videos/thumbs/" +  fileInfo.fileName().replace(".mp4", ".jpg")
            : "api/images/thumbs/" + fileInfo.fileName();
    }

    public List<FileInfo> listImageInfos(String prefixFilter) {
        return listFileInfos(IMAGES_BASE, prefixFilter);
    }

    public List<FileInfo> listVideoInfos(String prefixFilter) {
        return listFileInfos(VIDEOS_BASE, prefixFilter);
    }

    public List<FileInfo> listFileInfos(File base, String prefixFilter) {
        File[] files = base.listFiles((dir, name) -> !name.startsWith(".") && name.startsWith(prefixFilter));
        if (files == null) {
            return Collections.emptyList();
        }
        return Arrays.stream(files)
            .map(FileInfo::fromFile)
            .sorted((o1, o2) -> o1.lastModified().isBefore(o2.lastModified()) ? 1 : o1.lastModified().isAfter(o2.lastModified()) ? -1 : 0)
            .toList();
    }

    public void deleteImage(FileInfo fileInfo) {
        new File(IMAGES_BASE, fileInfo.fileName()).delete();
        new File(IMAGES_THUMBS, fileInfo.fileName()).delete();
    }

    public void deleteVideos(FileInfo fileInfo) {
        new File(VIDEOS_BASE, fileInfo.fileName()).delete();
        new File(VIDEOS_THUMBS, fileInfo.fileName()).delete();
    }

    public void deleteImages(Set<FileInfo> selectedItems) {
        selectedItems.forEach(this::deleteImage);
    }

    public void deleteVideos(Set<FileInfo> selectedItems) {
        selectedItems.forEach(this::deleteVideos);
    }
}
