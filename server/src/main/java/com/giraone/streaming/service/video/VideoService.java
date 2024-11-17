package com.giraone.streaming.service.video;

import com.giraone.imaging.ConversionCommand;
import com.giraone.imaging.ImagingProvider;
import com.giraone.imaging.java2.ProviderJava2D;
import com.giraone.streaming.service.FileService;
import com.giraone.streaming.service.video.model.TimelapseCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import static com.giraone.streaming.service.FileService.getFile;

@Service
public class VideoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VideoService.class);

    private static final String INFILE = "${INFILE}";
    private static final String OUTFILE = "${OUTFILE}";
    private static final String RATE = "${RATE}";

    private static final String BINARY_WINDOWS = "C:/Tools/Videos/ffmpeg/bin/ffmpeg";
    private static final String BINARY_LINUX = "/usr/bin/ffmpeg";
    private static final String[] COMMAND_THUMBNAIL = new String[]{
        BINARY_LINUX,
        "-hide_banner",
        "-loglevel", "error",
        "-i", INFILE,
        "-vf", "thumbnail=n=10",
        "-frames:v", "1",
        "-update", "1",
        "-y", OUTFILE // Overwrite output files without asking.
    };
    private static final String[] COMMAND_TIMELAPSE = new String[]{
        BINARY_LINUX,
        "-hide_banner",
        "-loglevel", "error",
        "-f", "concat",
        "-safe", "0",
        "-i", INFILE,
        "-framerate", "15",
        "-c:v", "libx264", // encode using H264 Video codec
        "-vf", "select='not(mod(n\\,${RATE}))',setpts=N/FRAME_RATE/TB,format=yuv420p", // aplly filter
        "-profile", "baseline",
        "-y", OUTFILE // Overwrite output files without asking.
    };

    private final ImagingProvider imagingProvider = new ProviderJava2D();

    public void createTimelapseVideo(TimelapseCommand timelapseCommand, File outputVideoFile) throws IOException {
        File inputListFile = File.createTempFile("f2mp4-list-", ".txt");
        try (PrintStream out = new PrintStream(new FileOutputStream(inputListFile))) {
            timelapseCommand.inputFileNames().forEach(filename -> {
                out.printf("file '%s'%n", getFile(FileService.Media.IMAGES, filename).getAbsolutePath().replace('\\', '/'));
            });
        }
        LOGGER.info("List file created: {}", inputListFile);
        String[] ffmpegCommands = makeOsCmd(COMMAND_TIMELAPSE);
        for (int i = 0; i < ffmpegCommands.length; i++) {
            if (INFILE.equals(ffmpegCommands[i])) ffmpegCommands[i] = inputListFile.getAbsolutePath().replace('\\', '/');
            if (OUTFILE.equals(ffmpegCommands[i])) ffmpegCommands[i] = outputVideoFile.getAbsolutePath().replace('\\', '/');
            ffmpegCommands[i] = ffmpegCommands[i].replace(RATE, Integer.toString(timelapseCommand.rate()));
        }
        OsUtil.OsCommandResult result = OsUtil.runCommandAndReadOutput(ffmpegCommands);
        if (result.code() == 0) {
            if (outputVideoFile.length() < 100L) {
                throw new RuntimeException("ffmpeg call not successful! No video file created!");
            } else {
                LOGGER.info("ffmpeg call successful with output to {} and {} bytes.",outputVideoFile.getAbsolutePath(), outputVideoFile.length());
            }
        } else if (result.code() > 0) {
            throw new RuntimeException("ffmpeg call not successful! Exit code = " + result.code() + ". " + result.output());
        } else {
            if (result.exception() != null) {
                throw new RuntimeException("ffmpeg call failed with exception!", result.exception());
            } else {
                throw new RuntimeException("ffmpeg call failed! " + result.output());
            }
        }
    }

    public void videoToThumbnail(File inputFile, File outputThumbnailFile) throws Exception {
        File tempFile = File.createTempFile("v2png-", ".png");
        String[] ffmpegCommands = makeOsCmd(COMMAND_THUMBNAIL);
        for (int i = 0; i < ffmpegCommands.length; i++) {
            if (INFILE.equals(ffmpegCommands[i])) ffmpegCommands[i] = inputFile.getAbsolutePath().replace('\\', '/');
            if (OUTFILE.equals(ffmpegCommands[i])) ffmpegCommands[i] = tempFile.getAbsolutePath().replace('\\', '/');
        }

        OsUtil.OsCommandResult result = OsUtil.runCommandAndReadOutput(ffmpegCommands);
        if (result.code() >= 0) {
            if (tempFile.length() > 100L) {
                imagingProvider.createThumbNail(tempFile, outputThumbnailFile, MediaType.IMAGE_JPEG_VALUE,
                    160, 120, ConversionCommand.CompressionQuality.LOSSY_BEST, ConversionCommand.SpeedHint.ULTRA_QUALITY);
            } else {
                throw new RuntimeException("Cannot create thumbnail for video \"" + inputFile + "\"! Empty PNG output.");
            }
        } else {
            if (result.exception() != null) {
                throw new RuntimeException("Cannot create thumbnail for video \"" + inputFile + "\"!", result.exception());
            } else {
                throw new RuntimeException("Cannot create thumbnail for video \"" + inputFile + "\"! " + result.output());
            }
        }
    }

    private static String[] makeOsCmd(String[] cmd) {
        if (System.getProperty("os.name").startsWith("Windows")) {
            cmd[0] = BINARY_WINDOWS;
        }
        return cmd;
    }
}
