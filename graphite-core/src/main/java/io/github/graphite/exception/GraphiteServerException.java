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
 * Exception indicating an error returned by the GraphQL server.
 *
 * <p>Server exceptions represent errors that occur on the server side, typically indicated by HTTP
 * error status codes (4xx or 5xx) or GraphQL-level errors in the response. These are distinct from
 * client exceptions, which represent errors that occur before the request reaches the server.
 *
 * <p>This exception includes the HTTP status code when available, allowing callers to implement
 * status-specific error handling:
 *
 * <ul>
 *   <li>4xx status codes indicate client errors (bad request, unauthorized, etc.)
 *   <li>5xx status codes indicate server errors (internal error, service unavailable, etc.)
 * </ul>
 *
 * <p>Common subclasses include:
 *
 * <ul>
 *   <li>{@link GraphiteGraphQLException} - GraphQL-level errors in the response
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try {
 *     client.execute(query);
 * } catch (GraphiteServerException e) {
 *     if (e.getStatusCode() == 401) {
 *         // Handle authentication error
 *     } else if (e.getStatusCode() >= 500) {
 *         // Handle server error, maybe retry
 *     }
 * }
 * }</pre>
 *
 * @see GraphiteGraphQLException
 */
public class GraphiteServerException extends GraphiteException {

  private static final long serialVersionUID = 1L;

  private final int statusCode;

  /**
   * Constructs a new server exception with the specified message and status code.
   *
   * @param message the detail message describing the error
   * @param statusCode the HTTP status code returned by the server
   */
  public GraphiteServerException(String message, int statusCode) {
    this(message, statusCode, null, null);
  }

  /**
   * Constructs a new server exception with the specified message, status code, and cause.
   *
   * @param message the detail message describing the error
   * @param statusCode the HTTP status code returned by the server
   * @param cause the underlying cause of this exception, may be {@code null}
   */
  public GraphiteServerException(String message, int statusCode, @Nullable Throwable cause) {
    this(message, statusCode, cause, null);
  }

  /**
   * Constructs a new server exception with the specified message, status code, cause, and error
   * code.
   *
   * @param message the detail message describing the error
   * @param statusCode the HTTP status code returned by the server
   * @param cause the underlying cause of this exception, may be {@code null}
   * @param errorCode an optional error code for programmatic error handling, may be {@code null}
   */
  public GraphiteServerException(
      String message, int statusCode, @Nullable Throwable cause, @Nullable String errorCode) {
    super(message, cause, errorCode);
    this.statusCode = statusCode;
  }

  /**
   * Returns the HTTP status code returned by the server.
   *
   * <p>Common status codes include:
   *
   * <ul>
   *   <li>400 - Bad Request
   *   <li>401 - Unauthorized
   *   <li>403 - Forbidden
   *   <li>404 - Not Found
   *   <li>429 - Too Many Requests
   *   <li>500 - Internal Server Error
   *   <li>502 - Bad Gateway
   *   <li>503 - Service Unavailable
   * </ul>
   *
   * @return the HTTP status code
   */
  public int getStatusCode() {
    return statusCode;
  }

  /**
   * Returns whether this error represents a client error (4xx status code).
   *
   * @return {@code true} if the status code is in the 4xx range
   */
  public boolean isClientError() {
    return statusCode >= 400 && statusCode < 500;
  }

  /**
   * Returns whether this error represents a server error (5xx status code).
   *
   * @return {@code true} if the status code is in the 5xx range
   */
  public boolean isServerError() {
    return statusCode >= 500 && statusCode < 600;
  }
}
