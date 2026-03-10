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
        assertThat(lockService.acquireLock(LOCK_KEY, OWNER_A, LONG_TTL)).isTrue();
        assertThat(sqlRowLockRepository.findVersion(LOCK_KEY)).hasValue(1L);

        assertThat(lockService.releaseLock(LOCK_KEY, OWNER_A)).isTrue();
        assertThat(sqlRowLockRepository.findVersion(LOCK_KEY)).hasValue(2L);

        assertThat(lockService.acquireLock(LOCK_KEY, OWNER_B, LONG_TTL)).isTrue();
        assertThat(sqlRowLockRepository.findVersion(LOCK_KEY)).hasValue(3L);
    }
}
