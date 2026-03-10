package com.springlock;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Test-only bootstrap class for {@code @SpringBootTest} contexts.
 * <p>
 * {@code lock-service} is a library module. This class exists solely to provide
 * a {@code @SpringBootApplication} root for integration tests, enabling Spring Boot's
 * auto-configuration, entity scanning, and JPA repository scanning across
 * {@code com.springlock.lock} and its sub-packages.
 * <p>
 * Not part of the production artifact.
 */
@SpringBootApplication(scanBasePackages = "com.springlock.lock")
class TestLockServiceApplication {
}