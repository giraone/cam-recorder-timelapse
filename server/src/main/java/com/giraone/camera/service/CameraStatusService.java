package com.giraone.camera.service;

import com.giraone.camera.service.api.CameraStatus;
import com.giraone.camera.service.model.CameraStatusRecord;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service, to store and return camera status (WiFi strength, number of errors)
 */
@Service
public class CameraStatusService {

    private final Map<String, List<CameraStatusRecord>> storage = new HashMap<>();

    public void reset(String cameraName) {
        storage.remove(cameraName);
        storage.put(cameraName, new ArrayList<>());
    }

    public void store(CameraStatus cameraStatus) {
        final CameraStatusRecord cameraStatusRecord = new CameraStatusRecord(
            LocalDateTime.now(),
            cameraStatus.rssi(),
            cameraStatus.imageCounter(),
            cameraStatus.imageErrors(),
            cameraStatus.cameraInitCounter(),
            cameraStatus.cameraInitErrors(),
            cameraStatus.uploadImageErrors(),
            cameraStatus.uploadStatusErrors()
        );
        final List<CameraStatusRecord> statusList = storage.computeIfAbsent(cameraStatus.cameraName(), _ -> new ArrayList<>());
        statusList.add(cameraStatusRecord);
    }

    public List<CameraStatusRecord> get(String cameraName) {
        return storage.computeIfAbsent(cameraName, _ -> new ArrayList<>());
    }

    public @Nullable Set<String> getCameras() {
        return storage.keySet();
    }
}
