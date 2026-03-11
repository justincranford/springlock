package com.springlock;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import com.springlock.lock.repository.SqlRowLockRepository;

/** Functional + concurrency + perf tests: H2 + JDBC optimistic strategy. */
@Tag("integration")
@ActiveProfiles("h2")
@TestPropertySource(properties = {
    "springlock.lock.strategy=jdbc",
    "springlock.lock.jdbc.mode=optimistic"
})
class LockServiceH2JdbcOptimisticIT extends AbstractLockServiceIT {

    @Autowired
    private SqlRowLockRepository repo;

    @Override
    protected String backendLabel() { return "h2-jdbc-optimistic"; }

    @Test
    void lockVersion_incrementsOnOwnershipTransfer() {
        String key = "version-key-optimistic";
        lockService.releaseLock(key, OWNER_A);
        lockService.releaseLock(key, OWNER_B);
        assertThat(lockService.acquireLock(key, OWNER_A, LONG_TTL)).isTrue();
        assertThat(repo.findVersion(key)).hasValue(1L);
        assertThat(lockService.releaseLock(key, OWNER_A)).isTrue();
        assertThat(repo.findVersion(key)).hasValue(2L);
        assertThat(lockService.acquireLock(key, OWNER_B, LONG_TTL)).isTrue();
        assertThat(repo.findVersion(key)).hasValue(3L);
        lockService.releaseLock(key, OWNER_B);
    }
}
