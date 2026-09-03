package com.mmiranda.pointsbackapi.exception;

/**
 * Thrown when an authenticated caller's role/establishment scope does not permit the
 * requested action. Also used, per the authorization spec, when a caller queries a
 * resource id that belongs to a different establishment or does not exist at all -
 * both cases return this same exception so a 403 never leaks whether the id exists.
 */
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}
