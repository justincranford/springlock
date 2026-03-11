package com.springlock.lock;

import java.time.Duration;

/** Distributed lock service abstraction (JPA, JDBC-pessimistic, JDBC-optimistic, Redis). */
public interface LockService {

    /** Acquires the lock. Returns {@code true} iff newly acquired. */
    boolean acquireLock(String lockKey, String owner, Duration ttl);

    /** Releases the lock held by owner. Returns {@code true} iff released. */
    boolean releaseLock(String lockKey, String owner);

    /** Returns {@code true} iff the lock is currently held and not expired. */
    boolean isLocked(String lockKey);

    /** Extends the TTL of a lock held by owner. Returns {@code true} iff renewed. */
    boolean renewLock(String lockKey, String owner, Duration ttl);
}
