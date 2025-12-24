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
package io.github.graphite;

import java.util.concurrent.CompletableFuture;
import org.jetbrains.annotations.NotNull;

/**
 * Client interface for executing GraphQL operations.
 *
 * <p>GraphiteClient provides a type-safe way to execute GraphQL queries and mutations against a
 * GraphQL server. It supports both synchronous and asynchronous execution modes.
 *
 * <p>The client handles:
 *
 * <ul>
 *   <li>HTTP transport to the GraphQL endpoint
 *   <li>Request/response serialization and deserialization
 *   <li>Error handling and exception mapping
 *   <li>Retry logic for transient failures
 *   <li>Rate limiting to prevent server overload
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create client using builder
 * GraphiteClient client = GraphiteClient.builder()
 *     .url("https://api.example.com/graphql")
 *     .header("Authorization", "Bearer " + token)
 *     .timeout(Duration.ofSeconds(30))
 *     .build();
 *
 * // Execute a query synchronously
 * GetUserQuery query = new GetUserQuery("123");
 * GraphiteResponse<UserDTO> response = client.execute(query);
 *
 * if (response.hasErrors()) {
 *     response.errors().forEach(System.err::println);
 * } else {
 *     UserDTO user = response.data();
 *     System.out.println("User: " + user.name());
 * }
 *
 * // Execute a mutation asynchronously
 * CreateUserMutation mutation = new CreateUserMutation("John Doe", "john@example.com");
 * client.executeAsync(mutation)
 *     .thenAccept(resp -> System.out.println("Created user: " + resp.data().id()))
 *     .exceptionally(ex -> {
 *         System.err.println("Failed to create user: " + ex.getMessage());
 *         return null;
 *     });
 *
 * // Close when done
 * client.close();
 * }</pre>
 *
 * <p>This interface extends {@link AutoCloseable}, allowing use with try-with-resources statements:
 *
 * <pre>{@code
 * try (GraphiteClient client = GraphiteClient.builder().url(url).build()) {
 *     GraphiteResponse<UserDTO> response = client.execute(new GetUserQuery("123"));
 *     // Process response...
 * }
 * }</pre>
 *
 * @see GraphQLOperation
 * @see GraphiteResponse
 * @see GraphiteClientBuilder
 */
public interface GraphiteClient extends AutoCloseable {

  /**
   * Executes a GraphQL operation synchronously.
   *
   * <p>This method blocks until the operation completes and returns the response. If the operation
   * fails due to network issues, timeouts, or server errors, an appropriate exception is thrown.
   *
   * <p>GraphQL-level errors (returned in the {@code errors} field of the response) do not cause
   * exceptions to be thrown. Instead, they are available via {@link GraphiteResponse#errors()}.
   *
   * @param <T> the type of data returned by the operation
   * @param operation the operation to execute
   * @return the response containing data and/or errors
   * @throws io.github.graphite.exception.GraphiteConnectionException if connection fails
   * @throws io.github.graphite.exception.GraphiteTimeoutException if the operation times out
   * @throws io.github.graphite.exception.GraphiteRateLimitException if rate limit is exceeded
   * @throws io.github.graphite.exception.GraphiteServerException for server errors (5xx)
   * @throws io.github.graphite.exception.GraphiteClientException for other client errors
   * @throws NullPointerException if operation is {@code null}
   */
  @NotNull
  <T> GraphiteResponse<T> execute(@NotNull GraphQLOperation<T> operation);

  /**
   * Executes a GraphQL operation asynchronously.
   *
   * <p>This method returns immediately with a {@link CompletableFuture} that will complete when the
   * operation finishes. The future may complete exceptionally with the same exceptions as {@link
   * #execute}.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * client.executeAsync(query)
   *     .thenApply(GraphiteResponse::data)
   *     .thenAccept(user -> System.out.println("Got user: " + user.name()))
   *     .exceptionally(ex -> {
   *         if (ex.getCause() instanceof GraphiteTimeoutException) {
   *             System.err.println("Request timed out");
   *         }
   *         return null;
   *     });
   * }</pre>
   *
   * @param <T> the type of data returned by the operation
   * @param operation the operation to execute
   * @return a future that completes with the response
   * @throws NullPointerException if operation is {@code null}
   */
  @NotNull
  <T> CompletableFuture<GraphiteResponse<T>> executeAsync(@NotNull GraphQLOperation<T> operation);

  /**
   * Closes this client and releases any associated resources.
   *
   * <p>After calling this method, any subsequent calls to {@link #execute} or {@link #executeAsync}
   * will result in undefined behavior.
   *
   * <p>This method is idempotent; calling it multiple times has no additional effect.
   */
  @Override
  void close();

  /**
   * Creates a new builder for constructing GraphiteClient instances.
   *
   * @return a new builder instance
   */
  @NotNull
  static GraphiteClientBuilder builder() {
    return new GraphiteClientBuilder();
  }
}
