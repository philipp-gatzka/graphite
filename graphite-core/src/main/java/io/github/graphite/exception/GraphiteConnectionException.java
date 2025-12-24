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
 * Exception indicating a connection failure to the GraphQL server.
 *
 * <p>This exception is thrown when the client cannot establish a connection to the GraphQL
 * endpoint. Common causes include:
 *
 * <ul>
 *   <li>DNS resolution failures (host not found)
 *   <li>Connection refused (server not listening)
 *   <li>Network unreachable
 *   <li>SSL/TLS handshake failures
 * </ul>
 *
 * <p>Connection exceptions are typically retryable after a delay, as the underlying network issue
 * may be transient.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try {
 *     client.execute(query);
 * } catch (GraphiteConnectionException e) {
 *     log.error("Failed to connect to {}: {}",
 *         e.getHost(), e.getMessage());
 *     // Retry with backoff
 * }
 * }</pre>
 *
 * @see GraphiteTimeoutException
 */
public class GraphiteConnectionException extends GraphiteClientException {

  private static final long serialVersionUID = 1L;

  @Nullable private final String host;

  @Nullable private final Integer port;

  /**
   * Constructs a new connection exception with the specified message.
   *
   * @param message the detail message describing the connection failure
   */
  public GraphiteConnectionException(String message) {
    this(message, null, null, null);
  }

  /**
   * Constructs a new connection exception with the specified message and cause.
   *
   * @param message the detail message describing the connection failure
   * @param cause the underlying cause (e.g., {@link java.net.ConnectException})
   */
  public GraphiteConnectionException(String message, @Nullable Throwable cause) {
    this(message, cause, null, null);
  }

  /**
   * Constructs a new connection exception with host and port information.
   *
   * @param message the detail message describing the connection failure
   * @param cause the underlying cause, may be {@code null}
   * @param host the host that could not be reached, may be {@code null}
   * @param port the port that could not be reached, may be {@code null}
   */
  public GraphiteConnectionException(
      String message, @Nullable Throwable cause, @Nullable String host, @Nullable Integer port) {
    super(message, cause, "CONNECTION_FAILED");
    this.host = host;
    this.port = port;
  }

  /**
   * Returns the host that could not be reached, if available.
   *
   * @return the host, or {@code null} if not specified
   */
  @Nullable
  public String getHost() {
    return host;
  }

  /**
   * Returns the port that could not be reached, if available.
   *
   * @return the port, or {@code null} if not specified
   */
  @Nullable
  public Integer getPort() {
    return port;
  }
}
