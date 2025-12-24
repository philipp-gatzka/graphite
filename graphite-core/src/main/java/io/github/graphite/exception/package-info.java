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

/**
 * Exception hierarchy for the Graphite GraphQL client.
 *
 * <p>This package contains all exceptions that may be thrown by the Graphite client.
 * The exception hierarchy is organized as follows:
 *
 * <pre>
 * GraphiteException (base)
 * ├── GraphiteClientException (client-side errors)
 * │   ├── GraphiteConnectionException (connection failures)
 * │   ├── GraphiteTimeoutException (timeouts)
 * │   └── GraphiteRateLimitException (rate limit exceeded)
 * └── GraphiteServerException (server-side errors)
 *     └── GraphiteGraphQLException (GraphQL-level errors)
 * </pre>
 *
 * <p>All exceptions extend {@link java.lang.RuntimeException}, making them unchecked.
 * This design choice allows callers to catch specific exceptions when needed while
 * not forcing exception handling at every call site.
 *
 * @see io.github.graphite.exception.GraphiteException
 */
package io.github.graphite.exception;
