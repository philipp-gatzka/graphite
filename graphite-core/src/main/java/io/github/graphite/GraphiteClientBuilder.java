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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builder for constructing {@link GraphiteClient} instances.
 *
 * <p>This builder provides a fluent API for configuring GraphiteClient with various options
 * including endpoint URL, headers, timeouts, retry policies, and rate limiting.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * GraphiteClient client = GraphiteClient.builder()
 *     .endpoint("https://api.example.com/graphql")
 *     .header("Authorization", "Bearer " + token)
 *     .connectTimeout(Duration.ofSeconds(10))
 *     .readTimeout(Duration.ofSeconds(30))
 *     .retryPolicy(RetryPolicy.of(3, ExponentialBackoff.defaults()))
 *     .build();
 * }</pre>
 *
 * <p>Required configuration:
 *
 * <ul>
 *   <li>{@link #endpoint(String)} - The GraphQL endpoint URL
 * </ul>
 *
 * <p>All other options have sensible defaults.
 *
 * @see GraphiteClient
 */
public final class GraphiteClientBuilder {

  /** Default connect timeout. */
  public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);

  /** Default read timeout. */
  public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);

  /** Default request timeout. */
  public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(60);

  private URI endpoint;
  private final Map<String, String> headers = new HashMap<>();
  private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
  private Duration readTimeout = DEFAULT_READ_TIMEOUT;
  private Duration requestTimeout = DEFAULT_REQUEST_TIMEOUT;
  private RetryPolicy retryPolicy = RetryPolicy.defaults();
  private RateLimiter rateLimiter;
  private ScalarRegistry scalarRegistry = ScalarRegistry.defaults();
  private final List<RequestInterceptor> requestInterceptors = new ArrayList<>();
  private final List<ResponseInterceptor> responseInterceptors = new ArrayList<>();
  private ObjectMapper objectMapper;

  /**
   * Creates a new builder instance.
   *
   * <p>Use {@link GraphiteClient#builder()} instead of this constructor.
   */
  GraphiteClientBuilder() {
    // Package-private constructor
  }

  /**
   * Creates a new builder with default settings.
   *
   * @return a new builder instance
   */
  @NotNull
  public static GraphiteClientBuilder create() {
    return new GraphiteClientBuilder();
  }

  /**
   * Sets the GraphQL endpoint URL.
   *
   * <p>This is required and must be called before {@link #build()}.
   *
   * @param endpoint the endpoint URL
   * @return this builder
   * @throws NullPointerException if endpoint is null
   * @throws IllegalArgumentException if endpoint is not a valid URI
   */
  @NotNull
  public GraphiteClientBuilder endpoint(@NotNull String endpoint) {
    if (endpoint == null) {
      throw new NullPointerException("endpoint must not be null");
    }
    this.endpoint = URI.create(endpoint);
    return this;
  }

  /**
   * Sets the GraphQL endpoint URI.
   *
   * <p>This is required and must be called before {@link #build()}.
   *
   * @param endpoint the endpoint URI
   * @return this builder
   * @throws NullPointerException if endpoint is null
   */
  @NotNull
  public GraphiteClientBuilder endpoint(@NotNull URI endpoint) {
    if (endpoint == null) {
      throw new NullPointerException("endpoint must not be null");
    }
    this.endpoint = endpoint;
    return this;
  }

  /**
   * Adds a header to be sent with all requests.
   *
   * <p>If a header with the same name is already set, it will be replaced.
   *
   * @param name the header name
   * @param value the header value
   * @return this builder
   * @throws NullPointerException if name or value is null
   */
  @NotNull
  public GraphiteClientBuilder header(@NotNull String name, @NotNull String value) {
    if (name == null) {
      throw new NullPointerException("header name must not be null");
    }
    if (value == null) {
      throw new NullPointerException("header value must not be null");
    }
    headers.put(name, value);
    return this;
  }

  /**
   * Adds multiple headers to be sent with all requests.
   *
   * <p>Existing headers with the same names will be replaced.
   *
   * @param headers the headers to add
   * @return this builder
   * @throws NullPointerException if headers is null
   */
  @NotNull
  public GraphiteClientBuilder headers(@NotNull Map<String, String> headers) {
    if (headers == null) {
      throw new NullPointerException("headers must not be null");
    }
    this.headers.putAll(headers);
    return this;
  }

  /**
   * Sets the connection timeout.
   *
   * <p>This is the maximum time to wait when establishing a connection to the server. Default is
   * {@value #DEFAULT_CONNECT_TIMEOUT}.
   *
   * @param timeout the connect timeout
   * @return this builder
   * @throws NullPointerException if timeout is null
   * @throws IllegalArgumentException if timeout is negative or zero
   */
  @NotNull
  public GraphiteClientBuilder connectTimeout(@NotNull Duration timeout) {
    validateTimeout(timeout, "connectTimeout");
    this.connectTimeout = timeout;
    return this;
  }

  /**
   * Sets the read timeout.
   *
   * <p>This is the maximum time to wait for data after a connection has been established. Default
   * is {@value #DEFAULT_READ_TIMEOUT}.
   *
   * @param timeout the read timeout
   * @return this builder
   * @throws NullPointerException if timeout is null
   * @throws IllegalArgumentException if timeout is negative or zero
   */
  @NotNull
  public GraphiteClientBuilder readTimeout(@NotNull Duration timeout) {
    validateTimeout(timeout, "readTimeout");
    this.readTimeout = timeout;
    return this;
  }

  /**
   * Sets the request timeout.
   *
   * <p>This is the maximum total time for an entire request, including connection, sending, and
   * receiving. Default is {@value #DEFAULT_REQUEST_TIMEOUT}.
   *
   * @param timeout the request timeout
   * @return this builder
   * @throws NullPointerException if timeout is null
   * @throws IllegalArgumentException if timeout is negative or zero
   */
  @NotNull
  public GraphiteClientBuilder requestTimeout(@NotNull Duration timeout) {
    validateTimeout(timeout, "requestTimeout");
    this.requestTimeout = timeout;
    return this;
  }

  /**
   * Sets the retry policy for handling transient failures.
   *
   * <p>The default policy retries up to 3 times with exponential backoff for connection and timeout
   * exceptions.
   *
   * @param retryPolicy the retry policy
   * @return this builder
   * @throws NullPointerException if retryPolicy is null
   * @see RetryPolicy
   */
  @NotNull
  public GraphiteClientBuilder retryPolicy(@NotNull RetryPolicy retryPolicy) {
    if (retryPolicy == null) {
      throw new NullPointerException("retryPolicy must not be null");
    }
    this.retryPolicy = retryPolicy;
    return this;
  }

  /**
   * Sets the rate limiter for controlling request rate.
   *
   * <p>When set, the client will acquire a permit before each request, blocking if necessary until
   * a permit is available.
   *
   * @param rateLimiter the rate limiter, or null to disable rate limiting
   * @return this builder
   * @see RateLimiter
   */
  @NotNull
  public GraphiteClientBuilder rateLimiter(@Nullable RateLimiter rateLimiter) {
    this.rateLimiter = rateLimiter;
    return this;
  }

  /**
   * Sets the scalar registry for custom scalar type handling.
   *
   * <p>The default registry includes coercings for common scalar types.
   *
   * @param scalarRegistry the scalar registry
   * @return this builder
   * @throws NullPointerException if scalarRegistry is null
   * @see ScalarRegistry
   */
  @NotNull
  public GraphiteClientBuilder scalarRegistry(@NotNull ScalarRegistry scalarRegistry) {
    if (scalarRegistry == null) {
      throw new NullPointerException("scalarRegistry must not be null");
    }
    this.scalarRegistry = scalarRegistry;
    return this;
  }

  /**
   * Adds a request interceptor.
   *
   * <p>Interceptors are called in the order they are added.
   *
   * @param interceptor the request interceptor
   * @return this builder
   * @throws NullPointerException if interceptor is null
   * @see RequestInterceptor
   */
  @NotNull
  public GraphiteClientBuilder requestInterceptor(@NotNull RequestInterceptor interceptor) {
    if (interceptor == null) {
      throw new NullPointerException("requestInterceptor must not be null");
    }
    requestInterceptors.add(interceptor);
    return this;
  }

  /**
   * Adds a response interceptor.
   *
   * <p>Interceptors are called in the order they are added.
   *
   * @param interceptor the response interceptor
   * @return this builder
   * @throws NullPointerException if interceptor is null
   * @see ResponseInterceptor
   */
  @NotNull
  public GraphiteClientBuilder responseInterceptor(@NotNull ResponseInterceptor interceptor) {
    if (interceptor == null) {
      throw new NullPointerException("responseInterceptor must not be null");
    }
    responseInterceptors.add(interceptor);
    return this;
  }

  /**
   * Sets a custom ObjectMapper for JSON serialization.
   *
   * <p>If not set, a default ObjectMapper will be used.
   *
   * @param objectMapper the ObjectMapper to use
   * @return this builder
   * @throws NullPointerException if objectMapper is null
   */
  @NotNull
  public GraphiteClientBuilder objectMapper(@NotNull ObjectMapper objectMapper) {
    if (objectMapper == null) {
      throw new NullPointerException("objectMapper must not be null");
    }
    this.objectMapper = objectMapper;
    return this;
  }

  /**
   * Builds the GraphiteClient with the configured options.
   *
   * @return a new GraphiteClient instance
   * @throws IllegalStateException if endpoint is not set
   */
  @NotNull
  public GraphiteClient build() {
    if (endpoint == null) {
      throw new IllegalStateException("endpoint is required");
    }

    ObjectMapper mapper = objectMapper != null ? objectMapper : createDefaultObjectMapper();

    return new DefaultGraphiteClient(
        endpoint,
        Map.copyOf(headers),
        connectTimeout,
        readTimeout,
        requestTimeout,
        retryPolicy,
        rateLimiter,
        scalarRegistry,
        List.copyOf(requestInterceptors),
        List.copyOf(responseInterceptors),
        mapper);
  }

  private void validateTimeout(Duration timeout, String name) {
    if (timeout == null) {
      throw new NullPointerException(name + " must not be null");
    }
    if (timeout.isNegative() || timeout.isZero()) {
      throw new IllegalArgumentException(name + " must be positive");
    }
  }

  private ObjectMapper createDefaultObjectMapper() {
    return new ObjectMapper().findAndRegisterModules();
  }
}
