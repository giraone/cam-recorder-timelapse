package com.giraone.camera.service.model;

public record VideoMetaInfo(String videoCodec, String audioCodec, int durationSeconds, String resolution, int framesPerSecond) {
    @Override
    public String toString() {
        if (audioCodec != null && !audioCodec.isBlank()) {
            return videoCodec + "/" + audioCodec + ", " + resolution + ", " + durationSeconds + "s, " + framesPerSecond + " fps";
        } else {
            return videoCodec + ", " + resolution + ", " + durationSeconds + "s, " + framesPerSecond + " fps";
        }
    }
}
