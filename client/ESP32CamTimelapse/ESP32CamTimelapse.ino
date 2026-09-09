/*
 Continuous loop to fetch camera settings from an image server and send photos taken with these settings to the image server using plain HTTP POST (no multipart).
 - The file name is build using the camera name (derived from IP address) and the current date and time (with a base time fetched from an NTP server)
 - The camera settings are fetch on each restart from the server
 - The server can force a "restart" after each upload, so then all new settings are applied
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

//-- Logging ----------------------------------------------------------------------

// Set to 1 for the detailed output on the serial monitor
#define LOG_DEBUG 0

// The arguments are compiled in both cases, so they cannot become outdated unnoticed.
// With LOG_DEBUG = 0 the compiler removes the call, because the condition is constant.
#define LOGD(...) do { if (LOG_DEBUG) Serial.printf(__VA_ARGS__); } while (0)

//-- WIFI -------------------------------------------------------------------------

const char* SSID = WIFI_SSID;
const char* PASSWORD = WIFI_PASSWORD;
// Give up waiting for the access point after this time and restart the device
const uint32_t WIFI_CONNECT_TIMEOUT_MS = 30000;

//-- HTTP -------------------------------------------------------------------------

// The only endpoint used by the camera. The 2 parameters are: cameraName, timeLabel
const char* TARGET_URL_IMAGE = "http://" SERVER_HOST ":" SERVER_PORT "/images/%s-%s.jpg";
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

// Fallback and guard rails for the loop delay, used when the server sends nothing usable.
// Active = interval between two images, paused = interval between two status requests.
const uint32_t DEFAULT_DELAY_ACTIVE_MS = 20000;
const uint32_t DEFAULT_DELAY_PAUSED_MS = 60000;
const uint32_t MIN_DELAY_MS = 1000;
const uint32_t MAX_DELAY_MS = 3600000;

JsonDocument workflowSettings;
JsonDocument cameraSettings;

//-- Status attributes ------------------------------------------------------------

// All values below are read and written exclusively from loop() resp. setup(), i.e. from
// the Arduino main task. If a second task is ever added, they have to be protected.

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

//-- Buffer for uptime ------------------------------------------------------------

char _timeBuffer[14];

//---------------------------------------------------------------------------------

void setup() {
  Serial.begin(115200);
  pinMode(BOARD_LED, OUTPUT);
  digitalWrite(BOARD_LED, BOARD_LED_OFF); // the pin defaults to LOW, which switches the LED on
  pinMode(FLASH_GPIO_NUM, OUTPUT);
  digitalWrite(FLASH_GPIO_NUM, LOW);

  // Only used until the first answer of the server arrives
  workflowSettings["restart"] = false;
  workflowSettings["pause"] = false;
  workflowSettings["delayMsActive"] = DEFAULT_DELAY_ACTIVE_MS;
  workflowSettings["delayMsPaused"] = DEFAULT_DELAY_PAUSED_MS;

  initWiFi();
  blinkLedOk();
  initNtp();
  blinkLedOk();
}

void loop() {
  const uint32_t loopStartedMs = millis();
  ensureWiFiConnected();

  // Camera settings that arrived with the response of the previous cycle
  if (cameraSettingsChanged && !cameraSettings.isNull()) {
     short result = initCameraWithSettings(cameraSettings.as<JsonVariantConst>());
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

  // Exactly one request per cycle: with an image when active, with an empty body when
  // paused. Both carry the status headers and both receive the settings as the answer.
  if (workflowSettings["pause"] | false) {
    sendToServer(NULL, 0, fetchTimeLabel());
  } else {
    shootAndSend();
  }

  if (workflowSettings["restart"] | false) {
      Serial.println(">>> Command for restarting received!");
      ESP.restart();
  }

  // The pause flag of the answer just received decides the interval, so that leaving the
  // paused mode takes effect immediately and not only after one long interval.
  const uint32_t configuredDelayMs = (workflowSettings["pause"] | false)
    ? (workflowSettings["delayMsPaused"] | DEFAULT_DELAY_PAUSED_MS)
    : (workflowSettings["delayMsActive"] | DEFAULT_DELAY_ACTIVE_MS);
  // Subtract the time needed for taking and uploading the photo, so that the interval
  // between two photos stays even
  const uint32_t delayMs = constrain(configuredDelayMs, MIN_DELAY_MS, MAX_DELAY_MS);
  const uint32_t elapsedMs = millis() - loopStartedMs;
  delay(elapsedMs < delayMs ? delayMs - elapsedMs : 0);
}

void shootAndSend() {
  char* timeLabel = fetchTimeLabel();
  if (flashLedForPicture) {
    LOGD(">>> Flash wanted. Using GPIO %d.\n", FLASH_GPIO_NUM);
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
    // Send the status anyway, otherwise a broken camera would never receive commands again
    sendToServer(NULL, 0, timeLabel);
  } else {
    imageCounter++;
    LOGD(">>> Photo %d taken with %u bytes.\n", imageCounter, (unsigned) frameBuffer->len);
    // sendToServer() already blinks, depending on the result of the upload
    sendToServer(frameBuffer->buf, frameBuffer->len, timeLabel);
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
  LOGD(">>> IP address: %s\n", ipAddress.toString().c_str());

  // Last octet of the IPv4 address, zero padded, e.g. 192.168.178.87 --> "c087"
  // Recalculated on every connect, because DHCP may hand out a different address
  snprintf(cameraName, sizeof(cameraName), "c%03u", (unsigned) ipAddress[3]);
  LOGD(">>> Camera Name: %s\n", cameraName);
  delay(100);
  wifiRssi = WiFi.RSSI();
  LOGD(">>> RSSI: %d\n", wifiRssi);
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

// The one and only request of a cycle. The status is always passed as headers, the body
// holds the image - or is empty, when there is nothing to upload (paused, no photo).
// The answer are the settings in both cases.
void sendToServer(const uint8_t* body, size_t length, const char* timeLabel) {
  char urlBuffer[128];
  snprintf(urlBuffer, sizeof(urlBuffer), TARGET_URL_IMAGE, cameraName, timeLabel);
  Serial.printf(">>> POST URL = \"%s\" with %u bytes\n", urlBuffer, (unsigned) length);
  HTTPClient http;
  http.begin(urlBuffer);
  http.setConnectTimeout(HTTP_CONNECT_TIMEOUT_MS);
  http.setTimeout(HTTP_TIMEOUT_MS);
  http.addHeader("Content-Type", MIME_TYPE_JPEG);
  http.addHeader("Accept", MIME_TYPE_JSON);
  if (length == 0) {
    // HTTPClient adds this header only for a non-empty payload, but the server uses it
    // to distinguish an image upload from a status-only request
    http.addHeader("Content-Length", "0");
  }
  addStatusHeaders(http);
  int httpResponseCode = http.POST((uint8_t*) body, length);
  if (isHttpSuccess(httpResponseCode)) {
    parseAndStoreSettings(http.getString());
    blinkLedOk();
  } else {
    if (length > 0) {
      uploadImageErrors++;
    } else {
      uploadStatusErrors++;
    }
    logHttpError(http, httpResponseCode);
    blinkLedError();
  }
  http.end();
}

// The device status, that used to be the JSON body of the separate status request
void addStatusHeaders(HTTPClient& http) {
  wifiRssi = WiFi.RSSI();
  http.addHeader("cam-status-camera-name", cameraName);
  http.addHeader("cam-status-rssi", String(wifiRssi));
  http.addHeader("cam-status-image-counter", String(imageCounter));
  http.addHeader("cam-status-image-errors", String(imageErrors));
  http.addHeader("cam-status-camera-init-counter", String(cameraInitCounter));
  http.addHeader("cam-status-camera-init-errors", String(cameraInitErrors));
  http.addHeader("cam-status-upload-image-errors", String(uploadImageErrors));
  http.addHeader("cam-status-upload-status-errors", String(uploadStatusErrors));
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

void parseAndStoreSettings(const String& jsonString) {
    LOGD(">>> settings = %s\n", jsonString.c_str());
    JsonDocument parsedSettings;
    if (!parseJson(jsonString, parsedSettings) || parsedSettings["workflow"].isNull()) {
      Serial.println(">>> Unusable settings in response - keeping the current ones!");
      return;
    }
    workflowSettings = parsedSettings["workflow"];
    flashLedForPicture = workflowSettings["flashLedForPicture"] | false;
    flashDurationMs = workflowSettings["flashDurationMs"] | 100;
    blinkOnSuccess = workflowSettings["blinkOnSuccess"] | true;
    blinkOnFailure = workflowSettings["blinkOnFailure"] | true;
    // The server sends the camera settings only once after a change, so the flag must not
    // be reset here - it is cleared in loop() when the settings were applied successfully.
    if (parsedSettings["camera"].is<JsonObjectConst>()) {
      cameraSettings = parsedSettings["camera"];
      cameraSettingsChanged = true;
    }
}

// Parse into the given document. Returns false, if the input was empty or not parsable.
bool parseJson(const String& jsonString, JsonDocument& jsonDoc) {
  if (jsonString.isEmpty()) {
    return false;
  }
  DeserializationError error = deserializeJson(jsonDoc, jsonString);
  if (error) {
    Serial.printf(">>> Parsing JSON input failed: %s\n", error.c_str());
    return false;
  }
  return true;
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

// Return e.g. 240213-074417 (YYMMdd-hhmmss)
char* fetchTimeLabel() {
  struct tm now;
  if (!getLocalTime(&now)){
    Serial.println(">>> Failed to obtain time!");
    // Uptime based fallback - unique, so that these images do not overwrite each other
    snprintf(_timeBuffer, sizeof(_timeBuffer), "nt-%09lu", millis());
  } else {
    snprintf(_timeBuffer, sizeof(_timeBuffer), "%02d%02d%02d-%02d%02d%02d",
    now.tm_year % 100, 1 + now.tm_mon, now.tm_mday, now.tm_hour, now.tm_min, now.tm_sec);
    LOGD(">>> Obtained time: %s\n", _timeBuffer);
  }
  return _timeBuffer;
}