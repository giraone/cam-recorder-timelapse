package com.giraone.streaming.service.video;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;

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
        // act
        videoService.videoToThumbnail(video, tempFile);
        // assert
        assertThat(tempFile).size().isGreaterThan(100);
    }
}