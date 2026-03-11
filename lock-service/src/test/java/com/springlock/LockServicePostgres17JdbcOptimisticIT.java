package com.springlock;

import com.springlock.fixtures.PostgreSQLContainerFixture;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Functional + concurrency + perf tests: postgres:17.9 + JDBC optimistic strategy. */
@Tag("integration")
@Testcontainers
@ActiveProfiles("postgres")
@TestPropertySource(properties = {
    "springlock.lock.strategy=jdbc",
    "springlock.lock.jdbc.mode=optimistic"
})
class LockServicePostgres17JdbcOptimisticIT extends AbstractLockServiceIT {

    static final String IMAGE = "postgres:17.9";

    @Container
    static final PostgreSQLContainerFixture postgres = new PostgreSQLContainerFixture(IMAGE);

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry r) { postgres.registerProperties(r); }

    @Override
    protected String backendLabel() { return "postgres17-jdbc-optimistic"; }
}
