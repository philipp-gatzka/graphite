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
package io.github.graphite;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.graphite.exception.GraphiteClientException;
import io.github.graphite.exception.GraphiteException;
import io.github.graphite.exception.GraphiteRateLimitException;
import io.github.graphite.exception.GraphiteServerException;
import io.github.graphite.http.DefaultHttpTransport;
import io.github.graphite.http.HttpRequest;
import io.github.graphite.http.HttpResponse;
import io.github.graphite.http.HttpTransport;
import io.github.graphite.http.HttpTransportConfig;
import io.github.graphite.interceptor.RequestInterceptor;
import io.github.graphite.interceptor.ResponseInterceptor;
import io.github.graphite.ratelimit.RateLimiter;
import io.github.graphite.retry.RetryListener;
import io.github.graphite.retry.RetryPolicy;
import io.github.graphite.scalar.ScalarRegistry;
import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default implementation of {@link GraphiteClient}.
 *
 * <p>This class provides the core functionality for executing GraphQL operations:
 *
 * <ul>
 *   <li>Serialization of operations to JSON
 *   <li>HTTP execution via {@link HttpTransport}
 *   <li>Response deserialization using Jackson
 *   <li>Retry policy application on failures
 *   <li>Rate limiting support
 *   <li>Request/response interceptor chains
 * </ul>
 *
 * <p>This class is package-private and should be created via {@link GraphiteClientBuilder}.
 *
 * @see GraphiteClient
 * @see GraphiteClientBuilder
 */
final class DefaultGraphiteClient implements GraphiteClient {

  private static final String CONTENT_TYPE_JSON = "application/json";

  private final GraphiteConfiguration configuration;
  private final ScalarRegistry scalarRegistry;
  private final List<RequestInterceptor> requestInterceptors;
  private final List<ResponseInterceptor> responseInterceptors;
  private final ObjectMapper objectMapper;
  private final HttpTransport httpTransport;

  private volatile boolean closed = false;

  DefaultGraphiteClient(
      @NotNull GraphiteConfiguration configuration,
      @NotNull ScalarRegistry scalarRegistry,
      @NotNull List<RequestInterceptor> requestInterceptors,
      @NotNull List<ResponseInterceptor> responseInterceptors,
      @NotNull ObjectMapper objectMapper) {
    this.configuration = configuration;
    this.scalarRegistry = scalarRegistry;
    this.requestInterceptors = requestInterceptors;
    this.responseInterceptors = responseInterceptors;
    this.objectMapper = objectMapper;
    this.httpTransport = createTransport();
  }

  private HttpTransport createTransport() {
    HttpTransportConfig config =
        HttpTransportConfig.builder()
            .connectTimeout(configuration.connectTimeout())
            .readTimeout(configuration.readTimeout())
            .requestTimeout(configuration.requestTimeout())
            .build();
    return new DefaultHttpTransport(config);
  }

  @Override
  @NotNull
  public <T> GraphiteResponse<T> execute(@NotNull GraphQLOperation<T> operation) {
    Objects.requireNonNull(operation, "operation must not be null");
    ensureNotClosed();

    try {
      // Step 1: Serialize operation to JSON
      String requestBody = serializeOperation(operation);

      // Step 2: Create HTTP request with headers
      HttpRequest request = createRequest(requestBody);

      // Step 3: Apply request interceptors
      request = applyRequestInterceptors(request);

      // Step 4: Acquire rate limit permit
      acquireRateLimitPermit();

      // Step 5: Execute with retry
      HttpResponse response = executeWithRetry(request);

      // Step 6: Apply response interceptors
      response = applyResponseInterceptors(response);

      // Step 7: Deserialize response and return
      return deserializeResponse(response, operation.responseType());

    } catch (GraphiteException e) {
      throw e;
    } catch (Exception e) {
      throw new GraphiteClientException("Failed to execute GraphQL operation", e);
    }
  }

  @Override
  @NotNull
  public <T> CompletableFuture<GraphiteResponse<T>> executeAsync(
      @NotNull GraphQLOperation<T> operation) {
    Objects.requireNonNull(operation, "operation must not be null");
    ensureNotClosed();

    return CompletableFuture.supplyAsync(() -> execute(operation));
  }

  @Override
  public void close() {
    if (!closed) {
      closed = true;
      httpTransport.close();
    }
  }

  private String serializeOperation(GraphQLOperation<?> operation) {
    try {
      Map<String, Object> payload = new HashMap<>();
      payload.put("query", operation.toGraphQL());
      payload.put("operationName", operation.operationName());

      Map<String, Object> variables = operation.variables();
      if (!variables.isEmpty()) {
        payload.put("variables", variables);
      }

      return objectMapper.writeValueAsString(payload);
    } catch (JsonProcessingException e) {
      throw new GraphiteClientException("Failed to serialize GraphQL operation", e);
    }
  }

  private HttpRequest createRequest(String body) {
    Map<String, String> requestHeaders = new HashMap<>(configuration.headers());
    requestHeaders.put("Content-Type", CONTENT_TYPE_JSON);
    requestHeaders.put("Accept", CONTENT_TYPE_JSON);

    return HttpRequest.post(configuration.endpoint(), requestHeaders, body);
  }

  private HttpRequest applyRequestInterceptors(HttpRequest request) {
    HttpRequest result = request;
    for (RequestInterceptor interceptor : requestInterceptors) {
      result = interceptor.intercept(result);
    }
    return result;
  }

  private void acquireRateLimitPermit() {
    RateLimiter limiter = configuration.rateLimiter();
    if (limiter != null && !limiter.tryAcquire()) {
      throw new GraphiteRateLimitException(
          "Rate limit exceeded: max " + limiter.getRequestsPerSecond() + " requests/second");
    }
  }

  private HttpResponse executeWithRetry(HttpRequest request) {
    RetryPolicy retryPolicy = configuration.retryPolicy();
    RetryListener listener = configuration.retryListener();
    int attempt = 0;

    while (true) {
      try {
        HttpResponse response = httpTransport.execute(request);

        // Check for server errors that might be retryable
        if (response.isServerError()) {
          GraphiteServerException serverException =
              new GraphiteServerException(
                  "Server error: HTTP " + response.statusCode(), response.statusCode());
          attempt = retryOrThrow(retryPolicy, listener, serverException, attempt);
        } else {
          // Success - notify listener if retries were needed
          if (attempt > 0 && listener != null) {
            listener.onRetrySuccess(attempt + 1);
          }
          return response;
        }

      } catch (GraphiteException e) {
        attempt = retryOrThrow(retryPolicy, listener, e, attempt);
      }
    }
  }

  private int retryOrThrow(
      RetryPolicy retryPolicy, RetryListener listener, GraphiteException e, int attempt) {
    if (!retryPolicy.shouldRetry(e, attempt + 1)) {
      // Retries exhausted - notify listener if any retries were attempted
      if (attempt > 0 && listener != null) {
        listener.onRetryExhausted(attempt + 1, e);
      }
      throw e;
    }
    attempt++;
    Duration delay = retryPolicy.getDelay(attempt);

    // Notify listener about retry attempt
    if (listener != null) {
      listener.onRetryAttempt(attempt, e, delay);
    }

    sleepForRetry(delay);
    return attempt;
  }

  private void sleepForRetry(Duration delay) {
    try {
      Thread.sleep(delay.toMillis());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new GraphiteClientException("Retry interrupted", e);
    }
  }

  private HttpResponse applyResponseInterceptors(HttpResponse response) {
    HttpResponse result = response;
    for (ResponseInterceptor interceptor : responseInterceptors) {
      result = interceptor.intercept(result);
    }
    return result;
  }

  private <T> GraphiteResponse<T> deserializeResponse(HttpResponse response, Class<T> responseType)
      throws JsonProcessingException {
    String body = response.body();
    if (body == null || body.isBlank()) {
      throw new GraphiteClientException("Empty response body from server");
    }

    JsonNode rootNode = objectMapper.readTree(body);

    // Parse errors
    List<GraphQLError> errors = parseErrors(rootNode);

    // Parse data
    T data = parseData(rootNode, responseType);

    // Parse extensions
    Map<String, Object> extensions = parseExtensions(rootNode);

    return new GraphiteResponse<>(data, errors, extensions);
  }

  private List<GraphQLError> parseErrors(JsonNode rootNode) {
    List<GraphQLError> errors = new ArrayList<>();
    JsonNode errorsNode = rootNode.get("errors");

    if (errorsNode != null && errorsNode.isArray()) {
      for (JsonNode errorNode : errorsNode) {
        String message =
            errorNode.has("message") ? errorNode.get("message").asText() : "Unknown error";

        List<GraphQLError.Location> locations = parseLocations(errorNode.get("locations"));
        List<Object> path = parsePath(errorNode.get("path"));
        Map<String, Object> extensions = parseExtensionsNode(errorNode.get("extensions"));

        errors.add(new GraphQLError(message, locations, path, extensions));
      }
    }

    return errors;
  }

  private List<GraphQLError.Location> parseLocations(JsonNode locationsNode) {
    if (locationsNode == null || !locationsNode.isArray()) {
      return List.of();
    }

    List<GraphQLError.Location> locations = new ArrayList<>();
    for (JsonNode locationNode : locationsNode) {
      int line = locationNode.has("line") ? locationNode.get("line").asInt() : 0;
      int column = locationNode.has("column") ? locationNode.get("column").asInt() : 0;
      locations.add(new GraphQLError.Location(line, column));
    }
    return locations;
  }

  private List<Object> parsePath(JsonNode pathNode) {
    if (pathNode == null || !pathNode.isArray()) {
      return List.of();
    }

    List<Object> path = new ArrayList<>();
    for (JsonNode element : pathNode) {
      if (element.isNumber()) {
        path.add(element.asInt());
      } else {
        path.add(element.asText());
      }
    }
    return path;
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> parseExtensionsNode(JsonNode extensionsNode) {
    if (extensionsNode == null || !extensionsNode.isObject()) {
      return Map.of();
    }
    try {
      Map<String, Object> result = objectMapper.treeToValue(extensionsNode, Map.class);
      return result != null ? result : Map.of();
    } catch (JsonProcessingException e) {
      return Map.of();
    }
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> parseExtensions(JsonNode rootNode) {
    JsonNode extensionsNode = rootNode.get("extensions");
    if (extensionsNode == null || !extensionsNode.isObject()) {
      return Map.of();
    }
    try {
      Map<String, Object> result = objectMapper.treeToValue(extensionsNode, Map.class);
      return result != null ? result : Map.of();
    } catch (JsonProcessingException e) {
      return Map.of();
    }
  }

  private <T> T parseData(JsonNode rootNode, Class<T> responseType) {
    JsonNode dataNode = rootNode.get("data");
    if (dataNode == null || dataNode.isNull()) {
      return null;
    }
    try {
      return objectMapper.treeToValue(dataNode, responseType);
    } catch (JsonProcessingException e) {
      throw new GraphiteClientException("Failed to deserialize response data", e);
    }
  }

  /**
   * Returns the endpoint URI.
   *
   * @return the endpoint
   */
  @NotNull
  URI getEndpoint() {
    return configuration.endpoint();
  }

  /**
   * Returns the configured headers.
   *
   * @return an unmodifiable map of headers
   */
  @NotNull
  Map<String, String> getHeaders() {
    return configuration.headers();
  }

  /**
   * Returns the connect timeout.
   *
   * @return the connect timeout
   */
  @NotNull
  Duration getConnectTimeout() {
    return configuration.connectTimeout();
  }

  /**
   * Returns the read timeout.
   *
   * @return the read timeout
   */
  @NotNull
  Duration getReadTimeout() {
    return configuration.readTimeout();
  }

  /**
   * Returns the request timeout.
   *
   * @return the request timeout
   */
  @NotNull
  Duration getRequestTimeout() {
    return configuration.requestTimeout();
  }

  /**
   * Returns the retry policy.
   *
   * @return the retry policy
   */
  @NotNull
  RetryPolicy getRetryPolicy() {
    return configuration.retryPolicy();
  }

  /**
   * Returns the rate limiter, if configured.
   *
   * @return the rate limiter, or null
   */
  @Nullable
  RateLimiter getRateLimiter() {
    return configuration.rateLimiter();
  }

  /**
   * Returns the retry listener, if configured.
   *
   * @return the retry listener, or null
   */
  @Nullable
  RetryListener getRetryListener() {
    return configuration.retryListener();
  }

  /**
   * Returns the scalar registry.
   *
   * @return the scalar registry
   */
  @NotNull
  ScalarRegistry getScalarRegistry() {
    return scalarRegistry;
  }

  /**
   * Returns the request interceptors.
   *
   * @return an unmodifiable list of interceptors
   */
  @NotNull
  List<RequestInterceptor> getRequestInterceptors() {
    return requestInterceptors;
  }

  /**
   * Returns the response interceptors.
   *
   * @return an unmodifiable list of interceptors
   */
  @NotNull
  List<ResponseInterceptor> getResponseInterceptors() {
    return responseInterceptors;
  }

  /**
   * Returns the ObjectMapper.
   *
   * @return the object mapper
   */
  @NotNull
  ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  /**
   * Returns whether this client is closed.
   *
   * @return true if closed
   */
  boolean isClosed() {
    return closed;
  }

  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Client has been closed");
    }
  }
}
