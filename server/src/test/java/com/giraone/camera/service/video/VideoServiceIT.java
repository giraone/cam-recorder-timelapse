package com.giraone.camera.service.video;

import com.giraone.camera.service.FileService;
import com.giraone.camera.service.model.VideoMetaInfo;
import com.giraone.camera.service.video.model.TimelapseCommand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public
class VideoServiceIT {

    @Autowired
    VideoService videoService;
    
    @Test
    void videoInfoFull() throws IOException {
        // arrange
        File video = new File("src/test/resources/testdata/video-640x480-1MB.mp4");
        // act
        String result = videoService.extractVideoInfoFull(video);
        // assert
        assertThat(result).startsWith("{");
    }

    @Test
    void videoMetaInfo() throws IOException {
        // arrange
        File video = new File("src/test/resources/testdata/video-640x480-1MB.mp4");
        // act
        VideoMetaInfo result = videoService.extractVideoMetaInfo(video);
        // assert
        assertThat(result.videoCodec()).isEqualTo("h264");
        assertThat(result.audioCodec()).isEqualTo("aac");
        assertThat(result.resolution()).isEqualTo("640x480");
        assertThat(result.durationSeconds()).isEqualTo(5);
        assertThat(result.framesPerSecond()).isEqualTo(25);
    }

    @Test
    void videoToThumbnail() throws Exception {
        // arrange
        File video = new File("src/test/resources/testdata/video-640x480-1MB.mp4");
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
        List<String> imageFiles = buildInputFiles();
        TimelapseCommand timelapseCommand = new TimelapseCommand("", imageFiles, 2 ,15);
        File tempOutputFile = File.createTempFile("createTempFile-", ".mp4");
        tempOutputFile.deleteOnExit();
        // act
        videoService.createTimelapseVideo(timelapseCommand, tempOutputFile);
        // assert
        assertThat(tempOutputFile).size().isGreaterThan(100);
    }

    public static List<String> buildInputFiles() {
        // arrange
        File images = FileService.getFileDirImages();
        File[] files = images.listFiles((dir, name) -> name.startsWith("cam-b-2024-11-17"));
        if (files == null || files.length == 0) {
            throw new IllegalStateException("No input files in \"" + images.getAbsolutePath() + "\"!");
        }
        return Arrays.stream(files).map(File::getName).toList();
    }
}