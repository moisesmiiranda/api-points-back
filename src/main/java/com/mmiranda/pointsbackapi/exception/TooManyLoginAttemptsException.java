package com.mmiranda.pointsbackapi.exception;

/** Too many failed logins for an IP/email pair inside the throttling window. Mapped to HTTP 429. */
public class TooManyLoginAttemptsException extends RuntimeException {
    public TooManyLoginAttemptsException(String message) {
        super(message);
    }
}
