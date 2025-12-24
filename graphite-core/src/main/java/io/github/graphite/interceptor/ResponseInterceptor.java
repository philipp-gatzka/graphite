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

import io.github.graphite.http.HttpResponse;
import org.jetbrains.annotations.NotNull;

/**
 * Functional interface for intercepting incoming HTTP responses.
 *
 * <p>Response interceptors allow inspection and modification of responses received from the GraphQL
 * server. Common use cases include:
 *
 * <ul>
 *   <li>Logging response details
 *   <li>Extracting and storing metadata (e.g., rate limit headers)
 *   <li>Error transformation
 *   <li>Response caching
 * </ul>
 *
 * <p>Interceptors are called in the order they are registered and may modify the response by
 * returning a new response instance.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ResponseInterceptor loggingInterceptor = response -> {
 *     log.debug("Response status: {}", response.statusCode());
 *     return response;
 * };
 *
 * GraphiteClient client = GraphiteClient.builder()
 *     .endpoint("https://api.example.com/graphql")
 *     .responseInterceptor(loggingInterceptor)
 *     .build();
 * }</pre>
 *
 * @see RequestInterceptor
 */
@FunctionalInterface
public interface ResponseInterceptor {

  /**
   * Intercepts an incoming HTTP response.
   *
   * <p>Implementations may modify the response by returning a new instance with the desired
   * changes. The original response should not be mutated.
   *
   * @param response the incoming response
   * @return the modified response, or the same response if no changes are needed
   */
  @NotNull
  HttpResponse intercept(@NotNull HttpResponse response);

  /**
   * Returns a composed interceptor that first applies this interceptor and then applies the given
   * interceptor.
   *
   * @param after the interceptor to apply after this one
   * @return a composed interceptor
   */
  @NotNull
  default ResponseInterceptor andThen(@NotNull ResponseInterceptor after) {
    return response -> after.intercept(intercept(response));
  }

  /**
   * Returns an interceptor that does nothing (identity).
   *
   * @return a no-op interceptor
   */
  @NotNull
  static ResponseInterceptor identity() {
    return response -> response;
  }
}
