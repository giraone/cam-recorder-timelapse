package com.giraone.streaming.service;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giraone.streaming.controller.FileController;
import com.giraone.streaming.service.model.CameraSettings;
import com.giraone.streaming.util.ObjectMapperBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Service
public class CameraSettingsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileController.class);

    public static final File SETTINGS_FILE = new File("camera-settings.json");

    private static final ObjectMapper objectMapper = ObjectMapperBuilder.build();

    private CameraSettings settings = new CameraSettings();

    public CameraSettingsService() {

        if (SETTINGS_FILE.exists()) {
            try {
                this.settings = loadSetting(SETTINGS_FILE);
            } catch (Exception exc) {
                LOGGER.warn("Cannot load camera settings JSON file \"{}\"!", SETTINGS_FILE, exc);
            }
        }
    }

    @Bean
    public CameraSettings getSettings() {
        return settings;
    }

    public File getFile() {
        return SETTINGS_FILE;
    }

    public void storeSetting(File file) throws IOException {
        objectMapper.writeValue(file, settings);
    }

    public void storeSetting() throws IOException {
        storeSetting(SETTINGS_FILE);
    }

    public CameraSettings loadSetting(File file) throws IOException {
        return objectMapper.readValue(file, CameraSettings.class);
    }

    public CameraSettings loadSetting() throws IOException {
        return loadSetting(SETTINGS_FILE);
    }



    //------------------------------------------------------------------------------------------------------------------

    public enum FrameSize {
        FRAMESIZE_96X96,    // 96x96
        FRAMESIZE_QQVGA,    // 160x120
        FRAMESIZE_QCIF,     // 176x144
        FRAMESIZE_HQVGA,    // 240x176
        FRAMESIZE_240X240,  // 240x240
        FRAMESIZE_QVGA,     // 320x240
        FRAMESIZE_CIF,      // 400x296
        FRAMESIZE_HVGA,     // 480x320
        FRAMESIZE_VGA,      // 640x480
        FRAMESIZE_SVGA,     // 800x600
        FRAMESIZE_XGA,      // 1024x768
        FRAMESIZE_HD,       // 1280x720
        FRAMESIZE_SXGA,     // 1280x1024
        FRAMESIZE_UXGA;     // 1600x1200

        @JsonValue
        public int getOrdinal() {
            return this.ordinal();
        }

        public static List<FrameSize> ALL = List.of(
            FRAMESIZE_96X96, FRAMESIZE_QQVGA, FRAMESIZE_QCIF, FRAMESIZE_HQVGA, FRAMESIZE_240X240,
            FRAMESIZE_QVGA, FRAMESIZE_CIF, FRAMESIZE_HVGA, FRAMESIZE_VGA, FRAMESIZE_SVGA,
            FRAMESIZE_XGA, FRAMESIZE_HD, FRAMESIZE_SXGA, FRAMESIZE_UXGA
        );
    }

    public enum Level {
        XS(-2), S(-1), M(0), L(1), XL(2);

        @JsonValue
        private final int intValue;

        Level(int intValue) {
            this.intValue = intValue;
        }

        public static List<Level> ALL = List.of(XS, S, M, L, XL);
    }

    public enum SpecialEffect {
        None, Negative, Grayscale, Red, Green, Blue, Sepia;

        @JsonValue
        public int getOrdinal() {
            return this.ordinal();
        }

        public static List<SpecialEffect> ALL = List.of(None, Negative, Grayscale, Red, Green, Blue, Sepia);
    }

    public enum WhiteBalanceMode {
        Auto, Sunny, Cloudy, Office, Home;

        @JsonValue
        public int getOrdinal() {
            return this.ordinal();
        }

        public static List<WhiteBalanceMode> ALL = List.of(Auto, Sunny, Cloudy, Office, Home);
    }
}
