package org.jfoundry.domain.exception;

/**
 * Indicates that the current domain object state does not allow the requested behavior.
 */
public class DomainStateException extends DomainException {

    public DomainStateException(String message) {
        super(message);
    }

    public DomainStateException(String message, Throwable cause) {
        super(message, cause);
    }
}
