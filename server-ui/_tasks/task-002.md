# Add display of camera status

## Context

The backend server provides also two endpoints to return the status of the
cameras, that are connected.

See [CameraController.java](../../server/src/main/java/com/giraone/camera/controller/CameraController.java)

The endpoint are `cameras` (method getCameras) and `status/{cameraName}` (method getStatusOfCamera).

## Objective

Implement a new screen named "Camera Status" and add it to the menu.
The screen should display a table for each camera containing all columns defined by
[CameraStatusRecord.java](../../server/src/main/java/com/giraone/camera/service/model/CameraStatusRecord.java).
The records should be ordered by `timestamp` in descending order.

## Hints

Always read [AGENTS.md](../AGENTS.md).
When ready, write a summary of the performed changes to this file in chapter "Done".

## Done

### New files

- `src/main/java/com/giraone/camera/service/model/CameraStatusRecord.java`
  Record mirroring `CameraStatusRecord` of the server module (`timestamp`, `rssi`, `imageCounter`,
  `imageErrors`, `cameraInitCounter`, `cameraInitErrors`, `uploadImageErrors`, `uploadStatusErrors`).
  It also carries the comparator `BY_TIMESTAMP_DESCENDING` (null timestamps last) and
  `timestampToDisplay()`, which formats the timestamp as ISO-8601 local date time.
- `src/main/java/com/giraone/camera/views/status/CameraStatusView.java`
  New screen at route `camera-status`, `@PermitAll`, inside `MainLayout`. It shows a toolbar with a
  "Reload" button and the number of cameras, followed by one section per camera: an `H3` with the camera
  name and the number of records plus a `Grid<CameraStatusRecord>` with one column per record attribute.
  The rows are sorted by timestamp in descending order and the grid's timestamp column is pre-sorted
  descending as well, so the ordering stays visible when the user re-sorts. Grid height is limited to
  `20em`, so tables with many records scroll instead of pushing the following cameras off the screen.
  If no camera has reported a status yet, a hint is displayed; if the backend call fails, the error message
  is displayed instead of the tables.

### Changed files

- `src/main/java/com/giraone/camera/service/FileViewService.java`
  Added `listCameras()` (endpoint `cameras`, names returned alphabetically) and
  `listCameraStatus(String cameraName)` (endpoint `status/{cameraName}`).
  The camera names are read via `bodyToMono(ParameterizedTypeReference<List<String>>)`: with
  `bodyToFlux(String.class)` the WebClient picks the raw `StringDecoder` instead of Jackson, which passes
  the undecoded JSON text through rather than the elements of the array.
- `src/main/java/com/giraone/camera/views/MainLayout.java`
  Drawer entry "Camera Status" added between "Videos" and "Workflow Settings".
- `pom.xml`, `CHANGELOG.md` - version 0.0.4-SNAPSHOT and its list of changes.

### Tests

- `CameraStatusRecordTest` - timestamp formatting (parameterized) and the descending comparator,
  including records without a timestamp.
- `CameraStatusViewTest` - builds the view with a test double of `FileViewService` (a subclass, no mocking
  framework) and asserts: one table per camera, a column per record attribute, rows ordered by timestamp
  descending, the "no camera" hint and the error message of an unreachable backend.
- `FileViewServiceTest` - runs the two new service methods against a `com.sun.net.httpserver.HttpServer`
  test double. Real HTTP is used on purpose, because the JSON decoding of the WebClient is part of the
  behaviour under test (this is what uncovered the `String` decoder problem described above).
- `RouteAccessTest` - extended by `CameraStatusView`, so the access rules of the new route are guarded too.

`mvn test`: 51 tests, 0 failures (the 2 skipped ones are the pre-existing TestBench E2E tests, which need
a browser).

### Verified against the running backend

The real `server` module was started and fed with status records for two cameras via `PUT /status`.
`FileViewService.listCameras()` and `listCameraStatus(...)` decoded the real responses correctly, including
the nanosecond precision timestamps, and the records arrive from the server in ascending order - which is
why the view sorts them.

Not verified: the visual appearance of the screen in a browser. No browser is available in this environment
(the TestBench E2E tests are skipped for the same reason), and a Vaadin view renders client side, so
fetching the route over HTTP only returns the bootstrap page.

## Costs

```
   Total cost:            $2.86
   Total duration (API):  6m 11s
   Total duration (wall): 16m 23s
   Total code changes:    0 lines added, 0 lines removed
   Usage by model:
       claude-haiku-4-5:  901 input, 13 output, 0 cache read, 0 cache write ($0.0010)
          claude-opus-5:  576 input, 31.2k output, 2.9m cache read, 97.1k cache write ($2.86)
   Prompt cache (main):   36 requests · 97% of input tokens from cache · no misses · warm (5m TTL, last activity 19s ago)
```