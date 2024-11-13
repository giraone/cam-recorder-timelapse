package com.giraone.streaming.service;

import com.giraone.streaming.service.model.FileInfo;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class FileServiceIT {

    @Autowired
    FileService fileService;

    @Test
    void createThumbnail() {

        for (FileInfo fileInfo : fileService.listFileInfos(FileService.Media.IMAGES, "000")) {
            fileService.createThumbnail(FileService.Media.IMAGES, FileService.getFile(FileService.Media.IMAGES, fileInfo.fileName()));
        }
    }
}