/*
 Take a photo and send it to the image server using HTTP POST.
 - This is done either in a loop or using "touch" on the PIN 12
 - The file name is build using the date (with base time fetched from an NTP server)
 - The camera settings are fetch on each restart from the server
 - The server can force a "restartNow" after each upload, so then all new settings are applied
 - The server can force pause/resume of the loop to take photo.
 - If paused, this software will periodically send requests to the server to receive new commands.
 - Besides sending photos, this software can transmit status information of the device to the server, e.g.
   - Number of successful photo transmissions
   - Number of unsuccessful photo transmissions
   - WiFi signal strength (rssi) 

 See
  - [Make-Magazin ESP32C_Mailkamera](https://github.com/MakeMagazinDE/ESP32C_Mailkamera)
  - https://RandomNerdTutorials.com/esp32-send-email-smtp-server-arduino-ide/
  - https://RandomNerdTutorials.com/esp32-http-get-post-arduino/
  - https://github.com/espressif/arduino-esp32/blob/master/libraries/HTTPClient/src/HTTPClient.h
*/

#include "WiFi.h"
#include <HTTPClient.h> // EspressIF HTTPClient
#include <ArduinoJson.h>
#include "init_camera.h"

//-- PICTURE mode -----------------------------------------------------------------

// if 0, we wait for touch on PIN 12, otherwise this is the loop delay
int loopDelaySeconds = 30;
// Is the camera in "paused mode". Start is always in paused mode to fetch the settings first.
bool paused = true;
// If true, read the camera settings before taking the next photo
bool readSettings = false;
// If true and in paused mode, send periodically the device status
bool sendStatus = false;
// true, when the paused mode should be left without a loop delay
bool leavingPausedMode = false;
// the WiFi signal strength
int wifiRssi = 0;
// count number of images taken
int imageCounter = 0;
// count number of errors to take an image
int imageErrors = 0;
// count number of errors to upload an image
int uploadErrors = 0;
// restart device after n images taken
int restartAfterAmount = 100;
// threshold for touch value
const uint8_t TOUCH_THRESHOLD = 20;

//-- Board LED --------------------------------------------------------------------

const int BOARD_LED = 33;
bool blinkOnSuccess = false;

//-- HTTP -------------------------------------------------------------------------

// 45 (lenovo) or 87 (air)
const char* TARGET_URL_PHOTO_UPLOAD = "http://192.168.178.45:9001/images/%s-%s.jpg";     // the 2 parameters are: filePrefix, timeString
const char* TARGET_URL_SETTINGS_DOWNLOAD = "http://192.168.178.45:9001/camera-settings";
const char* TARGET_URL_STATUS_UPLOAD = "http://192.168.178.45:9001/camera-status";
const char* MIME_TYPE_JPEG = "image/jpeg";
const char* MIME_TYPE_JSON = "application/json";
const char* FILE_PREFIX = "cam";

//-- JSON -------------------------------------------------------------------------

const String JSON_DEFAULT = "{\"clockFrequencyHz\":16000000,\"gammaCorrect\":true,\"autoExposureGainCeiling\":0,\"jpegQuality\":10,\"exposureCtrlDsp\":false,\"whitePixelCorrect\":false,\"lensCorrect\":true,\"autoExposureGainControl\":true,\"backPixelCorrect\":false,\"autoExposureLevel\":0,\"specialEffect\":2,\"exposureCtrlSensor\":true,\"saturation\":0,\"autoExposureValue\":300,\"verticalFlip\":false,\"frameSize\":13,\"brightness\":0,\"denoise\":0,\"autoWhitebalance\":1,\"autoWhitebalanceGain\":1,\"whitebalanceMode\":0,\"autoExposureGainValue\":0,\"contrast\":0,\"sharpness\":0,\"horizontalMirror\":false}";

//-- NTP --------------------------------------------------------------------------

const char* ntpServer = "de.pool.ntp.org";
const long gmtOffset_sec = 3600;
const int daylightOffset_sec = 0;

//-- Camera (Flash LED) -----------------------------------------------------------

const int FLASH_GPIO_NUM = 4;
int flashDurationMs = 100;
bool flashLedForPicture = false;

//-- WIFI -------------------------------------------------------------------------

const char* SSID = "<enter here>";
const char* PASSWORD = "<enter here>";

//---------------------------------------------------------------------------------

void setup() {
  Serial.begin(115200);
  pinMode(BOARD_LED, OUTPUT);
  pinMode(FLASH_GPIO_NUM, OUTPUT);
  
  initWiFi();
  blinkLedOk();
  initNtp();
  blinkLedOk();
  fetchAndApplySettings();
}

void loop() {

  if (loopDelaySeconds > 0) {
    if (leavingPausedMode) {
       leavingPausedMode = false;
    } else {
       delay(loopDelaySeconds * 1000);
    }
    if (sendStatus) {
      uploadStatus();
    }
    if (paused) {
      fetchAndApplySettings();
    } else {
      shootAndSend();
      if (imageCounter > restartAfterAmount) {
        Serial.printf(">>> Restarting after %d images!\n", restartAfterAmount);
        ESP.restart();
      }
    }
  } else {
    const bool touched = (touchRead(T5) < TOUCH_THRESHOLD);
    if (touched) {
      shootAndSend();
      delay(1000);
    }
  }
}

void shootAndSend() {
  char* timeString = fetchTimeString();
  if (flashLedForPicture) {
    Serial.printf(">>> Flash wanted. Using GPIO %d.\n", FLASH_GPIO_NUM);
    digitalWrite(FLASH_GPIO_NUM, HIGH);
    delay(flashDurationMs);
  }
  camera_fb_t* frameBuffer = esp_camera_fb_get();
  if (flashLedForPicture) {
    digitalWrite(FLASH_GPIO_NUM, LOW);
  }
  if (!frameBuffer) {
    imageErrors++;
    Serial.println(">>> No photo taken!");
    blinkLedError();
  } else {
    imageCounter++;
    Serial.printf(">>> Photo %d taken with %d bytes.\n", imageCounter, frameBuffer->len);
    sendPhotoViaHttp(frameBuffer, timeString);
    esp_camera_fb_return(frameBuffer);
    blinkLedOk();
  }
}

void initWiFi() {
  WiFi.begin(SSID, PASSWORD);
  Serial.print(">>> Connect to WiFi...");
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println();
  Serial.print(">>> IP address: ");
  Serial.println(WiFi.localIP());
  delay(500);
  wifiRssi = WiFi.RSSI();
  Serial.print(">>> RSSI: ");
  Serial.println(wifiRssi);
}

void initNtp() {
  configTime(gmtOffset_sec, daylightOffset_sec, ntpServer);
}

JsonDocument parseJson(String jsonString) {
  JsonDocument jsonDoc;
  DeserializationError error = deserializeJson(jsonDoc, jsonString);
  if (error) {
    Serial.println(">>> Parsing JSON input failed!");
    jsonDoc["error"] = true;
  }
  return jsonDoc;
}

void sendPhotoViaHttp(camera_fb_t* frameBuffer, char* timeString) {
  char urlBuffer[128];
  snprintf(urlBuffer, sizeof(urlBuffer), TARGET_URL_PHOTO_UPLOAD, FILE_PREFIX, timeString);
  Serial.printf(">>> POST URL = \"%s\"\n", urlBuffer);
  HTTPClient http;
  http.begin(urlBuffer);
  http.addHeader("Content-Type", MIME_TYPE_JPEG);
  int httpResponseCode = http.POST(frameBuffer->buf, frameBuffer->len);
  JsonDocument jsonResponse;
  if (httpResponseCode == 200) {
    String responseString = http.getString();
    Serial.print(">>> ");
    Serial.println(responseString);
    jsonResponse = parseJson(responseString);
    blinkLedOk();
  } else {
    uploadErrors++;
    Serial.printf(">>> HTTP Response code = %d\n", httpResponseCode);
    jsonResponse["restartNow"] = false;
    jsonResponse["readSettings"] = false;
    jsonResponse["sendStatus"] = false;
    blinkLedError();
  }
  http.end();
  if (jsonResponse["restartNow"] || jsonResponse["error"]) {
    ESP.restart();
  } else if (jsonResponse["readSettings"]) {
    fetchAndApplySettings();
  }
}

void fetchAndApplySettings() {
  JsonDocument settings = fetchAndParseCameraSettings();
  loopDelaySeconds = settings["loopDelaySeconds"];
  if (paused && !settings["paused"]) {
    leavingPausedMode = true;
    Serial.println(">>> Leaving paused mode.");
  } else if (!paused && settings["paused"]) {
    Serial.println(">>> Going into paused mode.");
  }
  paused = settings["paused"];
  sendStatus = settings["sendStatus"];
  restartAfterAmount = settings["restartAfterAmount"];
  blinkOnSuccess = settings["blinkOnSuccess"]; 
  flashLedForPicture = settings["flashLedForPicture"]; 
  flashDurationMs = settings["flashDurationMs"];
  if (leavingPausedMode) {
    initCameraWithSettings(settings);
  } 
}

void uploadStatus() {
  wifiRssi = WiFi.RSSI();
  JsonDocument data;
  data["rssi"] = wifiRssi;
  data["imageCounter"] = imageCounter;
  data["imageErrors"] = imageErrors;
  data["uploadErrors"] = uploadErrors;
  char jsonCharBuffer[100];
  serializeJson(data, jsonCharBuffer);
  HTTPClient http;
  http.begin(TARGET_URL_STATUS_UPLOAD);
  http.addHeader("Accept", MIME_TYPE_JSON);
  http.addHeader("Content-Type", MIME_TYPE_JSON);
  int httpResponseCode = http.PUT(jsonCharBuffer);
  Serial.printf(">>> HTTP Response code = %d\n", httpResponseCode);
  if (httpResponseCode == 200) {
    blinkLedOk();
  } else {
    blinkLedError();
  }
  http.end();
}

JsonDocument fetchAndParseCameraSettings() {
  String jsonString = fetchCameraSettings();
  return parseJson(jsonString);
}

String fetchCameraSettings() {
  HTTPClient http;
  http.begin(TARGET_URL_SETTINGS_DOWNLOAD);
  http.addHeader("Accept", MIME_TYPE_JSON);
  int httpResponseCode = http.GET();
  String jsonString; 
  if (httpResponseCode == 200) {
    jsonString = http.getString();
    Serial.print(">>> settings = ");
    Serial.println(jsonString);
    blinkLedOk();
  } else {
    Serial.printf(">>> HTTP Response code = %d\n", httpResponseCode);
    jsonString = JSON_DEFAULT;
    blinkLedError();
  }
  http.end();
  return jsonString;
}

void blinkLed(int count, int duration) {
  for (int i = 0; i < count; i++) {
    digitalWrite(BOARD_LED, HIGH);
    delay(duration);
    digitalWrite(BOARD_LED, LOW);
    delay(duration/2);
  }
}

void blinkLedOk() {
  if (blinkOnSuccess) {
    blinkLed(1,800);
  }
}

void blinkLedError() {
  blinkLed(3,300);
}

char _timeBuffer[22];
// Return e.g. 2024-02-13-07-44-17
char* fetchTimeString() {
  struct tm now;
  if (!getLocalTime(&now)){
    Serial.println(">>> Failed to obtain time!");
    snprintf(_timeBuffer, sizeof(_timeBuffer) - 1, "no-time");
  }

  snprintf(_timeBuffer, sizeof(_timeBuffer) - 1, "%04d-%02d-%02d-%02d-%02d-%02d",
    1900 + now.tm_year, 1 + now.tm_mon, now.tm_mday, now.tm_hour, now.tm_min, now.tm_sec);
  // Serial.printf(">>> Obtainted time: %s\n", _timeBuffer);
  return _timeBuffer;
}