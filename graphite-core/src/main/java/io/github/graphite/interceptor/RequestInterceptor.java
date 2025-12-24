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
package io.github.graphite.interceptor;

import io.github.graphite.http.HttpRequest;
import org.jetbrains.annotations.NotNull;

/**
 * Functional interface for intercepting outgoing HTTP requests.
 *
 * <p>Request interceptors allow modification of requests before they are sent to the GraphQL
 * server. Common use cases include:
 *
 * <ul>
 *   <li>Adding authentication headers
 *   <li>Adding tracing/correlation IDs
 *   <li>Logging request details
 *   <li>Modifying request content
 * </ul>
 *
 * <p>Interceptors are called in the order they are registered and may modify the request by
 * returning a new request instance.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * RequestInterceptor authInterceptor = request ->
 *     request.withHeader("Authorization", "Bearer " + tokenProvider.getToken());
 *
 * GraphiteClient client = GraphiteClient.builder()
 *     .endpoint("https://api.example.com/graphql")
 *     .requestInterceptor(authInterceptor)
 *     .build();
 * }</pre>
 *
 * @see ResponseInterceptor
 */
@FunctionalInterface
public interface RequestInterceptor {

  /**
   * Intercepts an outgoing HTTP request.
   *
   * <p>Implementations may modify the request by returning a new instance with the desired changes.
   * The original request should not be mutated.
   *
   * @param request the outgoing request
   * @return the modified request, or the same request if no changes are needed
   */
  @NotNull
  HttpRequest intercept(@NotNull HttpRequest request);

  /**
   * Returns a composed interceptor that first applies this interceptor and then applies the given
   * interceptor.
   *
   * @param after the interceptor to apply after this one
   * @return a composed interceptor
   */
  @NotNull
  default RequestInterceptor andThen(@NotNull RequestInterceptor after) {
    return request -> after.intercept(intercept(request));
  }

  /**
   * Returns an interceptor that does nothing (identity).
   *
   * @return a no-op interceptor
   */
  @NotNull
  static RequestInterceptor identity() {
    return request -> request;
  }
}
