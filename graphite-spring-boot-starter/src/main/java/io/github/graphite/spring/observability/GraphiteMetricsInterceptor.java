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
package io.github.graphite.spring.observability;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.graphite.http.HttpRequest;
import io.github.graphite.http.HttpResponse;
import io.github.graphite.interceptor.RequestInterceptor;
import io.github.graphite.interceptor.ResponseInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interceptor that records metrics for Graphite client operations.
 *
 * <p>This interceptor implements both {@link RequestInterceptor} and {@link ResponseInterceptor} to
 * measure request duration and record success/error metrics. It extracts the operation name from
 * the GraphQL request body.
 *
 * <p>The following metrics are recorded:
 *
 * <ul>
 *   <li>{@code graphite.client.requests} - Counter with tags: operation, status
 *   <li>{@code graphite.client.request.duration} - Timer with tags: operation
 * </ul>
 *
 * <p>Example configuration:
 *
 * <pre>{@code
 * GraphiteMetricsInterceptor metricsInterceptor = new GraphiteMetricsInterceptor(meterRegistry);
 *
 * GraphiteClient client = GraphiteClient.builder()
 *     .endpoint("https://api.example.com/graphql")
 *     .requestInterceptor(metricsInterceptor.requestInterceptor())
 *     .responseInterceptor(metricsInterceptor.responseInterceptor())
 *     .build();
 * }</pre>
 *
 * @see GraphiteMetrics
 * @see io.github.graphite.spring.autoconfigure.GraphiteMetricsAutoConfiguration
 */
public class GraphiteMetricsInterceptor {

  /** ThreadLocal to store the timer sample for the current request. */
  private static final ThreadLocal<Timer.Sample> TIMER_SAMPLE = new ThreadLocal<>();

  /** ThreadLocal to store the operation name for the current request. */
  private static final ThreadLocal<String> OPERATION_NAME = new ThreadLocal<>();

  private final GraphiteMetrics metrics;
  private final ObjectMapper objectMapper;

  /**
   * Creates a new metrics interceptor with the given registry.
   *
   * @param registry the meter registry for recording metrics
   */
  public GraphiteMetricsInterceptor(@NotNull MeterRegistry registry) {
    this(new GraphiteMetrics(registry), new ObjectMapper());
  }

  /**
   * Creates a new metrics interceptor with the given metrics instance.
   *
   * @param metrics the metrics instance
   * @param objectMapper the object mapper for parsing request body
   */
  public GraphiteMetricsInterceptor(
      @NotNull GraphiteMetrics metrics, @NotNull ObjectMapper objectMapper) {
    this.metrics = metrics;
    this.objectMapper = objectMapper;
  }

  /**
   * Returns a request interceptor that starts timing and extracts operation name.
   *
   * @return the request interceptor
   */
  @NotNull
  public RequestInterceptor requestInterceptor() {
    return request -> {
      // Start timer
      TIMER_SAMPLE.set(metrics.startTimer());

      // Extract operation name from request body
      String operationName = extractOperationName(request);
      OPERATION_NAME.set(operationName);

      return request;
    };
  }

  /**
   * Returns a response interceptor that records the request metrics.
   *
   * @return the response interceptor
   */
  @NotNull
  public ResponseInterceptor responseInterceptor() {
    return response -> {
      try {
        Timer.Sample sample = TIMER_SAMPLE.get();
        String operation = OPERATION_NAME.get();

        if (sample != null) {
          boolean success = isSuccess(response);
          if (success) {
            metrics.recordSuccess(operation, sample);
          } else {
            // For non-2xx responses, record as error
            metrics.recordDuration(operation, java.time.Duration.ZERO, false);
          }
        }
      } finally {
        // Clean up thread locals
        TIMER_SAMPLE.remove();
        OPERATION_NAME.remove();
      }

      return response;
    };
  }

  /**
   * Records an error that occurred during request execution.
   *
   * <p>This method should be called when an exception occurs during request execution to properly
   * record error metrics. After calling this method, the response interceptor will not record
   * duplicate metrics.
   *
   * @param error the exception that occurred
   */
  public void recordError(@NotNull Throwable error) {
    try {
      Timer.Sample sample = TIMER_SAMPLE.get();
      String operation = OPERATION_NAME.get();

      if (sample != null) {
        metrics.recordError(operation, error, sample);
      }
    } finally {
      // Clean up thread locals
      TIMER_SAMPLE.remove();
      OPERATION_NAME.remove();
    }
  }

  /**
   * Returns the underlying metrics instance.
   *
   * @return the metrics instance
   */
  @NotNull
  public GraphiteMetrics getMetrics() {
    return metrics;
  }

  /**
   * Extracts the operation name from the GraphQL request body.
   *
   * @param request the HTTP request
   * @return the operation name or null if not found
   */
  @Nullable
  private String extractOperationName(@NotNull HttpRequest request) {
    String body = request.body();
    if (body == null || body.isBlank()) {
      return null;
    }

    try {
      JsonNode json = objectMapper.readTree(body);

      // Try to get operationName field
      JsonNode operationNameNode = json.get("operationName");
      if (operationNameNode != null && operationNameNode.isTextual()) {
        return operationNameNode.asText();
      }

      // Fall back to extracting from query
      JsonNode queryNode = json.get("query");
      if (queryNode != null && queryNode.isTextual()) {
        return extractOperationNameFromQuery(queryNode.asText());
      }

    } catch (Exception e) {
      // Failed to parse, return null
    }

    return null;
  }

  /**
   * Extracts operation name from a GraphQL query string.
   *
   * @param query the GraphQL query
   * @return the operation name or null
   */
  @Nullable
  private String extractOperationNameFromQuery(@NotNull String query) {
    String trimmed = skipLeadingComments(query.trim());
    if (trimmed == null) {
      return null;
    }
    return extractNameFromOperation(trimmed);
  }

  @Nullable
  private String skipLeadingComments(@NotNull String query) {
    String result = query;
    while (result.startsWith("#")) {
      int newlineIndex = result.indexOf('\n');
      if (newlineIndex == -1) {
        return null;
      }
      result = result.substring(newlineIndex + 1).trim();
    }
    return result;
  }

  @Nullable
  private String extractNameFromOperation(@NotNull String trimmed) {
    String[] prefixes = {"query", "mutation", "subscription"};
    for (String prefix : prefixes) {
      if (trimmed.startsWith(prefix)) {
        return extractOperationName(trimmed.substring(prefix.length()).trim());
      }
    }
    return null;
  }

  @Nullable
  private String extractOperationName(@NotNull String remainder) {
    if (remainder.startsWith("{") || remainder.startsWith("(")) {
      return null;
    }
    StringBuilder name = new StringBuilder();
    for (char c : remainder.toCharArray()) {
      if (Character.isLetterOrDigit(c) || c == '_') {
        name.append(c);
      } else {
        break;
      }
    }
    return !name.isEmpty() ? name.toString() : null;
  }

  /**
   * Checks if the response indicates success.
   *
   * @param response the HTTP response
   * @return true if the response status is 2xx
   */
  private boolean isSuccess(@NotNull HttpResponse response) {
    int status = response.statusCode();
    return status >= 200 && status < 300;
  }
}
