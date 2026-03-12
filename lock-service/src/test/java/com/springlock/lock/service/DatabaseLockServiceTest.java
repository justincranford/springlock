package com.springlock.lock.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import com.springlock.lock.domain.DistributedLock;
import com.springlock.lock.model.LockInfo;
import com.springlock.lock.repository.LockRepository;

@ExtendWith(MockitoExtension.class)
class DatabaseLockServiceTest {

    @Mock
    LockRepository lockRepository;

    @InjectMocks
    DatabaseLockService service;

    @Test
    void acquireLock_whenNotHeld_returnsTrue() {
        when(lockRepository.existsByLockKeyAndExpiresAtGreaterThanEqual(eq("key"), any())).thenReturn(false);
        when(lockRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        boolean result = service.acquireLock("key", "owner", Duration.ofMinutes(5));

        assertThat(result).isTrue();
    }

    @Test
    void acquireLock_whenAlreadyHeld_returnsFalse() {
        when(lockRepository.existsByLockKeyAndExpiresAtGreaterThanEqual(eq("key"), any())).thenReturn(true);

        boolean result = service.acquireLock("key", "owner", Duration.ofMinutes(5));

        assertThat(result).isFalse();
    }

    @Test
    void acquireLock_whenConcurrentInsert_returnsFalse() {
        when(lockRepository.existsByLockKeyAndExpiresAtGreaterThanEqual(eq("key"), any())).thenReturn(false);
        when(lockRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("dup"));

        boolean result = service.acquireLock("key", "owner", Duration.ofMinutes(5));

        assertThat(result).isFalse();
    }

    @Test
    void releaseLock_whenOwnerMatches_returnsTrue() {
        when(lockRepository.deleteByLockKeyAndOwner("key", "owner")).thenReturn(1);

        assertThat(service.releaseLock("key", "owner")).isTrue();
    }

    @Test
    void releaseLock_whenNotFound_returnsFalse() {
        when(lockRepository.deleteByLockKeyAndOwner("key", "owner")).thenReturn(0);

        assertThat(service.releaseLock("key", "owner")).isFalse();
    }

    @Test
    void isLocked_whenActive_returnsTrue() {
        when(lockRepository.existsByLockKeyAndExpiresAtGreaterThanEqual(eq("key"), any())).thenReturn(true);

        assertThat(service.isLocked("key")).isTrue();
    }

    @Test
    void isLocked_whenNotHeld_returnsFalse() {
        when(lockRepository.existsByLockKeyAndExpiresAtGreaterThanEqual(eq("key"), any())).thenReturn(false);

        assertThat(service.isLocked("key")).isFalse();
    }

    @Test
    void renewLock_whenOwnerMatches_returnsTrue_and_savesCalled() {
        Instant originalExpiry = Instant.now().plusSeconds(60);
        DistributedLock lock = new DistributedLock("key", "owner", originalExpiry);
        when(lockRepository.findByLockKeyAndOwner("key", "owner")).thenReturn(Optional.of(lock));
        when(lockRepository.save(any())).thenReturn(lock);

        boolean result = service.renewLock("key", "owner", Duration.ofMinutes(5));

        assertThat(result).isTrue();
        verify(lockRepository).save(lock);
        assertThat(lock.getExpiresAt()).isAfter(originalExpiry);
    }

    @Test
    void renewLock_whenNotFound_returnsFalse() {
        when(lockRepository.findByLockKeyAndOwner("key", "other")).thenReturn(Optional.empty());

        assertThat(service.renewLock("key", "other", Duration.ofMinutes(5))).isFalse();
    }

    @Test
    void findLock_whenActive_returnsLockInfo() {
        Instant expiresAt = Instant.now().plusSeconds(60);
        DistributedLock lock = new DistributedLock("key", "owner", expiresAt);
        when(lockRepository.findByLockKeyAndExpiresAtGreaterThanEqual(eq("key"), any()))
            .thenReturn(Optional.of(lock));

        Optional<LockInfo> info = service.findLock("key");

        assertThat(info).isPresent();
        assertThat(info.get().lockKey()).isEqualTo("key");
        assertThat(info.get().owner()).isEqualTo("owner");
        assertThat(info.get().expiresAt()).isEqualTo(expiresAt);
    }

    @Test
    void findLock_whenNotHeld_returnsEmpty() {
        when(lockRepository.findByLockKeyAndExpiresAtGreaterThanEqual(eq("key"), any()))
            .thenReturn(Optional.empty());

        assertThat(service.findLock("key")).isEmpty();
    }

    @Test
    void acquireLock_deletesExpiredLocksBeforeAcquiring() {
        when(lockRepository.existsByLockKeyAndExpiresAtGreaterThanEqual(eq("key"), any())).thenReturn(false);
        when(lockRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));

        service.acquireLock("key", "owner", Duration.ofMinutes(5));

        verify(lockRepository).deleteExpired(any(Instant.class));
    }

    @Test
    void acquireLock_savesLockWithCorrectTtl() {
        when(lockRepository.existsByLockKeyAndExpiresAtGreaterThanEqual(eq("key"), any())).thenReturn(false);
        ArgumentCaptor<DistributedLock> captor = ArgumentCaptor.forClass(DistributedLock.class);
        when(lockRepository.saveAndFlush(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        Instant before = Instant.now();
        service.acquireLock("key", "owner", Duration.ofMinutes(5));
        Instant after = Instant.now();

        DistributedLock saved = captor.getValue();
        assertThat(saved.getLockKey()).isEqualTo("key");
        assertThat(saved.getOwner()).isEqualTo("owner");
        assertThat(saved.getExpiresAt())
            .isAfter(before.plus(Duration.ofMinutes(5)).minusSeconds(1))
            .isBefore(after.plus(Duration.ofMinutes(5)).plusSeconds(1));
    }
}