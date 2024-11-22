package com.giraone.streaming.service.video;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giraone.imaging.ConversionCommand;
import com.giraone.imaging.ImagingProvider;
import com.giraone.imaging.java2.ProviderJava2D;
import com.giraone.streaming.service.FileService;
import com.giraone.streaming.service.model.VideoMetaInfo;
import com.giraone.streaming.service.video.model.TimelapseCommand;
import com.giraone.streaming.util.ObjectMapperBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;

import static com.giraone.streaming.service.FileService.getFile;

@Service
public class VideoService {

    private static final Logger LOGGER = LoggerFactory.getLogger(VideoService.class);
    private static final ObjectMapper MAPPER = ObjectMapperBuilder.build();

    private static final String INFILE = "${INFILE}";
    private static final String OUTFILE = "${OUTFILE}";
    private static final String SELECT = "${SELECT}";
    private static final String FRAME_RATE = "${FRAME_RATE}";

    private static final String PROBE_BINARY_WINDOWS = "C:/Tools/Videos/ffmpeg/bin/ffprobe";
    private static final String PROBE_BINARY_LINUX = "/usr/bin/ffprobe";
    private static final String BINARY_WINDOWS = "C:/Tools/Videos/ffmpeg/bin/ffmpeg";
    private static final String BINARY_LINUX = "/usr/bin/ffmpeg";
    private static final String[] COMMAND_PROBE = new String[]{
        PROBE_BINARY_LINUX,
        "-v", "quiet",
        "-print_format", "json",
        "-show_format",
        "-show_streams",
        "-i", INFILE,
        "-o", OUTFILE // Overwrite output files without asking.
    };
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
        "-framerate", "${FRAME_RATE}",
        "-c:v", "libx264", // encode using H264 Video codec
        "-vf", "select='not(mod(n\\,${SELECT}))',setpts=N/FRAME_RATE/TB,format=yuv420p", // apply filter
        "-profile", "baseline",
        "-y", OUTFILE // Overwrite output files without asking.
    };

    private final ImagingProvider imagingProvider = new ProviderJava2D();

    public String extractVideoInfoFull(File inputFile) throws IOException {
        File tempFile = File.createTempFile("v-info-", ".json");
        try {
            String[] ffmpegCommands = makeOsCmdProbe(COMMAND_PROBE);
            for (int i = 0; i < ffmpegCommands.length; i++) {
                if (INFILE.equals(ffmpegCommands[i])) ffmpegCommands[i] = inputFile.getAbsolutePath().replace('\\', '/');
                if (OUTFILE.equals(ffmpegCommands[i])) ffmpegCommands[i] = tempFile.getAbsolutePath().replace('\\', '/');
            }

            OsUtil.OsCommandResult result = OsUtil.runCommand(ffmpegCommands);
            if (result.code() >= 0) {
                if (tempFile.length() > 0L) {
                    return Files.readString(tempFile.toPath());
                } else {
                    throw new RuntimeException("Cannot probe video \"" + inputFile + "\"! Code = " + result.code());
                }
            } else {
                if (result.exception() != null) {
                    throw new RuntimeException("Cannot probe video \"" + inputFile + "\"!", result.exception());
                } else {
                    throw new RuntimeException("Cannot probe video \"" + inputFile + "\"! " + result.output());
                }
            }
        } finally {
            tempFile.delete();
        }
    }

    public VideoMetaInfo extractVideoMetaInfo(File inputFile) throws IOException {
        String jsonString = extractVideoInfoFull(inputFile);
        if (jsonString == null) {
            return new VideoMetaInfo("ERROR (ffprobe)", "", 0, "ERROR (ffprobe)", 0);
        }
        try {
            return buildVideoInfoFromFfmpegJson(jsonString);
        } catch (JsonProcessingException e) {
            LOGGER.warn("Cannot parse: " + jsonString, e);
            return new VideoMetaInfo("ERROR (parse)", "", 0, "ERROR (parse)", 0);
        }
    }

    public boolean storeVideoMetaInfo(VideoMetaInfo videoMetaInfo, File outputFile) {
        try {
            MAPPER.writeValue(outputFile, videoMetaInfo);
            return true;
        } catch (IOException e) {
            LOGGER.warn("Cannot write VideoMetaInfo to {}", outputFile, e);
            return false;
        }
    }

    public void createTimelapseVideo(TimelapseCommand timelapseCommand, File outputVideoFile) throws IOException {
        File inputListFile = File.createTempFile("f2mp4-list-", ".txt");
        try (PrintStream out = new PrintStream(new FileOutputStream(inputListFile))) {
            timelapseCommand.inputFileNames().forEach(filename -> {
                try {
                    out.printf("file '%s'%n", getFile(FileService.Media.IMAGES, filename).getCanonicalPath().replace('\\', '/'));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        final long maxWaitTimeMs = timelapseCommand.inputFileNames().size() * 500L;
        LOGGER.info("List file created \"{}\" with {} entries. Max wait = {}ms",
            inputListFile, timelapseCommand.inputFileNames().size(), maxWaitTimeMs);
        String[] ffmpegCommands = makeOsCmdMpeg(COMMAND_TIMELAPSE);
        for (int i = 0; i < ffmpegCommands.length; i++) {
            if (INFILE.equals(ffmpegCommands[i])) ffmpegCommands[i] = inputListFile.getAbsolutePath().replace('\\', '/');
            if (OUTFILE.equals(ffmpegCommands[i])) ffmpegCommands[i] = outputVideoFile.getAbsolutePath().replace('\\', '/');
            ffmpegCommands[i] = ffmpegCommands[i].replace(SELECT, Integer.toString(timelapseCommand.select()));
            ffmpegCommands[i] = ffmpegCommands[i].replace(FRAME_RATE, Integer.toString(timelapseCommand.frameRate()));
        }
        final OsUtil.OsCommandResult result = OsUtil.runCommandAndReadOutput(ffmpegCommands, maxWaitTimeMs);
        if (result.code() == 0) {
            if (outputVideoFile.length() < 100L) {
                throw new RuntimeException("ffmpeg call not successful! No video file created!");
            } else {
                LOGGER.info("ffmpeg call successful with output to {} and {} bytes.", outputVideoFile.getAbsolutePath(), outputVideoFile.length());
                inputListFile.delete();
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
        try {
            String[] ffmpegCommands = makeOsCmdMpeg(COMMAND_THUMBNAIL);
            for (int i = 0; i < ffmpegCommands.length; i++) {
                if (INFILE.equals(ffmpegCommands[i])) ffmpegCommands[i] = inputFile.getAbsolutePath().replace('\\', '/');
                if (OUTFILE.equals(ffmpegCommands[i])) ffmpegCommands[i] = tempFile.getAbsolutePath().replace('\\', '/');
            }

            OsUtil.OsCommandResult result = OsUtil.runCommandAndReadOutput(ffmpegCommands, 5000L);
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
        } finally {
            tempFile.delete();
        }
    }

    private static String[] makeOsCmdProbe(String[] cmd) {
        if (System.getProperty("os.name").startsWith("Windows")) {
            cmd[0] = PROBE_BINARY_WINDOWS;
        }
        return cmd;
    }

    private static String[] makeOsCmdMpeg(String[] cmd) {
        if (System.getProperty("os.name").startsWith("Windows")) {
            cmd[0] = BINARY_WINDOWS;
        }
        return cmd;
    }

    static VideoMetaInfo buildVideoInfoFromFfmpegJson(String jsonString) throws JsonProcessingException {
        JsonNode jsonNode = MAPPER.readTree(jsonString);
        String videoCodec = jsonNode.at("/streams/0/codec_name").asText();
        String audioCodec = jsonNode.at("/streams/1/codec_name").asText();
        String width = jsonNode.at("/streams/0/width").asText();
        String height = jsonNode.at("/streams/0/height").asText();
        String durationSecondsString = jsonNode.at("/format/duration").asText();
        String framesPerSecondCalc = jsonNode.at("/streams/0/avg_frame_rate").asText();
        int i = framesPerSecondCalc.indexOf('/');
        int i1 = Integer.parseInt(framesPerSecondCalc.substring(0, i));
        int i2 = Integer.parseInt(framesPerSecondCalc.substring(i + 1));
        int framesPerSecond = i1 / i2;
        return new VideoMetaInfo(
            videoCodec,
            audioCodec,
            durationSecondsString.isEmpty() ? 0 : (int) Float.parseFloat(durationSecondsString),
            width + "x" + height,
            framesPerSecond
        );
    }
}
