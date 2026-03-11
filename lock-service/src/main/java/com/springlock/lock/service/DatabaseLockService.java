package com.springlock.lock.service;

import java.time.Duration;
import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.springlock.lock.LockService;
import com.springlock.lock.domain.DistributedLock;
import com.springlock.lock.repository.LockRepository;

/** JPA-based distributed lock service (profiles: postgres, h2; strategy: jpa). */
@Service
@Profile({"postgres", "h2"})
@ConditionalOnProperty(name = "springlock.lock.strategy", havingValue = "jpa", matchIfMissing = true)
public class DatabaseLockService implements LockService {

    private final LockRepository lockRepository;

    public DatabaseLockService(LockRepository lockRepository) {
        this.lockRepository = lockRepository;
    }

    @Override
    public boolean acquireLock(String lockKey, String owner, Duration ttl) {
        Instant now = Instant.now();
        lockRepository.deleteExpired(now);
        if (lockRepository.existsByLockKeyAndExpiresAtGreaterThanEqual(lockKey, now)) {
            return false;
        }
        try {
            lockRepository.saveAndFlush(new DistributedLock(lockKey, owner, now.plus(ttl)));
            return true;
        } catch (DataIntegrityViolationException e) {
            return false; // concurrent insert by another thread
        }
    }

    @Override
    @Transactional
    public boolean releaseLock(String lockKey, String owner) {
        return lockRepository.deleteByLockKeyAndOwner(lockKey, owner) > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isLocked(String lockKey) {
        return lockRepository.existsByLockKeyAndExpiresAtGreaterThanEqual(lockKey, Instant.now());
    }

    @Override
    @Transactional
    public boolean renewLock(String lockKey, String owner, Duration ttl) {
        return lockRepository.findByLockKeyAndOwner(lockKey, owner)
            .map(lock -> {
                lock.setExpiresAt(Instant.now().plus(ttl));
                lockRepository.save(lock);
                return true;
            })
            .orElse(false);
    }
}
