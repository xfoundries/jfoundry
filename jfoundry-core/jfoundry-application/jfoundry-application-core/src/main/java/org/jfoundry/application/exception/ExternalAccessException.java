package org.jfoundry.application.exception;

/**
 * Indicates that a use case failed while accessing an external capability through an outbound port.
 */
public class ExternalAccessException extends ApplicationException {

    public ExternalAccessException(String message) {
        super(message);
    }

    public ExternalAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
