#pragma once
#include "esp_camera.h"
#include <Arduino_JSON.h>

void initCamera();
void initCameraWithSettings(JSONVar jsonObject);
