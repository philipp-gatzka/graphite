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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.graphite.interceptor.RequestInterceptor;
import io.github.graphite.interceptor.ResponseInterceptor;
import io.github.graphite.ratelimit.RateLimiter;
import io.github.graphite.retry.RetryPolicy;
import io.github.graphite.scalar.ScalarRegistry;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Default implementation of {@link GraphiteClient}.
 *
 * <p>This class is package-private and should be created via {@link GraphiteClientBuilder}.
 */
final class DefaultGraphiteClient implements GraphiteClient {

  private final URI endpoint;
  private final Map<String, String> headers;
  private final Duration connectTimeout;
  private final Duration readTimeout;
  private final Duration requestTimeout;
  private final RetryPolicy retryPolicy;
  private final RateLimiter rateLimiter;
  private final ScalarRegistry scalarRegistry;
  private final List<RequestInterceptor> requestInterceptors;
  private final List<ResponseInterceptor> responseInterceptors;
  private final ObjectMapper objectMapper;

  private volatile boolean closed = false;

  DefaultGraphiteClient(
      @NotNull URI endpoint,
      @NotNull Map<String, String> headers,
      @NotNull Duration connectTimeout,
      @NotNull Duration readTimeout,
      @NotNull Duration requestTimeout,
      @NotNull RetryPolicy retryPolicy,
      @Nullable RateLimiter rateLimiter,
      @NotNull ScalarRegistry scalarRegistry,
      @NotNull List<RequestInterceptor> requestInterceptors,
      @NotNull List<ResponseInterceptor> responseInterceptors,
      @NotNull ObjectMapper objectMapper) {
    this.endpoint = endpoint;
    this.headers = headers;
    this.connectTimeout = connectTimeout;
    this.readTimeout = readTimeout;
    this.requestTimeout = requestTimeout;
    this.retryPolicy = retryPolicy;
    this.rateLimiter = rateLimiter;
    this.scalarRegistry = scalarRegistry;
    this.requestInterceptors = requestInterceptors;
    this.responseInterceptors = responseInterceptors;
    this.objectMapper = objectMapper;
  }

  @Override
  @NotNull
  public <T> GraphiteResponse<T> execute(@NotNull GraphQLOperation<T> operation) {
    if (operation == null) {
      throw new NullPointerException("operation must not be null");
    }
    ensureNotClosed();

    // TODO: Implement actual HTTP execution
    // For now, return an empty response as a placeholder
    return GraphiteResponse.success(null);
  }

  @Override
  @NotNull
  public <T> CompletableFuture<GraphiteResponse<T>> executeAsync(
      @NotNull GraphQLOperation<T> operation) {
    if (operation == null) {
      throw new NullPointerException("operation must not be null");
    }
    ensureNotClosed();

    return CompletableFuture.supplyAsync(() -> execute(operation));
  }

  @Override
  public void close() {
    closed = true;
  }

  /**
   * Returns the endpoint URI.
   *
   * @return the endpoint
   */
  @NotNull
  URI getEndpoint() {
    return endpoint;
  }

  /**
   * Returns the configured headers.
   *
   * @return an unmodifiable map of headers
   */
  @NotNull
  Map<String, String> getHeaders() {
    return headers;
  }

  /**
   * Returns the connect timeout.
   *
   * @return the connect timeout
   */
  @NotNull
  Duration getConnectTimeout() {
    return connectTimeout;
  }

  /**
   * Returns the read timeout.
   *
   * @return the read timeout
   */
  @NotNull
  Duration getReadTimeout() {
    return readTimeout;
  }

  /**
   * Returns the request timeout.
   *
   * @return the request timeout
   */
  @NotNull
  Duration getRequestTimeout() {
    return requestTimeout;
  }

  /**
   * Returns the retry policy.
   *
   * @return the retry policy
   */
  @NotNull
  RetryPolicy getRetryPolicy() {
    return retryPolicy;
  }

  /**
   * Returns the rate limiter, if configured.
   *
   * @return the rate limiter, or null
   */
  @Nullable
  RateLimiter getRateLimiter() {
    return rateLimiter;
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
