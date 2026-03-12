package com.springlock;

import org.junit.jupiter.api.Tag;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@Tag("integration")
@ActiveProfiles("h2")
@TestPropertySource(properties = {
    "springlock.lock.strategy=jdbc",
    "springlock.lock.jdbc.mode=pessimistic"
})
class PerfBenchmarkH2JdbcPessimisticIT extends AbstractPerformanceBenchmarkIT {
    @Override
    protected String backendLabel() { return "h2-jdbc-pessimistic"; }
}