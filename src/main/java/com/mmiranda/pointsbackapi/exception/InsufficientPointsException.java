package com.mmiranda.pointsbackapi.exception;

/** A points operation would leave a client's balance negative. Mapped to HTTP 422. */
public class InsufficientPointsException extends RuntimeException {
    public InsufficientPointsException(String message) {
        super(message);
    }
}
