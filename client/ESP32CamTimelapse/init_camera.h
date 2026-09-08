#pragma once
#include "esp_camera.h"
#include <ArduinoJson.h>

// The frame sizes offered by the server UI. The ordinals MUST match the enum
// CameraSettings.FrameSize on the server side, because the server sends the ordinal.
// They are deliberately NOT the values of framesize_t from esp_camera.h - that enum
// contains additional entries (128X128, 320X320), see FRAME_SIZES in init_camera.cpp.
typedef enum {
    EXT_FRAMESIZE_96X96,    //  0 - 96x96
    EXT_FRAMESIZE_QQVGA,    //  1 - 160x120
    EXT_FRAMESIZE_QCIF,     //  2 - 176x144
    EXT_FRAMESIZE_HQVGA,    //  3 - 240x176
    EXT_FRAMESIZE_240X240,  //  4 - 240x240
    EXT_FRAMESIZE_QVGA,     //  5 - 320x240
    EXT_FRAMESIZE_CIF,      //  6 - 400x296
    EXT_FRAMESIZE_HVGA,     //  7 - 480x320
    EXT_FRAMESIZE_VGA,      //  8 - 640x480
    EXT_FRAMESIZE_SVGA,     //  9 - 800x600
    EXT_FRAMESIZE_XGA,      // 10 - 1024x768
    EXT_FRAMESIZE_HD,       // 11 - 1280x720
    EXT_FRAMESIZE_SXGA,     // 12 - 1280x1024
    EXT_FRAMESIZE_UXGA,     // 13 - 1600x1200 - up to here possible with OV2640
    EXT_FRAMESIZE_FHD,      // 14 - 1920x1080 - from here only possible with OV5640
    EXT_FRAMESIZE_P_HD,     // 15 - 720x1280
    EXT_FRAMESIZE_P_864,    // 16 - 864x1536
    EXT_FRAMESIZE_QXGA,     // 17 - 2048x1536
    EXT_FRAMESIZE_QHD,      // 18 - 2560x1440
    EXT_FRAMESIZE_WQXGA,    // 19 - 2560x1600
    EXT_FRAMESIZE_P_1088,   // 20 - 1088x1920
    EXT_FRAMESIZE_5MP,      // 21 - 2560x1920
    EXT_FRAMESIZE_INVALID   // 22
} ext_framesize_t;

// Passed as a read-only reference into the document - no copy of the settings is made
short initCameraWithSettings(JsonVariantConst cameraSettings);
