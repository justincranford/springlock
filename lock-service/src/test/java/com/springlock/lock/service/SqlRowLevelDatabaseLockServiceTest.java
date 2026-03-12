package com.springlock.lock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.springlock.lock.model.LockInfo;
import com.springlock.lock.repository.SqlRowLockRepository;

@ExtendWith(MockitoExtension.class)
class SqlRowLevelDatabaseLockServiceTest {

    @Mock
    SqlRowLockRepository repository;

    @InjectMocks
    SqlRowLevelDatabaseLockService service;

    @Test
    void acquireLock_delegatesToTryAcquire_returnsTrue() {
        when(repository.tryAcquire(eq("key"), eq("owner"), any(Instant.class), any(Instant.class))).thenReturn(true);

        assertThat(service.acquireLock("key", "owner", Duration.ofMinutes(5))).isTrue();
    }

    @Test
    void acquireLock_delegatesToTryAcquire_returnsFalse() {
        when(repository.tryAcquire(eq("key"), eq("owner"), any(Instant.class), any(Instant.class))).thenReturn(false);

        assertThat(service.acquireLock("key", "owner", Duration.ofMinutes(5))).isFalse();
    }

    @Test
    void releaseLock_delegatesToRelease_returnsTrue() {
        when(repository.release(eq("key"), eq("owner"), any(Instant.class))).thenReturn(true);

        assertThat(service.releaseLock("key", "owner")).isTrue();
    }

    @Test
    void releaseLock_delegatesToRelease_returnsFalse() {
        when(repository.release(eq("key"), eq("owner"), any(Instant.class))).thenReturn(false);

        assertThat(service.releaseLock("key", "owner")).isFalse();
    }

    @Test
    void isLocked_delegatesToIsLocked_returnsTrue() {
        when(repository.isLocked(eq("key"), any(Instant.class))).thenReturn(true);

        assertThat(service.isLocked("key")).isTrue();
    }

    @Test
    void isLocked_delegatesToIsLocked_returnsFalse() {
        when(repository.isLocked(eq("key"), any(Instant.class))).thenReturn(false);

        assertThat(service.isLocked("key")).isFalse();
    }

    @Test
    void renewLock_delegatesToRenew_returnsTrue() {
        when(repository.renew(eq("key"), eq("owner"), any(Instant.class))).thenReturn(true);

        assertThat(service.renewLock("key", "owner", Duration.ofMinutes(5))).isTrue();
    }

    @Test
    void renewLock_delegatesToRenew_returnsFalse() {
        when(repository.renew(eq("key"), eq("owner"), any(Instant.class))).thenReturn(false);

        assertThat(service.renewLock("key", "owner", Duration.ofMinutes(5))).isFalse();
    }

    @Test
    void findLock_whenActive_returnsLockInfo() {
        LockInfo info = new LockInfo("key", "owner", Instant.now().plusSeconds(300));
        when(repository.findLockInfo(eq("key"), any(Instant.class))).thenReturn(Optional.of(info));

        Optional<LockInfo> result = service.findLock("key");

        assertThat(result).isPresent();
        assertThat(result.get().lockKey()).isEqualTo("key");
    }

    @Test
    void findLock_whenNotActive_returnsEmpty() {
        when(repository.findLockInfo(eq("key"), any(Instant.class))).thenReturn(Optional.empty());

        assertThat(service.findLock("key")).isEmpty();
    }
}
