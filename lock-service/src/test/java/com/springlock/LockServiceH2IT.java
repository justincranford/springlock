package com.springlock;

import org.junit.jupiter.api.Tag;
import org.springframework.test.context.ActiveProfiles;

@Tag("integration")
@ActiveProfiles("h2")
class LockServiceH2IT extends AbstractLockServiceIT {
    // H2 in-memory database is auto-configured via application-h2.properties
    // No container needed
}
