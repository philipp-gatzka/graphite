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
 * Represents an interface type definition in a GraphQL schema.
 *
 * <p>Interfaces define a set of fields that implementing types must include.
 *
 * <p>Example GraphQL:
 *
 * <pre>{@code
 * interface Node {
 *   id: ID!
 * }
 *
 * interface Timestamped {
 *   createdAt: DateTime!
 *   updatedAt: DateTime!
 * }
 * }</pre>
 *
 * @param name the interface name
 * @param description the interface description, may be null
 * @param fields the fields defined on this interface
 * @param possibleTypes the names of types that implement this interface
 * @see TypeDefinition
 */
public record InterfaceDefinition(
    @NotNull String name,
    @Nullable String description,
    @NotNull List<FieldDefinition> fields,
    @NotNull List<String> possibleTypes) {

  /**
   * Creates an interface definition without known implementing types.
   *
   * @param name the interface name
   * @param description the interface description
   * @param fields the fields
   */
  public InterfaceDefinition(
      @NotNull String name, @Nullable String description, @NotNull List<FieldDefinition> fields) {
    this(name, description, fields, List.of());
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
