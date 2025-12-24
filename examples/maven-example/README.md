# Graphite Maven Example

This example demonstrates how to use Graphite with Maven and Spring Boot.

## Prerequisites

- Java 21+
- Maven 3.8+

## Quick Start

1. **Add your GraphQL schema**

   Place your `schema.json` file in `src/main/resources/graphql/`.

2. **Generate code**

   ```bash
   mvn graphite:generate
   ```

3. **Run the application**

   ```bash
   mvn spring-boot:run
   ```

## Configuration

Edit `src/main/resources/application.yml` to configure:

- GraphQL endpoint URL
- Timeouts and retry settings
- Authentication headers

## Project Structure

```
maven-example/
├── pom.xml                    # Maven build configuration
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
mvn test
```

## License

This example is part of the Graphite project, licensed under Apache 2.0.
