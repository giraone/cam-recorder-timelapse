package com.giraone.streaming.service.model;

public record VideoMetaInfo(String videoCodec, String audioCodec, int durationSeconds, String resolution, int framesPerSecond) {
    @Override
    public String toString() {
        return videoCodec + "/" + audioCodec + ", " + resolution + ", " + durationSeconds + "s, " + framesPerSecond + " fps";
    }
}
