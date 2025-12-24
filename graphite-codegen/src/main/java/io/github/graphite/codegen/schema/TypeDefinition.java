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
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an object type definition in a GraphQL schema.
 *
 * <p>Object types are the primary building blocks of a GraphQL schema. They define a set of fields
 * that can be queried.
 *
 * <p>Example GraphQL:
 *
 * <pre>{@code
 * type User implements Node {
 *   id: ID!
 *   name: String!
 *   email: String
 *   posts: [Post!]!
 * }
 * }</pre>
 *
 * @param name the type name
 * @param description the type description, may be null
 * @param fields the fields defined on this type
 * @param interfaces the interfaces this type implements
 * @see FieldDefinition
 * @see InterfaceDefinition
 */
public record TypeDefinition(
    @NotNull String name,
    @Nullable String description,
    @NotNull List<FieldDefinition> fields,
    @NotNull List<String> interfaces) {

  /**
   * Creates a type definition without interfaces.
   *
   * @param name the type name
   * @param description the type description
   * @param fields the fields
   */
  public TypeDefinition(
      @NotNull String name, @Nullable String description, @NotNull List<FieldDefinition> fields) {
    this(name, description, fields, List.of());
  }

  /**
   * Returns whether this type implements any interfaces.
   *
   * @return true if this type implements interfaces
   */
  public boolean hasInterfaces() {
    return !interfaces.isEmpty();
  }

  /**
   * Finds a field by name.
   *
   * @param fieldName the field name to find
   * @return an Optional containing the field, or empty if not found
   */
  @NotNull
  public Optional<FieldDefinition> getField(@NotNull String fieldName) {
    return fields.stream().filter(f -> f.name().equals(fieldName)).findFirst();
  }
}
