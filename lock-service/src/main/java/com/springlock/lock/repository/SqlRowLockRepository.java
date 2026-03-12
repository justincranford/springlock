package com.springlock.lock.repository;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalLong;

import com.springlock.lock.model.LockInfo;

public interface SqlRowLockRepository {

    boolean tryAcquire(String lockKey, String owner, Instant now, Instant expiresAt);

    boolean release(String lockKey, String owner, Instant now);

    boolean isLocked(String lockKey, Instant now);

    boolean renew(String lockKey, String owner, Instant expiresAt);

    Optional<LockInfo> findLockInfo(String lockKey, Instant now);

    OptionalLong findVersion(String lockKey);
}
