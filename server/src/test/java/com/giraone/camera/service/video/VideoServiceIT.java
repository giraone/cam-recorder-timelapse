package com.giraone.camera.service.video;

import com.giraone.camera.service.FileService;
import com.giraone.camera.service.model.VideoMetaInfo;
import com.giraone.camera.service.video.model.TimelapseCommand;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
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
        Path video = Path.of("src/test/resources/testdata/video-640x480-1MB.mp4");
        // act
        String result = videoService.extractVideoInfoFull(video);
        // assert
        assertThat(result).startsWith("{");
    }

    @Test
    void videoMetaInfo() throws IOException {
        // arrange
        Path video = Path.of("src/test/resources/testdata/video-640x480-1MB.mp4");
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
        Path video = Path.of("src/test/resources/testdata/video-640x480-1MB.mp4");
        Path tempFile = Files.createTempFile("videoToThumbnail", ".jpg");
        // act
        videoService.videoToThumbnail(video, tempFile);
        // assert
        assertThat(Files.size(tempFile)).isGreaterThan(100);
    }

    @Test
    void createTimelapseVideo() throws Exception {
        // arrange
        List<String> imageFiles = buildInputFiles();
        TimelapseCommand timelapseCommand = new TimelapseCommand("", imageFiles, 2 ,15);
        Path tempOutputFile = Files.createTempFile("createTempFile-", ".mp4");
        // act
        videoService.createTimelapseVideo(timelapseCommand, tempOutputFile);
        // assert
        assertThat(Files.size(tempOutputFile)).isGreaterThan(100);
        Files.delete(tempOutputFile);
    }

    public static List<String> buildInputFiles() {
        // arrange
        final Path images = FileService.getFileDirImages();
        final List<String> files = new ArrayList<>();
        final DirectoryStream.Filter<? super Path> filter = path -> path.getFileName().toString().startsWith("cam-b-2024-11-17");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(images, filter)) {
            for (Path path: stream) {
                files.add(path.getFileName().toString());
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return files;
    }
}