package com.springlock.lock.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

class DistributedLockTest {

    @Test
    void constructor_setsAllFields() {
        Instant expiresAt = Instant.now().plusSeconds(60);
        DistributedLock lock = new DistributedLock("my-key", "owner-1", expiresAt);
        assertThat(lock.getLockKey()).isEqualTo("my-key");
        assertThat(lock.getOwner()).isEqualTo("owner-1");
        assertThat(lock.getExpiresAt()).isEqualTo(expiresAt);
        assertThat(lock.getLockVersion()).isEqualTo(DistributedLock.INITIAL_VERSION);
    }

    @Test
    void setExpiresAt_updatesExpiresAt() {
        Instant original = Instant.now().plusSeconds(60);
        DistributedLock lock = new DistributedLock("key", "owner", original);
        Instant renewed = Instant.now().plusSeconds(300);
        lock.setExpiresAt(renewed);
        assertThat(lock.getExpiresAt()).isEqualTo(renewed);
    }

    @Test
    void getId_returnsNullBeforePersistence() {
        DistributedLock lock = new DistributedLock("key", "owner", Instant.now());
        assertThat(lock.getId()).isNull();
    }
}