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
 * HTTP request and response interceptors for the Graphite client.
 *
 * <p>This package provides interceptor interfaces that allow customization of HTTP communication:
 *
 * <ul>
 *   <li>{@link io.github.graphite.interceptor.RequestInterceptor} - Intercept outgoing requests
 *   <li>{@link io.github.graphite.interceptor.ResponseInterceptor} - Intercept incoming responses
 * </ul>
 *
 * <p>Common use cases for interceptors:
 *
 * <ul>
 *   <li>Adding authentication headers dynamically
 *   <li>Logging request/response details
 *   <li>Adding tracing and correlation IDs
 *   <li>Transforming errors
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * RequestInterceptor authInterceptor = request ->
 *     request.withHeader("Authorization", "Bearer " + getToken());
 *
 * ResponseInterceptor loggingInterceptor = response -> {
 *     log.info("Status: {}", response.statusCode());
 *     return response;
 * };
 *
 * GraphiteClient client = GraphiteClient.builder()
 *     .endpoint("https://api.example.com/graphql")
 *     .requestInterceptor(authInterceptor)
 *     .responseInterceptor(loggingInterceptor)
 *     .build();
 * }</pre>
 *
 * @see io.github.graphite.GraphiteClientBuilder
 */
package io.github.graphite.interceptor;
