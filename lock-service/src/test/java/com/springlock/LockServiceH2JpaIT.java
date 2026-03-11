package com.springlock;

import org.junit.jupiter.api.Tag;
import org.springframework.test.context.ActiveProfiles;

/** Functional + concurrency + perf tests: H2 in-memory + JPA strategy. No Docker needed. */
@Tag("integration")
@ActiveProfiles("h2")
class LockServiceH2JpaIT extends AbstractLockServiceIT {

    @Override
    protected String backendLabel() { return "h2-jpa"; }
}
