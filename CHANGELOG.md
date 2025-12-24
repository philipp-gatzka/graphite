# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- Initial release of Graphite - Type-safe GraphQL client library for Spring Boot

#### Core Module (`graphite-core`)
- `GraphiteClient` interface for executing GraphQL operations
- `GraphiteClientBuilder` for fluent client configuration
- `HttpTransport` abstraction with `DefaultHttpTransport` implementation
- `GraphiteResponse` record for type-safe response handling
- Retry support with `RetryPolicy`, `ExponentialBackoff`, and `FixedBackoff`
- Rate limiting with token bucket algorithm
- Exception hierarchy: `GraphiteException`, `GraphiteClientException`, `GraphiteServerException`
- Scalar coercing for DateTime, Date, Time, UUID, Long, BigDecimal, BigInteger, JSON, Void
- Request and response interceptor support

#### Code Generation (`graphite-codegen`)
- `GraphiteCodegen` orchestrator for code generation
- Schema parsing from GraphQL introspection JSON (`schema.json`)
- Type generators for DTOs (records), input types (classes with builders), enums
- Query and mutation generators with type-safe builder pattern
- Projection generators for field selection
- Interface and union generators using sealed interfaces
- Incremental build support with hash tracking
- Custom scalar mapping support

#### Gradle Plugin (`graphite-gradle-plugin`)
- `GraphitePlugin` for Gradle 8.5+ integration
- `GraphiteExtension` DSL for configuration
- `GraphiteGenerateTask` with incremental build support
- Automatic source set integration for generated code
- IntelliJ IDEA compatibility

#### Maven Plugin (`graphite-maven-plugin`)
- `GraphiteGenerateMojo` for Maven 3.8+ integration
- Build helper integration for source root management
- Lifecycle binding to `generate-sources` phase

#### Spring Boot Starter (`graphite-spring-boot-starter`)
- `GraphiteAutoConfiguration` for automatic client setup
- `GraphiteProperties` for externalized configuration
- Metrics integration via Micrometer (`graphite.client.requests`, `graphite.client.request.duration`)
- Tracing integration with span attributes and header propagation
- Health indicator for GraphQL endpoint availability
- Configuration metadata for IDE auto-completion

#### Test Utilities (`graphite-test-utils`)
- `GraphiteMockServer` wrapping WireMock for GraphQL mocking
- `GraphiteRequestMatcher` for fluent request matching
- `GraphiteResponseBuilder` for fluent response construction
- `GraphiteErrorBuilder` for fluent error construction
- `GraphiteAssertions` for fluent response assertions

### Changed
- N/A

### Deprecated
- N/A

### Removed
- N/A

### Fixed
- N/A

### Security
- N/A

[Unreleased]: https://github.com/philipp-gatzka/graphite/compare/v0.1.0...HEAD
