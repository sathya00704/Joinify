package com.example.Joinify.exception;

// Logged in, but not allowed to touch this resource (e.g. another organizer's event) -> 403
public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) {
        super(message);
    }
}