package com.springlock;

import com.springlock.fixtures.PostgreSQLContainerFixture;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Tag("integration")
@Testcontainers
@ActiveProfiles("postgres")
class LockServicePostgres15IT extends AbstractLockServiceIT {

    @Container
    static final PostgreSQLContainerFixture postgres = new PostgreSQLContainerFixture("postgres:15.17");

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        postgres.registerProperties(registry);
    }
}
