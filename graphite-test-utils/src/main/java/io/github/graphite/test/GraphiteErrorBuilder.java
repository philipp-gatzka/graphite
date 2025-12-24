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
package io.github.graphite.test;

import io.github.graphite.GraphQLError;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builder for constructing GraphQL errors in tests.
 *
 * <p>This class provides a fluent API for building GraphQL errors with messages, locations, paths,
 * and extensions. It follows the GraphQL specification for error format.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Simple error
 * GraphQLError error = GraphiteErrorBuilder.create("User not found").build();
 *
 * // Error with code and location
 * GraphQLError error = GraphiteErrorBuilder.create("NOT_FOUND", "User not found")
 *     .location(1, 10)
 *     .path("user", "profile")
 *     .build();
 *
 * // Error with custom extensions
 * GraphQLError error = GraphiteErrorBuilder.create("VALIDATION_ERROR", "Invalid email")
 *     .extension("field", "email")
 *     .extension("constraint", "email")
 *     .build();
 * }</pre>
 *
 * @see GraphiteResponseBuilder
 * @see GraphQLError
 */
public class GraphiteErrorBuilder {

  private final String message;
  private final List<GraphQLError.Location> locations;
  private final List<Object> path;
  private final Map<String, Object> extensions;

  private GraphiteErrorBuilder(@NotNull String message) {
    this.message = message;
    this.locations = new ArrayList<>();
    this.path = new ArrayList<>();
    this.extensions = new LinkedHashMap<>();
  }

  /**
   * Creates a new error builder with the given message.
   *
   * @param message the error message
   * @return a new error builder
   */
  @NotNull
  public static GraphiteErrorBuilder create(@NotNull String message) {
    return new GraphiteErrorBuilder(message);
  }

  /**
   * Creates a new error builder with a code and message.
   *
   * <p>The code is added to the extensions under the "code" key.
   *
   * @param code the error code
   * @param message the error message
   * @return a new error builder
   */
  @NotNull
  public static GraphiteErrorBuilder create(@NotNull String code, @NotNull String message) {
    return new GraphiteErrorBuilder(message).extension("code", code);
  }

  /**
   * Creates an error builder for a "not found" error.
   *
   * @param entity the entity type that was not found
   * @param id the ID that was not found
   * @return a new error builder
   */
  @NotNull
  public static GraphiteErrorBuilder notFound(@NotNull String entity, @NotNull String id) {
    return create("NOT_FOUND", entity + " with id '" + id + "' not found")
        .extension("entity", entity)
        .extension("id", id);
  }

  /**
   * Creates an error builder for a validation error.
   *
   * @param field the field that failed validation
   * @param message the validation error message
   * @return a new error builder
   */
  @NotNull
  public static GraphiteErrorBuilder validation(@NotNull String field, @NotNull String message) {
    return create("VALIDATION_ERROR", message).extension("field", field);
  }

  /**
   * Creates an error builder for an authentication error.
   *
   * @return a new error builder
   */
  @NotNull
  public static GraphiteErrorBuilder unauthenticated() {
    return create("UNAUTHENTICATED", "Authentication required");
  }

  /**
   * Creates an error builder for an authorization error.
   *
   * @return a new error builder
   */
  @NotNull
  public static GraphiteErrorBuilder forbidden() {
    return create("FORBIDDEN", "Access denied");
  }

  /**
   * Creates an error builder for a rate limit error.
   *
   * @param retryAfterSeconds seconds until retry is allowed
   * @return a new error builder
   */
  @NotNull
  public static GraphiteErrorBuilder rateLimited(int retryAfterSeconds) {
    return create("RATE_LIMITED", "Rate limit exceeded").extension("retryAfter", retryAfterSeconds);
  }

  /**
   * Creates an error builder for an internal server error.
   *
   * @return a new error builder
   */
  @NotNull
  public static GraphiteErrorBuilder internalError() {
    return create("INTERNAL_ERROR", "Internal server error");
  }

  /**
   * Adds a location to the error.
   *
   * @param line the line number (1-indexed)
   * @param column the column number (1-indexed)
   * @return this builder for chaining
   */
  @NotNull
  public GraphiteErrorBuilder location(int line, int column) {
    this.locations.add(new GraphQLError.Location(line, column));
    return this;
  }

  /**
   * Adds a path segment to the error path.
   *
   * @param segments the path segments (field names or array indices)
   * @return this builder for chaining
   */
  @NotNull
  public GraphiteErrorBuilder path(@NotNull Object... segments) {
    this.path.addAll(Arrays.asList(segments));
    return this;
  }

  /**
   * Adds a path from a list.
   *
   * @param path the path segments
   * @return this builder for chaining
   */
  @NotNull
  public GraphiteErrorBuilder path(@NotNull List<Object> path) {
    this.path.addAll(path);
    return this;
  }

  /**
   * Adds an extension to the error.
   *
   * @param key the extension key
   * @param value the extension value
   * @return this builder for chaining
   */
  @NotNull
  public GraphiteErrorBuilder extension(@NotNull String key, @Nullable Object value) {
    this.extensions.put(key, value);
    return this;
  }

  /**
   * Adds multiple extensions to the error.
   *
   * @param extensions the extensions map
   * @return this builder for chaining
   */
  @NotNull
  public GraphiteErrorBuilder extensions(@NotNull Map<String, Object> extensions) {
    this.extensions.putAll(extensions);
    return this;
  }

  /**
   * Adds a classification extension to the error.
   *
   * <p>Common classifications include: "ValidationError", "DataFetchingException",
   * "OperationNotSupported".
   *
   * @param classification the error classification
   * @return this builder for chaining
   */
  @NotNull
  public GraphiteErrorBuilder classification(@NotNull String classification) {
    return extension("classification", classification);
  }

  /**
   * Returns the error message.
   *
   * @return the message
   */
  @NotNull
  public String getMessage() {
    return message;
  }

  /**
   * Returns the locations.
   *
   * @return the locations list
   */
  @NotNull
  public List<GraphQLError.Location> getLocations() {
    return new ArrayList<>(locations);
  }

  /**
   * Returns the path.
   *
   * @return the path list
   */
  @NotNull
  public List<Object> getPath() {
    return new ArrayList<>(path);
  }

  /**
   * Returns the extensions.
   *
   * @return the extensions map
   */
  @NotNull
  public Map<String, Object> getExtensions() {
    return new LinkedHashMap<>(extensions);
  }

  /**
   * Builds the GraphQL error.
   *
   * @return the constructed GraphQL error
   */
  @NotNull
  public GraphQLError build() {
    return new GraphQLError(
        message,
        locations.isEmpty() ? List.of() : new ArrayList<>(locations),
        path.isEmpty() ? List.of() : new ArrayList<>(path),
        extensions.isEmpty() ? Map.of() : new LinkedHashMap<>(extensions));
  }
}
