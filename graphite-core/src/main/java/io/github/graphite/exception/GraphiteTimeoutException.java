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
import org.jetbrains.annotations.Nullable;

/**
 * Exception indicating a timeout occurred during a GraphQL request.
 *
 * <p>This exception is thrown when a configured timeout is exceeded. The {@link #getTimeoutType()}
 * method indicates which timeout was exceeded:
 *
 * <ul>
 *   <li>{@link TimeoutType#CONNECT} - Connection establishment timeout
 *   <li>{@link TimeoutType#READ} - Response read timeout
 *   <li>{@link TimeoutType#REQUEST} - Overall request timeout
 * </ul>
 *
 * <p>Timeout exceptions may be retryable depending on the timeout type and operation idempotency.
 * Connect timeouts are generally safe to retry, while read and request timeouts may require
 * verification of server state for non-idempotent operations.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try {
 *     client.execute(query);
 * } catch (GraphiteTimeoutException e) {
 *     if (e.getTimeoutType() == TimeoutType.CONNECT) {
 *         // Safe to retry - no request was sent
 *         return retry(query);
 *     } else {
 *         // Request may have been processed
 *         log.warn("Request timed out after {}", e.getConfiguredTimeout());
 *     }
 * }
 * }</pre>
 *
 * @see TimeoutType
 * @see GraphiteConnectionException
 */
public class GraphiteTimeoutException extends GraphiteClientException {

  private static final long serialVersionUID = 1L;

  private final TimeoutType timeoutType;

  @Nullable private final Duration configuredTimeout;

  @Nullable private final Duration elapsedTime;

  /**
   * Constructs a new timeout exception with the specified message and timeout type.
   *
   * @param message the detail message describing the timeout
   * @param timeoutType the type of timeout that occurred
   */
  public GraphiteTimeoutException(String message, TimeoutType timeoutType) {
    this(message, timeoutType, null, null, null);
  }

  /**
   * Constructs a new timeout exception with message, type, and cause.
   *
   * @param message the detail message describing the timeout
   * @param timeoutType the type of timeout that occurred
   * @param cause the underlying cause (e.g., {@link java.net.SocketTimeoutException})
   */
  public GraphiteTimeoutException(
      String message, TimeoutType timeoutType, @Nullable Throwable cause) {
    this(message, timeoutType, null, null, cause);
  }

  /**
   * Constructs a new timeout exception with full details.
   *
   * @param message the detail message describing the timeout
   * @param timeoutType the type of timeout that occurred
   * @param configuredTimeout the configured timeout duration, may be {@code null}
   * @param elapsedTime the actual elapsed time before timeout, may be {@code null}
   * @param cause the underlying cause, may be {@code null}
   */
  public GraphiteTimeoutException(
      String message,
      TimeoutType timeoutType,
      @Nullable Duration configuredTimeout,
      @Nullable Duration elapsedTime,
      @Nullable Throwable cause) {
    super(message, cause, "TIMEOUT_" + timeoutType.name());
    this.timeoutType = timeoutType;
    this.configuredTimeout = configuredTimeout;
    this.elapsedTime = elapsedTime;
  }

  /**
   * Returns the type of timeout that occurred.
   *
   * @return the timeout type
   */
  public TimeoutType getTimeoutType() {
    return timeoutType;
  }

  /**
   * Returns the configured timeout duration, if available.
   *
   * @return the configured timeout, or {@code null} if not available
   */
  @Nullable
  public Duration getConfiguredTimeout() {
    return configuredTimeout;
  }

  /**
   * Returns the actual elapsed time before the timeout occurred, if available.
   *
   * <p>This may be slightly different from the configured timeout due to measurement granularity
   * and system scheduling.
   *
   * @return the elapsed time, or {@code null} if not available
   */
  @Nullable
  public Duration getElapsedTime() {
    return elapsedTime;
  }

  /**
   * Returns whether this timeout is safe to retry without risk of duplicate processing.
   *
   * <p>Only {@link TimeoutType#CONNECT} timeouts are considered safe to retry, as no request was
   * sent to the server. Other timeout types may have resulted in the server processing the request.
   *
   * @return {@code true} if the timeout occurred before any request was sent
   */
  public boolean isSafeToRetry() {
    return timeoutType == TimeoutType.CONNECT;
  }
}
