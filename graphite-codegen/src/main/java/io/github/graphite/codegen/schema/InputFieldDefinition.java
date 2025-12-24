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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an input field definition on a GraphQL input object type.
 *
 * <p>Input fields are used to construct input objects for mutations and query arguments.
 *
 * <p>Example GraphQL:
 *
 * <pre>{@code
 * input CreateUserInput {
 *   name: String!
 *   email: String!
 *   age: Int
 * }
 * }</pre>
 *
 * @param name the field name
 * @param description the field description, may be null
 * @param type the field type reference
 * @param defaultValue the default value as a JSON string, may be null
 * @see InputTypeDefinition
 */
public record InputFieldDefinition(
    @NotNull String name,
    @Nullable String description,
    @NotNull TypeReference type,
    @Nullable String defaultValue) {

  /**
   * Returns whether this field has a default value.
   *
   * @return true if a default value is defined
   */
  public boolean hasDefaultValue() {
    return defaultValue != null;
  }

  /**
   * Returns whether this field is required (non-null type without default).
   *
   * @return true if the field must be provided
   */
  public boolean isRequired() {
    return type.isNonNull() && !hasDefaultValue();
  }
}
