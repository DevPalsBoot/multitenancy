package com.example.multidata.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // COMMON
    INVALID_INPUT(400, "Invalid input"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not found"),
    INTERNAL_SERVER_ERROR(500, "Internal server error"),
    UNSUPPORTED_MEDIA_TYPE(800, "Results received from an external server are not what you expect"),
    // STORAGE 1000
    FAIL_TO_CREATE_STORAGE_CLIENT(11000, "fail to create storage client."),

    UNKNOWN(20000, "Unknown errors");

    private Integer code;
    private String message;
}
