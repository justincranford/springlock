package com.springlock;

import com.springlock.lock.repository.SqlRowLockRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@ActiveProfiles("h2")
@TestPropertySource(properties = "springlock.lock.strategy=sql-rowlock")
class SqlRowLockServiceH2IT extends AbstractLockServiceIT {

    @Autowired
    private SqlRowLockRepository sqlRowLockRepository;

    @Test
    void lockVersion_shouldIncreaseWhenLockStateChanges() {
        String versionKey = "version-lock";

        assertThat(lockService.releaseLock(versionKey, OWNER_A)).isFalse();
        assertThat(lockService.releaseLock(versionKey, OWNER_B)).isFalse();

        assertThat(lockService.acquireLock(versionKey, OWNER_A, LONG_TTL)).isTrue();
        assertThat(sqlRowLockRepository.findVersion(versionKey)).hasValue(1L);

        assertThat(lockService.releaseLock(versionKey, OWNER_A)).isTrue();
        assertThat(sqlRowLockRepository.findVersion(versionKey)).hasValue(2L);

        assertThat(lockService.acquireLock(versionKey, OWNER_B, LONG_TTL)).isTrue();
        assertThat(sqlRowLockRepository.findVersion(versionKey)).hasValue(3L);
    }
}
