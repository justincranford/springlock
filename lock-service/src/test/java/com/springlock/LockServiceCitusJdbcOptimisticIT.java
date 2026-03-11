package com.springlock;

import com.springlock.fixtures.CitusContainerFixture;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Functional + concurrency + perf tests: Citus (pg17) + JDBC optimistic strategy. */
@Tag("integration")
@Testcontainers
@ActiveProfiles("postgres")
@TestPropertySource(properties = {
    "springlock.lock.strategy=jdbc",
    "springlock.lock.jdbc.mode=optimistic"
})
class LockServiceCitusJdbcOptimisticIT extends AbstractLockServiceIT {

    @Container
    static final CitusContainerFixture citus = new CitusContainerFixture();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry r) { citus.registerProperties(r); }

    @Override
    protected String backendLabel() { return "citus-jdbc-optimistic"; }
}
