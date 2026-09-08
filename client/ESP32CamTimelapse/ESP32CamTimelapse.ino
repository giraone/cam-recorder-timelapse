/*
 Continuous loop to fetch camera settings from an image server and send photos taken with these settings to the image server using plain HTTP POST (no multipart).
 - The file name is build using the camera name (derived from IP address) and the current date and time (with a base time fetched from an NTP server)
 - The camera settings are fetch on each restart from the server
 - The server can force a "restartNow" after each upload, so then all new settings are applied
 - The server can force pause/resume of the loop to take photo.
 - If paused, this software will periodically send requests to the server to receive new commands.
 - Besides sending photos, this software can transmit status information of the device to the server, e.g.
   - Number of successful photo transmissions
   - Number of unsuccessful photo transmissions
   - WiFi signal strength (rssi) 

 Board: AI Thinker ESP32-CAM
 
 See also
  - [Make-Magazin ESP32C_Mailkamera](https://github.com/MakeMagazinDE/ESP32C_Mailkamera)
  - https://RandomNerdTutorials.com/esp32-send-email-smtp-server-arduino-ide/
  - https://RandomNerdTutorials.com/esp32-http-get-post-arduino/
  - https://github.com/espressif/arduino-esp32/blob/master/libraries/HTTPClient/src/HTTPClient.h
  - https://arduinojson.org/
*/

#include "WiFi.h"
#include <HTTPClient.h> // EspressIF HTTPClient
#include <ArduinoJson.h>
#include "init_camera.h"
// Credentials and server address - not in git, copy "secrets.h.example" to "secrets.h"
#include "secrets.h"

//-- WIFI -------------------------------------------------------------------------

const char* SSID = WIFI_SSID;
const char* PASSWORD = WIFI_PASSWORD;
// Give up waiting for the access point after this time and restart the device
const uint32_t WIFI_CONNECT_TIMEOUT_MS = 30000;

//-- HTTP -------------------------------------------------------------------------

const char* TARGET_URL_IMAGE = "http://" SERVER_HOST ":" SERVER_PORT "/images/%s-%s.jpg";     // the 2 parameters are: cameraName, timeLabel
const char* TARGET_URL_STATUS = "http://" SERVER_HOST ":" SERVER_PORT "/status";
const char* MIME_TYPE_JPEG = "image/jpeg";
const char* MIME_TYPE_JSON = "application/json";
// Do not wait too long for the server - the camera should keep its cadence
const int32_t HTTP_CONNECT_TIMEOUT_MS = 5000;
const uint16_t HTTP_TIMEOUT_MS = 10000;

//-- NTP --------------------------------------------------------------------------

const char* ntpServer1 = "de.pool.ntp.org";
const char* ntpServer2 = "pool.ntp.org";
// Central European Time, incl. the switch to/from daylight saving time
const char* timeZone = "CET-1CEST,M3.5.0,M10.5.0/3";
// Give up waiting for the first NTP answer after this time
const uint32_t NTP_SYNC_TIMEOUT_MS = 15000;

//-- Settings ---------------------------------------------------------------------

// Set when new camera settings arrived, cleared only after they were applied successfully
bool cameraSettingsChanged = true;

// Fallback and guard rails for the loop delay, used when the server sends nothing usable
const uint32_t DEFAULT_DELAY_MS = 20000;
const uint32_t MIN_DELAY_MS = 1000;
const uint32_t MAX_DELAY_MS = 3600000;

JsonDocument settings;
JsonDocument workflowSettings;
JsonDocument cameraSettings;

//-- Status attributes ------------------------------------------------------------

// the WiFi signal strength
int wifiRssi = 0;
// Will be replaces by c<IP>, where <IP> is the IPv4 address's last part
char cameraName[5]; 
// count number of images taken
int imageCounter = 0;
// count number of errors to take an image
int imageErrors = 0;
// count number of camera inits
int cameraInitCounter = 0;
// count number of error at camera inits
int cameraInitErrors = 0;
// count number of errors to upload an image
int uploadImageErrors = 0;
// count number of errors to upload status
int uploadStatusErrors = 0;

//-- Camera (Flash LED) -----------------------------------------------------------

const int FLASH_GPIO_NUM = 4;
int flashDurationMs = 100;
bool flashLedForPicture = false;
// Frames to throw away after switching on the flash, see shootAndSend()
const int FLASH_DISCARD_FRAMES = 2;

//-- Board LED --------------------------------------------------------------------

// The on-board LED of the AI-Thinker ESP32-CAM is active LOW
const int BOARD_LED = 33;
const int BOARD_LED_ON = LOW;
const int BOARD_LED_OFF = HIGH;
// Keep the blinking short - it blocks the loop and distorts the timelapse cadence
const int BLINK_DURATION_MS = 60;
bool blinkOnSuccess = true;
bool blinkOnFailure = true;

//---------------------------------------------------------------------------------

void setup() {
  Serial.begin(115200);
  pinMode(BOARD_LED, OUTPUT);
  digitalWrite(BOARD_LED, BOARD_LED_OFF); // the pin defaults to LOW, which switches the LED on
  pinMode(FLASH_GPIO_NUM, OUTPUT);
  digitalWrite(FLASH_GPIO_NUM, LOW);

  initWiFi();
  blinkLedOk();
  initNtp();
  blinkLedOk();
}

void loop() {
  const uint32_t loopStartedMs = millis();
  ensureWiFiConnected();

  workflowSettings["restart"] = false;
  workflowSettings["pause"] = false;
  workflowSettings["delayMs"] = DEFAULT_DELAY_MS;
  uploadStatus();

  if (cameraSettingsChanged && !cameraSettings.isNull()) {
     short result = initCameraWithSettings(cameraSettings);
     if (result == 0) {
        cameraInitErrors++;
     } else {
        // Clear only after the settings were really applied, so a failed init is retried
        cameraSettingsChanged = false;
        if (result == 1) {
          cameraInitCounter++;
        }
     }
  }

  if (workflowSettings["restart"]) {
      Serial.println(">>> Command for restarting received!");
      ESP.restart();
  }
  if (!workflowSettings["pause"]) {
    shootAndSend();
  }
  // Subtract the time needed for taking and uploading the photo, so that the interval
  // between two photos stays even
  const uint32_t configuredDelayMs = workflowSettings["delayMs"] | DEFAULT_DELAY_MS;
  const uint32_t delayMs = constrain(configuredDelayMs, MIN_DELAY_MS, MAX_DELAY_MS);
  const uint32_t elapsedMs = millis() - loopStartedMs;
  delay(elapsedMs < delayMs ? delayMs - elapsedMs : 0);
}

void shootAndSend() {
  char* timeLabel = fetchtimeLabel();
  if (flashLedForPicture) {
    //S Serial.printf(">>> Flash wanted. Using GPIO %d.\n", FLASH_GPIO_NUM);
    digitalWrite(FLASH_GPIO_NUM, HIGH);
    delay(flashDurationMs);
    // The next buffer may still hold a frame that was captured before the flash was on.
    // Throw some away, this also gives the exposure control time to adapt to the light.
    discardFrames(FLASH_DISCARD_FRAMES);
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
    //S Serial.printf(">>> Photo %d taken with %d bytes.\n", imageCounter, frameBuffer->len);
    // sendPhotoViaHttp() already blinks, depending on the result of the upload
    sendPhotoViaHttp(frameBuffer, timeLabel);
    esp_camera_fb_return(frameBuffer);
  }
}

// Fetch and immediately return frames, so that the next one is really an up to date one
void discardFrames(int count) {
  for (int i = 0; i < count; i++) {
    camera_fb_t* frameBuffer = esp_camera_fb_get();
    if (!frameBuffer) {
      return;
    }
    esp_camera_fb_return(frameBuffer);
  }
}

void initWiFi() {
  WiFi.mode(WIFI_STA);
  WiFi.setAutoReconnect(true);
  WiFi.setSleep(false); // modem sleep makes the uploads slow and unreliable
  connectWiFi();
}

// Connect to the access point and derive the camera name from the assigned IP address.
// Restarts the device, if no connection can be established - the AP may come back later.
void connectWiFi() {
  WiFi.begin(SSID, PASSWORD);
  Serial.print(">>> Connect to WiFi...");
  const uint32_t startedMs = millis();
  while (WiFi.status() != WL_CONNECTED) {
    if (millis() - startedMs > WIFI_CONNECT_TIMEOUT_MS) {
      Serial.println();
      Serial.println(">>> No WiFi connection - restarting!");
      ESP.restart();
    }
    delay(500);
    Serial.print(".");
  }
  Serial.println();
  IPAddress ipAddress = WiFi.localIP();
  //S Serial.print(">>> IP address: ");
  //S Serial.println(ipAddress);

  // Last octet of the IPv4 address, zero padded, e.g. 192.168.178.87 --> "c087"
  // Recalculated on every connect, because DHCP may hand out a different address
  snprintf(cameraName, sizeof(cameraName), "c%03u", (unsigned) ipAddress[3]);
  //S Serial.print(">>> Camera Name: ");
  //S Serial.println(cameraName);
  delay(100);
  wifiRssi = WiFi.RSSI();
  //S Serial.print(">>> RSSI: ");
  //S Serial.println(wifiRssi);
}

// Reconnect, if the connection was lost, e.g. after the access point was restarted
void ensureWiFiConnected() {
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println(">>> WiFi connection lost!");
    WiFi.disconnect();
    connectWiFi();
  }
}

void initNtp() {
  configTzTime(timeZone, ntpServer1, ntpServer2);
  // Wait for the first answer, otherwise the first images are named with the fallback label
  Serial.print(">>> Waiting for NTP time...");
  struct tm now;
  const uint32_t startedMs = millis();
  while (!getLocalTime(&now, 1000)) {
    if (millis() - startedMs > NTP_SYNC_TIMEOUT_MS) {
      Serial.println();
      Serial.println(">>> No NTP time yet - continuing without it!");
      return;
    }
    Serial.print(".");
  }
  Serial.println();
  Serial.printf(">>> NTP time: %04d-%02d-%02d %02d:%02d:%02d\n",
    1900 + now.tm_year, 1 + now.tm_mon, now.tm_mday, now.tm_hour, now.tm_min, now.tm_sec);
}

void sendPhotoViaHttp(camera_fb_t* frameBuffer, char* timeLabel) {
  char urlBuffer[128];
  snprintf(urlBuffer, sizeof(urlBuffer), TARGET_URL_IMAGE, cameraName, timeLabel);
  Serial.printf(">>> POST URL = \"%s\" %d\n", urlBuffer, cameraSettings["frameSize"]);
  HTTPClient http;
  http.begin(urlBuffer);
  http.setConnectTimeout(HTTP_CONNECT_TIMEOUT_MS);
  http.setTimeout(HTTP_TIMEOUT_MS);
  http.addHeader("Content-Type", MIME_TYPE_JPEG);
  int httpResponseCode = http.POST(frameBuffer->buf, frameBuffer->len);
  if (isHttpSuccess(httpResponseCode)) {
    parseAndStoreSettings(http.getString());
    blinkLedOk();
  } else {
    uploadImageErrors++;
    logHttpError(http, httpResponseCode);
    blinkLedError();
  }
  http.end();
}

void uploadStatus() {
  wifiRssi = WiFi.RSSI();
  JsonDocument data;
  data["rssi"] = wifiRssi;
  data["cameraName"] = cameraName;
  data["imageCounter"] = imageCounter;
  data["imageErrors"] = imageErrors;
  data["cameraInitCounter"] = cameraInitCounter;
  data["cameraInitErrors"] = cameraInitErrors;
  data["uploadImageErrors"] = uploadImageErrors;
  data["uploadStatusErrors"] = uploadStatusErrors;
  char jsonCharBuffer[256];
  serializeJson(data, jsonCharBuffer);
  Serial.printf(">>> PUT URL = \"%s\" %s\n", TARGET_URL_STATUS, jsonCharBuffer);
  HTTPClient http;
  http.begin(TARGET_URL_STATUS);
  http.setConnectTimeout(HTTP_CONNECT_TIMEOUT_MS);
  http.setTimeout(HTTP_TIMEOUT_MS);
  http.addHeader("Accept", MIME_TYPE_JSON);
  http.addHeader("Content-Type", MIME_TYPE_JSON);
  int httpResponseCode = http.PUT(jsonCharBuffer);
  if (isHttpSuccess(httpResponseCode)) {
    parseAndStoreSettings(http.getString());
    blinkLedOk();
  } else {
    uploadStatusErrors++;
    logHttpError(http, httpResponseCode);
    blinkLedError();
  }
  http.end();
}

// Every 2xx is a success - the server may answer with 200 or 201
bool isHttpSuccess(int httpResponseCode) {
  return httpResponseCode >= 200 && httpResponseCode < 300;
}

// Log the response code together with the error message of the server, resp. of the client
void logHttpError(HTTPClient& http, int httpResponseCode) {
  if (httpResponseCode > 0) {
    Serial.printf(">>> HTTP response code = %d, body = %s\n", httpResponseCode, http.getString().c_str());
  } else {
    Serial.printf(">>> HTTP request failed: %s\n", HTTPClient::errorToString(httpResponseCode).c_str());
  }
}

void parseAndStoreSettings(String jsonString) {
    //S Serial.print(">>> settings = ");
    //S Serial.println(jsonString);
    JsonDocument parsedSettings = parseJson(jsonString);
    if (parsedSettings["error"] || parsedSettings["workflow"].isNull()) {
      Serial.println(">>> Unusable settings in response - keeping the current ones!");
      return;
    }
    settings = parsedSettings;
    workflowSettings = settings["workflow"];
    flashLedForPicture = workflowSettings["flashLedForPicture"] | false;
    flashDurationMs = workflowSettings["flashDurationMs"] | 100;
    blinkOnSuccess = workflowSettings["blinkOnSuccess"] | true;
    blinkOnFailure = workflowSettings["blinkOnFailure"] | true;
    // The server sends the camera settings only once after a change, so the flag must not
    // be reset here - it is cleared in loop() when the settings were applied successfully.
    if (settings["camera"].is<JsonObjectConst>()) {
      cameraSettings = settings["camera"];
      cameraSettingsChanged = true;
    }
}

JsonDocument parseJson(String jsonString) {
  JsonDocument jsonDoc;
  if (jsonString == (String) 0) {
    jsonDoc["error"] = true;
  } else {
    DeserializationError error = deserializeJson(jsonDoc, jsonString);
    if (error) {
      Serial.println(">>> Parsing JSON input failed!");
      jsonDoc["error"] = true;
    }
  }
  return jsonDoc;
}

void blinkLedOk() {
  if (blinkOnSuccess) {
    blinkLed(1, BLINK_DURATION_MS);
  }
}

void blinkLedError() {
  if (blinkOnFailure) {
    blinkLed(3, BLINK_DURATION_MS);
  }
}

void blinkLed(int count, int duration) {
  for (int i = 0; i < count; i++) {
    digitalWrite(BOARD_LED, BOARD_LED_ON);
    delay(duration);
    digitalWrite(BOARD_LED, BOARD_LED_OFF);
    delay(duration/2);
  }
}

char _timeBuffer[14];
// Return e.g. 240213-074417 (YYMMdd-hhmmss)
char* fetchtimeLabel() {
  struct tm now;
  if (!getLocalTime(&now)){
    Serial.println(">>> Failed to obtain time!");
    // Uptime based fallback - unique, so that these images do not overwrite each other
    snprintf(_timeBuffer, sizeof(_timeBuffer), "nt-%09lu", millis());
  } else {
    snprintf(_timeBuffer, sizeof(_timeBuffer), "%02d%02d%02d-%02d%02d%02d",
    now.tm_year % 100, 1 + now.tm_mon, now.tm_mday, now.tm_hour, now.tm_min, now.tm_sec);
    //S Serial.printf(">>> Obtainted time: %s\n", _timeBuffer);
  }
  return _timeBuffer;
}