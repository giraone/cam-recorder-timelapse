package com.giraone.camera.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.net.InetAddress;
import java.net.UnknownHostException;

@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationProperties.class);

    /**
     * Log the configuration to the log on startup
     */
    private boolean showConfigOnStartup;

    /**
     * URL for downloading
     */
    private String hostUrl;

    public ApplicationProperties() {
        try {
            hostUrl = "http://" + InetAddress.getLocalHost().getHostName() + ":9001";
        } catch (UnknownHostException e) {
            LOGGER.warn("Cannot obtain host name!");
            hostUrl = "http://localhost:9001";
        }
    }

    public boolean isShowConfigOnStartup() {
        return showConfigOnStartup;
    }

    public void setShowConfigOnStartup(boolean showConfigOnStartup) {
        this.showConfigOnStartup = showConfigOnStartup;
    }

    public String getHostUrl() {
        return hostUrl;
    }

    public void setHostUrl(String hostUrl) {
        this.hostUrl = hostUrl;
    }

    @Override
    public String toString() {
        return "ApplicationProperties{" +
            "showConfigOnStartup=" + showConfigOnStartup +
            ", hostUrl='" + hostUrl + '\'' +
            '}';
    }

    @PostConstruct
    private void startup() {
        if (this.showConfigOnStartup) {
            LOGGER.info(this.toString());
        }
    }
}
