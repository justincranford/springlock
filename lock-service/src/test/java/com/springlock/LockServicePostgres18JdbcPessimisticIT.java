package com.springlock;

import com.springlock.fixtures.PostgreSQLContainerFixture;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Functional + concurrency + perf tests: postgres:18.3 + JDBC pessimistic strategy. */
@Tag("integration")
@Testcontainers
@ActiveProfiles("postgres")
@TestPropertySource(properties = {
    "springlock.lock.strategy=jdbc",
    "springlock.lock.jdbc.mode=pessimistic"
})
class LockServicePostgres18JdbcPessimisticIT extends AbstractLockServiceIT {

    static final String IMAGE = "postgres:18.3";

    @Container
    static final PostgreSQLContainerFixture POSTGRES = new PostgreSQLContainerFixture(IMAGE);

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry r) { POSTGRES.registerProperties(r); }

    @Override
    protected String backendLabel() { return "postgres18-jdbc-pessimistic"; }
}
