# Client and Server for recording ESP32 camera images

- [client](client/README.md)
- [server](server/README.md)
- [server-ui](server-ui/README.md)

## Flow

When the ESP32Cam starts, it
- sets up WiFi,
- fetches the correct time via NTP
- and enters the **main loop**

The **main loop** consists of
- passing the *device status* (WiFi signal strength, number of camera errors, number of image upload errors) via HTTP POST
  to the *image server* and receiving back the *workflow and camera settings*
  - *workflow settings* are e.g. delay time till next photo, going into paused mode, indicator for using the led as a flash, restarting the device, ...
  - *cameras settings* are a larger amount of properties like image size, JPEG quality, exposure levels, white balance, ... 
- initialzing the camera with the received settings and taking the first photo using these settings
- uploading the photo to the image server and receiving back again the settings, when they have been changed
- applying the received delay time till the next action
- if the settings were changed, the ESP32Cam applies them by re-initialzing the camera and will take and upload the next photo
- if the workflow setting had indicated going into the pause mode, the ESP32Cam will only upload its status without taking a photo
  and receives again new setting, that may indicate to leave to paused mode now

## Workflow and cameras settings 

```json
{
 "workflow": {
   "restart":                 false/true,
   "pause":                   false/true,
   "delayMs":                 10 - 3600000,
   "blinkOnSuccess":          false/true,
   "blinkOnFailure":          false/true,
   "flashLedForPicture":      false/true,
   "flashDurationMs":         10-1000
  },
 "camera": {
   "clockFrequencyHz":        16000000/20000000,
   "frameSize":               1-20,
   "jpegQuality":             0-63,
   "blackPixelCorrect":       0/1,
   "whitePixelCorrect":       0/1,
   "gammaCorrect":            0/1,
   "lensCorrect":             0/1,
   "horizontalMirror":        0/1,
   "verticalFlip":            0/1,
   "brightness":              -2/-1/0/1/2,
   "contrast":                -2/-1/0/1/2,
   "sharpness":               -2/-1/0/1/2,
   "saturation":              -2/-1/0/1/2,
   "denoise":                 -2/-1/0/1/2,
   "specialEffect":           0-6,
   "autoWhitebalance":        0/1,
   "autoWhitebalanceGain":    1,
   "whitebalanceMode":        0-4,
   "exposureCtrlSensor":      0/1,
   "exposureCtrlDsp":         0/1,
   "autoExposureLevel":       0,
   "autoExposureValue":       0-1023,
   "autoExposureGainControl": 0/1,
   "autoExposureGainValue":   0-30,
   "autoExposureGainCeiling": 0-6
 }
}
```

## UI

Form for Camera Settings

![camera-settings](docs/images/camera-settings.png)

List of captured images

![images](docs/images/images.png)

List of created timelapse videos

![videos](docs/images/videos.png)

Captured image list with image display side-by-side

![images-half-viewer](docs/images/images-half-viewer.png)

Full view of a captured image

![images-full-viewer](docs/images/images-full-viewer.png)