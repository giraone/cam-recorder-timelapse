/*
 Take photos and send them via HTTP Post
  
 See
  - [Make-Magazin ESP32C_Mailkamera](https://github.com/MakeMagazinDE/ESP32C_Mailkamera)
  - https://RandomNerdTutorials.com/esp32-send-email-smtp-server-arduino-ide/
  - https://RandomNerdTutorials.com/esp32-http-get-post-arduino/
  - https://github.com/espressif/arduino-esp32/blob/master/libraries/HTTPClient/src/HTTPClient.h
*/

#include "WiFi.h"
#include <HTTPClient.h> // EspressIF HTTPClient
#include "init_camera.h"

//-- PICTURE mode -----------------------------------------------------------------

// if 0, we wait for touch on PIN 12, otherwise this is the loop delay
const int PICTURE_LOOP_DELAY_SECONDS = 30;
// threshold for touch value
const uint8_t TOUCH_THRESHOLD = 20;

//-- HTTP -------------------------------------------------------------------------

const char* TARGET_URL = "http://192.168.178.45:8080/files/images";
const char* MIME_TYPE = "image/jpeg";

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
  initCamera();
  if (FLASH_LED_FOR_PICTURE) {
    pinMode(FLASH_GPIO_NUM, OUTPUT);
  }
  initWiFi(); 
  initNtp(); 
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

bool sendPhotoViaHttp(camera_fb_t* frameBuffer, char* timeString) {
 
  char urlBuffer[128];
  snprintf(urlBuffer, sizeof(urlBuffer), "%s/esp32-cam1-%s.jpg", TARGET_URL, timeString);
  Serial.printf(">>> POST URL = \"%s\"\n", urlBuffer);
 
  HTTPClient http;
  http.begin(urlBuffer);
  http.addHeader("Content-Type", MIME_TYPE);
  int httpResponseCode = http.POST(frameBuffer->buf, frameBuffer->len);
  
  bool ok = httpResponseCode == 200;
  if (ok) {
    String payload = http.getString();
    Serial.print(">>> ");
    Serial.println(payload);
  } else {
    Serial.printf(">>> HTTP Response code = %d\n", httpResponseCode);
  }
  http.end();
  return ok;
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