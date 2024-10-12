package com.giraone.streaming.service.model;

import com.giraone.streaming.service.CameraSettingsService;

public class CameraSettings {

    public int loopDelaySeconds = 10;
    public int clockFrequencyHz = 16000000;
    CameraSettingsService.FrameSize frameSize = CameraSettingsService.FrameSize.FRAMESIZE_UXGA;
    /**
     * 0 - 63 (smaller is less compression and better)
     */
    public int jpegQuality = 10;

    public boolean blackPixelCorrect;
    public boolean whitePixelCorrect;
    public boolean gammaCorrect = true;
    public boolean lensCorrect = true;

    public boolean horizontalMirror;
    public boolean verticalFlip;

    public CameraSettingsService.Level brightness = CameraSettingsService.Level.M;
    public CameraSettingsService.Level contrast = CameraSettingsService.Level.M;
    public CameraSettingsService.Level sharpness = CameraSettingsService.Level.M;
    public CameraSettingsService.Level saturation = CameraSettingsService.Level.M;
    public CameraSettingsService.Level denoise = CameraSettingsService.Level.M;
    public CameraSettingsService.SpecialEffect specialEffect = CameraSettingsService.SpecialEffect.None;

    public boolean autoWhitebalance = true;
    public boolean autoWhitebalanceGain = true;
    public CameraSettingsService.WhiteBalanceMode whitebalanceMode = CameraSettingsService.WhiteBalanceMode.Auto;

    public boolean exposureCtrlSensor = true;
    public boolean exposureCtrlDsp;
    public CameraSettingsService.Level autoExposureLevel;
    /** 0 - 1024 **/
    public int autoExposureValue = 1000;
    public boolean autoExposureGainControl;
    /** 0 - 30 **/
    public int autoExposureGainValue = 25;
    /** 0=2x, 1=4x, 2=8x, 3=16x, 4=32x, 5=64x, 6=128x **/
    public int autoExposureGainCeiling = 2;

    public int getLoopDelaySeconds() {
        return loopDelaySeconds;
    }

    public void setLoopDelaySeconds(int loopDelaySeconds) {
        this.loopDelaySeconds = loopDelaySeconds;
    }

    public int getClockFrequencyHz() {
        return clockFrequencyHz;
    }

    public void setClockFrequencyHz(int clockFrequencyHz) {
        this.clockFrequencyHz = clockFrequencyHz;
    }

    public CameraSettingsService.FrameSize getFrameSize() {
        return frameSize;
    }

    public void setFrameSize(CameraSettingsService.FrameSize frameSize) {
        this.frameSize = frameSize;
    }

    public int getJpegQuality() {
        return jpegQuality;
    }

    public void setJpegQuality(int jpegQuality) {
        this.jpegQuality = jpegQuality;
    }

    public boolean isBlackPixelCorrect() {
        return blackPixelCorrect;
    }

    public void setBlackPixelCorrect(boolean blackPixelCorrect) {
        this.blackPixelCorrect = blackPixelCorrect;
    }

    public boolean isWhitePixelCorrect() {
        return whitePixelCorrect;
    }

    public void setWhitePixelCorrect(boolean whitePixelCorrect) {
        this.whitePixelCorrect = whitePixelCorrect;
    }

    public boolean isGammaCorrect() {
        return gammaCorrect;
    }

    public void setGammaCorrect(boolean gammaCorrect) {
        this.gammaCorrect = gammaCorrect;
    }

    public boolean isLensCorrect() {
        return lensCorrect;
    }

    public void setLensCorrect(boolean lensCorrect) {
        this.lensCorrect = lensCorrect;
    }

    public boolean isHorizontalMirror() {
        return horizontalMirror;
    }

    public void setHorizontalMirror(boolean horizontalMirror) {
        this.horizontalMirror = horizontalMirror;
    }

    public boolean isVerticalFlip() {
        return verticalFlip;
    }

    public void setVerticalFlip(boolean verticalFlip) {
        this.verticalFlip = verticalFlip;
    }

    public CameraSettingsService.Level getBrightness() {
        return brightness;
    }

    public void setBrightness(CameraSettingsService.Level brightness) {
        this.brightness = brightness;
    }

    public CameraSettingsService.Level getContrast() {
        return contrast;
    }

    public void setContrast(CameraSettingsService.Level contrast) {
        this.contrast = contrast;
    }

    public CameraSettingsService.Level getSharpness() {
        return sharpness;
    }

    public void setSharpness(CameraSettingsService.Level sharpness) {
        this.sharpness = sharpness;
    }

    public CameraSettingsService.Level getSaturation() {
        return saturation;
    }

    public void setSaturation(CameraSettingsService.Level saturation) {
        this.saturation = saturation;
    }

    public CameraSettingsService.Level getDenoise() {
        return denoise;
    }

    public void setDenoise(CameraSettingsService.Level denoise) {
        this.denoise = denoise;
    }

    public CameraSettingsService.SpecialEffect getSpecialEffect() {
        return specialEffect;
    }

    public void setSpecialEffect(CameraSettingsService.SpecialEffect specialEffect) {
        this.specialEffect = specialEffect;
    }

    public boolean isAutoWhitebalance() {
        return autoWhitebalance;
    }

    public void setAutoWhitebalance(boolean autoWhitebalance) {
        this.autoWhitebalance = autoWhitebalance;
    }

    public boolean isAutoWhitebalanceGain() {
        return autoWhitebalanceGain;
    }

    public void setAutoWhitebalanceGain(boolean autoWhitebalanceGain) {
        this.autoWhitebalanceGain = autoWhitebalanceGain;
    }

    public CameraSettingsService.WhiteBalanceMode getWhitebalanceMode() {
        return whitebalanceMode;
    }

    public void setWhitebalanceMode(CameraSettingsService.WhiteBalanceMode whitebalanceMode) {
        this.whitebalanceMode = whitebalanceMode;
    }

    public boolean isExposureCtrlSensor() {
        return exposureCtrlSensor;
    }

    public void setExposureCtrlSensor(boolean exposureCtrlSensor) {
        this.exposureCtrlSensor = exposureCtrlSensor;
    }

    public boolean isExposureCtrlDsp() {
        return exposureCtrlDsp;
    }

    public void setExposureCtrlDsp(boolean exposureCtrlDsp) {
        this.exposureCtrlDsp = exposureCtrlDsp;
    }

    public CameraSettingsService.Level getAutoExposureLevel() {
        return autoExposureLevel;
    }

    public void setAutoExposureLevel(CameraSettingsService.Level autoExposureLevel) {
        this.autoExposureLevel = autoExposureLevel;
    }

    public int getAutoExposureValue() {
        return autoExposureValue;
    }

    public void setAutoExposureValue(int autoExposureValue) {
        this.autoExposureValue = autoExposureValue;
    }

    public boolean isAutoExposureGainControl() {
        return autoExposureGainControl;
    }

    public void setAutoExposureGainControl(boolean autoExposureGainControl) {
        this.autoExposureGainControl = autoExposureGainControl;
    }

    public int getAutoExposureGainValue() {
        return autoExposureGainValue;
    }

    public void setAutoExposureGainValue(int autoExposureGainValue) {
        this.autoExposureGainValue = autoExposureGainValue;
    }

    public int getAutoExposureGainCeiling() {
        return autoExposureGainCeiling;
    }

    public void setAutoExposureGainCeiling(int autoExposureGainCeiling) {
        this.autoExposureGainCeiling = autoExposureGainCeiling;
    }

    @Override
    public String toString() {
        return "CameraSettings{" +
            "loopDelaySeconds=" + loopDelaySeconds +
            ", clockFrequencyHz=" + clockFrequencyHz +
            ", frameSize=" + frameSize +
            ", jpegQuality=" + jpegQuality +
            ", blackPixelCorrect=" + blackPixelCorrect +
            ", whitePixelCorrect=" + whitePixelCorrect +
            ", gammaCorrect=" + gammaCorrect +
            ", lensCorrect=" + lensCorrect +
            ", horizontalMirror=" + horizontalMirror +
            ", verticalFlip=" + verticalFlip +
            ", brightness=" + brightness +
            ", contrast=" + contrast +
            ", sharpness=" + sharpness +
            ", saturation=" + saturation +
            ", denoise=" + denoise +
            ", specialEffect=" + specialEffect +
            ", autoWhitebalance=" + autoWhitebalance +
            ", autoWhitebalanceGain=" + autoWhitebalanceGain +
            ", whitebalanceMode=" + whitebalanceMode +
            ", exposureCtrlSensor=" + exposureCtrlSensor +
            ", exposureCtrlDsp=" + exposureCtrlDsp +
            ", autoExposureLevel=" + autoExposureLevel +
            ", autoExposureValue=" + autoExposureValue +
            ", autoExposureGainControl=" + autoExposureGainControl +
            ", autoExposureGainValue=" + autoExposureGainValue +
            ", autoExposureGainCeiling=" + autoExposureGainCeiling +
            '}';
    }
}

