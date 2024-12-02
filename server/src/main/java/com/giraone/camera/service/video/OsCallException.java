package com.giraone.camera.service.video;

public class OsCallException extends RuntimeException {
    public OsCallException(String message) {
        super(message);
    }
    public OsCallException(String message, Throwable cause) {
        super(message, cause);
    }
}
