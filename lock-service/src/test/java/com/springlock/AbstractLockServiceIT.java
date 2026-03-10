package com.springlock;

import com.springlock.lock.LockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = LockServiceApplication.class)
public abstract class AbstractLockServiceIT {

    protected static final String LOCK_KEY = "test-lock";
    protected static final String OWNER_A = "service-instance-A";
    protected static final String OWNER_B = "service-instance-B";
    protected static final Duration SHORT_TTL = Duration.ofSeconds(2);
    protected static final Duration LONG_TTL = Duration.ofMinutes(5);

    @Autowired
    protected LockService lockService;

    @BeforeEach
    void releaseAll() {
        lockService.releaseLock(LOCK_KEY, OWNER_A);
        lockService.releaseLock(LOCK_KEY, OWNER_B);
    }

    @Test
    void acquireLock_whenNotHeld_shouldSucceed() {
        boolean acquired = lockService.acquireLock(LOCK_KEY, OWNER_A, LONG_TTL);
        assertThat(acquired).isTrue();
    }

    @Test
    void acquireLock_whenAlreadyHeldBySameOwner_shouldFail() {
        lockService.acquireLock(LOCK_KEY, OWNER_A, LONG_TTL);
        boolean secondAcquire = lockService.acquireLock(LOCK_KEY, OWNER_A, LONG_TTL);
        assertThat(secondAcquire).isFalse();
    }

    @Test
    void acquireLock_whenHeldByOtherOwner_shouldFail() {
        lockService.acquireLock(LOCK_KEY, OWNER_A, LONG_TTL);
        boolean acquired = lockService.acquireLock(LOCK_KEY, OWNER_B, LONG_TTL);
        assertThat(acquired).isFalse();
    }

    @Test
    void releaseLock_byOwner_shouldSucceed() {
        lockService.acquireLock(LOCK_KEY, OWNER_A, LONG_TTL);
        boolean released = lockService.releaseLock(LOCK_KEY, OWNER_A);
        assertThat(released).isTrue();
    }

    @Test
    void releaseLock_byNonOwner_shouldFail() {
        lockService.acquireLock(LOCK_KEY, OWNER_A, LONG_TTL);
        boolean released = lockService.releaseLock(LOCK_KEY, OWNER_B);
        assertThat(released).isFalse();
    }

    @Test
    void releaseLock_whenNotHeld_shouldReturnFalse() {
        boolean released = lockService.releaseLock(LOCK_KEY, OWNER_A);
        assertThat(released).isFalse();
    }

    @Test
    void isLocked_whenHeld_shouldReturnTrue() {
        lockService.acquireLock(LOCK_KEY, OWNER_A, LONG_TTL);
        assertThat(lockService.isLocked(LOCK_KEY)).isTrue();
    }

    @Test
    void isLocked_whenNotHeld_shouldReturnFalse() {
        assertThat(lockService.isLocked(LOCK_KEY)).isFalse();
    }

    @Test
    void isLocked_afterRelease_shouldReturnFalse() {
        lockService.acquireLock(LOCK_KEY, OWNER_A, LONG_TTL);
        lockService.releaseLock(LOCK_KEY, OWNER_A);
        assertThat(lockService.isLocked(LOCK_KEY)).isFalse();
    }

    @Test
    void renewLock_byOwner_shouldSucceed() {
        lockService.acquireLock(LOCK_KEY, OWNER_A, SHORT_TTL);
        boolean renewed = lockService.renewLock(LOCK_KEY, OWNER_A, LONG_TTL);
        assertThat(renewed).isTrue();
        assertThat(lockService.isLocked(LOCK_KEY)).isTrue();
    }

    @Test
    void renewLock_byNonOwner_shouldFail() {
        lockService.acquireLock(LOCK_KEY, OWNER_A, LONG_TTL);
        boolean renewed = lockService.renewLock(LOCK_KEY, OWNER_B, LONG_TTL);
        assertThat(renewed).isFalse();
    }

    @Test
    void acquireLock_afterRelease_shouldSucceedForNewOwner() {
        lockService.acquireLock(LOCK_KEY, OWNER_A, LONG_TTL);
        lockService.releaseLock(LOCK_KEY, OWNER_A);
        boolean acquired = lockService.acquireLock(LOCK_KEY, OWNER_B, LONG_TTL);
        assertThat(acquired).isTrue();
    }
}
