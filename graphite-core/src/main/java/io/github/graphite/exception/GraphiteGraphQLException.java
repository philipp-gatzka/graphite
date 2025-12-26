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

import io.github.graphite.GraphQLError;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Exception indicating GraphQL-level errors in the response.
 *
 * <p>This exception is thrown when the GraphQL server returns errors in the response's {@code
 * errors} field. Unlike HTTP-level errors, GraphQL errors may be partial - the response may contain
 * both data and errors.
 *
 * <p>The exception provides access to all errors returned by the server:
 *
 * <ul>
 *   <li>{@link #getErrors()} - The list of all GraphQL errors
 *   <li>{@link #getFirstError()} - Convenience method for the first error
 *   <li>{@link #hasErrorWithCode(String)} - Check if a specific error code is present
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try {
 *     client.execute(query);
 * } catch (GraphiteGraphQLException e) {
 *     for (GraphQLError error : e.getErrors()) {
 *         log.error("GraphQL error at {}: {}",
 *             error.getFormattedPath(), error.message());
 *     }
 *     if (e.hasErrorWithCode("VALIDATION_ERROR")) {
 *         // Handle validation errors
 *     }
 * }
 * }</pre>
 *
 * @see GraphQLError
 * @see GraphiteServerException
 */
public class GraphiteGraphQLException extends GraphiteServerException {

  private static final long serialVersionUID = 1L;

  private final ArrayList<GraphQLError> errors;

  /**
   * Constructs a new GraphQL exception with the specified errors.
   *
   * @param errors the list of GraphQL errors from the response
   * @throws IllegalArgumentException if errors is null or empty
   */
  public GraphiteGraphQLException(List<GraphQLError> errors) {
    this(errors, 200);
  }

  /**
   * Constructs a new GraphQL exception with errors and HTTP status code.
   *
   * @param errors the list of GraphQL errors from the response
   * @param statusCode the HTTP status code (usually 200 for GraphQL errors)
   * @throws IllegalArgumentException if errors is null or empty
   */
  public GraphiteGraphQLException(List<GraphQLError> errors, int statusCode) {
    super(buildMessage(errors), statusCode, null, "GRAPHQL_ERROR");
    if (errors == null || errors.isEmpty()) {
      throw new IllegalArgumentException("errors must not be null or empty");
    }
    this.errors = new ArrayList<>(errors);
  }

  /**
   * Returns the list of GraphQL errors from the response.
   *
   * <p>The returned list is unmodifiable.
   *
   * @return the list of errors, never null or empty
   */
  public List<GraphQLError> getErrors() {
    return Collections.unmodifiableList(errors);
  }

  /**
   * Returns the first error from the response.
   *
   * <p>This is a convenience method for cases where only the first error is needed.
   *
   * @return the first error
   */
  public GraphQLError getFirstError() {
    return errors.get(0);
  }

  /**
   * Returns the number of errors in the response.
   *
   * @return the error count
   */
  public int getErrorCount() {
    return errors.size();
  }

  /**
   * Checks if any error has the specified error code.
   *
   * <p>Error codes are typically found in the {@code extensions.code} field of each error.
   *
   * @param code the error code to check for
   * @return {@code true} if any error has the specified code
   */
  public boolean hasErrorWithCode(String code) {
    return errors.stream().anyMatch(error -> code.equals(error.getCode()));
  }

  /**
   * Returns all errors with the specified error code.
   *
   * @param code the error code to filter by
   * @return a list of errors with the specified code, may be empty
   */
  public List<GraphQLError> getErrorsWithCode(String code) {
    return errors.stream().filter(error -> code.equals(error.getCode())).toList();
  }

  /**
   * Returns all error messages as a newline-separated string.
   *
   * @return all error messages concatenated
   */
  public String getAllMessages() {
    return errors.stream().map(GraphQLError::message).collect(Collectors.joining("\n"));
  }

  private static String buildMessage(List<GraphQLError> errors) {
    if (errors == null || errors.isEmpty()) {
      return "GraphQL errors occurred";
    }
    if (errors.size() == 1) {
      return errors.get(0).message();
    }
    return errors.get(0).message() + " (and " + (errors.size() - 1) + " more errors)";
  }
}
