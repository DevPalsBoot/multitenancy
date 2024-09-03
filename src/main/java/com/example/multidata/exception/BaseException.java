package com.example.multidata.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.IOException;

@Data
@EqualsAndHashCode(callSuper = true)
public class BaseException extends RuntimeException {

    private ErrorCode errorCode;
    private String appendMessage;

    public BaseException(ErrorCode errorCode) {
        super("ErrorCode : " + errorCode.getCode()+ ", " + errorCode.name() + " (" + errorCode.getMessage() + ")");
        this.errorCode = errorCode;
    }
    public BaseException(ErrorCode errorCode, Throwable cause) {
        super(cause);
        this.errorCode = errorCode;
    }

}
