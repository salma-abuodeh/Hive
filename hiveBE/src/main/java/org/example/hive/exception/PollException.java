package org.example.hive.exception;

import org.springframework.http.HttpStatus;

public class PollException extends RuntimeException {

    private final HttpStatus status;

    public PollException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}