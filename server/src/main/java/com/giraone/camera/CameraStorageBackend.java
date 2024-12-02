package com.giraone.camera;

import com.giraone.camera.config.ApplicationProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;

@SpringBootApplication
@EnableConfigurationProperties({ApplicationProperties.class})
public class CameraStorageBackend {

    private static final Logger LOGGER = LoggerFactory.getLogger(CameraStorageBackend.class);

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(CameraStorageBackend.class);
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

        MemoryUsage memoryUsage = ManagementFactory.getMemoryMXBean().getHeapMemoryUsage();
        long xmx = memoryUsage.getMax() / 1024 / 1024;
        long xms = memoryUsage.getInit() / 1024 / 1024;
        LOGGER.info("""
                ----------------------------------------------------------
                ~~~ Application '{}' is running! Access URLs:
                ~~~ - Local:      {}://localhost:{}{}
                ~~~ - External:   {}://{}:{}{}
                ~~~ Java version:      {} / {}
                ~~~ Processors:        {}
                ~~~ Memory (xms/xmx):  {} MB / {} MB
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
            xms, xmx,
            env.getActiveProfiles(),
            Charset.defaultCharset().displayName(),
            System.getProperty("file.encoding")
        );
    }
}
