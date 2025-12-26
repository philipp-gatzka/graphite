# Security Policy

## Supported Versions

We release patches for security vulnerabilities in the following versions:

| Version | Supported          |
| ------- | ------------------ |
| 0.1.x   | :white_check_mark: |

## Reporting a Vulnerability

We take security vulnerabilities seriously. If you discover a security issue, please report it responsibly.

### How to Report

1. **Do NOT** open a public GitHub issue for security vulnerabilities
2. Email security concerns to the maintainers (see [CODEOWNERS](.github/CODEOWNERS) or repository owner)
3. Include the following in your report:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested fix (if any)

### What to Expect

- **Acknowledgment**: We will acknowledge receipt within 48 hours
- **Assessment**: We will assess the vulnerability and determine its severity within 7 days
- **Resolution**: We aim to release a fix within 30 days for critical vulnerabilities
- **Disclosure**: We will coordinate with you on public disclosure timing

### Scope

The following are in scope for security reports:

- Graphite library code (`graphite-core`, `graphite-codegen`, etc.)
- Build plugins (`graphite-gradle-plugin`, `graphite-maven-plugin`)
- Spring Boot auto-configuration (`graphite-spring-boot-starter`)

The following are out of scope:

- Vulnerabilities in dependencies (report to the respective projects)
- Issues in example code or documentation

## Security Best Practices

When using Graphite in your application, follow these security recommendations:

### Configuration

```yaml
# application.yml
graphite:
  client:
    # Always use HTTPS in production
    endpoint: https://api.example.com/graphql

    # Set reasonable timeouts to prevent resource exhaustion
    timeout:
      connect: 5s
      read: 30s
      request: 60s

    # Configure retry limits to prevent infinite loops
    retry:
      max-attempts: 3
```

### Sensitive Data

- **Never log request/response bodies** containing sensitive data in production
- Use environment variables or secrets management for API keys and tokens
- Configure headers securely:

```java
GraphiteClient client = GraphiteClient.builder()
    .endpoint("https://api.example.com/graphql")
    .header("Authorization", "Bearer " + System.getenv("API_TOKEN"))
    .build();
```

### Input Validation

- Always validate user input before including in GraphQL variables
- Use parameterized queries (which Graphite enforces by design)
- Validate responses before processing

### Network Security

- Use TLS 1.2 or higher for all connections
- Consider certificate pinning for sensitive applications
- Use network policies to restrict outbound connections

## Known Security Considerations

### Introspection Queries

The code generation requires a GraphQL introspection query result (`schema.json`). This file may contain sensitive information about your API schema. Store it securely and do not commit production schemas to public repositories.

### Generated Code

Generated code is safe by design and does not include:
- Reflection-based serialization
- Dynamic class loading
- Execution of arbitrary code

## Dependency Security

We use the following tools to maintain dependency security:

- **OWASP Dependency-Check**: Scans for known vulnerabilities in dependencies
- **Dependabot**: Automated dependency updates
- **SonarCloud**: Static analysis for security issues

To run a security scan locally:

```bash
./gradlew dependencyCheckAnalyze
```

## Security Updates

Security updates are released as patch versions (e.g., 0.1.1, 0.1.2). Subscribe to GitHub releases to be notified of security updates.
