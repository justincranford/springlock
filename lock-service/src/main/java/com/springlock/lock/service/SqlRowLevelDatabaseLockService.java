package com.springlock.lock.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.springlock.lock.LockService;
import com.springlock.lock.model.LockInfo;
import com.springlock.lock.repository.SqlRowLockRepository;

/** JDBC-based lock service (pessimistic FOR UPDATE or optimistic CAS), profiles: postgres, h2. */
@Service
@Profile({"postgres", "h2"})
@ConditionalOnProperty(name = "springlock.lock.strategy", havingValue = "jdbc")
public class SqlRowLevelDatabaseLockService implements LockService {

    private final SqlRowLockRepository repository;

    public SqlRowLevelDatabaseLockService(SqlRowLockRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public boolean acquireLock(String lockKey, String owner, Duration ttl) {
        Instant now = Instant.now();
        return repository.tryAcquire(lockKey, owner, now, now.plus(ttl));
    }

    @Override
    @Transactional
    public boolean releaseLock(String lockKey, String owner) {
        return repository.release(lockKey, owner, Instant.now());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLocked(String lockKey) {
        return repository.isLocked(lockKey, Instant.now());
    }

    @Override
    @Transactional
    public boolean renewLock(String lockKey, String owner, Duration ttl) {
        return repository.renew(lockKey, owner, Instant.now().plus(ttl));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LockInfo> findLock(String lockKey) {
        return repository.findLockInfo(lockKey, Instant.now());
    }
}
