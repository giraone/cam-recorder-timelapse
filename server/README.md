# HTTP service for receiving and storing ESP32 camera images

- Receive and store images in local file system via HTTP POST received from (ESP32) camera
- Create thumbnails for uploaded images
- Provide camera configuration (brightness, exposure, resolution, ...) to be loaded by camera
- Store video and create thumbnails using  *ffmpeg*
- Create timelapse videos using *ffmpeg*
- UI to change camera settings - see server-ui
- UI to view uploaded images - see server-ui

## Build

```
mvn package
```

## Run

```
./run.sh
```

## Usage of file upload and download

Upload a JPEG image

```bash
curl --request POST \
  --header "Content-Type: image/jpeg" \
  --data-binary @FILES/0000-ferrari.jpg \
  http://localhost:9001/images/my-file.jpg
```

This should return a JSON like this (the "restart" attribute is used to tell the camera to restart and re-load the settings):

```json
{
  "success": true,
  "size": 10240,
  "restart": false,
  "paused": false
}
```

Download the uploaded image

```bash
curl --request GET --output out.jpg http://localhost:9001/images/my-file.jpg
```

This should return the file.

## Usage of fetch camera settings

```bash
curl --request GET http://localhost:9001/camera-settings
```

Will return sth. like

```json
{
  "gammaCorrect": true,
  "autoExposureGainCeiling": 2,
  "jpegQuality": 10,
  "blackPixelCorrect": false,
  "whitePixelCorrect": false,
  "autoWhitebalance": true,
  "autoExposureGainControl": true,
  "autoExposureLevel": 1,
  "saturation": 0,
  "loopDelaySeconds": 5,
  "verticalFlip": false,
  "whitebalanceMode": 0,
  "autoExposureGainValue": 25,
  "sharpness": 0,
  "horizontalMirror": false,
  "clockFrequencyHz": 16000000,
  "exposureCtrlDsp": false,
  "lensCorrect": true,
  "specialEffect": 0,
  "exposureCtrlSensor": true,
  "autoExposureValue": 1000,
  "frameSize": 13,
  "brightness": 0,
  "denoise": 0,
  "autoWhitebalanceGain": true,
  "contrast": 0
}
```
