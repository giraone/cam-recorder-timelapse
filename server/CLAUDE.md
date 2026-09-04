# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.
Read also [AGENTS.md](AGENTS.md).

## Project Overview

This is a Spring Boot (WebFlux) backend service for receiving, storing, and processing ESP32 camera images. The system creates timelapse videos from captured images and provides camera configuration management.

## Common Development Commands

### Build
```bash
mvn package
```

### Run Application
```bash
./run.sh
```
or
```bash
java -Xms256M -Xmx1024M -jar target/cam-recorder.jar
```

### Run Tests
```bash
mvn test
```

### Run Specific Test
```bash
mvn test -Dtest=ClassName
mvn test -Dtest=ClassName#methodName
```

### Integration Tests
Integration tests (classes ending with `IT`) are included in the standard test phase.

## Architecture Overview

### Core Components

**CameraController** (`src/main/java/com/giraone/camera/controller/CameraController.java`)
- Main REST API controller handling image upload/download, camera settings, and timelapse creation
- Endpoints: `/images/*`, `/camera-settings`, `/timelapse`, `/files`
- Uses `@CrossOrigin` for frontend integration

**FileService** (`src/main/java/com/giraone/camera/service/FileService.java`)
- Handles file system operations for images and thumbnails
- Manages reactive file I/O using WebFlux
- Integrates with imaging-kit library for thumbnail generation

**VideoService** (`src/main/java/com/giraone/camera/service/video/VideoService.java`)
- Creates timelapse videos using ffmpeg/ffprobe
- Extracts video metadata and generates video thumbnails
- Platform-aware binary paths (Windows/Linux)

### Key Features

**Reactive Architecture**
- Built on Spring WebFlux for non-blocking I/O
- Custom reactive file handling classes (`AsynchronousFileChannelAdapter`, `FileReadFlux`)
- Uses Project Reactor (`Flux`/`Mono`)

**ESP32 Integration**
- Receives camera status and returns workflow/camera settings
- Settings stored in `../camera-settings.json` (relative to server directory)
- Supports camera configuration: resolution, quality, exposure, white balance, etc.

**File Storage**
- Images stored in file system (typically `../STORAGE/IMAGES/`)
- Automatic thumbnail generation in `.thumbs/` subdirectory
- Video files stored separately with metadata

**Custom Serialization**
- Custom Jackson deserializers for ESP32 enum types in `service/api/serde/`
- Boolean serializers for ESP32 compatibility

### Configuration

**Application Properties** (`src/main/resources/application.yml`)
- Server runs on port 9001
- Actuator endpoints enabled for monitoring
- Custom properties in `ApplicationProperties.java`:
  - `generate-thumbnails`: Enable/disable thumbnail creation
  - `show-config-on-startup`: Log configuration on startup

**External Dependencies**
- Uses `giraone-imaging-kit` library for image processing
- Requires `ffmpeg` and `ffprobe` binaries for video operations

### API Endpoints

**Image Management**
- `POST /images/{filename}` - Upload image from ESP32 camera
- `GET /images/{filename}` - Download image
- `DELETE /images/{filename}` - Delete image

**Camera Configuration**
- `GET /camera-settings` - Get current camera configuration
- `PUT /camera-settings` - Update camera settings
- `POST /camera-status` - Receive camera status, return workflow settings

**File Listing**
- `GET /files` - List images with filtering/sorting options
- `GET /files/{filename}` - Get specific file info

**Video/Timelapse**
- `POST /timelapse` - Create timelapse video from images

### Testing Structure

- Unit tests: Standard JUnit tests (`*Test.java`)
- Integration tests: Classes ending with `IT.java`
- Test utilities in `FluxUtilTest` for reactive stream testing

### Development Notes

- Uses Java 25 and Spring Boot 4.X
- Maven-based project with standard directory structure
- Imaging operations delegated to external `imaging-kit` library
- Platform-specific paths handled for Windows/Linux compatibility
- Settings persistence outside project directory for deployment flexibility