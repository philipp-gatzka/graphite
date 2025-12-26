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
package io.github.graphite.http;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.Executor;
import org.jetbrains.annotations.Nullable;

/**
 * Configuration for HTTP transport behavior.
 *
 * <p>This record encapsulates all configurable aspects of HTTP transport:
 *
 * <ul>
 *   <li>{@code connectTimeout} - Maximum time to establish a connection
 *   <li>{@code readTimeout} - Maximum time to wait for response data
 *   <li>{@code requestTimeout} - Maximum total time for the entire request
 *   <li>{@code executor} - Custom executor for async operations
 *   <li>{@code maxConcurrentRequests} - Maximum number of concurrent requests
 *   <li>{@code keepAliveTimeout} - How long to keep idle connections alive
 * </ul>
 *
 * <p><strong>Connection Pooling:</strong> The underlying Java HttpClient manages connection pooling
 * automatically. You can control the behavior through:
 *
 * <ul>
 *   <li>Setting a custom {@code executor} to control async operation parallelism
 *   <li>Setting {@code maxConcurrentRequests} to limit concurrent requests
 *   <li>Using system properties like {@code jdk.httpclient.connectionPoolSize}
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * HttpTransportConfig config = HttpTransportConfig.builder()
 *     .connectTimeout(Duration.ofSeconds(10))
 *     .readTimeout(Duration.ofSeconds(30))
 *     .requestTimeout(Duration.ofSeconds(60))
 *     .maxConcurrentRequests(100)
 *     .keepAliveTimeout(Duration.ofMinutes(5))
 *     .build();
 * }</pre>
 *
 * @param connectTimeout the maximum time to wait for connection establishment
 * @param readTimeout the maximum time to wait for response data
 * @param requestTimeout the maximum total time for the entire request
 * @param executor the executor for async operations, or null for default
 * @param maxConcurrentRequests the maximum concurrent requests, or 0 for unlimited
 * @param keepAliveTimeout the idle connection timeout, or null to use default
 * @see HttpTransport
 * @see DefaultHttpTransport
 */
public record HttpTransportConfig(
    Duration connectTimeout,
    Duration readTimeout,
    Duration requestTimeout,
    @Nullable Executor executor,
    int maxConcurrentRequests,
    @Nullable Duration keepAliveTimeout) {

  /** Default connect timeout of 10 seconds. */
  public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);

  /** Default read timeout of 30 seconds. */
  public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);

  /** Default request timeout of 60 seconds. */
  public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(60);

  /** Default maximum concurrent requests (0 means unlimited). */
  public static final int DEFAULT_MAX_CONCURRENT_REQUESTS = 0;

  /** Default keep-alive timeout of 5 minutes. */
  public static final Duration DEFAULT_KEEP_ALIVE_TIMEOUT = Duration.ofMinutes(5);

  /**
   * Creates a new configuration with the specified settings.
   *
   * @param connectTimeout the maximum time to wait for connection establishment
   * @param readTimeout the maximum time to wait for response data
   * @param requestTimeout the maximum total time for the entire request
   * @param executor the executor for async operations, or null for default
   * @param maxConcurrentRequests the maximum concurrent requests, or 0 for unlimited
   * @param keepAliveTimeout the idle connection timeout, or null to use default
   * @throws NullPointerException if any required timeout is null
   * @throws IllegalArgumentException if any timeout is negative or maxConcurrentRequests is
   *     negative
   */
  public HttpTransportConfig {
    Objects.requireNonNull(connectTimeout, "connectTimeout must not be null");
    Objects.requireNonNull(readTimeout, "readTimeout must not be null");
    Objects.requireNonNull(requestTimeout, "requestTimeout must not be null");

    if (connectTimeout.isNegative()) {
      throw new IllegalArgumentException("connectTimeout must not be negative");
    }
    if (readTimeout.isNegative()) {
      throw new IllegalArgumentException("readTimeout must not be negative");
    }
    if (requestTimeout.isNegative()) {
      throw new IllegalArgumentException("requestTimeout must not be negative");
    }
    if (maxConcurrentRequests < 0) {
      throw new IllegalArgumentException("maxConcurrentRequests must not be negative");
    }
    if (keepAliveTimeout != null && keepAliveTimeout.isNegative()) {
      throw new IllegalArgumentException("keepAliveTimeout must not be negative");
    }
  }

  /**
   * Returns a configuration with default values.
   *
   * <p>Default values:
   *
   * <ul>
   *   <li>Connect timeout: 10 seconds
   *   <li>Read timeout: 30 seconds
   *   <li>Request timeout: 60 seconds
   *   <li>Executor: null (uses default)
   *   <li>Max concurrent requests: 0 (unlimited)
   *   <li>Keep-alive timeout: 5 minutes
   * </ul>
   *
   * @return a configuration with default values
   */
  public static HttpTransportConfig defaults() {
    return new HttpTransportConfig(
        DEFAULT_CONNECT_TIMEOUT,
        DEFAULT_READ_TIMEOUT,
        DEFAULT_REQUEST_TIMEOUT,
        null,
        DEFAULT_MAX_CONCURRENT_REQUESTS,
        DEFAULT_KEEP_ALIVE_TIMEOUT);
  }

  /**
   * Creates a new builder for constructing configurations.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder for creating {@link HttpTransportConfig} instances.
   *
   * <p>All values default to the standard defaults if not specified.
   */
  public static final class Builder {

    private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private Duration readTimeout = DEFAULT_READ_TIMEOUT;
    private Duration requestTimeout = DEFAULT_REQUEST_TIMEOUT;
    private Executor executor;
    private int maxConcurrentRequests = DEFAULT_MAX_CONCURRENT_REQUESTS;
    private Duration keepAliveTimeout = DEFAULT_KEEP_ALIVE_TIMEOUT;

    private Builder() {}

    /**
     * Sets the connect timeout.
     *
     * @param connectTimeout the maximum time to wait for connection establishment
     * @return this builder
     * @throws NullPointerException if connectTimeout is null
     */
    public Builder connectTimeout(Duration connectTimeout) {
      this.connectTimeout =
          Objects.requireNonNull(connectTimeout, "connectTimeout must not be null");
      return this;
    }

    /**
     * Sets the read timeout.
     *
     * @param readTimeout the maximum time to wait for response data
     * @return this builder
     * @throws NullPointerException if readTimeout is null
     */
    public Builder readTimeout(Duration readTimeout) {
      this.readTimeout = Objects.requireNonNull(readTimeout, "readTimeout must not be null");
      return this;
    }

    /**
     * Sets the request timeout.
     *
     * @param requestTimeout the maximum total time for the entire request
     * @return this builder
     * @throws NullPointerException if requestTimeout is null
     */
    public Builder requestTimeout(Duration requestTimeout) {
      this.requestTimeout =
          Objects.requireNonNull(requestTimeout, "requestTimeout must not be null");
      return this;
    }

    /**
     * Sets the executor for async operations.
     *
     * <p>If not specified, the default executor from HttpClient will be used.
     *
     * @param executor the executor to use for async operations, or null for default
     * @return this builder
     */
    public Builder executor(@Nullable Executor executor) {
      this.executor = executor;
      return this;
    }

    /**
     * Sets the maximum number of concurrent requests.
     *
     * <p>When set to a positive value, a semaphore will be used to limit the number of concurrent
     * requests. Set to 0 for unlimited concurrent requests (default).
     *
     * @param maxConcurrentRequests the maximum concurrent requests, or 0 for unlimited
     * @return this builder
     * @throws IllegalArgumentException if maxConcurrentRequests is negative
     */
    public Builder maxConcurrentRequests(int maxConcurrentRequests) {
      if (maxConcurrentRequests < 0) {
        throw new IllegalArgumentException("maxConcurrentRequests must not be negative");
      }
      this.maxConcurrentRequests = maxConcurrentRequests;
      return this;
    }

    /**
     * Sets the keep-alive timeout for idle connections.
     *
     * <p>Note: This setting is advisory. The underlying Java HttpClient manages connection pooling
     * internally, and this timeout affects how long connections are kept alive when idle.
     *
     * @param keepAliveTimeout the keep-alive timeout, or null to use default
     * @return this builder
     * @throws IllegalArgumentException if keepAliveTimeout is negative
     */
    public Builder keepAliveTimeout(@Nullable Duration keepAliveTimeout) {
      if (keepAliveTimeout != null && keepAliveTimeout.isNegative()) {
        throw new IllegalArgumentException("keepAliveTimeout must not be negative");
      }
      this.keepAliveTimeout = keepAliveTimeout;
      return this;
    }

    /**
     * Builds the configuration.
     *
     * @return the configured {@link HttpTransportConfig}
     * @throws IllegalArgumentException if any timeout is negative
     */
    public HttpTransportConfig build() {
      return new HttpTransportConfig(
          connectTimeout,
          readTimeout,
          requestTimeout,
          executor,
          maxConcurrentRequests,
          keepAliveTimeout);
    }
  }
}
