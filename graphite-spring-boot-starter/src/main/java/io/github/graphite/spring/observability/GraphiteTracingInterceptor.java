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
import io.github.graphite.interceptor.RequestInterceptor;
import io.github.graphite.interceptor.ResponseInterceptor;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Interceptor that creates distributed tracing spans for Graphite client operations.
 *
 * <p>This interceptor implements both {@link RequestInterceptor} and {@link ResponseInterceptor} to
 * create spans that track the full lifecycle of GraphQL requests.
 *
 * <p>The following span attributes are recorded:
 *
 * <ul>
 *   <li>{@code graphql.operation.name} - The GraphQL operation name
 *   <li>{@code graphql.operation.type} - The operation type (query/mutation/subscription)
 *   <li>{@code http.url} - The target URL
 *   <li>{@code http.status_code} - The HTTP response status code
 * </ul>
 *
 * <p>Example configuration:
 *
 * <pre>{@code
 * GraphiteTracingInterceptor tracingInterceptor = new GraphiteTracingInterceptor(tracer);
 *
 * GraphiteClient client = GraphiteClient.builder()
 *     .endpoint("https://api.example.com/graphql")
 *     .requestInterceptor(tracingInterceptor.requestInterceptor())
 *     .responseInterceptor(tracingInterceptor.responseInterceptor())
 *     .build();
 * }</pre>
 *
 * @see io.github.graphite.spring.autoconfigure.GraphiteTracingAutoConfiguration
 */
public class GraphiteTracingInterceptor {

  /** Span attribute for GraphQL operation name. */
  public static final String ATTR_OPERATION_NAME = "graphql.operation.name";

  /** Span attribute for GraphQL operation type. */
  public static final String ATTR_OPERATION_TYPE = "graphql.operation.type";

  /** Span attribute for HTTP URL. */
  public static final String ATTR_HTTP_URL = "http.url";

  /** Span attribute for HTTP status code. */
  public static final String ATTR_HTTP_STATUS_CODE = "http.status_code";

  /** Default span name. */
  public static final String SPAN_NAME = "graphite";

  /** ThreadLocal to store the span for the current request. */
  private static final ThreadLocal<Span> CURRENT_SPAN = new ThreadLocal<>();

  private final Tracer tracer;
  private final ObjectMapper objectMapper;

  /**
   * Creates a new tracing interceptor with the given tracer.
   *
   * @param tracer the Micrometer tracer
   */
  public GraphiteTracingInterceptor(@NotNull Tracer tracer) {
    this(tracer, new ObjectMapper());
  }

  /**
   * Creates a new tracing interceptor with the given tracer and object mapper.
   *
   * @param tracer the Micrometer tracer
   * @param objectMapper the object mapper for parsing request body
   */
  public GraphiteTracingInterceptor(@NotNull Tracer tracer, @NotNull ObjectMapper objectMapper) {
    this.tracer = tracer;
    this.objectMapper = objectMapper;
  }

  /**
   * Returns a request interceptor that starts a new span.
   *
   * @return the request interceptor
   */
  @NotNull
  public RequestInterceptor requestInterceptor() {
    return request -> {
      // Extract operation info
      OperationInfo opInfo = extractOperationInfo(request);
      String spanName =
          opInfo.name() != null ? SPAN_NAME + " " + opInfo.name() : SPAN_NAME + " unknown";

      // Start a new span
      Span span = tracer.nextSpan().name(spanName);
      span.tag(ATTR_HTTP_URL, request.uri().toString());

      if (opInfo.name() != null) {
        span.tag(ATTR_OPERATION_NAME, opInfo.name());
      }
      if (opInfo.type() != null) {
        span.tag(ATTR_OPERATION_TYPE, opInfo.type());
      }

      span.start();
      CURRENT_SPAN.set(span);

      return request;
    };
  }

  /**
   * Returns a response interceptor that completes the span.
   *
   * @return the response interceptor
   */
  @NotNull
  public ResponseInterceptor responseInterceptor() {
    return response -> {
      try {
        Span span = CURRENT_SPAN.get();
        if (span != null) {
          span.tag(ATTR_HTTP_STATUS_CODE, String.valueOf(response.statusCode()));

          // Mark as error if non-2xx status
          if (response.statusCode() < 200 || response.statusCode() >= 300) {
            span.error(new RuntimeException("HTTP " + response.statusCode()));
          }

          span.end();
        }
      } finally {
        CURRENT_SPAN.remove();
      }

      return response;
    };
  }

  /**
   * Records an error that occurred during request execution.
   *
   * <p>This method should be called when an exception occurs during request execution to properly
   * record the error on the span.
   *
   * @param error the exception that occurred
   */
  public void recordError(@NotNull Throwable error) {
    try {
      Span span = CURRENT_SPAN.get();
      if (span != null) {
        span.error(error);
        span.end();
      }
    } finally {
      CURRENT_SPAN.remove();
    }
  }

  /**
   * Returns the tracer.
   *
   * @return the tracer
   */
  @NotNull
  public Tracer getTracer() {
    return tracer;
  }

  /**
   * Extracts operation information from the request.
   *
   * @param request the HTTP request
   * @return the operation info
   */
  @NotNull
  private OperationInfo extractOperationInfo(@NotNull HttpRequest request) {
    String body = request.body();
    if (body == null || body.isBlank()) {
      return new OperationInfo(null, null);
    }

    try {
      JsonNode json = objectMapper.readTree(body);

      // Get operation name
      String name = null;
      JsonNode operationNameNode = json.get("operationName");
      if (operationNameNode != null && operationNameNode.isTextual()) {
        name = operationNameNode.asText();
      }

      // Get operation type from query
      String type = null;
      JsonNode queryNode = json.get("query");
      if (queryNode != null && queryNode.isTextual()) {
        String query = queryNode.asText().trim();

        // Skip leading comments
        while (query.startsWith("#")) {
          int newlineIndex = query.indexOf('\n');
          if (newlineIndex == -1) {
            break;
          }
          query = query.substring(newlineIndex + 1).trim();
        }

        if (query.startsWith("query")) {
          type = "query";
          if (name == null) {
            name = extractNameFromQuery(query, "query");
          }
        } else if (query.startsWith("mutation")) {
          type = "mutation";
          if (name == null) {
            name = extractNameFromQuery(query, "mutation");
          }
        } else if (query.startsWith("subscription")) {
          type = "subscription";
          if (name == null) {
            name = extractNameFromQuery(query, "subscription");
          }
        } else if (query.startsWith("{")) {
          // Anonymous query
          type = "query";
        }
      }

      return new OperationInfo(name, type);

    } catch (Exception e) {
      return new OperationInfo(null, null);
    }
  }

  /**
   * Extracts the operation name from a query string.
   *
   * @param query the GraphQL query
   * @param prefix the operation prefix (query/mutation/subscription)
   * @return the operation name or null
   */
  @Nullable
  private String extractNameFromQuery(@NotNull String query, @NotNull String prefix) {
    String remainder = query.substring(prefix.length()).trim();

    // Skip if starts with { or ( (anonymous operation)
    if (remainder.isEmpty() || remainder.charAt(0) == '{' || remainder.charAt(0) == '(') {
      return null;
    }

    // Extract name (word characters until space, (, or {)
    StringBuilder name = new StringBuilder();
    for (char c : remainder.toCharArray()) {
      if (Character.isLetterOrDigit(c) || c == '_') {
        name.append(c);
      } else {
        break;
      }
    }

    return name.length() > 0 ? name.toString() : null;
  }

  /** Record for holding operation information. */
  private record OperationInfo(@Nullable String name, @Nullable String type) {}
}
