package com.springlock;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Test-only bootstrap for {@code @SpringBootTest} contexts. Not part of the production artifact. */
@SpringBootApplication(scanBasePackages = "com.springlock.lock")
class TestLockServiceApplication {
}
