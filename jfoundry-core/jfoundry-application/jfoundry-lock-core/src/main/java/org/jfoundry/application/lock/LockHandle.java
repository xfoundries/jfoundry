package org.jfoundry.application.lock;

import java.util.Objects;

/**
 * Result of a distributed lock acquisition attempt.
 */
public record LockHandle(String name, boolean acquired, Runnable releaseAction) {

    public LockHandle {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        releaseAction = Objects.requireNonNull(releaseAction, "releaseAction must not be null");
    }

    public LockHandle(String name, boolean acquired) {
        this(name, acquired, () -> {
        });
    }

    public void release() {
        if (acquired) {
            releaseAction.run();
        }
    }
}
