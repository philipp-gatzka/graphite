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

import io.github.graphite.exception.GraphiteConnectionException;
import io.github.graphite.exception.GraphiteException;
import io.github.graphite.exception.GraphiteServerException;
import io.github.graphite.exception.GraphiteTimeoutException;
import java.time.Duration;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Configuration for retry behavior on failed requests.
 *
 * <p>A retry policy defines:
 *
 * <ul>
 *   <li>Maximum number of retry attempts
 *   <li>Backoff strategy for calculating delays
 *   <li>Conditions for when to retry (which exceptions)
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * RetryPolicy policy = RetryPolicy.builder()
 *     .maxAttempts(3)
 *     .backoffStrategy(ExponentialBackoff.builder()
 *         .initialDelay(Duration.ofMillis(100))
 *         .maxDelay(Duration.ofSeconds(5))
 *         .build())
 *     .retryOn(GraphiteConnectionException.class)
 *     .retryOn(GraphiteTimeoutException.class)
 *     .build();
 *
 * if (policy.shouldRetry(exception, attemptNumber)) {
 *     Duration delay = policy.getDelay(attemptNumber);
 *     Thread.sleep(delay.toMillis());
 *     // retry the request
 * }
 * }</pre>
 *
 * @param maxAttempts the maximum number of retry attempts (excluding the initial request)
 * @param backoffStrategy the strategy for calculating delays between retries
 * @param retryPredicate predicate to determine if an exception should be retried
 * @see BackoffStrategy
 */
public record RetryPolicy(
    int maxAttempts, BackoffStrategy backoffStrategy, Predicate<Throwable> retryPredicate) {

  /** Default maximum retry attempts. */
  public static final int DEFAULT_MAX_ATTEMPTS = 3;

  /** Default set of retryable exception types. */
  public static final Set<Class<? extends Throwable>> DEFAULT_RETRYABLE_EXCEPTIONS =
      Set.of(GraphiteConnectionException.class, GraphiteTimeoutException.class);

  /**
   * Creates a new retry policy with the specified parameters.
   *
   * @param maxAttempts the maximum number of retry attempts
   * @param backoffStrategy the backoff strategy
   * @param retryPredicate predicate for retryable exceptions
   * @throws NullPointerException if backoffStrategy or retryPredicate is null
   * @throws IllegalArgumentException if maxAttempts is negative
   */
  public RetryPolicy {
    Objects.requireNonNull(backoffStrategy, "backoffStrategy must not be null");
    Objects.requireNonNull(retryPredicate, "retryPredicate must not be null");
    if (maxAttempts < 0) {
      throw new IllegalArgumentException("maxAttempts must not be negative");
    }
  }

  /**
   * Returns a disabled retry policy that never retries.
   *
   * @return a policy with no retries
   */
  public static RetryPolicy disabled() {
    return new RetryPolicy(0, BackoffStrategy.none(), throwable -> false);
  }

  /**
   * Returns a retry policy with default settings.
   *
   * <p>Default settings:
   *
   * <ul>
   *   <li>Max attempts: 3
   *   <li>Backoff: 100ms fixed delay
   *   <li>Retries: connection and timeout exceptions
   * </ul>
   *
   * @return a policy with default settings
   */
  public static RetryPolicy defaults() {
    return builder().build();
  }

  /**
   * Creates a new builder for constructing retry policies.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Determines if a failed request should be retried.
   *
   * @param throwable the exception that caused the failure
   * @param attempt the current attempt number (1-based)
   * @return {@code true} if the request should be retried
   */
  public boolean shouldRetry(Throwable throwable, int attempt) {
    if (attempt > maxAttempts) {
      return false;
    }
    return retryPredicate.test(throwable);
  }

  /**
   * Returns the delay before the specified retry attempt.
   *
   * @param attempt the retry attempt number (1-based)
   * @return the delay duration
   */
  public Duration getDelay(int attempt) {
    return backoffStrategy.calculateDelay(attempt);
  }

  /**
   * Returns whether retries are enabled.
   *
   * @return {@code true} if maxAttempts is greater than 0
   */
  public boolean isEnabled() {
    return maxAttempts > 0;
  }

  /** Builder for creating {@link RetryPolicy} instances. */
  public static final class Builder {

    private int maxAttempts = DEFAULT_MAX_ATTEMPTS;
    private BackoffStrategy backoffStrategy = BackoffStrategy.fixed(Duration.ofMillis(100));
    private Predicate<Throwable> retryPredicate = defaultRetryPredicate();

    private Builder() {}

    /**
     * Sets the maximum number of retry attempts.
     *
     * @param maxAttempts the maximum attempts (0 to disable retries)
     * @return this builder
     * @throws IllegalArgumentException if maxAttempts is negative
     */
    public Builder maxAttempts(int maxAttempts) {
      if (maxAttempts < 0) {
        throw new IllegalArgumentException("maxAttempts must not be negative");
      }
      this.maxAttempts = maxAttempts;
      return this;
    }

    /**
     * Sets the backoff strategy.
     *
     * @param backoffStrategy the strategy for calculating delays
     * @return this builder
     * @throws NullPointerException if backoffStrategy is null
     */
    public Builder backoffStrategy(BackoffStrategy backoffStrategy) {
      this.backoffStrategy =
          Objects.requireNonNull(backoffStrategy, "backoffStrategy must not be null");
      return this;
    }

    /**
     * Adds an exception type to retry on.
     *
     * <p>This replaces the default retry predicate with one that matches the specified exception
     * type (and any previously added types).
     *
     * @param exceptionType the exception class to retry on
     * @return this builder
     * @throws NullPointerException if exceptionType is null
     */
    public Builder retryOn(Class<? extends Throwable> exceptionType) {
      Objects.requireNonNull(exceptionType, "exceptionType must not be null");
      Predicate<Throwable> previous = this.retryPredicate;
      this.retryPredicate =
          throwable -> previous.test(throwable) || exceptionType.isInstance(throwable);
      return this;
    }

    /**
     * Sets a custom retry predicate.
     *
     * <p>This replaces any previously configured retry conditions.
     *
     * @param retryPredicate predicate to determine retryable exceptions
     * @return this builder
     * @throws NullPointerException if retryPredicate is null
     */
    public Builder retryPredicate(Predicate<Throwable> retryPredicate) {
      this.retryPredicate =
          Objects.requireNonNull(retryPredicate, "retryPredicate must not be null");
      return this;
    }

    /**
     * Configures to only retry on server errors (5xx status codes).
     *
     * @return this builder
     */
    public Builder retryOnServerErrors() {
      this.retryPredicate =
          throwable -> throwable instanceof GraphiteServerException gse && gse.isServerError();
      return this;
    }

    /**
     * Configures to retry on all GraphiteExceptions.
     *
     * @return this builder
     */
    public Builder retryOnAllErrors() {
      this.retryPredicate = throwable -> throwable instanceof GraphiteException;
      return this;
    }

    /**
     * Configures to never retry.
     *
     * @return this builder
     */
    public Builder noRetry() {
      this.maxAttempts = 0;
      this.retryPredicate = throwable -> false;
      return this;
    }

    /**
     * Builds the retry policy.
     *
     * @return the configured retry policy
     */
    public RetryPolicy build() {
      return new RetryPolicy(maxAttempts, backoffStrategy, retryPredicate);
    }

    private static Predicate<Throwable> defaultRetryPredicate() {
      return throwable ->
          DEFAULT_RETRYABLE_EXCEPTIONS.stream().anyMatch(clazz -> clazz.isInstance(throwable));
    }
  }
}
