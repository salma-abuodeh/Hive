package org.example.hive.exception;

import org.springframework.http.HttpStatus;

public class TeamException extends RuntimeException {

    private final HttpStatus status;

    public TeamException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
