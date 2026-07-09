package org.jfoundry.application.lock;

/**
 * Raised when a distributed lock cannot be acquired and the caller requested failure propagation.
 */
public class DistributedLockUnavailableException extends RuntimeException {

    public DistributedLockUnavailableException(String name) {
        super("Distributed lock is unavailable: " + name);
    }
}
