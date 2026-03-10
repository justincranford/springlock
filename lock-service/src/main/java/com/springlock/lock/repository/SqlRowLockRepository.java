package com.springlock.lock.repository;

import java.time.Instant;
import java.util.OptionalLong;

public interface SqlRowLockRepository {

    boolean tryAcquire(String lockKey, String owner, Instant now, Instant expiresAt);

    boolean release(String lockKey, String owner, Instant now);

    boolean isLocked(String lockKey, Instant now);

    boolean renew(String lockKey, String owner, Instant expiresAt);

    OptionalLong findVersion(String lockKey);
}
