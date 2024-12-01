/*
 Continous loop to fetch camera settings from an image server using HTTP GET and send photos taken with these settings to the image server using HTTP POST.
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

//-- WIFI -------------------------------------------------------------------------

const char* SSID = "<enter here>";
const char* PASSWORD = "<enter here>";

//-- HTTP -------------------------------------------------------------------------

// 45 (lenovo) or 87 (air)
const char* TARGET_URL_IMAGE = "http://192.168.178.45:9001/images/%s-%s.jpg";     // the 2 parameters are: cameraName, timeLabel
const char* TARGET_URL_STATUS = "http://192.168.178.45:9001/status";
const char* MIME_TYPE_JPEG = "image/jpeg";
const char* MIME_TYPE_JSON = "application/json";

//-- NTP --------------------------------------------------------------------------

const char* ntpServer = "de.pool.ntp.org";
const long gmtOffset_sec = 3600;
const int daylightOffset_sec = 0;

//-- Settings ---------------------------------------------------------------------

bool cameraSettingsChanged = true;

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
// restart device after n images taken
int restartAfterAmount = 100;

//-- Camera (Flash LED) -----------------------------------------------------------

const int FLASH_GPIO_NUM = 4;
int flashDurationMs = 100;
bool flashLedForPicture = false;

//-- Board LED --------------------------------------------------------------------

const int BOARD_LED = 33;
bool blinkOnSuccess = true;
bool blinkOnFailure = true;

//---------------------------------------------------------------------------------

void setup() {
  Serial.begin(115200);
  pinMode(BOARD_LED, OUTPUT);
  pinMode(FLASH_GPIO_NUM, OUTPUT);
  
  initWiFi();
  blinkLedOk();
  initNtp();
  blinkLedOk();
}

void loop() {
  workflowSettings["restart"] = false;
  workflowSettings["pause"] = false;
  workflowSettings["delayMs"] = 20000;
  uploadStatus();

  if (cameraSettingsChanged) {
     short result = initCameraWithSettings(cameraSettings);
     if (result == 1) {
        cameraInitCounter++;
     } else if (result == 0) {
        cameraInitErrors++;
     }
  }
 
  if (workflowSettings["restart"]) {
      Serial.println(">>> Command for restarting received!"); 
      ESP.restart();
      return;
  }
  if (!workflowSettings["pause"]) {
    shootAndSend();
  }
  delay(workflowSettings["delayMs"]);
}

void shootAndSend() {
  char* timeLabel = fetchtimeLabel();
  if (flashLedForPicture) {
    //S Serial.printf(">>> Flash wanted. Using GPIO %d.\n", FLASH_GPIO_NUM);
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
    //S Serial.printf(">>> Photo %d taken with %d bytes.\n", imageCounter, frameBuffer->len);
    sendPhotoViaHttp(frameBuffer, timeLabel);
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
  IPAddress ipAddress = WiFi.localIP();
  //S Serial.print(">>> IP address: ");
  //S Serial.println(ipAddress);
 
  snprintf(cameraName, sizeof(cameraName), "c%03s", lastIpPart(ipAddress));
  //S Serial.print(">>> Camera Name: ");
  //S Serial.println(cameraName);
  delay(100);
  wifiRssi = WiFi.RSSI();
  //S Serial.print(">>> RSSI: ");
  //S Serial.println(wifiRssi);
}

void initNtp() {
  configTime(gmtOffset_sec, daylightOffset_sec, ntpServer);
}

void sendPhotoViaHttp(camera_fb_t* frameBuffer, char* timeLabel) {
  char urlBuffer[128];
  snprintf(urlBuffer, sizeof(urlBuffer), TARGET_URL_IMAGE, cameraName, timeLabel);
  //S Serial.printf(">>> POST URL = \"%s\"\n", urlBuffer);
  HTTPClient http;
  http.begin(urlBuffer);
  http.addHeader("Content-Type", MIME_TYPE_JPEG);
  int httpResponseCode = http.POST(frameBuffer->buf, frameBuffer->len);
  JsonDocument jsonResponse;
  if (httpResponseCode == 200) {
    parseAndStoreSettings(http.getString());
    blinkLedOk();
  } else {
    uploadImageErrors++;
    Serial.printf(">>> HTTP Response code = %d\n", httpResponseCode);
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
  http.addHeader("Accept", MIME_TYPE_JSON);
  http.addHeader("Content-Type", MIME_TYPE_JSON);
  int httpResponseCode = http.PUT(jsonCharBuffer);
  if (httpResponseCode == 200) { 
    parseAndStoreSettings(http.getString());
    blinkLedOk();
  } else {
    Serial.printf(">>> HTTP Response code = %d\n", httpResponseCode);
    uploadStatusErrors++;
    blinkLedError();
  }
  http.end();
}

void parseAndStoreSettings(String jsonString) {
    //S Serial.print(">>> settings = ");
    //S Serial.println(jsonString);
    settings = parseJson(jsonString);
    workflowSettings = settings["workflow"];
    flashLedForPicture = workflowSettings["flashLedForPicture"];
    flashDurationMs = workflowSettings["flashDurationMs"];
    blinkOnSuccess = workflowSettings["blinkOnSuccess"];
    blinkOnFailure = workflowSettings["blinkOnFailure"];
    if (settings["camera"]) {
      cameraSettingsChanged = true;
      cameraSettings = settings["camera"];
    } else {
      cameraSettingsChanged = false;
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

String lastIpPart(IPAddress ipAddress) {
  String ipAddressString = ipAddress.toString();
  int i = ipAddressString.lastIndexOf(".");
  return ipAddressString.substring(i + 1);
}

void blinkLedOk() {
  if (blinkOnSuccess) {
    blinkLed(1,800);
  }
}

void blinkLedError() {
  if (blinkOnFailure) {
    blinkLed(3,300);
  }
}

void blinkLed(int count, int duration) {
  for (int i = 0; i < count; i++) {
    digitalWrite(BOARD_LED, HIGH);
    delay(duration);
    digitalWrite(BOARD_LED, LOW);
    delay(duration/2);
  }
}

char _timeBuffer[14];
// Return e.g. 240213-074417 (YYMMdd-hhmmss)
char* fetchtimeLabel() {
  struct tm now;
  if (!getLocalTime(&now)){
    Serial.println(">>> Failed to obtain time!");
    snprintf(_timeBuffer, sizeof(_timeBuffer) - 1, "no-time");
  } else {
    snprintf(_timeBuffer, sizeof(_timeBuffer) - 1, "%02d%02d%02d-%02d%02d%02d",
    now.tm_year % 100, 1 + now.tm_mon, now.tm_mday, now.tm_hour, now.tm_min, now.tm_sec);
    //S Serial.printf(">>> Obtainted time: %s\n", _timeBuffer);
  }
  return _timeBuffer;
}