package org.jfoundry.domain.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class DomainExceptionTest {

    @Test
    void ruleViolationPreservesMessageAndCause() {
        RuntimeException cause = new RuntimeException("quota service returned stale data");

        DomainRuleViolationException exception = new DomainRuleViolationException("Quota exceeded", cause);

        assertInstanceOf(DomainException.class, exception);
        assertEquals("Quota exceeded", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void stateExceptionPreservesMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("current state is RUNNING");

        DomainStateException exception = new DomainStateException("Cannot delete running environment", cause);

        assertInstanceOf(DomainException.class, exception);
        assertEquals("Cannot delete running environment", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
