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

/**
 * Configuration for HTTP transport behavior.
 *
 * <p>This record encapsulates all configurable aspects of HTTP transport:
 *
 * <ul>
 *   <li>{@code connectTimeout} - Maximum time to establish a connection
 *   <li>{@code readTimeout} - Maximum time to wait for response data
 *   <li>{@code requestTimeout} - Maximum total time for the entire request
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * HttpTransportConfig config = HttpTransportConfig.builder()
 *     .connectTimeout(Duration.ofSeconds(10))
 *     .readTimeout(Duration.ofSeconds(30))
 *     .requestTimeout(Duration.ofSeconds(60))
 *     .build();
 * }</pre>
 *
 * @param connectTimeout the maximum time to wait for connection establishment
 * @param readTimeout the maximum time to wait for response data
 * @param requestTimeout the maximum total time for the entire request
 * @see HttpTransport
 * @see DefaultHttpTransport
 */
public record HttpTransportConfig(
    Duration connectTimeout, Duration readTimeout, Duration requestTimeout) {

  /** Default connect timeout of 10 seconds. */
  public static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);

  /** Default read timeout of 30 seconds. */
  public static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);

  /** Default request timeout of 60 seconds. */
  public static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(60);

  /**
   * Creates a new configuration with the specified timeouts.
   *
   * @param connectTimeout the maximum time to wait for connection establishment
   * @param readTimeout the maximum time to wait for response data
   * @param requestTimeout the maximum total time for the entire request
   * @throws NullPointerException if any timeout is null
   * @throws IllegalArgumentException if any timeout is negative
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
  }

  /**
   * Returns a configuration with default timeout values.
   *
   * <p>Default values:
   *
   * <ul>
   *   <li>Connect timeout: 10 seconds
   *   <li>Read timeout: 30 seconds
   *   <li>Request timeout: 60 seconds
   * </ul>
   *
   * @return a configuration with default values
   */
  public static HttpTransportConfig defaults() {
    return new HttpTransportConfig(
        DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT, DEFAULT_REQUEST_TIMEOUT);
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
   * <p>All timeout values default to the standard defaults if not specified.
   */
  public static final class Builder {

    private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private Duration readTimeout = DEFAULT_READ_TIMEOUT;
    private Duration requestTimeout = DEFAULT_REQUEST_TIMEOUT;

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
     * Builds the configuration.
     *
     * @return the configured {@link HttpTransportConfig}
     * @throws IllegalArgumentException if any timeout is negative
     */
    public HttpTransportConfig build() {
      return new HttpTransportConfig(connectTimeout, readTimeout, requestTimeout);
    }
  }
}
