package com.springlock;

import com.springlock.fixtures.CitusContainerFixture;
import org.junit.jupiter.api.Tag;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Functional + concurrency + perf tests: Citus (pg17) + JPA strategy. */
@Tag("integration")
@Testcontainers
@ActiveProfiles("postgres")
class LockServiceCitusJpaIT extends AbstractLockServiceIT {

    @Container
    static final CitusContainerFixture citus = new CitusContainerFixture();

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry r) { citus.registerProperties(r); }

    @Override
    protected String backendLabel() { return "citus-jpa"; }
}
