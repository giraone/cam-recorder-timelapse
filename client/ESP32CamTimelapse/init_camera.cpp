#include <Arduino.h>
#include "init_camera.h"

void initCamera() {
  static const int8_t PWDN_GPIO_NUM = 32;
  static const int8_t RESET_GPIO_NUM = -1;
  static const int8_t XCLK_GPIO_NUM = 0;
  static const int8_t SIOD_GPIO_NUM = 26;
  static const int8_t SIOC_GPIO_NUM = 27;
  static const int8_t Y9_GPIO_NUM = 35;
  static const int8_t Y8_GPIO_NUM = 34;
  static const int8_t Y7_GPIO_NUM = 39;
  static const int8_t Y6_GPIO_NUM = 36;
  static const int8_t Y5_GPIO_NUM = 21;
  static const int8_t Y4_GPIO_NUM = 19;
  static const int8_t Y3_GPIO_NUM = 18;
  static const int8_t Y2_GPIO_NUM = 5;
  static const int8_t VSYNC_GPIO_NUM = 25;
  static const int8_t HREF_GPIO_NUM = 23;
  static const int8_t PCLK_GPIO_NUM = 22;

  camera_config_t config = {
    .pin_pwdn = PWDN_GPIO_NUM,
    .pin_reset = RESET_GPIO_NUM,
    .pin_xclk = XCLK_GPIO_NUM,
    .pin_sscb_sda = SIOD_GPIO_NUM,
    .pin_sscb_scl = SIOC_GPIO_NUM,
    .pin_d7 = Y9_GPIO_NUM,
    .pin_d6 = Y8_GPIO_NUM,
    .pin_d5 = Y7_GPIO_NUM,
    .pin_d4 = Y6_GPIO_NUM,
    .pin_d3 = Y5_GPIO_NUM,
    .pin_d2 = Y4_GPIO_NUM,
    .pin_d1 = Y3_GPIO_NUM,
    .pin_d0 = Y2_GPIO_NUM,
    .pin_vsync = VSYNC_GPIO_NUM,
    .pin_href = HREF_GPIO_NUM,
    .pin_pclk = PCLK_GPIO_NUM,

    // Frequency of XCLK signal, in Hz. Default: 20000000
    // Set to 16MHz on ESP32-S2 or ESP32-S3 to enable EDMA mode
    .xclk_freq_hz = 16000000,
    .ledc_timer = LEDC_TIMER_0,
    .ledc_channel = LEDC_CHANNEL_0,
    .pixel_format = PIXFORMAT_JPEG,
    // QVGA|CIF|VGA|SVGA|XGA|SXGA|UXGA
    .frame_size = FRAMESIZE_UXGA,
    // 0 - 63
    .jpeg_quality = 10,
    .fb_count = 2,
    .grab_mode = CAMERA_GRAB_LATEST
  };
  
  const esp_err_t status = esp_camera_init(&config);
  if (status != ESP_OK) {
    Serial.printf(">>> Camera not initialized: status=0x%x", status);
    return;
  }

  sensor_t* sensor = esp_camera_sensor_get();

  // Brightness - -2,-1,0,1,2
  sensor->set_brightness(sensor, 0);
  // Contrast - -2,-1,0,1,2
  sensor->set_contrast(sensor, 0);
  // Sharpness - -2,-1,0,1,2
  sensor->set_sharpness(sensor, 0);
  // Saturation - -2,-1,0,1,2
  sensor->set_saturation(sensor, 0);
  // Denoise - -2,-1,0,1,2
  sensor->set_denoise(sensor, 0);

  // Special color effects - 0=None|1=Negative|2=Grayscale|3=Red Tint|4=Green Tint|5=Blue Int|6=Sepia
  sensor->set_special_effect(sensor, 0);

  // Auto White Balance - 0: no, 1: yes
  sensor->set_whitebal(sensor, 1);
  // Auto White Balance Gain - 0: no, 1: yes - nur bei yes kann man set_wb_mode nutzen
  sensor->set_awb_gain(sensor, 1);
  // White Balance Mode - 0=Auto|1=Sunny|2=Cloudy|3=Office|4=Home
  sensor->set_wb_mode(sensor, 0);

  // Auto Exposure Control (via Sensor) - 0: no, 1: yes
  sensor->set_exposure_ctrl(sensor, 1);
  // Auto Exposure Control (via DSP) - 0: no, 1: yes
  sensor->set_aec2(sensor, 0);
  // Auto Exposure Level - -2,-1,0,1,2
  sensor->set_ae_level(sensor, 0);
  // Auto Exposure Value - 0 - 1024
  sensor->set_aec_value(sensor, 300);
  // Auto Gain Control - 0: no, 1: yes
  sensor->set_gain_ctrl(sensor, 1);
  // Auto Gain Control Gain - 0 - 30 - Mit 0 starten und erhöhen, wenn zu dunkel 
  sensor->set_agc_gain(sensor, 20);
  // Gain ceiling - 0=2x, 1=4x, 2=8x, 3=16x, 4=32x, 5=64x, 6=128x
  sensor->set_gainceiling(sensor, (gainceiling_t) 2);

  // Black Pixel Correct - 0: no, 1: yes
  sensor->set_bpc(sensor, 0);
  // White Pixel Correct - 0: no, 1: yes
  sensor->set_wpc(sensor, 1);
  // Auto Gamma Correct: no, 1: yes - mit yes i.d.R. besser
  sensor->set_raw_gma(sensor, 1);
  // Auto Lens Correct - 0: no, 1: yes - mit yes i.d.R. besser
  sensor->set_lenc(sensor, 1);
  // Horizontal mirror image - 0: no, 1: yes
  sensor->set_hmirror(sensor, 0);
  // Vertical flip image - 0: no, 1: yes 
  sensor->set_vflip(sensor, 0);
  // Downscale image - 0: no, 1: yes - Wenn eine andere Auflösung als UXGA gewählt ist, dann muss das 1 sein!
  sensor->set_dcw(sensor, 1);
  // Display Test Color-Bar
  sensor->set_colorbar(sensor, 0);
}

framesize_t framesizeFromInt(int value) {
  switch(value) {
    case 0:
      return framesize_t::FRAMESIZE_96X96;
    case 1:
      return framesize_t::FRAMESIZE_QQVGA;
    case 2:
      return framesize_t::FRAMESIZE_QCIF;
    case 3:
      return framesize_t::FRAMESIZE_HQVGA;
    case 4:
      return framesize_t::FRAMESIZE_240X240;
    case 5:
      return framesize_t::FRAMESIZE_CIF;
    case 6:
      return framesize_t::FRAMESIZE_HVGA;
    case 7:
      return framesize_t::FRAMESIZE_VGA;
    case 8:
      return framesize_t::FRAMESIZE_SVGA;
    case 9:
      return framesize_t::FRAMESIZE_XGA;
    case 10:
      return framesize_t::FRAMESIZE_HD;
    case 11:
      return framesize_t::FRAMESIZE_HQVGA;
    case 12:
      return framesize_t::FRAMESIZE_SXGA;
    case 13:
      return framesize_t::FRAMESIZE_UXGA;
    default:
      return framesize_t::FRAMESIZE_UXGA;
  }
}

gainceiling_t gainceilingFromInt(int value) {
  switch(value) {
    case 0:
      return gainceiling_t::GAINCEILING_2X;
    case 1:
      return gainceiling_t::GAINCEILING_4X;
    case 2:
      return gainceiling_t::GAINCEILING_8X;
    case 3:
      return gainceiling_t::GAINCEILING_16X;
    case 4:
      return gainceiling_t::GAINCEILING_32X;
    case 5:
      return gainceiling_t::GAINCEILING_64X;
    case 6:
      return gainceiling_t::GAINCEILING_128X;
    default:
      return gainceiling_t::GAINCEILING_2X;
  }
}

void initCameraWithSettings(JSONVar settingsJson) {
  static const int8_t PWDN_GPIO_NUM = 32;
  static const int8_t RESET_GPIO_NUM = -1;
  static const int8_t XCLK_GPIO_NUM = 0;
  static const int8_t SIOD_GPIO_NUM = 26;
  static const int8_t SIOC_GPIO_NUM = 27;
  static const int8_t Y9_GPIO_NUM = 35;
  static const int8_t Y8_GPIO_NUM = 34;
  static const int8_t Y7_GPIO_NUM = 39;
  static const int8_t Y6_GPIO_NUM = 36;
  static const int8_t Y5_GPIO_NUM = 21;
  static const int8_t Y4_GPIO_NUM = 19;
  static const int8_t Y3_GPIO_NUM = 18;
  static const int8_t Y2_GPIO_NUM = 5;
  static const int8_t VSYNC_GPIO_NUM = 25;
  static const int8_t HREF_GPIO_NUM = 23;
  static const int8_t PCLK_GPIO_NUM = 22;

  camera_config_t config = {
    .pin_pwdn = PWDN_GPIO_NUM,
    .pin_reset = RESET_GPIO_NUM,
    .pin_xclk = XCLK_GPIO_NUM,
    .pin_sscb_sda = SIOD_GPIO_NUM,
    .pin_sscb_scl = SIOC_GPIO_NUM,
    .pin_d7 = Y9_GPIO_NUM,
    .pin_d6 = Y8_GPIO_NUM,
    .pin_d5 = Y7_GPIO_NUM,
    .pin_d4 = Y6_GPIO_NUM,
    .pin_d3 = Y5_GPIO_NUM,
    .pin_d2 = Y4_GPIO_NUM,
    .pin_d1 = Y3_GPIO_NUM,
    .pin_d0 = Y2_GPIO_NUM,
    .pin_vsync = VSYNC_GPIO_NUM,
    .pin_href = HREF_GPIO_NUM,
    .pin_pclk = PCLK_GPIO_NUM,

    // Frequency of XCLK signal, in Hz. Default: 20000000
    // Set to 16MHz on ESP32-S2 or ESP32-S3 to enable EDMA mode
    .xclk_freq_hz = settingsJson["clockFrequencyHz"],
    .ledc_timer = LEDC_TIMER_0,
    .ledc_channel = LEDC_CHANNEL_0,
    .pixel_format = PIXFORMAT_JPEG,
    // QVGA|CIF|VGA|SVGA|XGA|SXGA|UXGA
    .frame_size = framesizeFromInt(settingsJson["frameSize"]),
    // 0 - 63 (smaller is less compression and better)
    .jpeg_quality = settingsJson["jpegQuality"],
    .fb_count = 2,
    .grab_mode = CAMERA_GRAB_LATEST
  };
  
  const esp_err_t status = esp_camera_init(&config);
  if (status != ESP_OK) {
    Serial.printf(">>> Camera not initialized: status=0x%x", status);
    return;
  } else {
    Serial.println(">>> Camera successfully initialized.");
  }

  sensor_t* sensor = esp_camera_sensor_get();

  // Brightness - -2,-1,0,1,2
  sensor->set_brightness(sensor, settingsJson["brightness"]);
  // Contrast - -2,-1,0,1,2
  sensor->set_contrast(sensor, settingsJson["contrast"]);
  // Sharpness - -2,-1,0,1,2
  sensor->set_sharpness(sensor, settingsJson["sharpness"]);
  // Saturation - -2,-1,0,1,2
  sensor->set_saturation(sensor, settingsJson["saturation"]);
  // Denoise - -2,-1,0,1,2
  sensor->set_denoise(sensor, settingsJson["denoise"]);

  // Special color effects - 0=None|1=Negative|2=Grayscale|3=Red Tint|4=Green Tint|5=Blue Int|6=Sepia
  sensor->set_special_effect(sensor, settingsJson["specialEffect"]);

  // Auto White Balance - 0: no, 1: yes
  sensor->set_whitebal(sensor, settingsJson["autoWhitebalance"]);
  // Auto White Balance Gain - 0: no, 1: yes - nur bei yes kann man set_wb_mode nutzen
  sensor->set_awb_gain(sensor, settingsJson["autoWhitebalanceGain"]);
  // White Balance Mode - 0=Auto|1=Sunny|2=Cloudy|3=Office|4=Home
  sensor->set_wb_mode(sensor, settingsJson["whitebalanceMode"]);

  // Auto Exposure Control (via Sensor) - 0: no, 1: yes
  sensor->set_exposure_ctrl(sensor, settingsJson["exposureCtrlSensor"]);
  // Auto Exposure Control (via DSP) - 0: no, 1: yes
  sensor->set_aec2(sensor, settingsJson["exposureCtrlDsp"]);
  // Auto Exposure Level - -2,-1,0,1,2
  sensor->set_ae_level(sensor, settingsJson["autoExposureLevel"]);
  // Auto Exposure Value - 0 - 1024
  sensor->set_aec_value(sensor, settingsJson["autoExposureValue"]);
  // Auto Gain Control - 0: no, 1: yes
  sensor->set_gain_ctrl(sensor, settingsJson["autoExposureGainControl"]);
  // Auto Gain Control Gain - 0 - 30 - Mit 0 starten und erhöhen, wenn zu dunkel 
  sensor->set_agc_gain(sensor, settingsJson["autoExposureGainValue"]);
  // Gain ceiling - 0=2x, 1=4x, 2=8x, 3=16x, 4=32x, 5=64x, 6=128x
  sensor->set_gainceiling(sensor, gainceilingFromInt(settingsJson["autoExposureGainCeiling"]));

  // Black Pixel Correct - 0: no, 1: yes
  sensor->set_bpc(sensor, settingsJson["blackPixelCorrect"]);
  // White Pixel Correct - 0: no, 1: yes
  sensor->set_wpc(sensor, settingsJson["whitePixelCorrect"]);
  // Auto Gamma Correct: no, 1: yes - mit yes i.d.R. besser
  sensor->set_raw_gma(sensor, settingsJson["gammaCorrect"]);
  // Auto Lens Correct - 0: no, 1: yes - mit yes i.d.R. besser
  sensor->set_lenc(sensor, settingsJson["lensCorrect"]);

  // Horizontal mirror image - 0: no, 1: yes
  sensor->set_hmirror(sensor, settingsJson["horizontalMirror"]);
  // Vertical flip image - 0: no, 1: yes 
  sensor->set_vflip(sensor, settingsJson["verticalFlip"]);
  // Downscale image - 0: no, 1: yes - Wenn eine andere Auflösung als UXGA gewählt ist, dann muss das 1 sein!
  sensor->set_dcw(sensor, 1);
  // Display Test Color-Bar
  sensor->set_colorbar(sensor, 0);
}


