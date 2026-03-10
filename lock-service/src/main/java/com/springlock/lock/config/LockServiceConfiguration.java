package com.springlock.lock.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration for the springlock library.
 * <p>
 * Loaded automatically via META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports.
 * Triggers component scanning of all lock service implementations in com.springlock.lock.
 * <p>
 * External consumers whose {@code @SpringBootApplication} does not cover
 * {@code com.springlock.lock} should also add:
 * <pre>
 *   {@code @EntityScan("com.springlock.lock.domain")}
 *   {@code @EnableJpaRepositories("com.springlock.lock.repository")}
 * </pre>
 */
@Configuration
@ComponentScan("com.springlock.lock")
public class LockServiceConfiguration {
}
