package org.jfoundry.application.exception;

/**
 * Indicates that a use case conflicts with the current application state.
 */
public class ConflictException extends ApplicationException {

    public ConflictException(String message) {
        super(message);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, cause);
    }
}
