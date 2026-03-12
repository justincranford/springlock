package com.springlock;

import org.junit.jupiter.api.Tag;
import org.springframework.test.context.ActiveProfiles;

@Tag("integration")
@ActiveProfiles("h2")
class PerfBenchmarkH2JpaIT extends AbstractPerformanceBenchmarkIT {
    @Override
    protected String backendLabel() { return "h2-jpa"; }
}