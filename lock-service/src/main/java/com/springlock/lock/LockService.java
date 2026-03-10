package com.springlock.lock;

import java.time.Duration;

/**
 * Distributed lock service abstraction.
 * Supports up to 100 concurrent microservices sharing a common DB or Redis backend.
 */
public interface LockService {

    /**
     * Attempts to acquire a distributed lock.
     *
     * @param lockKey unique name of the lock resource
     * @param owner   identifier of the caller (e.g. service instance ID)
     * @param ttl     maximum time the lock should be held before automatic expiry
     * @return {@code true} if the lock was acquired, {@code false} if already held
     */
    boolean acquireLock(String lockKey, String owner, Duration ttl);

    /**
     * Releases a distributed lock held by the given owner.
     *
     * @param lockKey unique name of the lock resource
     * @param owner   identifier of the caller that originally acquired the lock
     * @return {@code true} if the lock was released, {@code false} if not held by owner
     */
    boolean releaseLock(String lockKey, String owner);

    /**
     * Returns whether a lock is currently active (not expired).
     *
     * @param lockKey unique name of the lock resource
     * @return {@code true} if the lock is currently held and not expired
     */
    boolean isLocked(String lockKey);

    /**
     * Extends the TTL of a lock currently held by the given owner.
     *
     * @param lockKey unique name of the lock resource
     * @param owner   identifier of the caller that holds the lock
     * @param ttl     new duration from now
     * @return {@code true} if the lock was renewed, {@code false} if not held by owner
     */
    boolean renewLock(String lockKey, String owner, Duration ttl);
}
