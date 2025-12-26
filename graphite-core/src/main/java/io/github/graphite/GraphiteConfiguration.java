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

import io.github.graphite.ratelimit.RateLimiter;
import io.github.graphite.retry.RetryListener;
import io.github.graphite.retry.RetryPolicy;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Immutable configuration for a GraphiteClient.
 *
 * <p>This record encapsulates all configuration options for a GraphQL client, including the
 * endpoint URL, headers, timeouts, retry policy, and rate limiter.
 *
 * <p>GraphiteConfiguration is typically created via {@link GraphiteClientBuilder} or can be
 * constructed directly for programmatic configuration.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * GraphiteConfiguration config = new GraphiteConfiguration(
 *     URI.create("https://api.example.com/graphql"),
 *     Map.of("Authorization", "Bearer token"),
 *     Duration.ofSeconds(10),
 *     Duration.ofSeconds(30),
 *     Duration.ofSeconds(60),
 *     RetryPolicy.defaults(),
 *     RateLimiter.create(100)
 * );
 * }</pre>
 *
 * @param endpoint the GraphQL endpoint URI
 * @param headers the HTTP headers to send with each request
 * @param connectTimeout the connection timeout
 * @param readTimeout the read timeout
 * @param requestTimeout the total request timeout
 * @param retryPolicy the retry policy for failed requests
 * @param rateLimiter the rate limiter, or null if no rate limiting
 * @param retryListener the retry listener, or null if no retry events should be reported
 * @see GraphiteClient
 * @see GraphiteClientBuilder
 */
public record GraphiteConfiguration(
    @NotNull URI endpoint,
    @NotNull Map<String, String> headers,
    @NotNull Duration connectTimeout,
    @NotNull Duration readTimeout,
    @NotNull Duration requestTimeout,
    @NotNull RetryPolicy retryPolicy,
    @Nullable RateLimiter rateLimiter,
    @Nullable RetryListener retryListener) {

  /** Default connect timeout of 10 seconds. */
  public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);

  /** Default read timeout of 30 seconds. */
  public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);

  /** Default request timeout of 60 seconds. */
  public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(60);

  /**
   * Creates a new configuration with validation.
   *
   * @param endpoint the GraphQL endpoint URI
   * @param headers the HTTP headers
   * @param connectTimeout the connection timeout
   * @param readTimeout the read timeout
   * @param requestTimeout the total request timeout
   * @param retryPolicy the retry policy
   * @param rateLimiter the rate limiter (may be null)
   * @throws NullPointerException if any required parameter is null
   * @throws IllegalArgumentException if any timeout is not positive
   */
  public GraphiteConfiguration {
    if (endpoint == null) {
      throw new NullPointerException("endpoint must not be null");
    }
    if (headers == null) {
      throw new NullPointerException("headers must not be null");
    }
    if (connectTimeout == null) {
      throw new NullPointerException("connectTimeout must not be null");
    }
    if (readTimeout == null) {
      throw new NullPointerException("readTimeout must not be null");
    }
    if (requestTimeout == null) {
      throw new NullPointerException("requestTimeout must not be null");
    }
    if (retryPolicy == null) {
      throw new NullPointerException("retryPolicy must not be null");
    }
    if (connectTimeout.isNegative() || connectTimeout.isZero()) {
      throw new IllegalArgumentException("connectTimeout must be positive");
    }
    if (readTimeout.isNegative() || readTimeout.isZero()) {
      throw new IllegalArgumentException("readTimeout must be positive");
    }
    if (requestTimeout.isNegative() || requestTimeout.isZero()) {
      throw new IllegalArgumentException("requestTimeout must be positive");
    }
    // Make headers immutable
    headers = Collections.unmodifiableMap(headers);
  }

  /**
   * Creates a configuration with the specified endpoint and default settings.
   *
   * @param endpoint the GraphQL endpoint URL
   * @return a new configuration with defaults
   * @throws NullPointerException if endpoint is null
   */
  @NotNull
  public static GraphiteConfiguration withEndpoint(@NotNull String endpoint) {
    return withEndpoint(URI.create(endpoint));
  }

  /**
   * Creates a configuration with the specified endpoint and default settings.
   *
   * @param endpoint the GraphQL endpoint URI
   * @return a new configuration with defaults
   * @throws NullPointerException if endpoint is null
   */
  @NotNull
  public static GraphiteConfiguration withEndpoint(@NotNull URI endpoint) {
    return new GraphiteConfiguration(
        endpoint,
        Map.of(),
        DEFAULT_CONNECT_TIMEOUT,
        DEFAULT_READ_TIMEOUT,
        DEFAULT_REQUEST_TIMEOUT,
        RetryPolicy.defaults(),
        null,
        null);
  }

  /**
   * Returns whether rate limiting is enabled.
   *
   * @return {@code true} if a rate limiter is configured
   */
  public boolean hasRateLimiter() {
    return rateLimiter != null;
  }

  /**
   * Returns whether retry is enabled.
   *
   * @return {@code true} if the retry policy has at least one retry attempt
   */
  public boolean hasRetry() {
    return retryPolicy.maxAttempts() > 0;
  }

  /**
   * Returns a new configuration with the specified header added.
   *
   * @param name the header name
   * @param value the header value
   * @return a new configuration with the header added
   */
  @NotNull
  public GraphiteConfiguration withHeader(@NotNull String name, @NotNull String value) {
    var newHeaders = new java.util.HashMap<>(headers);
    newHeaders.put(name, value);
    return new GraphiteConfiguration(
        endpoint,
        newHeaders,
        connectTimeout,
        readTimeout,
        requestTimeout,
        retryPolicy,
        rateLimiter,
        retryListener);
  }

  /**
   * Returns a new configuration with the specified connect timeout.
   *
   * @param timeout the new connect timeout
   * @return a new configuration with the timeout
   */
  @NotNull
  public GraphiteConfiguration withConnectTimeout(@NotNull Duration timeout) {
    return new GraphiteConfiguration(
        endpoint,
        headers,
        timeout,
        readTimeout,
        requestTimeout,
        retryPolicy,
        rateLimiter,
        retryListener);
  }

  /**
   * Returns a new configuration with the specified read timeout.
   *
   * @param timeout the new read timeout
   * @return a new configuration with the timeout
   */
  @NotNull
  public GraphiteConfiguration withReadTimeout(@NotNull Duration timeout) {
    return new GraphiteConfiguration(
        endpoint,
        headers,
        connectTimeout,
        timeout,
        requestTimeout,
        retryPolicy,
        rateLimiter,
        retryListener);
  }

  /**
   * Returns a new configuration with the specified request timeout.
   *
   * @param timeout the new request timeout
   * @return a new configuration with the timeout
   */
  @NotNull
  public GraphiteConfiguration withRequestTimeout(@NotNull Duration timeout) {
    return new GraphiteConfiguration(
        endpoint,
        headers,
        connectTimeout,
        readTimeout,
        timeout,
        retryPolicy,
        rateLimiter,
        retryListener);
  }

  /**
   * Returns a new configuration with the specified retry policy.
   *
   * @param policy the new retry policy
   * @return a new configuration with the policy
   */
  @NotNull
  public GraphiteConfiguration withRetryPolicy(@NotNull RetryPolicy policy) {
    return new GraphiteConfiguration(
        endpoint,
        headers,
        connectTimeout,
        readTimeout,
        requestTimeout,
        policy,
        rateLimiter,
        retryListener);
  }

  /**
   * Returns a new configuration with the specified rate limiter.
   *
   * @param limiter the new rate limiter (may be null to disable)
   * @return a new configuration with the limiter
   */
  @NotNull
  public GraphiteConfiguration withRateLimiter(@Nullable RateLimiter limiter) {
    return new GraphiteConfiguration(
        endpoint,
        headers,
        connectTimeout,
        readTimeout,
        requestTimeout,
        retryPolicy,
        limiter,
        retryListener);
  }

  /**
   * Returns a new configuration with the specified retry listener.
   *
   * @param listener the new retry listener (may be null to disable)
   * @return a new configuration with the listener
   */
  @NotNull
  public GraphiteConfiguration withRetryListener(@Nullable RetryListener listener) {
    return new GraphiteConfiguration(
        endpoint,
        headers,
        connectTimeout,
        readTimeout,
        requestTimeout,
        retryPolicy,
        rateLimiter,
        listener);
  }

  /**
   * Returns whether a retry listener is configured.
   *
   * @return {@code true} if a retry listener is configured
   */
  public boolean hasRetryListener() {
    return retryListener != null;
  }
}
