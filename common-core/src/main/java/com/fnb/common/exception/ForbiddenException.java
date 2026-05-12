package com.fnb.common.exception;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends BaseException {
    public ForbiddenException(String message) {
        super(message, HttpStatus.FORBIDDEN);
    }
    public ForbiddenException() {
        super("Access denied", HttpStatus.FORBIDDEN);
    }
}
