package com.springlock;

import com.springlock.fixtures.PostgreSQLContainerFixture;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Functional + concurrency + perf tests: postgres:16.13 + JPA strategy. */
@Tag("integration")
@Testcontainers
@ActiveProfiles("postgres")
class LockServicePostgres16JpaIT extends AbstractLockServiceIT {

    static final String IMAGE = "postgres:16.13";

    @Container
    static final PostgreSQLContainerFixture postgres = new PostgreSQLContainerFixture(IMAGE);

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry r) { postgres.registerProperties(r); }

    @Override
    protected String backendLabel() { return "postgres16-jpa"; }
}
