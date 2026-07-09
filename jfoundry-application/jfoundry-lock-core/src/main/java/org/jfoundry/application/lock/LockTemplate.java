package org.jfoundry.application.lock;

import java.util.Objects;

/**
 * Programmatic entry point for executing callbacks under a distributed lock.
 */
public class LockTemplate {

    private final DistributedLockClient lockClient;

    public LockTemplate(DistributedLockClient lockClient) {
        this.lockClient = Objects.requireNonNull(lockClient, "lockClient must not be null");
    }

    public <T> T execute(String name, LockCallback<T> callback) throws Exception {
        return execute(name, LockOptions.defaults(), callback);
    }

    public <T> T execute(String name, LockOptions options, LockCallback<T> callback) throws Exception {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name must not be blank");
        }
        Objects.requireNonNull(options, "options must not be null");
        Objects.requireNonNull(callback, "callback must not be null");

        LockHandle handle = lockClient.tryLock(name, options);
        if (!handle.acquired()) {
            if (options.failureMode() == LockFailureMode.SKIP) {
                return null;
            }
            throw new DistributedLockUnavailableException(name);
        }
        try {
            return callback.execute();
        } finally {
            handle.release();
        }
    }
}
