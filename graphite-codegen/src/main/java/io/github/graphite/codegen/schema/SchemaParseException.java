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
package io.github.graphite.codegen.schema;

import io.github.graphite.codegen.CodegenException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exception thrown when parsing a GraphQL schema fails.
 *
 * <p>This exception is thrown when:
 *
 * <ul>
 *   <li>The schema JSON is malformed or cannot be parsed
 *   <li>Required fields are missing from the introspection result
 *   <li>Type references point to non-existent types
 *   <li>The schema structure is invalid
 * </ul>
 *
 * <p>Example:
 *
 * <pre>{@code
 * try {
 *     SchemaModel schema = parser.parse(schemaFile);
 * } catch (SchemaParseException e) {
 *     System.err.println("Schema parse error: " + e.getMessage());
 *     System.err.println("Location: " + e.getLocation());
 * }
 * }</pre>
 *
 * @see SchemaParser
 */
public class SchemaParseException extends CodegenException {

  private static final long serialVersionUID = 1L;

  private final String location;

  /**
   * Creates a new exception with the specified message.
   *
   * @param message the detail message
   */
  public SchemaParseException(@NotNull String message) {
    super(message);
    this.location = null;
  }

  /**
   * Creates a new exception with the specified message and location.
   *
   * @param message the detail message
   * @param location the location in the schema where the error occurred
   */
  public SchemaParseException(@NotNull String message, @Nullable String location) {
    super(message + (location != null ? " at " + location : ""));
    this.location = location;
  }

  /**
   * Creates a new exception with the specified message and cause.
   *
   * @param message the detail message
   * @param cause the underlying cause
   */
  public SchemaParseException(@NotNull String message, @Nullable Throwable cause) {
    super(message, cause);
    this.location = null;
  }

  /**
   * Creates a new exception with the specified message, location, and cause.
   *
   * @param message the detail message
   * @param location the location in the schema where the error occurred
   * @param cause the underlying cause
   */
  public SchemaParseException(
      @NotNull String message, @Nullable String location, @Nullable Throwable cause) {
    super(message + (location != null ? " at " + location : ""), cause);
    this.location = location;
  }

  /**
   * Returns the location in the schema where the error occurred.
   *
   * @return the location, or null if not available
   */
  @Nullable
  public String getLocation() {
    return location;
  }
}
