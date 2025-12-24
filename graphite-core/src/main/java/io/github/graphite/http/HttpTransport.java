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
package io.github.graphite.http;

import io.github.graphite.exception.GraphiteException;
import java.util.concurrent.CompletableFuture;

/**
 * HTTP transport abstraction for sending GraphQL requests.
 *
 * <p>This interface abstracts the HTTP client implementation, allowing for:
 *
 * <ul>
 *   <li>Easy testing with mock implementations
 *   <li>Swapping HTTP client libraries
 *   <li>Custom transport implementations (e.g., for WebSocket)
 * </ul>
 *
 * <p>Implementations must be thread-safe and support concurrent requests. The transport is
 * responsible for:
 *
 * <ul>
 *   <li>Executing HTTP requests
 *   <li>Managing connection pools
 *   <li>Handling timeouts
 *   <li>Translating I/O errors to Graphite exceptions
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try (HttpTransport transport = new DefaultHttpTransport(config)) {
 *     HttpRequest request = HttpRequest.post(uri, headers, body);
 *     HttpResponse response = transport.execute(request);
 *     // Process response
 * }
 * }</pre>
 *
 * @see HttpRequest
 * @see HttpResponse
 * @see DefaultHttpTransport
 */
public interface HttpTransport extends AutoCloseable {

  /**
   * Executes an HTTP request synchronously.
   *
   * <p>This method blocks until the response is received or an error occurs. For non-blocking
   * execution, use {@link #executeAsync(HttpRequest)}.
   *
   * @param request the HTTP request to execute
   * @return the HTTP response
   * @throws GraphiteException if the request fails
   */
  HttpResponse execute(HttpRequest request) throws GraphiteException;

  /**
   * Executes an HTTP request asynchronously.
   *
   * <p>This method returns immediately with a {@link CompletableFuture} that will be completed when
   * the response is received or an error occurs.
   *
   * <p>The future will be completed exceptionally with a {@link GraphiteException} if the request
   * fails.
   *
   * @param request the HTTP request to execute
   * @return a future that will be completed with the HTTP response
   */
  CompletableFuture<HttpResponse> executeAsync(HttpRequest request);

  /**
   * Closes this transport and releases any resources.
   *
   * <p>After calling this method, the transport should not be used. Pending async requests may be
   * cancelled or allowed to complete, depending on the implementation.
   */
  @Override
  void close();
}
