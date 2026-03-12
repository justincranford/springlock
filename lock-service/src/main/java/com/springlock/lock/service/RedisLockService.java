package com.springlock.lock.service;

import java.time.Duration;
import java.util.Optional;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.springlock.lock.LockService;
import com.springlock.lock.model.LockInfo;

/** Redis-based distributed lock service (profile: redis). */
@Service
@Profile("redis")
public class RedisLockService implements LockService {

    private static final String LOCK_PREFIX = "springlock:lock:";

    private final RedisTemplate<String, String> redisTemplate;

    public RedisLockService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public boolean acquireLock(String lockKey, String owner, Duration ttl) {
        return Boolean.TRUE.equals(
            redisTemplate.opsForValue().setIfAbsent(LOCK_PREFIX + lockKey, owner, ttl));
    }

    @Override
    public boolean releaseLock(String lockKey, String owner) {
        String key = LOCK_PREFIX + lockKey;
        if (owner.equals(redisTemplate.opsForValue().get(key))) {
            redisTemplate.delete(key);
            return true;
        }
        return false;
    }

    @Override
    public boolean isLocked(String lockKey) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(LOCK_PREFIX + lockKey));
    }

    @Override
    public boolean renewLock(String lockKey, String owner, Duration ttl) {
        String key = LOCK_PREFIX + lockKey;
        if (owner.equals(redisTemplate.opsForValue().get(key))) {
            redisTemplate.expire(key, ttl);
            return true;
        }
        return false;
    }

    @Override
    public Optional<LockInfo> findLock(String lockKey) {
        String key = LOCK_PREFIX + lockKey;
        String owner = redisTemplate.opsForValue().get(key);
        if (owner == null) {
            return Optional.empty();
        }
        Long ttlMillis = redisTemplate.getExpire(key, java.util.concurrent.TimeUnit.MILLISECONDS);
        if (ttlMillis == null || ttlMillis < 0) {
            return Optional.empty();
        }
        return Optional.of(new LockInfo(lockKey, owner,
            java.time.Instant.now().plusMillis(ttlMillis)));
    }
}
