# Graphite

[![Build Status](https://github.com/philipp-gatzka/graphite/actions/workflows/ci.yml/badge.svg)](https://github.com/philipp-gatzka/graphite/actions)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=graphite&metric=coverage)](https://sonarcloud.io/dashboard?id=graphite)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

Type-safe GraphQL client library for Spring Boot with code generation.

## Features

- **Type-safe query builder** - No `.graphql` files needed, build queries programmatically
- **Code generation** - Generate Java types from `schema.json`
- **Spring Boot integration** - Auto-configuration with metrics, tracing, and health checks
- **Gradle & Maven plugins** - Seamless build tool integration
- **Zero runtime reflection** - Fast and GraalVM-friendly

## Quick Example

```java
// Type-safe query building
var response = client.query()
    .user(u -> u
        .id("123")
        .selecting(s -> s.id().name().email())
    )
    .execute();

// Access data safely
UserDTO user = response.getDataOrThrow();
System.out.println(user.name());
```

## Modules

| Module | Description |
|--------|-------------|
| `graphite-core` | Runtime client library |
| `graphite-codegen` | Code generation engine |
| `graphite-gradle-plugin` | Gradle plugin for code generation |
| `graphite-maven-plugin` | Maven plugin for code generation |
| `graphite-spring-boot-starter` | Spring Boot auto-configuration |
| `graphite-test-utils` | Testing utilities |

## Requirements

- Java 21+
- Gradle 8.5+ or Maven 3.8+
- Spring Boot 3.x (for starter module)

## Installation

### Gradle (Kotlin DSL)

```kotlin
plugins {
    id("io.github.graphite") version "0.1.0"
}

dependencies {
    implementation("io.github.graphite:graphite-spring-boot-starter:0.1.0")
}

graphite {
    schemaFile = file("src/main/resources/graphql/schema.json")
    packageName = "com.example.graphql"
}
```

### Maven

```xml
<plugin>
    <groupId>io.github.graphite</groupId>
    <artifactId>graphite-maven-plugin</artifactId>
    <version>0.1.0</version>
    <executions>
        <execution>
            <goals>
                <goal>generate</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <schemaFile>${project.basedir}/src/main/resources/graphql/schema.json</schemaFile>
        <packageName>com.example.graphql</packageName>
    </configuration>
</plugin>

<dependency>
    <groupId>io.github.graphite</groupId>
    <artifactId>graphite-spring-boot-starter</artifactId>
    <version>0.1.0</version>
</dependency>
```

## Configuration

```yaml
graphite:
  url: https://api.example.com/graphql
  timeout:
    connect: 10s
    read: 30s
    request: 60s
  retry:
    max-attempts: 3
    initial-delay: 100ms
  headers:
    Authorization: Bearer ${TOKEN}
```

## Documentation

- [Getting Started](wiki/Getting-Started.md)
- [Development Workflow](wiki/Development-Workflow.md)
- [Contributing](wiki/Contributing.md)
- [API Documentation](https://philipp-gatzka.github.io/graphite/javadoc/)

## Building from Source

```bash
git clone --recurse-submodules https://github.com/philipp-gatzka/graphite.git
cd graphite
./scripts/setup.sh
./gradlew build
```

## License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.
