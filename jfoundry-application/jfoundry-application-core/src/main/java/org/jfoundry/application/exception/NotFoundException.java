package org.jfoundry.application.exception;

/**
 * Indicates that data required by a use case cannot be found.
 */
public class NotFoundException extends ApplicationException {

    public NotFoundException(String message) {
        super(message);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
