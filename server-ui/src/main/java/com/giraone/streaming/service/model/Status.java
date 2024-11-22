package com.giraone.streaming.service.model;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record Status(boolean success, String error) {
}
