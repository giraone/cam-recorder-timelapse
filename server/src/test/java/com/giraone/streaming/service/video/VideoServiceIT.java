package com.giraone.streaming.service.video;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class VideoServiceIT {

    @Autowired
    VideoService videoService;

    @Test
    void videoToThumbnail() throws Exception {

        // arrange
        File video = new File("src/test/resources/testdata/video-720x480-1MB.mp4");
        File tempFile = File.createTempFile("videoToThumbnail", ".jpg");
        tempFile.deleteOnExit();
        // act
        videoService.videoToThumbnail(video, tempFile);
        // assert
        assertThat(tempFile).size().isGreaterThan(100);
    }

    @Test
    void createTimelapseVideo() throws Exception {

        // arrange
        String dir = "src/test/resources/testdata/images";
        File[] files = new File(dir).listFiles();
        if (files == null || files.length == 0) {
            throw new IllegalStateException("No input files in \"" + dir + "\"!");
        }
        List<File> imageFiles = Arrays.asList(files);
        File tempFile = File.createTempFile("createTempFile-", ".mp4");
        tempFile.deleteOnExit();
        // act
        videoService.createTimelapseVideo(imageFiles, tempFile, 2);
        // assert
        assertThat(tempFile).size().isGreaterThan(100);
    }
}