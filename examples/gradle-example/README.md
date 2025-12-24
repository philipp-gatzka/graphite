# Graphite Gradle Example

This example demonstrates how to use Graphite with Gradle and Spring Boot.

## Prerequisites

- Java 21+
- Gradle 8.5+

## Quick Start

1. **Add your GraphQL schema**

   Place your `schema.json` file in `src/main/resources/graphql/`.

2. **Generate code**

   ```bash
   ./gradlew graphiteGenerate
   ```

3. **Run the application**

   ```bash
   ./gradlew bootRun
   ```

## Configuration

Edit `src/main/resources/application.yml` to configure:

- GraphQL endpoint URL
- Timeouts and retry settings
- Authentication headers

## Project Structure

```
gradle-example/
├── build.gradle.kts           # Gradle build configuration
├── src/
│   ├── main/
│   │   ├── java/              # Application code
│   │   └── resources/
│   │       ├── application.yml     # Spring Boot configuration
│   │       └── graphql/
│   │           └── schema.json     # GraphQL schema (add your own)
│   └── test/
│       └── java/              # Test code
└── README.md
```

## Dependencies

- `io.github.graphite:graphite-spring-boot-starter` - Spring Boot integration
- `io.github.graphite:graphite-test-utils` - Testing utilities

## Testing

Run tests:

```bash
./gradlew test
```

## License

This example is part of the Graphite project, licensed under Apache 2.0.
