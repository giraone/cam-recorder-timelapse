/*
 Schiesse Foto und sende es per HTTP Post
  
 Siehe
  - [Make-Magazin ESP32C_Mailkamera](https://github.com/MakeMagazinDE/ESP32C_Mailkamera)
  - https://RandomNerdTutorials.com/esp32-send-email-smtp-server-arduino-ide/
  - https://RandomNerdTutorials.com/esp32-http-get-post-arduino/
  - https://github.com/espressif/arduino-esp32/blob/master/libraries/HTTPClient/src/HTTPClient.h
*/

#include "WiFi.h"
#include <HTTPClient.h> // EspressIF HTTPClient
#include <Arduino_JSON.h> // JSON processing of camera settings
#include "init_camera.h"

//-- PICTURE mode -----------------------------------------------------------------

// if 0, we wait for touch on PIN 12, otherwise this is the loop delay
const int PICTURE_LOOP_DELAY_SECONDS = 30;
// threshold for touch value
const uint8_t TOUCH_THRESHOLD = 20;

//-- HTTP -------------------------------------------------------------------------

const char* TARGET_URL = "http://192.168.178.45:8080";
const char* MIME_TYPE_JPEG = "image/jpeg";
const char* MIME_TYPE_JSON = "application/json";

//-- JSON -------------------------------------------------------------------------

const String JSON_DEFAULT = "{\"clockFrequencyHz\":16000000,\"gammaCorrect\":true,\"autoExposureGainCeiling\":0,\"jpegQuality\":10,\"exposureCtrlDsp\":false,\"whitePixelCorrect\":false,\"lensCorrect\":true,\"autoExposureGainControl\":true,\"backPixelCorrect\":false,\"autoExposureLevel\":0,\"specialEffect\":2,\"exposureCtrlSensor\":true,\"saturation\":0,\"autoExposureValue\":300,\"verticalFlip\":false,\"frameSize\":13,\"brightness\":0,\"denoise\":0,\"autoWhitebalance\":1,\"autoWhitebalanceGain\":1,\"whitebalanceMode\":0,\"autoExposureGainValue\":0,\"contrast\":0,\"sharpness\":0,\"horizontalMirror\":false}";

//-- NTP --------------------------------------------------------------------------

const char* ntpServer = "pool.ntp.org";
const long gmtOffset_sec = 3600;
const int daylightOffset_sec = 0;

//-- Camera (Flash LED) -----------------------------------------------------------

const int FLASH_GPIO_NUM = 4;
const int FLASH_DURATION_MS = 100;
const bool FLASH_LED_FOR_PICTURE = false;

//-- WIFI -------------------------------------------------------------------------

const char* SSID = "<enter here>";
const char* PASSWORD = "<enter here>";

//---------------------------------------------------------------------------------

void setup() {
  Serial.begin(115200);
 
  if (FLASH_LED_FOR_PICTURE) {
    pinMode(FLASH_GPIO_NUM, OUTPUT);
  }
  initWiFi(); 
  initNtp();

  JSONVar settingsJson = fetchAndParseCameraSettings();
  initCameraWithSettings(settingsJson);
}

void loop() {

  if (PICTURE_LOOP_DELAY_SECONDS > 0) {
    delay(PICTURE_LOOP_DELAY_SECONDS * 1000);
    shootAndSend();
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

  if (FLASH_LED_FOR_PICTURE) {
    digitalWrite(FLASH_GPIO_NUM, HIGH);
    delay(FLASH_DURATION_MS);
  }
  // Serial.println(">>> Take photo...");
  camera_fb_t* frameBuffer = esp_camera_fb_get();
  if (FLASH_LED_FOR_PICTURE) {
    digitalWrite(FLASH_GPIO_NUM, LOW);
  }
  if (!frameBuffer) {
    Serial.println(">>> No photo taken!");
  } else {
    Serial.printf(">>> Photo taken with %d bytes.\n", frameBuffer->len);
    sendPhotoViaHttp(frameBuffer, timeString);
    esp_camera_fb_return(frameBuffer);
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
}

void initNtp() {
  configTime(gmtOffset_sec, daylightOffset_sec, ntpServer);
}

JSONVar parseJson(String jsonString) {

  JSONVar jsonObject = JSON.parse(jsonString);
  if (JSON.typeof(jsonObject) == "undefined") {
    Serial.println(">>> Parsing JSON input failed!");
    return NULL;
  }
  Serial.print(">>> JSON object = ");
  return jsonObject;
}

bool sendPhotoViaHttp(camera_fb_t* frameBuffer, char* timeString) {
 
  char urlBuffer[128];
  snprintf(urlBuffer, sizeof(urlBuffer), "%s/files/images/esp32-cam1-%s.jpg", TARGET_URL, timeString);
  Serial.printf(">>> POST URL = \"%s\"\n", urlBuffer);
 
  HTTPClient http;
  http.begin(urlBuffer);
  http.addHeader("Content-Type", MIME_TYPE_JPEG);
  int httpResponseCode = http.POST(frameBuffer->buf, frameBuffer->len);
  
  JSONVar jsonResponse;
  bool ok = httpResponseCode == 200;
  if (ok) {
    String responseString = http.getString();
    Serial.print(">>> ");
    Serial.println(responseString);
    jsonResponse = parseJson(responseString);
  } else {
    Serial.printf(">>> HTTP Response code = %d\n", httpResponseCode);
    jsonResponse = "{\"restart\":true}";
  }
  http.end();

  if (jsonResponse["restart"]) {
    ESP.restart();
  }
  return ok;
}

JSONVar fetchAndParseCameraSettings() {

  String jsonString = fetchCameraSettings();
  return parseJson(jsonString);
}

String fetchCameraSettings() {
 
  char urlBuffer[128];
  snprintf(urlBuffer, sizeof(urlBuffer), "%s/camera/settings", TARGET_URL);
  Serial.printf(">>> GET URL = \"%s\"\n", urlBuffer);
 
  HTTPClient http;
  http.begin(urlBuffer);
  http.addHeader("Accept", MIME_TYPE_JSON);
  int httpResponseCode = http.GET();
  
  bool ok = httpResponseCode == 200;
  String jsonString; 
  if (ok) {
    jsonString = http.getString();
    Serial.print(">>> ");
    Serial.println(jsonString);
  } else {
    Serial.printf(">>> HTTP Response code = %d\n", httpResponseCode);
    jsonString = JSON_DEFAULT;
  }
  http.end();

  return jsonString;
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