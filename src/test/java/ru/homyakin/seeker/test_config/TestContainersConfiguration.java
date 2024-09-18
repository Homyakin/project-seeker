package ru.homyakin.seeker.test_config;

import jakarta.annotation.PreDestroy;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration
public class TestContainersConfiguration {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName
        .parse("postgres:14.4-alpine");
    private static PostgreSQLContainer<?> postgresContainer;

    @Bean
    public PostgreSQLContainer<?> postgresContainer() {
        if (postgresContainer == null) {
            postgresContainer = new PostgreSQLContainer<>(POSTGRES_IMAGE);
            postgresContainer.start();

            System.setProperty("DB_HOST", postgresContainer.getHost());
            System.setProperty("DB_PORT", postgresContainer.getFirstMappedPort().toString());
            System.setProperty("DB_NAME", postgresContainer.getDatabaseName());
            System.setProperty("DB_USERNAME", postgresContainer.getUsername());
            System.setProperty("DB_PASSWORD", postgresContainer.getPassword());
        }
        return postgresContainer;
    }

    @PreDestroy
    public void stopContainer() {
        if (postgresContainer != null) {
            postgresContainer.stop();
        }
    }
}
