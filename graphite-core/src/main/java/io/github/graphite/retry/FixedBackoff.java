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
package io.github.graphite.retry;

import java.time.Duration;
import java.util.Objects;

/**
 * A backoff strategy with a constant delay between retry attempts.
 *
 * <p>Unlike exponential backoff, fixed backoff uses the same delay for every retry attempt. This is
 * useful when:
 *
 * <ul>
 *   <li>The server has a known recovery time
 *   <li>You want predictable retry behavior
 *   <li>Rate limiting requires evenly spaced retries
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * BackoffStrategy backoff = FixedBackoff.of(Duration.ofMillis(500));
 *
 * Duration delay = backoff.calculateDelay(1); // 500ms
 * Duration delay2 = backoff.calculateDelay(5); // 500ms
 * }</pre>
 *
 * <p>With jitter:
 *
 * <pre>{@code
 * BackoffStrategy backoff = FixedBackoff.of(Duration.ofSeconds(1))
 *     .withJitter(0.2); // +/- 20% variation
 * }</pre>
 *
 * @see BackoffStrategy
 * @see ExponentialBackoff
 * @see RetryPolicy
 */
public final class FixedBackoff implements BackoffStrategy {

  /** Default delay of 1 second. */
  public static final Duration DEFAULT_DELAY = Duration.ofSeconds(1);

  private final Duration delay;

  private FixedBackoff(Duration delay) {
    this.delay = delay;
  }

  /**
   * Creates a fixed backoff with the specified delay.
   *
   * @param delay the constant delay between retries
   * @return a fixed backoff strategy
   * @throws NullPointerException if delay is null
   * @throws IllegalArgumentException if delay is negative
   */
  public static FixedBackoff of(Duration delay) {
    Objects.requireNonNull(delay, "delay must not be null");
    if (delay.isNegative()) {
      throw new IllegalArgumentException("delay must not be negative");
    }
    return new FixedBackoff(delay);
  }

  /**
   * Returns a fixed backoff with the default delay of 1 second.
   *
   * @return a fixed backoff with default delay
   */
  public static FixedBackoff defaults() {
    return new FixedBackoff(DEFAULT_DELAY);
  }

  /**
   * Creates a fixed backoff with zero delay.
   *
   * <p>This is useful for testing or when immediate retries are desired.
   *
   * @return a zero-delay fixed backoff
   */
  public static FixedBackoff noDelay() {
    return new FixedBackoff(Duration.ZERO);
  }

  @Override
  public Duration calculateDelay(int attempt) {
    if (attempt < 1) {
      throw new IllegalArgumentException("attempt must be at least 1, got: " + attempt);
    }
    return delay;
  }

  @Override
  public boolean isAtMaxDelay(int attempt) {
    // Fixed backoff is always at its "max" since it never changes
    return true;
  }

  /**
   * Returns the configured delay.
   *
   * @return the delay duration
   */
  public Duration getDelay() {
    return delay;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    FixedBackoff that = (FixedBackoff) obj;
    return Objects.equals(delay, that.delay);
  }

  @Override
  public int hashCode() {
    return Objects.hash(delay);
  }

  @Override
  public String toString() {
    return "FixedBackoff[delay=" + delay + "]";
  }
}
