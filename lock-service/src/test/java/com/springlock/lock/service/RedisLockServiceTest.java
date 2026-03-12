package com.springlock.lock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.springlock.lock.model.LockInfo;

@ExtendWith(MockitoExtension.class)
class RedisLockServiceTest {

    private static final String KEY_PREFIX = "springlock:lock:";

    @Mock
    RedisTemplate<String, String> redisTemplate;

    @Mock
    ValueOperations<String, String> valueOps;

    @InjectMocks
    RedisLockService service;

    @Test
    void acquireLock_whenKeyAbsent_returnsTrue() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq(KEY_PREFIX + "key"), eq("owner"), any(Duration.class))).thenReturn(true);

        boolean result = service.acquireLock("key", "owner", Duration.ofMinutes(5));

        assertThat(result).isTrue();
    }

    @Test
    void acquireLock_whenKeyExists_returnsFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(eq(KEY_PREFIX + "key"), eq("owner"), any(Duration.class))).thenReturn(false);

        boolean result = service.acquireLock("key", "owner", Duration.ofMinutes(5));

        assertThat(result).isFalse();
    }

    @Test
    void acquireLock_whenSetIfAbsentReturnsNull_returnsFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(any(), any(), any(Duration.class))).thenReturn(null);

        boolean result = service.acquireLock("key", "owner", Duration.ofMinutes(5));

        assertThat(result).isFalse();
    }

    @Test
    void releaseLock_whenOwnerMatches_returnsTrueAndDeletes() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(KEY_PREFIX + "key")).thenReturn("owner");

        boolean result = service.releaseLock("key", "owner");

        assertThat(result).isTrue();
        verify(redisTemplate).delete(KEY_PREFIX + "key");
    }

    @Test
    void releaseLock_whenOwnerMismatch_returnsFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(KEY_PREFIX + "key")).thenReturn("other-owner");

        boolean result = service.releaseLock("key", "owner");

        assertThat(result).isFalse();
    }

    @Test
    void releaseLock_whenKeyNotExists_returnsFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(KEY_PREFIX + "key")).thenReturn(null);

        boolean result = service.releaseLock("key", "owner");

        assertThat(result).isFalse();
    }

    @Test
    void isLocked_whenKeyExists_returnsTrue() {
        when(redisTemplate.hasKey(KEY_PREFIX + "key")).thenReturn(true);

        assertThat(service.isLocked("key")).isTrue();
    }

    @Test
    void isLocked_whenKeyNotExists_returnsFalse() {
        when(redisTemplate.hasKey(KEY_PREFIX + "key")).thenReturn(false);

        assertThat(service.isLocked("key")).isFalse();
    }

    @Test
    void isLocked_whenHasKeyReturnsNull_returnsFalse() {
        when(redisTemplate.hasKey(KEY_PREFIX + "key")).thenReturn(null);

        assertThat(service.isLocked("key")).isFalse();
    }

    @Test
    void renewLock_whenOwnerMatches_returnsTrueAndExpiresKey() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(KEY_PREFIX + "key")).thenReturn("owner");

        boolean result = service.renewLock("key", "owner", Duration.ofMinutes(5));

        assertThat(result).isTrue();
        verify(redisTemplate).expire(eq(KEY_PREFIX + "key"), eq(Duration.ofMinutes(5)));
    }

    @Test
    void renewLock_whenOwnerMismatch_returnsFalse() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(KEY_PREFIX + "key")).thenReturn("other");

        boolean result = service.renewLock("key", "owner", Duration.ofMinutes(5));

        assertThat(result).isFalse();
    }

    @Test
    void findLock_whenActive_returnsLockInfo() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(KEY_PREFIX + "key")).thenReturn("owner");
        when(redisTemplate.getExpire(KEY_PREFIX + "key", TimeUnit.MILLISECONDS)).thenReturn(300000L);

        Optional<LockInfo> info = service.findLock("key");

        assertThat(info).isPresent();
        assertThat(info.get().lockKey()).isEqualTo("key");
        assertThat(info.get().owner()).isEqualTo("owner");
    }

    @Test
    void findLock_whenKeyNotExists_returnsEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(KEY_PREFIX + "key")).thenReturn(null);

        Optional<LockInfo> info = service.findLock("key");

        assertThat(info).isEmpty();
    }

    @Test
    void findLock_whenExpired_returnsEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(KEY_PREFIX + "key")).thenReturn("owner");
        when(redisTemplate.getExpire(KEY_PREFIX + "key", TimeUnit.MILLISECONDS)).thenReturn(-1L);

        Optional<LockInfo> info = service.findLock("key");

        assertThat(info).isEmpty();
    }

    @Test
    void findLock_whenTtlZero_returnsLockInfo() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(KEY_PREFIX + "key")).thenReturn("owner");
        when(redisTemplate.getExpire(KEY_PREFIX + "key", TimeUnit.MILLISECONDS)).thenReturn(0L);

        Optional<LockInfo> info = service.findLock("key");

        assertThat(info).isPresent();
        assertThat(info.get().lockKey()).isEqualTo("key");
    }

    @Test
    void findLock_whenGetExpireReturnsNull_returnsEmpty() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.get(KEY_PREFIX + "key")).thenReturn("owner");
        when(redisTemplate.getExpire(KEY_PREFIX + "key", TimeUnit.MILLISECONDS)).thenReturn(null);

        Optional<LockInfo> info = service.findLock("key");

        assertThat(info).isEmpty();
    }
}
