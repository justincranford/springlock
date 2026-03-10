package com.springlock.lock;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;

@Service
@Profile({"postgres", "h2"})
public class DatabaseLockService implements LockService {

    private final LockRepository lockRepository;

    public DatabaseLockService(LockRepository lockRepository) {
        this.lockRepository = lockRepository;
    }

    @Override
    @Transactional
    public boolean acquireLock(String lockKey, String owner, Duration ttl) {
        Instant now = Instant.now();
        lockRepository.deleteExpired(now);
        if (lockRepository.existsByLockKeyAndExpiresAtGreaterThanEqual(lockKey, now)) {
            return false;
        }
        lockRepository.saveAndFlush(new DistributedLock(lockKey, owner, now.plus(ttl)));
        return true;
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
