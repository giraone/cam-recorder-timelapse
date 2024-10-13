package com.giraone.streaming.service.model;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.giraone.streaming.service.model.serde.CustomEnumDeserializerFrameSize;
import com.giraone.streaming.service.model.serde.CustomEnumDeserializerLevel;
import com.giraone.streaming.service.model.serde.CustomEnumDeserializerSpecialEffect;
import com.giraone.streaming.service.model.serde.CustomEnumDeserializerWhiteBalanceMode;

import java.util.List;

public class CameraSettings {

    public int loopDelaySeconds = 10;
    public int clockFrequencyHz = 16000000;
    FrameSize frameSize = FrameSize.FRAMESIZE_UXGA;
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

    public Level brightness = Level.M;
    public Level contrast = Level.M;
    public Level sharpness = Level.M;
    public Level saturation = Level.M;
    public Level denoise = Level.M;
    public SpecialEffect specialEffect = SpecialEffect.None;

    public boolean autoWhitebalance = true;
    public boolean autoWhitebalanceGain = true;
    public WhiteBalanceMode whitebalanceMode = WhiteBalanceMode.Auto;

    public boolean exposureCtrlSensor = true;
    public boolean exposureCtrlDsp;
    public Level autoExposureLevel;
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

    public FrameSize getFrameSize() {
        return frameSize;
    }

    public void setFrameSize(FrameSize frameSize) {
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

    public Level getBrightness() {
        return brightness;
    }

    public void setBrightness(Level brightness) {
        this.brightness = brightness;
    }

    public Level getContrast() {
        return contrast;
    }

    public void setContrast(Level contrast) {
        this.contrast = contrast;
    }

    public Level getSharpness() {
        return sharpness;
    }

    public void setSharpness(Level sharpness) {
        this.sharpness = sharpness;
    }

    public Level getSaturation() {
        return saturation;
    }

    public void setSaturation(Level saturation) {
        this.saturation = saturation;
    }

    public Level getDenoise() {
        return denoise;
    }

    public void setDenoise(Level denoise) {
        this.denoise = denoise;
    }

    public SpecialEffect getSpecialEffect() {
        return specialEffect;
    }

    public void setSpecialEffect(SpecialEffect specialEffect) {
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

    public WhiteBalanceMode getWhitebalanceMode() {
        return whitebalanceMode;
    }

    public void setWhitebalanceMode(WhiteBalanceMode whitebalanceMode) {
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

    public Level getAutoExposureLevel() {
        return autoExposureLevel;
    }

    public void setAutoExposureLevel(Level autoExposureLevel) {
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

    //------------------------------------------------------------------------------------------------------------------

    @JsonDeserialize(using = CustomEnumDeserializerFrameSize.class)
    public enum FrameSize {
        FRAMESIZE_96X96,    // 96x96
        FRAMESIZE_QQVGA,    // 160x120
        FRAMESIZE_QCIF,     // 176x144
        FRAMESIZE_HQVGA,    // 240x176
        FRAMESIZE_240X240,  // 240x240
        FRAMESIZE_QVGA,     // 320x240
        FRAMESIZE_CIF,      // 400x296
        FRAMESIZE_HVGA,     // 480x320
        FRAMESIZE_VGA,      // 640x480
        FRAMESIZE_SVGA,     // 800x600
        FRAMESIZE_XGA,      // 1024x768
        FRAMESIZE_HD,       // 1280x720
        FRAMESIZE_SXGA,     // 1280x1024
        FRAMESIZE_UXGA;     // 1600x1200

        @JsonValue
        public int getOrdinal() {
            return this.ordinal();
        }

        public static List<FrameSize> ALL = List.of(
            FRAMESIZE_96X96, FRAMESIZE_QQVGA, FRAMESIZE_QCIF, FRAMESIZE_HQVGA, FRAMESIZE_240X240,
            FRAMESIZE_QVGA, FRAMESIZE_CIF, FRAMESIZE_HVGA, FRAMESIZE_VGA, FRAMESIZE_SVGA,
            FRAMESIZE_XGA, FRAMESIZE_HD, FRAMESIZE_SXGA, FRAMESIZE_UXGA
        );
    }

    @JsonDeserialize(using = CustomEnumDeserializerLevel.class)
    public enum Level {
        XS(-2), S(-1), M(0), L(1), XL(2);

        @JsonValue
        private final int intValue;

        Level(int intValue) {
            this.intValue = intValue;
        }

        public static List<Level> ALL = List.of(XS, S, M, L, XL);
    }

    @JsonDeserialize(using = CustomEnumDeserializerSpecialEffect.class)
    public enum SpecialEffect {
        None, Negative, Grayscale, Red, Green, Blue, Sepia;

        @JsonValue
        public int getOrdinal() {
            return this.ordinal();
        }

        public static List<SpecialEffect> ALL = List.of(None, Negative, Grayscale, Red, Green, Blue, Sepia);
    }

    @JsonDeserialize(using = CustomEnumDeserializerWhiteBalanceMode.class)
    public enum WhiteBalanceMode {
        Auto, Sunny, Cloudy, Office, Home;

        @JsonValue
        public int getOrdinal() {
            return this.ordinal();
        }

        public static List<WhiteBalanceMode> ALL = List.of(Auto, Sunny, Cloudy, Office, Home);
    }
}
