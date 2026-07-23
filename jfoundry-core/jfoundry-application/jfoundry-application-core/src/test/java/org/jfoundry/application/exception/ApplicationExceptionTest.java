package org.jfoundry.application.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

class ApplicationExceptionTest {

    @Test
    void invalidArgumentPreservesMessageAndCause() {
        IllegalArgumentException cause = new IllegalArgumentException("pageSize");

        InvalidArgumentException exception = new InvalidArgumentException("Invalid page size", cause);

        assertInstanceOf(ApplicationException.class, exception);
        assertEquals("Invalid page size", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void notFoundPreservesMessageAndCause() {
        RuntimeException cause = new RuntimeException("missing row");

        NotFoundException exception = new NotFoundException("Environment not found", cause);

        assertInstanceOf(ApplicationException.class, exception);
        assertEquals("Environment not found", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void conflictPreservesMessageAndCause() {
        RuntimeException cause = new RuntimeException("version mismatch");

        ConflictException exception = new ConflictException("Environment was modified", cause);

        assertInstanceOf(ApplicationException.class, exception);
        assertEquals("Environment was modified", exception.getMessage());
        assertSame(cause, exception.getCause());
    }

    @Test
    void externalAccessPreservesMessageAndCause() {
        RuntimeException cause = new RuntimeException("remote timeout");

        ExternalAccessException exception = new ExternalAccessException("Container platform failed", cause);

        assertInstanceOf(ApplicationException.class, exception);
        assertEquals("Container platform failed", exception.getMessage());
        assertSame(cause, exception.getCause());
    }
}
