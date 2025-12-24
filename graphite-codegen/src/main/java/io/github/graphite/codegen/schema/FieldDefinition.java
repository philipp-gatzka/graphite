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

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a field definition on a GraphQL object or interface type.
 *
 * <p>Fields are the fundamental unit of data selection in GraphQL queries.
 *
 * <p>Example GraphQL:
 *
 * <pre>{@code
 * type User {
 *   id: ID!
 *   name: String!
 *   email: String
 *   posts(limit: Int): [Post!]!
 * }
 * }</pre>
 *
 * @param name the field name
 * @param description the field description, may be null
 * @param type the field type reference
 * @param arguments the field arguments, may be empty
 * @param isDeprecated whether the field is deprecated
 * @param deprecationReason the deprecation reason, may be null
 * @see TypeDefinition
 * @see InterfaceDefinition
 */
public record FieldDefinition(
    @NotNull String name,
    @Nullable String description,
    @NotNull TypeReference type,
    @NotNull List<ArgumentDefinition> arguments,
    boolean isDeprecated,
    @Nullable String deprecationReason) {

  /**
   * Creates a field definition without deprecation.
   *
   * @param name the field name
   * @param description the field description
   * @param type the field type
   * @param arguments the field arguments
   */
  public FieldDefinition(
      @NotNull String name,
      @Nullable String description,
      @NotNull TypeReference type,
      @NotNull List<ArgumentDefinition> arguments) {
    this(name, description, type, arguments, false, null);
  }

  /**
   * Returns whether this field accepts any arguments.
   *
   * @return true if the field has arguments
   */
  public boolean hasArguments() {
    return !arguments.isEmpty();
  }
}
