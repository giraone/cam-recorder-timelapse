package com.giraone.camera.service.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@SuppressWarnings("unused") // Used by JSON
@JsonInclude(JsonInclude.Include.NON_NULL)
public record Status(boolean success, String error) {
}
