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

import org.jetbrains.annotations.Nullable;

/**
 * Exception indicating an error that occurred on the client side before or during a GraphQL
 * request.
 *
 * <p>Client exceptions represent errors that occur in the Graphite client itself, such as network
 * failures, timeouts, or configuration issues. These are distinct from server exceptions, which
 * represent errors returned by the GraphQL server.
 *
 * <p>Common subclasses include:
 *
 * <ul>
 *   <li>{@link GraphiteConnectionException} - Connection establishment failures
 *   <li>{@link GraphiteTimeoutException} - Request or connection timeouts
 *   <li>{@link GraphiteRateLimitException} - Client-side rate limit exceeded
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try {
 *     client.execute(query);
 * } catch (GraphiteClientException e) {
 *     // Handle client-side errors (network, timeout, etc.)
 *     if (e instanceof GraphiteTimeoutException) {
 *         // Retry with backoff
 *     }
 * } catch (GraphiteServerException e) {
 *     // Handle server-side errors
 * }
 * }</pre>
 *
 * @see GraphiteConnectionException
 * @see GraphiteTimeoutException
 * @see GraphiteRateLimitException
 */
public class GraphiteClientException extends GraphiteException {

  private static final long serialVersionUID = 1L;

  /**
   * Constructs a new client exception with the specified message.
   *
   * @param message the detail message describing the error
   */
  public GraphiteClientException(String message) {
    super(message);
  }

  /**
   * Constructs a new client exception with the specified message and cause.
   *
   * @param message the detail message describing the error
   * @param cause the underlying cause of this exception, may be {@code null}
   */
  public GraphiteClientException(String message, @Nullable Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs a new client exception with the specified message and error code.
   *
   * @param message the detail message describing the error
   * @param errorCode an optional error code for programmatic error handling, may be {@code null}
   */
  public GraphiteClientException(String message, @Nullable String errorCode) {
    super(message, errorCode);
  }

  /**
   * Constructs a new client exception with the specified message, cause, and error code.
   *
   * @param message the detail message describing the error
   * @param cause the underlying cause of this exception, may be {@code null}
   * @param errorCode an optional error code for programmatic error handling, may be {@code null}
   */
  public GraphiteClientException(
      String message, @Nullable Throwable cause, @Nullable String errorCode) {
    super(message, cause, errorCode);
  }
}
