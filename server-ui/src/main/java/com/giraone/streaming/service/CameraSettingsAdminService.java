package com.giraone.streaming.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.giraone.streaming.service.model.CameraSettings;
import com.giraone.streaming.util.ObjectMapperBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class CameraSettingsAdminService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CameraSettingsAdminService.class);

    private static final File SETTINGS_FILE = new File("../camera-settings.json");

    private static final ObjectMapper objectMapper = ObjectMapperBuilder.build();

    private CameraSettings settings = new CameraSettings();

    public CameraSettingsAdminService() {

        if (SETTINGS_FILE.exists()) {
            try {
                this.settings = loadSetting();
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
        settings.setRestartNow(false);
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
}
