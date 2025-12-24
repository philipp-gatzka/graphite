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

/**
 * Enumeration of timeout types that can occur during a GraphQL request.
 *
 * <p>Different timeout types indicate where in the request lifecycle the timeout occurred, which
 * can inform retry strategies:
 *
 * <ul>
 *   <li>{@link #CONNECT} - Timeout establishing the connection; safe to retry
 *   <li>{@link #READ} - Timeout reading the response; may need to verify server state
 *   <li>{@link #REQUEST} - Overall request timeout; may need to verify server state
 * </ul>
 *
 * @see GraphiteTimeoutException
 */
public enum TimeoutType {

  /**
   * Timeout occurred while establishing the TCP connection.
   *
   * <p>This timeout is safe to retry as no request was sent to the server.
   */
  CONNECT,

  /**
   * Timeout occurred while reading the response from the server.
   *
   * <p>The request was sent and may have been processed by the server. Retrying may result in
   * duplicate processing for non-idempotent operations.
   */
  READ,

  /**
   * Timeout for the overall request duration.
   *
   * <p>This covers the entire request lifecycle. The request may have been partially or fully
   * processed by the server.
   */
  REQUEST
}
