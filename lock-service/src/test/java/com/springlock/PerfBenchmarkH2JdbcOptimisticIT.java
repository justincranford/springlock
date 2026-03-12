package com.springlock;

import org.junit.jupiter.api.Tag;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@Tag("integration")
@ActiveProfiles("h2")
@TestPropertySource(properties = {
    "springlock.lock.strategy=jdbc",
    "springlock.lock.jdbc.mode=optimistic"
})
class PerfBenchmarkH2JdbcOptimisticIT extends AbstractPerformanceBenchmarkIT {
    @Override
    protected String backendLabel() { return "h2-jdbc-optimistic"; }
}