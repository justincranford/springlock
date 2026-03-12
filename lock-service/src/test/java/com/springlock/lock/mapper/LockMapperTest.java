package com.springlock.lock.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;

import com.springlock.lock.domain.DistributedLock;
import com.springlock.lock.model.LockInfo;

class LockMapperTest {

    @Test
    void toLockInfo_mapsAllFields() {
        Instant expiresAt = Instant.now().plusSeconds(60);
        DistributedLock lock = new DistributedLock("my-key", "owner-1", expiresAt);

        LockInfo info = LockMapper.toLockInfo(lock);

        assertThat(info.lockKey()).isEqualTo("my-key");
        assertThat(info.owner()).isEqualTo("owner-1");
        assertThat(info.expiresAt()).isEqualTo(expiresAt);
    }
}