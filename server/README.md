# HTTP service for receiving and storing ESP32 camera images

- Receive and store images in local file system via HTTP POST
- Provide camera configuration - t.b.d.

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
  --data-binary @src/test/resources/testdata/small.jpg \
  http://localhost:8080/files/images/my-file.jpg
```

This should return

```json
{"success":true,"size":10240}
```

Download the uploaded image

```bash
curl --request GET \
  --silent \
  --output out.jpg \
  http://localhost:8080/files/images/my-file.jpg
```

This should return the file.

## Usage of fetch camera settings

```bash
curl --request GET \
  --silent \
  http://localhost:8080/camera/settings
```
