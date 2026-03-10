package com.springlock.lock.redis;

import com.springlock.lock.LockService;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

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
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(LOCK_PREFIX + lockKey, owner, ttl);
        return Boolean.TRUE.equals(acquired);
    }

    @Override
    public boolean releaseLock(String lockKey, String owner) {
        String key = LOCK_PREFIX + lockKey;
        String currentOwner = redisTemplate.opsForValue().get(key);
        if (owner.equals(currentOwner)) {
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
        String currentOwner = redisTemplate.opsForValue().get(key);
        if (owner.equals(currentOwner)) {
            redisTemplate.expire(key, ttl);
            return true;
        }
        return false;
    }
}
