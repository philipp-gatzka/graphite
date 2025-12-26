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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.graphite.GraphQLError;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builder for constructing GraphQL responses in tests.
 *
 * <p>This class provides a fluent API for building GraphQL responses with data, errors, and
 * extensions. It can be used with {@link GraphiteMockServer} or standalone for unit testing.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Simple response with data
 * GraphiteResponseBuilder response = GraphiteResponseBuilder.success()
 *     .data("user", Map.of("id", "123", "name", "John"));
 *
 * // Response with errors
 * GraphiteResponseBuilder errorResponse = GraphiteResponseBuilder.withError("NOT_FOUND", "User not found")
 *     .withLocation(1, 10);
 *
 * // Partial response with data and errors
 * GraphiteResponseBuilder partial = GraphiteResponseBuilder.success()
 *     .data("user", Map.of("id", "123", "name", null))
 *     .error(GraphiteErrorBuilder.create("NULLABLE_FIELD", "Name is null")
 *         .path("user", "name")
 *         .build());
 * }</pre>
 *
 * @see GraphiteMockServer
 * @see GraphiteErrorBuilder
 */
public class GraphiteResponseBuilder {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final String ROOT_KEY = "_root";

  private final Map<String, Object> data;
  private final List<GraphQLError> errors;
  private final Map<String, Object> extensions;

  private GraphiteResponseBuilder() {
    this.data = new LinkedHashMap<>();
    this.errors = new ArrayList<>();
    this.extensions = new LinkedHashMap<>();
  }

  /**
   * Creates a new response builder for a successful response.
   *
   * @return a new response builder
   */
  @NotNull
  public static GraphiteResponseBuilder success() {
    return new GraphiteResponseBuilder();
  }

  /**
   * Creates a new response builder with an error.
   *
   * @param code the error code
   * @param message the error message
   * @return a new response builder with the error
   */
  @NotNull
  public static GraphiteResponseBuilder withError(@NotNull String code, @NotNull String message) {
    GraphiteResponseBuilder builder = new GraphiteResponseBuilder();
    builder.errors.add(GraphiteErrorBuilder.create(code, message).build());
    return builder;
  }

  /**
   * Creates a new response builder with a simple error message.
   *
   * @param message the error message
   * @return a new response builder with the error
   */
  @NotNull
  public static GraphiteResponseBuilder withError(@NotNull String message) {
    GraphiteResponseBuilder builder = new GraphiteResponseBuilder();
    builder.errors.add(GraphiteErrorBuilder.create(message).build());
    return builder;
  }

  /**
   * Creates a new response builder with null data (for queries that return null).
   *
   * @return a new response builder with null data
   */
  @NotNull
  public static GraphiteResponseBuilder nullData() {
    return new GraphiteResponseBuilder();
  }

  /**
   * Adds data to the response under the specified key.
   *
   * @param key the data key (e.g., "user", "users")
   * @param value the data value
   * @return this builder for chaining
   */
  @NotNull
  public GraphiteResponseBuilder data(@NotNull String key, @Nullable Object value) {
    this.data.put(key, value);
    return this;
  }

  /**
   * Sets the entire data object.
   *
   * @param data the data object
   * @return this builder for chaining
   */
  @NotNull
  public GraphiteResponseBuilder data(@Nullable Object data) {
    if (data instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> mapData = (Map<String, Object>) data;
      this.data.clear();
      this.data.putAll(mapData);
    } else if (data != null) {
      // For non-map data, we need to wrap it or handle specially
      this.data.clear();
      this.data.put(ROOT_KEY, data);
    }
    return this;
  }

  /**
   * Adds an error to the response.
   *
   * @param error the GraphQL error
   * @return this builder for chaining
   */
  @NotNull
  public GraphiteResponseBuilder error(@NotNull GraphQLError error) {
    this.errors.add(error);
    return this;
  }

  /**
   * Adds an error to the response using an error builder.
   *
   * @param errorBuilder the error builder
   * @return this builder for chaining
   */
  @NotNull
  public GraphiteResponseBuilder error(@NotNull GraphiteErrorBuilder errorBuilder) {
    this.errors.add(errorBuilder.build());
    return this;
  }

  /**
   * Adds multiple errors to the response.
   *
   * @param errors the errors to add
   * @return this builder for chaining
   */
  @NotNull
  public GraphiteResponseBuilder errors(@NotNull List<GraphQLError> errors) {
    this.errors.addAll(errors);
    return this;
  }

  /**
   * Adds an extension to the response.
   *
   * @param key the extension key
   * @param value the extension value
   * @return this builder for chaining
   */
  @NotNull
  public GraphiteResponseBuilder extension(@NotNull String key, @Nullable Object value) {
    this.extensions.put(key, value);
    return this;
  }

  /**
   * Sets multiple extensions at once.
   *
   * @param extensions the extensions map
   * @return this builder for chaining
   */
  @NotNull
  public GraphiteResponseBuilder extensions(@NotNull Map<String, Object> extensions) {
    this.extensions.putAll(extensions);
    return this;
  }

  /**
   * Returns the data map.
   *
   * @return the data map (may be empty but never null)
   */
  @NotNull
  public Map<String, Object> getData() {
    return new LinkedHashMap<>(data);
  }

  /**
   * Returns the data as an object suitable for use with GraphiteMockServer.
   *
   * @return the data object or null if empty
   */
  @Nullable
  public Object getDataObject() {
    if (data.isEmpty()) {
      return null;
    }
    if (data.size() == 1 && data.containsKey(ROOT_KEY)) {
      return data.get(ROOT_KEY);
    }
    return new LinkedHashMap<>(data);
  }

  /**
   * Returns the list of errors.
   *
   * @return the errors list (may be empty but never null)
   */
  @NotNull
  public List<GraphQLError> getErrors() {
    return new ArrayList<>(errors);
  }

  /**
   * Returns the extensions map.
   *
   * @return the extensions map (may be empty but never null)
   */
  @NotNull
  public Map<String, Object> getExtensions() {
    return new LinkedHashMap<>(extensions);
  }

  /**
   * Checks if this response has any data.
   *
   * @return true if data is present
   */
  public boolean hasData() {
    return !data.isEmpty();
  }

  /**
   * Checks if this response has any errors.
   *
   * @return true if errors are present
   */
  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  /**
   * Checks if this response has any extensions.
   *
   * @return true if extensions are present
   */
  public boolean hasExtensions() {
    return !extensions.isEmpty();
  }

  /**
   * Builds the response as a JSON string.
   *
   * @return the JSON response string
   * @throws RuntimeException if serialization fails
   */
  @NotNull
  public String toJson() {
    try {
      Map<String, Object> response = new LinkedHashMap<>();

      if (!data.isEmpty()) {
        if (data.size() == 1 && data.containsKey(ROOT_KEY)) {
          response.put("data", data.get(ROOT_KEY));
        } else {
          response.put("data", data);
        }
      } else {
        response.put("data", null);
      }

      if (!errors.isEmpty()) {
        response.put("errors", errors);
      }

      if (!extensions.isEmpty()) {
        response.put("extensions", extensions);
      }

      return OBJECT_MAPPER.writeValueAsString(response);
    } catch (JsonProcessingException e) {
      throw new GraphiteTestException("Failed to serialize response to JSON", e);
    }
  }

  /**
   * Builds the response as a map.
   *
   * @return the response as a map
   */
  @NotNull
  public Map<String, Object> toMap() {
    Map<String, Object> response = new LinkedHashMap<>();

    if (!data.isEmpty()) {
      if (data.size() == 1 && data.containsKey(ROOT_KEY)) {
        response.put("data", data.get(ROOT_KEY));
      } else {
        response.put("data", data);
      }
    } else {
      response.put("data", null);
    }

    if (!errors.isEmpty()) {
      response.put("errors", errors);
    }

    if (!extensions.isEmpty()) {
      response.put("extensions", extensions);
    }

    return response;
  }
}
