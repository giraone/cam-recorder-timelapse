package com.giraone.streaming;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CameraAdministrationIT {

    @Autowired
    private CameraAdministration cameraAdministration;

    @Test
    void contextLoads() {
        assertThat(cameraAdministration).isNotNull();
    }
}
