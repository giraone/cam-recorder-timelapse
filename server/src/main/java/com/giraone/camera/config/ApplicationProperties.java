package com.giraone.camera.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "application", ignoreUnknownFields = false)
public class ApplicationProperties {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationProperties.class);

    /**
     * Log the configuration to the log on startup
     */
    private boolean showConfigOnStartup;
    /**
     * If true, generate thumbnails for uploaded images.
     */
    private boolean generateThumbnails;

    public boolean isShowConfigOnStartup() {
        return showConfigOnStartup;
    }

    public void setShowConfigOnStartup(boolean showConfigOnStartup) {
        this.showConfigOnStartup = showConfigOnStartup;
    }

    public boolean isGenerateThumbnails() {
        return generateThumbnails;
    }

    public void setGenerateThumbnails(boolean generateThumbnails) {
        this.generateThumbnails = generateThumbnails;
    }

    @Override
    public String toString() {
        return "ApplicationProperties{" +
            "showConfigOnStartup=" + showConfigOnStartup +
            ", generateThumbnails=" + generateThumbnails +
            '}';
    }

    @PostConstruct
    private void startup() {
        if (this.showConfigOnStartup) {
            LOGGER.info(this.toString());
        }
    }
}
