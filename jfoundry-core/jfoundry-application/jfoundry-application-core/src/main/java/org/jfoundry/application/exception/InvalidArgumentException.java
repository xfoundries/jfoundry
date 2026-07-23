package org.jfoundry.application.exception;

/**
 * Indicates that use-case input arguments are invalid.
 */
public class InvalidArgumentException extends ApplicationException {

    public InvalidArgumentException(String message) {
        super(message);
    }

    public InvalidArgumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
