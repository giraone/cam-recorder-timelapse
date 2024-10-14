package com.giraone.streaming.service;

import com.giraone.streaming.service.model.CameraSettings;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CameraSettingsAdminServiceIT {

    @Autowired
    CameraSettingsAdminService cameraSettingsAdminService;

    @Test
    void getSettings() {

        // act
        CameraSettings settings = cameraSettingsAdminService.getSettings();
        // assert
        assertThat(settings).isNotNull();
        assertThat(settings.getJpegQuality()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void storeSetting() throws IOException {

        // arrange
        File tmpFile = File.createTempFile("settings", ".json");
        // act
        cameraSettingsAdminService.storeSetting(tmpFile);
        // assert
        assertThat(tmpFile).exists();
        tmpFile.delete();
    }
}