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
 * Represents an argument definition on a GraphQL field.
 *
 * <p>Arguments allow fields to accept parameters that modify their behavior or filter results.
 *
 * <p>Example GraphQL:
 *
 * <pre>{@code
 * type Query {
 *   user(id: ID!): User
 *   users(limit: Int = 10, offset: Int = 0): [User!]!
 * }
 * }</pre>
 *
 * @param name the argument name
 * @param description the argument description, may be null
 * @param type the argument type reference
 * @param defaultValue the default value as a JSON string, may be null
 * @see FieldDefinition
 */
public record ArgumentDefinition(
    @NotNull String name,
    @Nullable String description,
    @NotNull TypeReference type,
    @Nullable String defaultValue) {

  /**
   * Returns whether this argument has a default value.
   *
   * @return true if a default value is defined
   */
  public boolean hasDefaultValue() {
    return defaultValue != null;
  }

  /**
   * Returns whether this argument is required (non-null type without default).
   *
   * @return true if the argument must be provided
   */
  public boolean isRequired() {
    return type.isNonNull() && !hasDefaultValue();
  }
}
