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
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Assertion helpers for testing GraphQL responses.
 *
 * <p>This class provides fluent assertion methods for verifying GraphQL response structure,
 * including data, errors, and extensions.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Assert on successful response
 * GraphiteAssertions.assertThat(response)
 *     .hasNoErrors()
 *     .hasData("user")
 *     .dataAt("user.name").isEqualTo("John");
 *
 * // Assert on error response
 * GraphiteAssertions.assertThat(response)
 *     .hasErrors()
 *     .errorAt(0).hasCode("NOT_FOUND")
 *     .errorAt(0).hasMessage("User not found");
 * }</pre>
 *
 * @see GraphiteResponseBuilder
 */
public class GraphiteAssertions {

  private static final String ERRORS_KEY = "errors";
  private static final String EXTENSIONS_KEY = "extensions";

  private GraphiteAssertions() {
    // Utility class
  }

  /**
   * Creates an assertion for a GraphQL response map.
   *
   * @param response the response map containing data, errors, and extensions
   * @return a new response assertion
   */
  @NotNull
  public static GraphiteResponseAssert assertThat(@NotNull Map<String, Object> response) {
    return new GraphiteResponseAssert(response);
  }

  /**
   * Creates an assertion for a GraphQL response builder.
   *
   * @param responseBuilder the response builder
   * @return a new response assertion
   */
  @NotNull
  public static GraphiteResponseAssert assertThat(
      @NotNull GraphiteResponseBuilder responseBuilder) {
    return new GraphiteResponseAssert(responseBuilder.toMap());
  }

  /**
   * Creates an assertion for a GraphQL error.
   *
   * @param error the GraphQL error
   * @return a new error assertion
   */
  @NotNull
  public static GraphiteErrorAssert assertThat(@NotNull GraphQLError error) {
    return new GraphiteErrorAssert(error);
  }

  /** Assertion class for GraphQL responses. */
  public static class GraphiteResponseAssert {

    private final Map<String, Object> response;

    private GraphiteResponseAssert(@NotNull Map<String, Object> response) {
      this.response = response;
    }

    /**
     * Asserts that the response has data.
     *
     * @return this assertion for chaining
     * @throws AssertionError if data is null or empty
     */
    @NotNull
    public GraphiteResponseAssert hasData() {
      Object data = response.get("data");
      if (data == null) {
        throw new AssertionError("Expected response to have data, but data was null");
      }
      return this;
    }

    /**
     * Asserts that the response has null data.
     *
     * @return this assertion for chaining
     * @throws AssertionError if data is not null
     */
    @NotNull
    public GraphiteResponseAssert hasNullData() {
      Object data = response.get("data");
      if (data != null) {
        throw new AssertionError("Expected response to have null data, but was: " + data);
      }
      return this;
    }

    /**
     * Asserts that the response has data at the specified key.
     *
     * @param key the data key
     * @return this assertion for chaining
     * @throws AssertionError if the key is not present in data
     */
    @NotNull
    public GraphiteResponseAssert hasData(@NotNull String key) {
      Object data = response.get("data");
      if (data == null) {
        throw new AssertionError("Expected data to contain key '" + key + "', but data was null");
      }
      if (!(data instanceof Map)) {
        throw new AssertionError(
            "Expected data to be a Map, but was: " + data.getClass().getSimpleName());
      }
      @SuppressWarnings("unchecked")
      Map<String, Object> dataMap = (Map<String, Object>) data;
      if (!dataMap.containsKey(key)) {
        throw new AssertionError(
            "Expected data to contain key '" + key + "', but keys were: " + dataMap.keySet());
      }
      return this;
    }

    /**
     * Gets the data value at the specified path for further assertions.
     *
     * @param path the path to the data (e.g., "user.name" or "users[0].id")
     * @return a data assertion for the value at the path
     */
    @NotNull
    public DataAssert dataAt(@NotNull String path) {
      Object data = response.get("data");
      Object value = navigatePath(data, path);
      return new DataAssert(value, path);
    }

    /**
     * Asserts that the response has no errors.
     *
     * @return this assertion for chaining
     * @throws AssertionError if errors are present
     */
    @NotNull
    public GraphiteResponseAssert hasNoErrors() {
      Object errors = response.get(ERRORS_KEY);
      if (errors != null) {
        if (errors instanceof List && !((List<?>) errors).isEmpty()) {
          throw new AssertionError("Expected no errors, but found: " + errors);
        }
      }
      return this;
    }

    /**
     * Asserts that the response has errors.
     *
     * @return this assertion for chaining
     * @throws AssertionError if no errors are present
     */
    @NotNull
    public GraphiteResponseAssert hasErrors() {
      Object errors = response.get(ERRORS_KEY);
      if (errors == null || (errors instanceof List && ((List<?>) errors).isEmpty())) {
        throw new AssertionError("Expected response to have errors, but found none");
      }
      return this;
    }

    /**
     * Asserts that the response has exactly the specified number of errors.
     *
     * @param count the expected error count
     * @return this assertion for chaining
     * @throws AssertionError if error count doesn't match
     */
    @NotNull
    public GraphiteResponseAssert hasErrorCount(int count) {
      Object errors = response.get(ERRORS_KEY);
      int actualCount = 0;
      if (errors instanceof List) {
        actualCount = ((List<?>) errors).size();
      }
      if (actualCount != count) {
        throw new AssertionError("Expected " + count + " errors, but found " + actualCount);
      }
      return this;
    }

    /**
     * Gets the error at the specified index for further assertions.
     *
     * @param index the error index
     * @return an error assertion for the error
     * @throws AssertionError if no error exists at the index
     */
    @NotNull
    public GraphiteErrorAssert errorAt(int index) {
      Object errors = response.get(ERRORS_KEY);
      if (!(errors instanceof List)) {
        throw new AssertionError("Expected errors to be a List, but was: " + errors);
      }
      List<?> errorList = (List<?>) errors;
      if (index < 0 || index >= errorList.size()) {
        throw new AssertionError(
            "Error index " + index + " out of bounds, only " + errorList.size() + " errors");
      }
      Object error = errorList.get(index);
      if (error instanceof GraphQLError graphQLError) {
        return new GraphiteErrorAssert(graphQLError);
      }
      if (error instanceof Map<?, ?> map) {
        @SuppressWarnings("unchecked")
        Map<String, Object> errorMap = (Map<String, Object>) map;
        return new GraphiteErrorAssert(mapToGraphQLError(errorMap));
      }
      throw new AssertionError("Unexpected error type: " + error.getClass());
    }

    /**
     * Asserts that the response has extensions.
     *
     * @return this assertion for chaining
     * @throws AssertionError if no extensions are present
     */
    @NotNull
    public GraphiteResponseAssert hasExtensions() {
      Object extensions = response.get(EXTENSIONS_KEY);
      if (extensions == null || (extensions instanceof Map && ((Map<?, ?>) extensions).isEmpty())) {
        throw new AssertionError("Expected response to have extensions, but found none");
      }
      return this;
    }

    /**
     * Asserts that the response has the specified extension.
     *
     * @param key the extension key
     * @return this assertion for chaining
     * @throws AssertionError if the extension is not present
     */
    @NotNull
    public GraphiteResponseAssert hasExtension(@NotNull String key) {
      Object extensions = response.get(EXTENSIONS_KEY);
      if (!(extensions instanceof Map)) {
        throw new AssertionError("Expected extensions to be present, but was: " + extensions);
      }
      @SuppressWarnings("unchecked")
      Map<String, Object> extMap = (Map<String, Object>) extensions;
      if (!extMap.containsKey(key)) {
        throw new AssertionError(
            "Expected extension '" + key + "', but keys were: " + extMap.keySet());
      }
      return this;
    }

    /**
     * Applies custom assertions to the response.
     *
     * @param assertions the custom assertions
     * @return this assertion for chaining
     */
    @NotNull
    public GraphiteResponseAssert satisfies(@NotNull Consumer<Map<String, Object>> assertions) {
      assertions.accept(response);
      return this;
    }

    @Nullable
    private Object navigatePath(@Nullable Object data, @NotNull String path) {
      if (data == null) {
        return null;
      }

      String[] parts = path.split("\\.");
      Object current = data;

      for (String part : parts) {
        if (current == null) {
          return null;
        }

        // Handle array access like "users[0]"
        if (part.contains("[")) {
          int bracketStart = part.indexOf('[');
          int bracketEnd = part.indexOf(']');
          String fieldName = part.substring(0, bracketStart);
          int index = Integer.parseInt(part.substring(bracketStart + 1, bracketEnd));

          if (current instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) current;
            current = map.get(fieldName);
          }

          if (current instanceof List) {
            List<?> list = (List<?>) current;
            if (index < list.size()) {
              current = list.get(index);
            } else {
              return null;
            }
          }
        } else {
          if (current instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) current;
            current = map.get(part);
          } else {
            return null;
          }
        }
      }

      return current;
    }

    @NotNull
    private GraphQLError mapToGraphQLError(@NotNull Map<String, Object> map) {
      String message = (String) map.getOrDefault("message", "");

      @SuppressWarnings("unchecked")
      List<Map<String, Object>> locationsRaw =
          (List<Map<String, Object>>) map.getOrDefault("locations", List.of());
      List<GraphQLError.Location> locations =
          locationsRaw.stream()
              .map(
                  loc ->
                      new GraphQLError.Location(
                          ((Number) loc.get("line")).intValue(),
                          ((Number) loc.get("column")).intValue()))
              .toList();

      @SuppressWarnings("unchecked")
      List<Object> path = (List<Object>) map.getOrDefault("path", List.of());

      @SuppressWarnings("unchecked")
      Map<String, Object> extensions =
          (Map<String, Object>) map.getOrDefault(EXTENSIONS_KEY, Map.of());

      return new GraphQLError(message, locations, path, extensions);
    }
  }

  /** Assertion class for data values. */
  public static class DataAssert {

    private final Object value;
    private final String path;

    private DataAssert(@Nullable Object value, @NotNull String path) {
      this.value = value;
      this.path = path;
    }

    /**
     * Asserts that the value is equal to the expected value.
     *
     * @param expected the expected value
     * @return this assertion for chaining
     * @throws AssertionError if values don't match
     */
    @NotNull
    public DataAssert isEqualTo(@Nullable Object expected) {
      if (value == null && expected == null) {
        return this;
      }
      if (value == null || !value.equals(expected)) {
        throw new AssertionError(
            "Expected data at '" + path + "' to be " + expected + ", but was: " + value);
      }
      return this;
    }

    /**
     * Asserts that the value is null.
     *
     * @return this assertion for chaining
     * @throws AssertionError if value is not null
     */
    @NotNull
    public DataAssert isNull() {
      if (value != null) {
        throw new AssertionError("Expected data at '" + path + "' to be null, but was: " + value);
      }
      return this;
    }

    /**
     * Asserts that the value is not null.
     *
     * @return this assertion for chaining
     * @throws AssertionError if value is null
     */
    @NotNull
    public DataAssert isNotNull() {
      if (value == null) {
        throw new AssertionError("Expected data at '" + path + "' to be not null, but was null");
      }
      return this;
    }

    /**
     * Asserts that the value is a list with the expected size.
     *
     * @param size the expected size
     * @return this assertion for chaining
     * @throws AssertionError if not a list or size doesn't match
     */
    @NotNull
    public DataAssert hasSize(int size) {
      if (!(value instanceof List)) {
        throw new AssertionError("Expected data at '" + path + "' to be a List, but was: " + value);
      }
      int actualSize = ((List<?>) value).size();
      if (actualSize != size) {
        throw new AssertionError(
            "Expected list at '" + path + "' to have size " + size + ", but was: " + actualSize);
      }
      return this;
    }

    /**
     * Asserts that the value is an empty list.
     *
     * @return this assertion for chaining
     * @throws AssertionError if not a list or not empty
     */
    @NotNull
    public DataAssert isEmpty() {
      if (!(value instanceof List)) {
        throw new AssertionError("Expected data at '" + path + "' to be a List, but was: " + value);
      }
      if (!((List<?>) value).isEmpty()) {
        throw new AssertionError(
            "Expected list at '"
                + path
                + "' to be empty, but had size: "
                + ((List<?>) value).size());
      }
      return this;
    }

    /**
     * Asserts that the value contains the expected item.
     *
     * @param item the expected item
     * @return this assertion for chaining
     * @throws AssertionError if not a list or doesn't contain item
     */
    @NotNull
    public DataAssert contains(@NotNull Object item) {
      if (!(value instanceof List)) {
        throw new AssertionError("Expected data at '" + path + "' to be a List, but was: " + value);
      }
      if (!((List<?>) value).contains(item)) {
        throw new AssertionError(
            "Expected list at '" + path + "' to contain " + item + ", but was: " + value);
      }
      return this;
    }

    /**
     * Returns the actual value for further custom assertions.
     *
     * @return the value
     */
    @Nullable
    public Object getValue() {
      return value;
    }
  }

  /** Assertion class for GraphQL errors. */
  public static class GraphiteErrorAssert {

    private final GraphQLError error;

    private GraphiteErrorAssert(@NotNull GraphQLError error) {
      this.error = error;
    }

    /**
     * Asserts that the error has the expected message.
     *
     * @param message the expected message
     * @return this assertion for chaining
     * @throws AssertionError if messages don't match
     */
    @NotNull
    public GraphiteErrorAssert hasMessage(@NotNull String message) {
      if (!error.message().equals(message)) {
        throw new AssertionError(
            "Expected error message '" + message + "', but was: '" + error.message() + "'");
      }
      return this;
    }

    /**
     * Asserts that the error message contains the expected text.
     *
     * @param text the expected text
     * @return this assertion for chaining
     * @throws AssertionError if message doesn't contain text
     */
    @NotNull
    public GraphiteErrorAssert messageContains(@NotNull String text) {
      if (!error.message().contains(text)) {
        throw new AssertionError(
            "Expected error message to contain '" + text + "', but was: '" + error.message() + "'");
      }
      return this;
    }

    /**
     * Asserts that the error has the expected code in extensions.
     *
     * @param code the expected code
     * @return this assertion for chaining
     * @throws AssertionError if code doesn't match
     */
    @NotNull
    public GraphiteErrorAssert hasCode(@NotNull String code) {
      Object actualCode = error.extensions().get("code");
      if (!code.equals(actualCode)) {
        throw new AssertionError(
            "Expected error code '" + code + "', but was: '" + actualCode + "'");
      }
      return this;
    }

    /**
     * Asserts that the error has a path.
     *
     * @return this assertion for chaining
     * @throws AssertionError if path is empty
     */
    @NotNull
    public GraphiteErrorAssert hasPath() {
      if (error.path().isEmpty()) {
        throw new AssertionError("Expected error to have a path, but path was empty");
      }
      return this;
    }

    /**
     * Asserts that the error has the expected path.
     *
     * @param segments the expected path segments
     * @return this assertion for chaining
     * @throws AssertionError if path doesn't match
     */
    @NotNull
    public GraphiteErrorAssert hasPath(@NotNull Object... segments) {
      List<Object> expectedPath = List.of(segments);
      if (!error.path().equals(expectedPath)) {
        throw new AssertionError(
            "Expected error path " + expectedPath + ", but was: " + error.path());
      }
      return this;
    }

    /**
     * Asserts that the error has locations.
     *
     * @return this assertion for chaining
     * @throws AssertionError if locations are empty
     */
    @NotNull
    public GraphiteErrorAssert hasLocations() {
      if (error.locations().isEmpty()) {
        throw new AssertionError("Expected error to have locations, but locations were empty");
      }
      return this;
    }

    /**
     * Asserts that the error has the expected extension.
     *
     * @param key the extension key
     * @param value the expected value
     * @return this assertion for chaining
     * @throws AssertionError if extension doesn't match
     */
    @NotNull
    public GraphiteErrorAssert hasExtension(@NotNull String key, @Nullable Object value) {
      Object actualValue = error.extensions().get(key);
      if (value == null && actualValue == null) {
        return this;
      }
      if (value == null || !value.equals(actualValue)) {
        throw new AssertionError(
            "Expected extension '" + key + "' to be " + value + ", but was: " + actualValue);
      }
      return this;
    }

    /**
     * Applies custom assertions to the error.
     *
     * @param assertions the custom assertions
     * @return this assertion for chaining
     */
    @NotNull
    public GraphiteErrorAssert satisfies(@NotNull Consumer<GraphQLError> assertions) {
      assertions.accept(error);
      return this;
    }

    /**
     * Returns the underlying error for further assertions.
     *
     * @return the GraphQL error
     */
    @NotNull
    public GraphQLError getError() {
      return error;
    }
  }
}
