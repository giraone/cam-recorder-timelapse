package com.giraone.streaming.service.video;

import com.giraone.imaging.ConversionCommand;
import com.giraone.imaging.ImagingProvider;
import com.giraone.imaging.java2.ProviderJava2D;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class VideoService {

    private static final String INFILE = "INFILE";
    private static final String OUTFILE = "OUTFILE";

    private static final String BINARY_WINDOWS = "C:/Tools/Videos/ffmpeg/bin/ffmpeg";
    private static final String[] COMMAND_WINDOWS = new String[]{BINARY_WINDOWS, "-i", INFILE, "-vf", "thumbnail=n=10", "-frames:v", "1", "-update", "1", "-y", OUTFILE};

    private static final String BINARY_LINUX = "/usr/bin/ffmpeg";
    private static final String[] COMMAND_LINUX = new String[]{BINARY_LINUX, "-i", INFILE, "-vf", "thumbnail=n=10", "-frames:v", "1", "-update", "1", "-y", OUTFILE};

    private final ImagingProvider imagingProvider = new ProviderJava2D();

    public void videoToThumbnail(File inputFile, File outputThumbnailFile) throws Exception {
        File tempFile = File.createTempFile("v2png", ".png");
        String[] ffmpegCommands;
        if (System.getProperty("os.name").startsWith("Windows")) {
            ffmpegCommands = COMMAND_WINDOWS.clone();
        } else {
            ffmpegCommands = COMMAND_LINUX.clone();
        }
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
                throw new RuntimeException("Cannot create thumbnail for video \"" + inputFile + "\"! ", result.exception());
            } else {
                throw new RuntimeException("Cannot create thumbnail for video \"" + inputFile + "\"! " + result.output());
            }
        }
    }
}
