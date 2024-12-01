package com.giraone.camera.service.model;

public record FileInfoQuery(String prefixFilter, int offset, int limit, FileInfoOrder order) {
}
