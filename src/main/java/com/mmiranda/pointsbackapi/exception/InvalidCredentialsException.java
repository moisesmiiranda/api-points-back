package com.mmiranda.pointsbackapi.exception;

/**
 * Thrown on any login failure (unknown email or wrong password). Deliberately generic
 * so a caller cannot tell which of the two occurred.
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
