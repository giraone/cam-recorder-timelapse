package com.giraone.camera.service.api;

import org.jspecify.annotations.NonNull;

public class VideoCreationSettings {

    private static final VideoCreationSettings CURRENT = new VideoCreationSettings();

    /**
     * Modulo select of images (1 = Use every image, 2 = use every 2nd image)
     */
    private int moduloSelectImage = 1;

    /**
     * Frame rate (5-60) - Normal = 25 or 30 fps
     */
    private int frameRate = 25;

    //------------------------------------------------------------------------------------------------------------------

    public VideoCreationSettings() {
    }

    public VideoCreationSettings(int moduloSelectImage, int frameRate) {
        this.moduloSelectImage = moduloSelectImage;
        this.frameRate = frameRate;
    }

    //------------------------------------------------------------------------------------------------------------------

    public static VideoCreationSettings getCurrent() {
        return new VideoCreationSettings(CURRENT.moduloSelectImage, CURRENT.frameRate);
    }

    public static void setCurrent(@NonNull VideoCreationSettings videoCreationSettings) {
        CURRENT.moduloSelectImage = videoCreationSettings.getModuloSelectImage();
        CURRENT.frameRate = videoCreationSettings.getFrameRate();
    }

    //------------------------------------------------------------------------------------------------------------------

    public int getModuloSelectImage() {
        return moduloSelectImage;
    }

    public void setModuloSelectImage(int moduloSelectImage) {
        this.moduloSelectImage = moduloSelectImage;
    }

    public int getFrameRate() {
        return frameRate;
    }

    public void setFrameRate(int frameRate) {
        this.frameRate = frameRate;
    }

    @Override
    public String toString() {
        return "VideoCreationSettings{" +
            "moduloSelectImage=" + moduloSelectImage +
            ", frameRate=" + frameRate +
            '}';
    }
}
