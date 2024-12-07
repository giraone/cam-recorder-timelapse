package com.giraone.camera;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCrypt;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CameraAdministrationIT {

    @Autowired
    private CameraAdministration cameraAdministration;

    @Test
    void contextLoads() {
        System.err.println(BCrypt.hashpw("boss-secret", BCrypt.gensalt()));
        assertThat(cameraAdministration).isNotNull();
    }
}
