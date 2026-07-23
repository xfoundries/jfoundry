package org.jfoundry.domain.exception;

/**
 * Indicates that a domain rule cannot be satisfied.
 */
public class DomainRuleViolationException extends DomainException {

    public DomainRuleViolationException(String message) {
        super(message);
    }

    public DomainRuleViolationException(String message, Throwable cause) {
        super(message, cause);
    }
}
