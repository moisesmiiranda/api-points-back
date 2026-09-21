package com.mmiranda.pointsbackapi.exception;

/** The caller's establishment is suspended (or its trial ended). Mapped to HTTP 402 Payment Required. */
public class EstablishmentSuspendedException extends RuntimeException {
    public EstablishmentSuspendedException(String message) {
        super(message);
    }
}
