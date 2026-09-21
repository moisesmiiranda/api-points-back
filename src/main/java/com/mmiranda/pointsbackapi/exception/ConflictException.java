package com.mmiranda.pointsbackapi.exception;

/** The request conflicts with existing data (for example a client that is already registered). Mapped to HTTP 409. */
public class ConflictException extends RuntimeException {
    public ConflictException(String message) {
        super(message);
    }
}
