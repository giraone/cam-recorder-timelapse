#pragma once
#include "esp_camera.h"
#include <ArduinoJson.h>

// A pimped version of framesize_t from esp_camera.h
typedef enum {
    EXT_FRAMESIZE_96X96,    // 96x96
    EXT_FRAMESIZE_QQVGA,    // 160x120
    EXT_FRAMESIZE_QQVGA2,   // 128x160
    EXT_FRAMESIZE_QCIF,     // 176x144
    EXT_FRAMESIZE_HQVGA,    // 240x176
    EXT_FRAMESIZE_240X240,  // 240x240
    EXT_FRAMESIZE_QVGA,     // 320x240
    EXT_FRAMESIZE_CIF,      // 400x296
    EXT_FRAMESIZE_HVGA,     // 480x320
    EXT_FRAMESIZE_VGA,      // 640x480
    EXT_FRAMESIZE_SVGA,     // 800x600
    EXT_FRAMESIZE_XGA,      // 1024x768
    EXT_FRAMESIZE_HD,       // 1280x720
    EXT_FRAMESIZE_SXGA,     // 1280x1024
    EXT_FRAMESIZE_UXGA,     // 1600x1200 - up to here possible with OV2640
    EXT_FRAMESIZE_FHD,      // 1920x1080 - from here onyl possible with OV5640
    EXT_FRAMESIZE_P_HD,     // 720x1280
    EXT_FRAMESIZE_P_3MP,    // 864x1536
    EXT_FRAMESIZE_QXGA,     // 2048x1536
    EXT_FRAMESIZE_QHD,      // 2560x1440
    EXT_FRAMESIZE_WQXGA,    // 2560x1600
    EXT_FRAMESIZE_5MP,      // 2592x1944
    EXT_FRAMESIZE_INVALID
} ext_framesize_t;

short initCameraWithSettings(JsonDocument cameraSetings);
