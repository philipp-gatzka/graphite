/*
 * Graphite Gradle Example Application
 */
package com.example.graphql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Example Spring Boot application demonstrating Graphite usage.
 *
 * <p>This application shows how to:
 * <ul>
 *   <li>Configure Graphite with Spring Boot</li>
 *   <li>Execute type-safe GraphQL queries</li>
 *   <li>Handle responses and errors</li>
 *   <li>Use observability features (metrics, health)</li>
 * </ul>
 */
@SpringBootApplication
public class ExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExampleApplication.class, args);
    }
}
