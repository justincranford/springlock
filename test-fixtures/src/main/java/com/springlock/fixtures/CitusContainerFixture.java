package com.springlock.fixtures;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Reusable Testcontainers fixture for Citus (distributed PostgreSQL).
 * Uses citusdata/citus:14.0.0-pg17 - the latest stable Citus build (Citus 14, PostgreSQL 17).
 * No Citus image exists for PostgreSQL 18 as of this writing.
 */
public class CitusContainerFixture extends PostgreSQLContainer<CitusContainerFixture> {

    public static final String CITUS_IMAGE = "citusdata/citus:14.0.0-pg17";

    private static final DockerImageName CITUS_DOCKER_IMAGE =
        DockerImageName.parse(CITUS_IMAGE).asCompatibleSubstituteFor("postgres");

    public CitusContainerFixture() {
        super(CITUS_DOCKER_IMAGE);
        withDatabaseName("springlock_test");
        withUsername("springlock");
        withPassword("springlock");
    }

    public void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", this::getJdbcUrl);
        registry.add("spring.datasource.username", this::getUsername);
        registry.add("spring.datasource.password", this::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }
}