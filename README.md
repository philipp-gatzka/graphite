# Graphite

[![Build Status](https://github.com/philipp-gatzka/graphite/actions/workflows/ci.yml/badge.svg)](https://github.com/philipp-gatzka/graphite/actions)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=philipp-gatzka_graphite&metric=coverage)](https://sonarcloud.io/dashboard?id=philipp-gatzka_graphite)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

Type-safe GraphQL client library for Spring Boot with code generation.

## Features

- **Type-safe query builder** - No `.graphql` files needed, build queries programmatically
- **Code generation** - Generate Java types from `schema.json`
- **Spring Boot integration** - Auto-configuration with metrics, tracing, and health checks
- **Gradle & Maven plugins** - Seamless build tool integration
- **Zero runtime reflection** - Fast and GraalVM-friendly

## Quick Start

### 1. Add Dependencies

**Gradle (Kotlin DSL)**

```kotlin
plugins {
    id("io.github.graphite") version "0.1.0"
}

dependencies {
    implementation("io.github.graphite:graphite-spring-boot-starter:0.1.0")
    testImplementation("io.github.graphite:graphite-test-utils:0.1.0")
}

graphite {
    schemaFile = file("src/main/resources/graphql/schema.json")
    packageName = "com.example.graphql"
}
```

**Maven**

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

<dependencies>
    <dependency>
        <groupId>io.github.graphite</groupId>
        <artifactId>graphite-spring-boot-starter</artifactId>
        <version>0.1.0</version>
    </dependency>
    <dependency>
        <groupId>io.github.graphite</groupId>
        <artifactId>graphite-test-utils</artifactId>
        <version>0.1.0</version>
        <scope>test</scope>
    </dependency>
</dependencies>
```

### 2. Configure the Client

Add to `application.yml`:

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
    Authorization: Bearer ${GRAPHQL_TOKEN}
```

### 3. Add Your Schema

Place your GraphQL introspection schema at `src/main/resources/graphql/schema.json`. Generate it from your GraphQL server:

```bash
# Using graphql-cli
graphql get-schema --endpoint https://api.example.com/graphql --json > src/main/resources/graphql/schema.json
```

### 4. Generate Code

```bash
# Gradle
./gradlew graphiteGenerate

# Maven
mvn graphite:generate
```

This generates type-safe Java classes in `build/generated/sources/graphite/` (Gradle) or `target/generated-sources/graphite/` (Maven).

### 5. Use the Client

```java
import org.springframework.stereotype.Service;
import io.github.graphite.GraphiteClient;
import com.example.graphql.query.GetUserQuery;
import com.example.graphql.type.UserDTO;

@Service
public class UserService {

    private final GraphiteClient client;

    public UserService(GraphiteClient client) {
        this.client = client;
    }

    public UserDTO getUser(String id) {
        var response = client.execute(
            GetUserQuery.builder()
                .id(id)
                .selecting(s -> s.id().name().email().createdAt())
                .build()
        );

        return response.getDataOrThrow();
    }
}
```

### 6. Write Tests

```java
import io.github.graphite.test.GraphiteMockServer;
import io.github.graphite.test.GraphiteAssertions;
import org.junit.jupiter.api.Test;
import java.util.Map;

class UserServiceTest {

    @Test
    void shouldFetchUser() {
        try (GraphiteMockServer server = GraphiteMockServer.create()) {
            // Stub the GraphQL response
            server.stubQuery("GetUser", Map.of(
                "id", "123",
                "name", "John Doe",
                "email", "john@example.com"
            ));

            // Configure client to use mock server
            GraphiteClient client = GraphiteClient.builder()
                .endpoint(URI.create(server.getUrl()))
                .build();

            // Execute and verify
            UserDTO user = new UserService(client).getUser("123");

            assertThat(user.name()).isEqualTo("John Doe");
            server.verify("GetUser", 1);
        }
    }
}
```

## Advanced Usage

### Custom Scalars

Configure custom scalar type mappings in your build tool:

**Gradle**
```kotlin
graphite {
    schemaFile = file("src/main/resources/graphql/schema.json")
    packageName = "com.example.graphql"
    scalars = mapOf(
        "DateTime" to "java.time.OffsetDateTime",
        "Date" to "java.time.LocalDate",
        "Time" to "java.time.LocalTime",
        "JSON" to "com.fasterxml.jackson.databind.JsonNode",
        "Long" to "java.lang.Long",
        "UUID" to "java.util.UUID"
    )
}
```

**Maven**
```xml
<configuration>
    <schemaFile>${project.basedir}/src/main/resources/graphql/schema.json</schemaFile>
    <packageName>com.example.graphql</packageName>
    <scalars>
        <DateTime>java.time.OffsetDateTime</DateTime>
        <Date>java.time.LocalDate</Date>
        <JSON>com.fasterxml.jackson.databind.JsonNode</JSON>
    </scalars>
</configuration>
```

### Error Handling

Graphite provides a rich exception hierarchy for precise error handling:

```java
import io.github.graphite.exception.*;

try {
    var response = client.execute(query);
    return response.getDataOrThrow();

} catch (GraphiteConnectionException e) {
    // Network connectivity issues
    log.error("Connection failed: {}", e.getMessage());
    throw new ServiceUnavailableException("GraphQL server unreachable");

} catch (GraphiteTimeoutException e) {
    // Request timed out
    if (e.isSafeToRetry()) {
        // Connect timeout - no request was sent, safe to retry
        return retryRequest(query);
    }
    throw new TimeoutException("Request timed out: " + e.getTimeoutType());

} catch (GraphiteRateLimitException e) {
    // Rate limit exceeded
    log.warn("Rate limited: {}", e.getMessage());
    throw new TooManyRequestsException();

} catch (GraphiteServerException e) {
    // Server returned 5xx error
    log.error("Server error (HTTP {}): {}", e.getStatusCode(), e.getMessage());
    throw new InternalServerException();

} catch (GraphiteGraphQLException e) {
    // GraphQL-level errors in response
    log.warn("GraphQL errors: {}", e.getErrors());
    throw new BadRequestException(e.getMessage());

} catch (GraphiteException e) {
    // Catch-all for any Graphite error
    log.error("Unexpected error: {}", e.getMessage(), e);
    throw new InternalServerException();
}
```

For partial responses (data with errors), check the response directly:

```java
var response = client.execute(query);

if (response.hasErrors()) {
    response.getErrors().forEach(error ->
        log.warn("GraphQL warning: {} at {}", error.message(), error.path())
    );
}

if (response.hasData()) {
    return response.getData();
}

throw new NoDataException("No data in response");
```

### Retry Configuration

Configure retry behavior programmatically:

```java
import io.github.graphite.retry.*;

// Exponential backoff with custom settings
var backoff = ExponentialBackoff.builder()
    .initialDelay(Duration.ofMillis(100))
    .maxDelay(Duration.ofSeconds(10))
    .multiplier(2.0)
    .build();

// Add jitter to prevent thundering herd
var jitteredBackoff = backoff.withJitter(0.25);

// Create retry policy
var retryPolicy = RetryPolicy.builder()
    .maxAttempts(5)
    .backoffStrategy(jitteredBackoff)
    .retryOn(GraphiteConnectionException.class)
    .retryOn(GraphiteTimeoutException.class)
    .retryOn(GraphiteServerException.class)
    .build();

// Use with client
var client = GraphiteClient.builder()
    .endpoint("https://api.example.com/graphql")
    .retryPolicy(retryPolicy)
    .build();
```

Or via Spring Boot configuration:

```yaml
graphite:
  url: https://api.example.com/graphql
  retry:
    enabled: true
    max-attempts: 5
    initial-delay: 100ms
    multiplier: 2.0
    max-delay: 10s
```

### Observability

#### Metrics

Graphite automatically exposes Micrometer metrics when the starter is used:

| Metric | Type | Tags | Description |
|--------|------|------|-------------|
| `graphite.client.requests` | Counter | operation, status | Total requests |
| `graphite.client.request.duration` | Timer | operation | Request duration |
| `graphite.client.errors` | Counter | operation, error_type | Error count |
| `graphite.client.retry.attempts` | Counter | exception_type | Retry attempts |
| `graphite.client.retry.exhausted` | Counter | exception_type | Exhausted retries |
| `graphite.client.retry.success` | Counter | - | Successful retries |

Access metrics programmatically:

```java
import io.github.graphite.spring.observability.GraphiteMetrics;

@Service
public class GraphQLMonitoringService {

    private final GraphiteMetrics metrics;

    public GraphQLMonitoringService(GraphiteMetrics metrics) {
        this.metrics = metrics;
    }

    public void executeWithCustomMetrics(GraphQLOperation<?> operation) {
        var sample = metrics.startTimer();
        try {
            client.execute(operation);
            metrics.recordSuccess(operation.operationName(), sample);
        } catch (Exception e) {
            metrics.recordError(operation.operationName(), e, sample);
            throw e;
        }
    }
}
```

#### Tracing

Enable distributed tracing with Micrometer Tracing:

```yaml
# application.yml
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0  # Sample all requests (adjust for production)
```

Graphite propagates trace context automatically in requests.

#### Health Checks

Enable the health indicator:

```yaml
management:
  health:
    graphite:
      enabled: true
  endpoint:
    health:
      show-details: always
```

The health check performs a lightweight introspection query to verify endpoint availability.

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

## Spring Boot Configuration Reference

```yaml
graphite:
  # Required: GraphQL endpoint URL
  url: https://api.example.com/graphql

  # Optional: Static headers
  headers:
    Authorization: Bearer ${TOKEN}
    X-Custom-Header: value

  # Optional: Timeout configuration
  timeout:
    connect: 10s      # Connection timeout (default: 10s)
    read: 30s         # Read timeout (default: 30s)
    request: 60s      # Overall request timeout (default: 60s)

  # Optional: Retry configuration
  retry:
    max-attempts: 3           # Max retry attempts (default: 3)
    initial-delay: 100ms      # Initial delay between retries (default: 100ms)
    multiplier: 2.0           # Backoff multiplier (default: 2.0)
    max-delay: 5s             # Maximum delay between retries (default: 5s)

  # Optional: Rate limiting
  rate-limit:
    requests-per-second: 100  # Max requests per second (default: unlimited)
    burst-capacity: 150       # Burst capacity (default: same as rps)

  # Optional: Connection pool
  connection-pool:
    max-connections: 50       # Maximum connections (default: 50)
    idle-timeout: 30s         # Idle connection timeout (default: 30s)
```

## Observability

Graphite integrates with Spring Boot Actuator for observability:

- **Metrics** - Request duration, success/error counts via Micrometer
- **Tracing** - Distributed tracing with trace context propagation
- **Health** - Health indicator for GraphQL endpoint availability

Enable in `application.yml`:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics
  health:
    graphite:
      enabled: true
```

## Testing Utilities

The `graphite-test-utils` module provides:

- **GraphiteMockServer** - WireMock-based mock GraphQL server
- **GraphiteRequestMatcher** - Fluent request matching
- **GraphiteResponseBuilder** - Fluent response building
- **GraphiteAssertions** - Fluent assertions for responses

Example with advanced matching:

```java
try (GraphiteMockServer server = GraphiteMockServer.create()) {
    // Match requests with specific variables and headers
    GraphiteRequestMatcher matcher = GraphiteRequestMatcher.forQuery("GetUser")
        .withVariable("id", "123")
        .withBearerToken("secret-token");

    server.stub(matcher, Map.of("id", "123", "name", "John"));

    // ... execute request ...

    server.verify("GetUser", 1);
}
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
