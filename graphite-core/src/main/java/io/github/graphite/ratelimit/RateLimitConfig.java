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
package io.github.graphite.ratelimit;

/**
 * Configuration for rate limiting behavior.
 *
 * <p>This record encapsulates the configuration for a rate limiter:
 *
 * <ul>
 *   <li>{@code requestsPerSecond} - The sustained rate limit
 *   <li>{@code burstCapacity} - The maximum burst size allowed
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * RateLimitConfig config = RateLimitConfig.of(100, 150);
 * RateLimiter limiter = config.createLimiter();
 * }</pre>
 *
 * @param requestsPerSecond the maximum sustained requests per second
 * @param burstCapacity the maximum burst capacity
 * @see RateLimiter
 */
public record RateLimitConfig(double requestsPerSecond, int burstCapacity) {

  /** Default requests per second. */
  public static final double DEFAULT_REQUESTS_PER_SECOND = 100.0;

  /** Default burst capacity. */
  public static final int DEFAULT_BURST_CAPACITY = 150;

  /**
   * Creates a new rate limit configuration.
   *
   * @param requestsPerSecond the maximum sustained requests per second
   * @param burstCapacity the maximum burst capacity
   * @throws IllegalArgumentException if requestsPerSecond is not positive
   * @throws IllegalArgumentException if burstCapacity is not positive
   */
  public RateLimitConfig {
    if (requestsPerSecond <= 0) {
      throw new IllegalArgumentException("requestsPerSecond must be positive");
    }
    if (burstCapacity <= 0) {
      throw new IllegalArgumentException("burstCapacity must be positive");
    }
  }

  /**
   * Creates a configuration with the specified rates.
   *
   * @param requestsPerSecond the maximum sustained requests per second
   * @param burstCapacity the maximum burst capacity
   * @return a new configuration
   */
  public static RateLimitConfig of(double requestsPerSecond, int burstCapacity) {
    return new RateLimitConfig(requestsPerSecond, burstCapacity);
  }

  /**
   * Creates a configuration with the specified rate and matching burst capacity.
   *
   * @param requestsPerSecond the maximum requests per second (also used as burst capacity)
   * @return a new configuration
   */
  public static RateLimitConfig of(double requestsPerSecond) {
    return new RateLimitConfig(requestsPerSecond, (int) Math.ceil(requestsPerSecond));
  }

  /**
   * Returns a configuration with default values.
   *
   * <p>Default values:
   *
   * <ul>
   *   <li>Requests per second: 100
   *   <li>Burst capacity: 150
   * </ul>
   *
   * @return a configuration with defaults
   */
  public static RateLimitConfig defaults() {
    return new RateLimitConfig(DEFAULT_REQUESTS_PER_SECOND, DEFAULT_BURST_CAPACITY);
  }

  /**
   * Returns a disabled configuration that allows unlimited requests.
   *
   * @return an unlimited configuration
   */
  public static RateLimitConfig unlimited() {
    return new RateLimitConfig(Double.MAX_VALUE, Integer.MAX_VALUE);
  }

  /**
   * Creates a rate limiter with this configuration.
   *
   * @return a new rate limiter
   */
  public RateLimiter createLimiter() {
    return RateLimiter.builder()
        .requestsPerSecond(requestsPerSecond)
        .burstCapacity(burstCapacity)
        .build();
  }

  /**
   * Returns whether this configuration effectively disables rate limiting.
   *
   * @return {@code true} if rate limiting is effectively disabled
   */
  public boolean isUnlimited() {
    return requestsPerSecond >= Double.MAX_VALUE / 2;
  }
}
