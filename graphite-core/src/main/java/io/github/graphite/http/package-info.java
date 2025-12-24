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
 * HTTP transport layer for the Graphite GraphQL client.
 *
 * <p>This package contains the HTTP transport abstraction and implementations:
 * <ul>
 *   <li>{@link io.github.graphite.http.HttpTransport} - Transport interface</li>
 *   <li>{@link io.github.graphite.http.HttpRequest} - Request record</li>
 *   <li>{@link io.github.graphite.http.HttpResponse} - Response record</li>
 *   <li>{@link io.github.graphite.http.HttpMethod} - HTTP method enum</li>
 * </ul>
 *
 * <p>The default implementation uses Java's built-in {@code java.net.http.HttpClient}
 * for zero external dependencies.
 *
 * @see io.github.graphite.http.HttpTransport
 */
package io.github.graphite.http;
