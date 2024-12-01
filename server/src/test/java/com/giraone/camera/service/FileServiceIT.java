package com.giraone.camera.service;

import com.giraone.camera.service.model.FileInfo;
import com.giraone.camera.service.model.FileInfoOrder;
import com.giraone.camera.service.model.FileInfoQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class FileServiceIT {

    @Autowired
    FileService fileService;

    @Test
    void createThumbnail() {

        FileInfoQuery query = new FileInfoQuery("000", 0, 10, new FileInfoOrder("fileName", false));
        for (FileInfo fileInfo : fileService.listFileInfos(FileService.Media.IMAGES, query)) {
            fileService.createThumbnail(FileService.Media.IMAGES, FileService.getFile(FileService.Media.IMAGES, fileInfo.fileName()));
        }
    }

    @ParameterizedTest
    // @formatter:off
    @CsvSource({
        "0000-ferrari, 0,10, fileName,false,     1,0000-ferrari.jpg",
        "000,          0,10, fileName,false,     2,0000-ferrari.jpg",
        "000,          0,10, fileName,true,      2,0001-porsche.jpg",
        "000,          0,10, sizeInBytes,false,  2,0000-ferrari.jpg",
        "000,          0,10, sizeInBytes,true,   2,0001-porsche.jpg",
        "000,          1,10, fileName,false,     1,0001-porsche.jpg"
    })
    // @formatter:on
    void listFileInfos(String prefix, int offset, int limit, String orderAttribute, boolean desc,
                       int expectedSize, String expectedName) {

        // arrange
        FileInfoQuery query = new FileInfoQuery(prefix, offset, limit, new FileInfoOrder(orderAttribute, desc));
        // act
        List<FileInfo> result = fileService.listFileInfos(FileService.Media.IMAGES, query);
        // assert
        assertThat(result).hasSize(expectedSize);
        assertThat(result.get(0).fileName()).isEqualTo(expectedName);
    }
}