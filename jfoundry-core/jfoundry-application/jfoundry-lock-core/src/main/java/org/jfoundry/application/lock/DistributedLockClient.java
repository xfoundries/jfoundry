package org.jfoundry.application.lock;

/**
 * Framework-neutral SPI for acquiring named distributed locks.
 */
@FunctionalInterface
public interface DistributedLockClient {

    /**
     * Attempts to acquire a named lock.
     *
     * @param name lock name
     * @param options acquisition options
     * @return lock handle with acquisition state and release callback
     * @throws Exception if the backend fails while acquiring the lock
     */
    LockHandle tryLock(String name, LockOptions options) throws Exception;
}
