package com.giraone.camera.service.model;

import reactor.core.publisher.Flux;

import java.nio.ByteBuffer;

public record FileInfoAndContent(Flux<ByteBuffer> content, FileInfo fileInfo) {
}
