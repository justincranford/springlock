package com.springlock.fixtures;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Reusable Testcontainers fixture for PostgreSQL.
 * Accepts any postgres:XX.YY Docker image tag for cross-version testing.
 */
public class PostgreSQLContainerFixture extends PostgreSQLContainer<PostgreSQLContainerFixture> {

    public PostgreSQLContainerFixture(String dockerImageName) {
        super(dockerImageName);
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
