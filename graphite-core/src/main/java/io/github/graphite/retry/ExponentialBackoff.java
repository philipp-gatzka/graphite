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
 * A backoff strategy with exponentially increasing delays.
 *
 * <p>The delay for each attempt is calculated as:
 *
 * <pre>
 * delay = min(initialDelay * multiplier^(attempt-1), maxDelay)
 * </pre>
 *
 * <p>Example delays with default settings (100ms initial, 2x multiplier, 30s max):
 *
 * <ul>
 *   <li>Attempt 1: 100ms
 *   <li>Attempt 2: 200ms
 *   <li>Attempt 3: 400ms
 *   <li>Attempt 4: 800ms
 *   <li>Attempt 5: 1600ms
 *   <li>...
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * BackoffStrategy backoff = ExponentialBackoff.builder()
 *     .initialDelay(Duration.ofMillis(100))
 *     .maxDelay(Duration.ofSeconds(30))
 *     .multiplier(2.0)
 *     .build();
 *
 * Duration delay = backoff.calculateDelay(3); // 400ms
 * }</pre>
 *
 * @see BackoffStrategy
 * @see RetryPolicy
 */
public final class ExponentialBackoff implements BackoffStrategy {

  /** Default initial delay of 100 milliseconds. */
  public static final Duration DEFAULT_INITIAL_DELAY = Duration.ofMillis(100);

  /** Default maximum delay of 30 seconds. */
  public static final Duration DEFAULT_MAX_DELAY = Duration.ofSeconds(30);

  /** Default multiplier of 2.0 (doubling). */
  public static final double DEFAULT_MULTIPLIER = 2.0;

  private final Duration initialDelay;
  private final Duration maxDelay;
  private final double multiplier;

  private ExponentialBackoff(Duration initialDelay, Duration maxDelay, double multiplier) {
    this.initialDelay = initialDelay;
    this.maxDelay = maxDelay;
    this.multiplier = multiplier;
  }

  /**
   * Returns a strategy with default settings.
   *
   * <p>Default values:
   *
   * <ul>
   *   <li>Initial delay: 100ms
   *   <li>Max delay: 30s
   *   <li>Multiplier: 2.0
   * </ul>
   *
   * @return an exponential backoff with default settings
   */
  public static ExponentialBackoff defaults() {
    return new ExponentialBackoff(DEFAULT_INITIAL_DELAY, DEFAULT_MAX_DELAY, DEFAULT_MULTIPLIER);
  }

  /**
   * Creates a new builder for constructing exponential backoff strategies.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public Duration calculateDelay(int attempt) {
    if (attempt < 1) {
      throw new IllegalArgumentException("attempt must be at least 1, got: " + attempt);
    }

    // Calculate exponential delay: initialDelay * multiplier^(attempt-1)
    double delayMillis = initialDelay.toMillis() * Math.pow(multiplier, (double) attempt - 1);

    // Cap at max delay
    long cappedMillis = Math.min((long) delayMillis, maxDelay.toMillis());

    return Duration.ofMillis(cappedMillis);
  }

  @Override
  public boolean isAtMaxDelay(int attempt) {
    return calculateDelay(attempt).equals(maxDelay);
  }

  /**
   * Returns the initial delay.
   *
   * @return the initial delay
   */
  public Duration getInitialDelay() {
    return initialDelay;
  }

  /**
   * Returns the maximum delay.
   *
   * @return the maximum delay
   */
  public Duration getMaxDelay() {
    return maxDelay;
  }

  /**
   * Returns the multiplier.
   *
   * @return the multiplier
   */
  public double getMultiplier() {
    return multiplier;
  }

  /** Builder for creating {@link ExponentialBackoff} instances. */
  public static final class Builder {

    private Duration initialDelay = DEFAULT_INITIAL_DELAY;
    private Duration maxDelay = DEFAULT_MAX_DELAY;
    private double multiplier = DEFAULT_MULTIPLIER;

    private Builder() {}

    /**
     * Sets the initial delay for the first retry.
     *
     * @param initialDelay the initial delay
     * @return this builder
     * @throws NullPointerException if initialDelay is null
     * @throws IllegalArgumentException if initialDelay is negative or zero
     */
    public Builder initialDelay(Duration initialDelay) {
      Objects.requireNonNull(initialDelay, "initialDelay must not be null");
      if (initialDelay.isNegative() || initialDelay.isZero()) {
        throw new IllegalArgumentException("initialDelay must be positive");
      }
      this.initialDelay = initialDelay;
      return this;
    }

    /**
     * Sets the maximum delay cap.
     *
     * @param maxDelay the maximum delay
     * @return this builder
     * @throws NullPointerException if maxDelay is null
     * @throws IllegalArgumentException if maxDelay is negative or zero
     */
    public Builder maxDelay(Duration maxDelay) {
      Objects.requireNonNull(maxDelay, "maxDelay must not be null");
      if (maxDelay.isNegative() || maxDelay.isZero()) {
        throw new IllegalArgumentException("maxDelay must be positive");
      }
      this.maxDelay = maxDelay;
      return this;
    }

    /**
     * Sets the multiplier applied to the delay between attempts.
     *
     * @param multiplier the multiplier (must be greater than 1.0)
     * @return this builder
     * @throws IllegalArgumentException if multiplier is not greater than 1.0
     */
    public Builder multiplier(double multiplier) {
      if (multiplier <= 1.0) {
        throw new IllegalArgumentException(
            "multiplier must be greater than 1.0, got: " + multiplier);
      }
      this.multiplier = multiplier;
      return this;
    }

    /**
     * Builds the exponential backoff strategy.
     *
     * @return the configured strategy
     * @throws IllegalArgumentException if initialDelay is greater than maxDelay
     */
    public ExponentialBackoff build() {
      if (initialDelay.compareTo(maxDelay) > 0) {
        throw new IllegalArgumentException("initialDelay must not be greater than maxDelay");
      }
      return new ExponentialBackoff(initialDelay, maxDelay, multiplier);
    }
  }
}
