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
    FAIL_TO_CREATE_BUCKET(11001, "fail to create bucket"),
    FAILED_TO_CHECK_IF_BUCKET_EXISTS (11002, "Failed to check if the bucket exists"),
    FAIL_TO_UPLOAD_FILE(11003, "Failed to uplaod file"),
    FAIL_TO_DOWNLOAD_FILE(11003, "Failed to download file"),

    UNKNOWN(20000, "Unknown errors");

    private Integer code;
    private String message;
}
