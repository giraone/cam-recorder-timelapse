package com.giraone.camera;

import com.giraone.camera.config.ApplicationProperties;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

/**
 * The entry point of the Spring Boot application.
 * Use the @PWA annotation make the application installable on phones, tablets and desktop browsers.
 */
@SpringBootApplication
@EnableConfigurationProperties({ApplicationProperties.class})
@Theme(value = "flowcrmtutorial")
@PWA(
    name = "Camera Application",
    shortName = "CAMAPP",
    offlinePath = "offline.html",
    offlineResources = {"images/offline.png"}
)
public class CameraAdministration implements AppShellConfigurator {

    private final static Logger LOGGER = LoggerFactory.getLogger(CameraAdministration.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(CameraAdministration.class);
        Environment env = app.run(args).getEnvironment();
        logApplicationStartup(env);
    }

    private static void logApplicationStartup(Environment env) {

        String protocol = "http";
        if (env.getProperty("server.ssl.key-store") != null) {
            protocol = "https";
        }
        String serverPort = env.getProperty("server.port");
        String contextPath = env.getProperty("server.servlet.context-path");
        if (!StringUtils.hasText(contextPath)) {
            contextPath = "/";
        }
        String hostAddress = "localhost";
        try {
            hostAddress = InetAddress.getLocalHost().getHostAddress();
        } catch (UnknownHostException e) {
            LOGGER.warn("The host name could not be determined, using `localhost` as fallback");
        }
        LOGGER.info("""
                ----------------------------------------------------------
                ~~~ Application '{}' is running! Access URLs:
                ~~~ - Local:      {}://localhost:{}{}
                ~~~ - External:   {}://{}:{}{}
                ~~~ Java version:      {} / {}
                ~~~ Processors:        {}
                ~~~ Profile(s):        {}
                ~~~ Default charset:   {}
                ~~~ File encoding:     {}
                ----------------------------------------------------------""",
            env.getProperty("spring.application.name"),
            protocol,
            serverPort,
            contextPath,
            protocol,
            hostAddress,
            serverPort,
            contextPath,
            System.getProperty("java.version"), System.getProperty("java.vm.name"),
            Runtime.getRuntime().availableProcessors(),
            env.getActiveProfiles(),
            Charset.defaultCharset().displayName(),
            System.getProperty("file.encoding")
        );
    }
}
