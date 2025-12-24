/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.graphite.spring.actuator;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.actuate.health.AbstractHealthIndicator;
import org.springframework.boot.actuate.health.Health;

/**
 * Health indicator for the Graphite GraphQL client.
 *
 * <p>This health indicator sends a lightweight introspection query ({@code { __typename }}) to the
 * GraphQL endpoint to verify connectivity. The health check is considered UP if the endpoint
 * responds successfully, and DOWN otherwise.
 *
 * <p>The health response includes:
 *
 * <ul>
 *   <li>{@code url} - The GraphQL endpoint URL
 *   <li>{@code responseTime} - The time taken for the health check request
 * </ul>
 *
 * <p>Example health response:
 *
 * <pre>{@code
 * {
 *   "status": "UP",
 *   "details": {
 *     "url": "https://api.example.com/graphql",
 *     "responseTime": "45ms"
 *   }
 * }
 * }</pre>
 *
 * @see GraphiteHealthIndicatorAutoConfiguration
 */
public class GraphiteHealthIndicator extends AbstractHealthIndicator {

  /** The introspection query used for health checks. */
  private static final String INTROSPECTION_QUERY = "{\"query\":\"{ __typename }\"}";

  /** Default timeout for health check requests. */
  private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(5);

  private final String url;
  private final HttpClient httpClient;
  private final Duration timeout;

  /**
   * Creates a new health indicator for the given GraphQL endpoint URL.
   *
   * @param url the GraphQL endpoint URL
   */
  public GraphiteHealthIndicator(@NotNull String url) {
    this(url, DEFAULT_TIMEOUT);
  }

  /**
   * Creates a new health indicator with custom timeout.
   *
   * @param url the GraphQL endpoint URL
   * @param timeout the timeout for health check requests
   */
  public GraphiteHealthIndicator(@NotNull String url, @NotNull Duration timeout) {
    super("Graphite GraphQL health check failed");
    this.url = url;
    this.timeout = timeout;
    this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
  }

  /**
   * Creates a new health indicator with custom HTTP client.
   *
   * @param url the GraphQL endpoint URL
   * @param httpClient the HTTP client to use
   * @param timeout the timeout for health check requests
   */
  public GraphiteHealthIndicator(
      @NotNull String url, @NotNull HttpClient httpClient, @NotNull Duration timeout) {
    super("Graphite GraphQL health check failed");
    this.url = url;
    this.httpClient = httpClient;
    this.timeout = timeout;
  }

  @Override
  protected void doHealthCheck(@NotNull Health.Builder builder) throws Exception {
    long startTime = System.currentTimeMillis();

    try {
      HttpRequest request =
          HttpRequest.newBuilder()
              .uri(URI.create(url))
              .timeout(timeout)
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(INTROSPECTION_QUERY))
              .build();

      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      long responseTime = System.currentTimeMillis() - startTime;

      if (response.statusCode() >= 200 && response.statusCode() < 300) {
        builder
            .up()
            .withDetail("url", url)
            .withDetail("responseTime", responseTime + "ms")
            .withDetail("statusCode", response.statusCode());
      } else {
        builder
            .down()
            .withDetail("url", url)
            .withDetail("responseTime", responseTime + "ms")
            .withDetail("statusCode", response.statusCode())
            .withDetail("reason", "Non-2xx response");
      }

    } catch (Exception e) {
      long responseTime = System.currentTimeMillis() - startTime;
      builder
          .down(e)
          .withDetail("url", url)
          .withDetail("responseTime", responseTime + "ms")
          .withDetail("reason", e.getMessage());
    }
  }

  /**
   * Returns the GraphQL endpoint URL.
   *
   * @return the URL
   */
  @NotNull
  public String getUrl() {
    return url;
  }

  /**
   * Returns the timeout duration.
   *
   * @return the timeout
   */
  @NotNull
  public Duration getTimeout() {
    return timeout;
  }
}
