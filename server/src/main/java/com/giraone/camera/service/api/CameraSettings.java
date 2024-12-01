package com.giraone.camera.service.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.giraone.camera.service.api.serde.CustomDeserializerBoolean;
import com.giraone.camera.service.api.serde.CustomEnumDeserializerFrameSize;
import com.giraone.camera.service.api.serde.CustomEnumDeserializerLevel;
import com.giraone.camera.service.api.serde.CustomEnumDeserializerSpecialEffect;
import com.giraone.camera.service.api.serde.CustomEnumDeserializerWhiteBalanceMode;
import com.giraone.camera.service.api.serde.CustomSerializerBoolean;

import java.util.Arrays;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CameraSettings {

    private int clockFrequencyHz = 16000000;
    FrameSize frameSize = FrameSize.FRAMESIZE_UXGA;
    /**
     * 0 - 63 (smaller is less compression and better)
     */
    private int jpegQuality = 10;

    private boolean blackPixelCorrect;
    private boolean whitePixelCorrect;
    private boolean gammaCorrect = true;
    private boolean lensCorrect = true;

    private boolean horizontalMirror;
    private boolean verticalFlip;

    private Level brightness = Level.M;
    private Level contrast = Level.M;
    private Level sharpness = Level.M;
    private Level saturation = Level.M;
    private Level denoise = Level.M;
    private SpecialEffect specialEffect = SpecialEffect.None;

    private boolean autoWhitebalance = true;
    private boolean autoWhitebalanceGain = true;
    private WhiteBalanceMode whitebalanceMode = WhiteBalanceMode.Auto;

    private boolean exposureCtrlSensor = true;
    private boolean exposureCtrlDsp;
    private Level autoExposureLevel;
    /**
     * 0 - 1024
     **/
    private int autoExposureValue = 1000;
    private boolean autoExposureGainControl;
    /**
     * 0 - 30
     **/
    private int autoExposureGainValue = 25;
    /**
     * 0=2x, 1=4x, 2=8x, 3=16x, 4=32x, 5=64x, 6=128x
     **/
    private int autoExposureGainCeiling = 2;

    //------------------------------------------------------------------------------------------------------------------

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

    @JsonDeserialize(using = CustomDeserializerBoolean.class)
    public boolean isBlackPixelCorrect() {
        return blackPixelCorrect;
    }

    @JsonSerialize(using = CustomSerializerBoolean.class)
    public void setBlackPixelCorrect(boolean blackPixelCorrect) {
        this.blackPixelCorrect = blackPixelCorrect;
    }

    @JsonDeserialize(using = CustomDeserializerBoolean.class)
    public boolean isWhitePixelCorrect() {
        return whitePixelCorrect;
    }

    @JsonSerialize(using = CustomSerializerBoolean.class)
    public void setWhitePixelCorrect(boolean whitePixelCorrect) {
        this.whitePixelCorrect = whitePixelCorrect;
    }

    @JsonDeserialize(using = CustomDeserializerBoolean.class)
    public boolean isGammaCorrect() {
        return gammaCorrect;
    }

    @JsonSerialize(using = CustomSerializerBoolean.class)
    public void setGammaCorrect(boolean gammaCorrect) {
        this.gammaCorrect = gammaCorrect;
    }

    @JsonDeserialize(using = CustomDeserializerBoolean.class)
    public boolean isLensCorrect() {
        return lensCorrect;
    }

    @JsonSerialize(using = CustomSerializerBoolean.class)
    public void setLensCorrect(boolean lensCorrect) {
        this.lensCorrect = lensCorrect;
    }

    @JsonDeserialize(using = CustomDeserializerBoolean.class)
    public boolean isHorizontalMirror() {
        return horizontalMirror;
    }

    @JsonSerialize(using = CustomSerializerBoolean.class)
    public void setHorizontalMirror(boolean horizontalMirror) {
        this.horizontalMirror = horizontalMirror;
    }

    @JsonDeserialize(using = CustomDeserializerBoolean.class)
    public boolean isVerticalFlip() {
        return verticalFlip;
    }

    @JsonSerialize(using = CustomSerializerBoolean.class)
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

    @JsonDeserialize(using = CustomDeserializerBoolean.class)
    public boolean isAutoWhitebalance() {
        return autoWhitebalance;
    }

    @JsonSerialize(using = CustomSerializerBoolean.class)
    public void setAutoWhitebalance(boolean autoWhitebalance) {
        this.autoWhitebalance = autoWhitebalance;
    }

    @JsonDeserialize(using = CustomDeserializerBoolean.class)
    public boolean isAutoWhitebalanceGain() {
        return autoWhitebalanceGain;
    }

    @JsonSerialize(using = CustomSerializerBoolean.class)
    public void setAutoWhitebalanceGain(boolean autoWhitebalanceGain) {
        this.autoWhitebalanceGain = autoWhitebalanceGain;
    }

    public WhiteBalanceMode getWhitebalanceMode() {
        return whitebalanceMode;
    }

    public void setWhitebalanceMode(WhiteBalanceMode whitebalanceMode) {
        this.whitebalanceMode = whitebalanceMode;
    }

    @JsonDeserialize(using = CustomDeserializerBoolean.class)
    public boolean isExposureCtrlSensor() {
        return exposureCtrlSensor;
    }

    @JsonSerialize(using = CustomSerializerBoolean.class)
    public void setExposureCtrlSensor(boolean exposureCtrlSensor) {
        this.exposureCtrlSensor = exposureCtrlSensor;
    }

    @JsonDeserialize(using = CustomDeserializerBoolean.class)
    public boolean isExposureCtrlDsp() {
        return exposureCtrlDsp;
    }

    @JsonSerialize(using = CustomSerializerBoolean.class)
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

    @JsonDeserialize(using = CustomDeserializerBoolean.class)
    public boolean isAutoExposureGainControl() {
        return autoExposureGainControl;
    }

    @JsonSerialize(using = CustomSerializerBoolean.class)
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
            "clockFrequencyHz=" + clockFrequencyHz +
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
        FRAMESIZE_96X96("96x96"),
        FRAMESIZE_QQVGA("160x120 (QQVGA)"),
        FRAMESIZE_QCIF("176x144 (QCIF)"),
        FRAMESIZE_HQVGA("240x176 (HQVGA)"),
        FRAMESIZE_240X240("240x240"),
        FRAMESIZE_QVGA("320x240 (QVGA)"),
        FRAMESIZE_CIF("400x296 (CIF)"),
        FRAMESIZE_HVGA("480x320 (HVGA)"),
        FRAMESIZE_VGA("640x480 (VGA)"),
        FRAMESIZE_SVGA("800x600 (SVGA)"),
        FRAMESIZE_XGA("1024x768 (XGA)"),
        FRAMESIZE_HD(" 1280x720 (HD)"),
        FRAMESIZE_SXGA("1280x1024 (SXGA)"),
        FRAMESIZE_UXGA("1600x1200 (UXGA)"),
        FRAMESIZE_FHD("1920x1080 (FHD) - from here OV5640 only"),
        FRAMESIZE_P_HD("720x1280 (Portrait HD)"),
        FRAMESIZE_P_864("864x1536 (Portrait)"),
        FRAMESIZE_QXGA("2048x1536 (QXGA)"),
        FRAMESIZE_QHD("2560x1440 (QHD)"),
        FRAMESIZE_WQXGA("2560x1600 (WQXGA)"),
        FRAMESIZE_P_1088("1088x1920 (Portrait)"),
        FRAMESIZE_5MP("2560x1920 (5MP)");

        private final String label;

        FrameSize(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        @JsonValue
        public int getOrdinal() {
            return this.ordinal();
        }

        public static List<FrameSize> ALL = Arrays.asList(FrameSize.values());
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
