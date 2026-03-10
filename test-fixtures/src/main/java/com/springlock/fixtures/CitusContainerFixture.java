package com.springlock.fixtures;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Reusable Testcontainers fixture for Citus (distributed PostgreSQL).
 * Uses citusdata/citus:14.0.0-pg17 - the latest stable Citus build (Citus 14, PostgreSQL 17).
 * No Citus image exists for PostgreSQL 18 as of this writing.
 */
public class CitusContainerFixture extends PostgreSQLContainer<CitusContainerFixture> {

    public static final String CITUS_IMAGE = "citusdata/citus:14.0.0-pg17";

    public CitusContainerFixture() {
        super(CITUS_IMAGE);
        withDatabaseName("springlock_test");
        withUsername("springlock");
        withPassword("springlock");
    }

    /**
     * Registers Spring datasource properties from this container into the
     * given {@link DynamicPropertyRegistry}.
     *
     * @param registry the Spring test property registry
     */
    public void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", this::getJdbcUrl);
        registry.add("spring.datasource.username", this::getUsername);
        registry.add("spring.datasource.password", this::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }
}
