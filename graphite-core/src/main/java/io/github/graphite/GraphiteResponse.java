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

import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a response from a GraphQL operation.
 *
 * <p>A GraphQL response contains:
 *
 * <ul>
 *   <li>{@code data} - The result of the operation (may be {@code null})
 *   <li>{@code errors} - Any errors that occurred during execution
 *   <li>{@code extensions} - Optional metadata from the server
 * </ul>
 *
 * <p>According to the GraphQL specification, both {@code data} and {@code errors} may be present in
 * the same response. This can happen when some fields resolve successfully while others fail.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * GraphiteResponse<UserDTO> response = client.execute(query);
 *
 * if (response.hasErrors()) {
 *     for (GraphQLError error : response.errors()) {
 *         System.err.println("Error: " + error.message());
 *     }
 * }
 *
 * if (response.hasData()) {
 *     UserDTO user = response.data();
 *     System.out.println("User: " + user.name());
 * }
 * }</pre>
 *
 * @param <T> the type of data in this response
 * @param data the operation result, may be {@code null} if errors prevented execution
 * @param errors the list of errors, empty if no errors occurred
 * @param extensions optional server extensions metadata
 * @see GraphiteClient
 * @see GraphQLError
 */
public record GraphiteResponse<T>(
    @Nullable T data, @NotNull List<GraphQLError> errors, @NotNull Map<String, Object> extensions) {

  /**
   * Creates a response with the given data, errors, and extensions.
   *
   * @param data the operation result
   * @param errors the list of errors (must not be null)
   * @param extensions the server extensions (must not be null)
   */
  public GraphiteResponse {
    if (errors == null) {
      throw new NullPointerException("errors must not be null");
    }
    if (extensions == null) {
      throw new NullPointerException("extensions must not be null");
    }
  }

  /**
   * Creates a successful response with data only.
   *
   * @param <T> the data type
   * @param data the operation result
   * @return a response with the given data and no errors
   */
  @NotNull
  public static <T> GraphiteResponse<T> success(@Nullable T data) {
    return new GraphiteResponse<>(data, List.of(), Map.of());
  }

  /**
   * Creates an error response with the given errors.
   *
   * @param <T> the data type
   * @param errors the errors that occurred
   * @return a response with no data and the given errors
   */
  @NotNull
  public static <T> GraphiteResponse<T> error(@NotNull List<GraphQLError> errors) {
    return new GraphiteResponse<>(null, errors, Map.of());
  }

  /**
   * Returns whether this response has data.
   *
   * @return {@code true} if data is present, {@code false} otherwise
   */
  public boolean hasData() {
    return data != null;
  }

  /**
   * Returns whether this response has errors.
   *
   * @return {@code true} if there are errors, {@code false} otherwise
   */
  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  /**
   * Returns whether this response is successful (has data and no errors).
   *
   * @return {@code true} if successful, {@code false} otherwise
   */
  public boolean isSuccess() {
    return hasData() && !hasErrors();
  }

  /**
   * Returns the data or throws an exception if there are errors.
   *
   * <p>This method provides a convenient way to access the data when you expect the operation to
   * succeed. If there are any errors in the response, a {@link
   * io.github.graphite.exception.GraphiteGraphQLException} is thrown containing all the errors.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * // When you expect success
   * UserDTO user = client.execute(query).dataOrThrow();
   *
   * // Equivalent to:
   * GraphiteResponse<UserDTO> response = client.execute(query);
   * if (response.hasErrors()) {
   *     throw new GraphiteGraphQLException(response.errors());
   * }
   * return response.data();
   * }</pre>
   *
   * @return the data, may be {@code null} if the operation returned null data without errors
   * @throws io.github.graphite.exception.GraphiteGraphQLException if the response contains errors
   */
  @Nullable
  public T dataOrThrow() {
    if (hasErrors()) {
      throw new io.github.graphite.exception.GraphiteGraphQLException(errors);
    }
    return data;
  }
}
