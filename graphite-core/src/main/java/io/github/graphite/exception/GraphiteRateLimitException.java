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
package io.github.graphite.exception;

import java.time.Duration;
import java.time.Instant;
import org.jetbrains.annotations.Nullable;

/**
 * Exception indicating that a rate limit has been exceeded.
 *
 * <p>This exception can be thrown in two scenarios:
 *
 * <ul>
 *   <li>Client-side rate limiting when the local rate limiter is exhausted
 *   <li>Server-side rate limiting when the server returns HTTP 429
 * </ul>
 *
 * <p>The exception provides information about when the request can be retried:
 *
 * <ul>
 *   <li>{@link #getRetryAfter()} - Duration to wait before retrying
 *   <li>{@link #getResetTime()} - Absolute time when the rate limit resets
 *   <li>{@link #getLimit()} - The rate limit ceiling
 *   <li>{@link #getRemaining()} - Remaining requests in the current window
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try {
 *     client.execute(query);
 * } catch (GraphiteRateLimitException e) {
 *     Duration waitTime = e.getRetryAfter();
 *     if (waitTime != null) {
 *         Thread.sleep(waitTime.toMillis());
 *         return client.execute(query); // Retry
 *     }
 *     throw e;
 * }
 * }</pre>
 *
 * @see GraphiteClientException
 */
public class GraphiteRateLimitException extends GraphiteClientException {

  private static final long serialVersionUID = 1L;

  @Nullable private final Duration retryAfter;

  @Nullable private final Instant resetTime;

  @Nullable private final Integer limit;

  @Nullable private final Integer remaining;

  /**
   * Constructs a new rate limit exception with the specified message.
   *
   * @param message the detail message describing the rate limit error
   */
  public GraphiteRateLimitException(String message) {
    this(message, null, null, null, null);
  }

  /**
   * Constructs a new rate limit exception with retry-after information.
   *
   * @param message the detail message describing the rate limit error
   * @param retryAfter the duration to wait before retrying, may be {@code null}
   */
  public GraphiteRateLimitException(String message, @Nullable Duration retryAfter) {
    this(message, retryAfter, null, null, null);
  }

  /**
   * Constructs a new rate limit exception with full details.
   *
   * @param message the detail message describing the rate limit error
   * @param retryAfter the duration to wait before retrying, may be {@code null}
   * @param resetTime the absolute time when the rate limit resets, may be {@code null}
   * @param limit the rate limit ceiling, may be {@code null}
   * @param remaining the remaining requests in the current window, may be {@code null}
   */
  public GraphiteRateLimitException(
      String message,
      @Nullable Duration retryAfter,
      @Nullable Instant resetTime,
      @Nullable Integer limit,
      @Nullable Integer remaining) {
    super(message, "RATE_LIMIT_EXCEEDED");
    this.retryAfter = retryAfter;
    this.resetTime = resetTime;
    this.limit = limit;
    this.remaining = remaining;
  }

  /**
   * Returns the recommended duration to wait before retrying the request.
   *
   * <p>This value is typically derived from the server's {@code Retry-After} header or the client's
   * rate limiter configuration.
   *
   * @return the duration to wait, or {@code null} if not available
   */
  @Nullable
  public Duration getRetryAfter() {
    return retryAfter;
  }

  /**
   * Returns the absolute time when the rate limit resets.
   *
   * <p>This is the earliest time at which a request is likely to succeed.
   *
   * @return the reset time, or {@code null} if not available
   */
  @Nullable
  public Instant getResetTime() {
    return resetTime;
  }

  /**
   * Returns the rate limit ceiling (maximum requests per window).
   *
   * <p>This value is typically provided by the server via rate limit headers such as {@code
   * X-RateLimit-Limit}.
   *
   * @return the limit, or {@code null} if not available
   */
  @Nullable
  public Integer getLimit() {
    return limit;
  }

  /**
   * Returns the number of remaining requests in the current window.
   *
   * <p>This value is typically provided by the server via rate limit headers such as {@code
   * X-RateLimit-Remaining}.
   *
   * @return the remaining requests, or {@code null} if not available
   */
  @Nullable
  public Integer getRemaining() {
    return remaining;
  }
}
