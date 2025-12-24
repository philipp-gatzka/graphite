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

/**
 * Strategy for calculating delay between retry attempts.
 *
 * <p>Implementations of this interface determine how long to wait before the next retry attempt.
 * Common strategies include:
 *
 * <ul>
 *   <li>Fixed delay - constant wait time between attempts
 *   <li>Exponential backoff - increasing delays with each attempt
 *   <li>Decorrelated jitter - randomized exponential backoff
 * </ul>
 *
 * <p>Example implementation:
 *
 * <pre>{@code
 * BackoffStrategy exponential = attempt ->
 *     Duration.ofMillis((long) Math.pow(2, attempt) * 100);
 * }</pre>
 *
 * <p>Example usage with retry:
 *
 * <pre>{@code
 * BackoffStrategy backoff = ExponentialBackoff.builder()
 *     .initialDelay(Duration.ofMillis(100))
 *     .maxDelay(Duration.ofSeconds(30))
 *     .multiplier(2.0)
 *     .build();
 *
 * Duration delay = backoff.calculateDelay(3); // 3rd retry attempt
 * }</pre>
 *
 * @see RetryPolicy
 */
@FunctionalInterface
public interface BackoffStrategy {

  /**
   * Calculates the delay before the specified retry attempt.
   *
   * <p>The attempt number starts at 1 for the first retry (i.e., the second overall request).
   * Implementations should ensure the returned duration is non-negative.
   *
   * @param attempt the retry attempt number (1-based)
   * @return the duration to wait before this retry attempt
   * @throws IllegalArgumentException if attempt is less than 1
   */
  Duration calculateDelay(int attempt);

  /**
   * Returns whether this strategy has reached its maximum delay.
   *
   * <p>This can be used to determine if further retries would result in the same delay, allowing
   * for optimization in retry logic.
   *
   * @param attempt the retry attempt number
   * @return {@code true} if the delay has reached its maximum
   */
  default boolean isAtMaxDelay(int attempt) {
    return false;
  }

  /**
   * Returns a strategy that adds random jitter to this strategy's delays.
   *
   * <p>Jitter helps prevent thundering herd problems when multiple clients retry simultaneously.
   * The jitter factor determines the maximum percentage variation applied to the delay.
   *
   * @param jitterFactor the maximum jitter as a fraction (0.0 to 1.0)
   * @return a new strategy with jitter applied
   * @throws IllegalArgumentException if jitterFactor is outside [0.0, 1.0]
   */
  default BackoffStrategy withJitter(double jitterFactor) {
    if (jitterFactor < 0.0 || jitterFactor > 1.0) {
      throw new IllegalArgumentException(
          "jitterFactor must be between 0.0 and 1.0, got: " + jitterFactor);
    }
    if (jitterFactor == 0.0) {
      return this;
    }
    return attempt -> {
      Duration baseDelay = this.calculateDelay(attempt);
      double jitter = 1.0 - (Math.random() * jitterFactor);
      long jitteredMillis = (long) (baseDelay.toMillis() * jitter);
      return Duration.ofMillis(jitteredMillis);
    };
  }

  /**
   * Creates a strategy with a fixed delay.
   *
   * @param delay the constant delay between attempts
   * @return a fixed backoff strategy
   * @throws NullPointerException if delay is null
   */
  static BackoffStrategy fixed(Duration delay) {
    if (delay == null) {
      throw new NullPointerException("delay must not be null");
    }
    return attempt -> delay;
  }

  /**
   * Creates a strategy with no delay between attempts.
   *
   * @return a zero-delay backoff strategy
   */
  static BackoffStrategy none() {
    return attempt -> Duration.ZERO;
  }
}
