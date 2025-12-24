/*
 * Graphite Gradle Example Project
 *
 * This example demonstrates how to use Graphite with Gradle
 * to build a type-safe GraphQL client application.
 */

plugins {
    java
    id("org.springframework.boot") version "3.4.1"
    id("io.spring.dependency-management") version "1.1.7"
    // id("io.github.graphite") version "0.1.0"  // Uncomment when published
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    // Uncomment for snapshot versions:
    // maven { url = uri("https://maven.pkg.github.com/philipp-gatzka/graphite") }
}

dependencies {
    // Graphite Spring Boot Starter
    // implementation("io.github.graphite:graphite-spring-boot-starter:0.1.0")

    // For now, use project dependency (remove when published)
    implementation(project(":graphite-spring-boot-starter"))

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    // testImplementation("io.github.graphite:graphite-test-utils:0.1.0")
    testImplementation(project(":graphite-test-utils"))
}

// Uncomment when Graphite plugin is published:
// graphite {
//     schemaFile = file("src/main/resources/graphql/schema.json")
//     packageName = "com.example.graphql.generated"
// }

tasks.withType<Test> {
    useJUnitPlatform()
}
