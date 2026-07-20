package org.example.hive.exception;

import org.springframework.http.HttpStatus;

public class CompanyException extends RuntimeException {

    private final HttpStatus status;

    public CompanyException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}