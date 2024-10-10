package com.giraone.streaming.service;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class CameraSettingsService {

    public Map<String, Object> getSettings() {
        Map<String, Object> ret = new HashMap<>();

        ret.put("loopDelaySeconds", 5);

        ret.put("clockFrequencyHz", 16000000);
        ret.put("frameSize", Resolution.FRAMESIZE_UXGA.intValue);
        ret.put("jpegQuality", 10); // 0 - 63 (smaller is less compression and better)

        ret.put("backPixelCorrect", false);
        ret.put("whitePixelCorrect", false);
        ret.put("gammaCorrect", true);
        ret.put("lensCorrect", true);

        ret.put("horizontalMirror", false);
        ret.put("verticalFlip", false);

        ret.put("brightness", Level.M.intValue);
        ret.put("contrast", Level.M.intValue);
        ret.put("sharpness", Level.M.intValue);
        ret.put("saturation", Level.M.intValue);
        ret.put("denoise", Level.M.intValue);

        ret.put("specialEffect", SpecialEffect.None.intValue);

        ret.put("autoWhitebalance", true);
        ret.put("autoWhitebalanceGain", true);
        ret.put("whitebalanceMode", WhiteBalanceMode.Auto.intValue);

        ret.put("exposureCtrlSensor", true);
        ret.put("exposureCtrlDsp", false);
        ret.put("autoExposureLevel", Level.L.intValue);
        ret.put("autoExposureValue", 1000); // 0 - 1024
        ret.put("autoExposureGainControl", true);
        ret.put("autoExposureGainValue", 25); // 0 - 30
        ret.put("autoExposureGainCeiling", 2); // 0=2x, 1=4x, 2=8x, 3=16x, 4=32x, 5=64x, 6=128x

        return ret;
    }

    private static final AtomicInteger RESOLUTION_COUNTER = new AtomicInteger();

    enum Resolution {
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

        private final int intValue;

        Resolution() {
            this.intValue = RESOLUTION_COUNTER.getAndIncrement();
        }
    }

    enum Level {
        XS(-2), S(-1), M(0), L(1), XL(2);

        private final int intValue;

        Level(int intValue) {
            this.intValue = intValue;
        }
    }

    enum SpecialEffect {
        None(0), Negative(1), Grayscale(2), Red(3), Green(4), Blue(5), Sepia(6);

        private final int intValue;

        SpecialEffect(int intValue) {
            this.intValue = intValue;
        }
    }

    enum WhiteBalanceMode {
        Auto(0), Sunny(1), Cloudy(2), Office(3), Home(4);

        private final int intValue;

        WhiteBalanceMode(int intValue) {
            this.intValue = intValue;
        }
    }
}
