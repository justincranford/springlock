package com.springlock.lock.service;

import java.time.Duration;

import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.springlock.lock.LockService;

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
}
