package com.giraone.camera.service.api;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Status(boolean success, String error) {
}
