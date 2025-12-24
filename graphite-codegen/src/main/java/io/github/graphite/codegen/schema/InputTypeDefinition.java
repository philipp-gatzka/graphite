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
 * Represents an input object type definition in a GraphQL schema.
 *
 * <p>Input types are used for mutation and query arguments. Unlike object types, input types cannot
 * have circular references.
 *
 * <p>Example GraphQL:
 *
 * <pre>{@code
 * input CreateUserInput {
 *   name: String!
 *   email: String!
 *   role: UserRole = USER
 * }
 * }</pre>
 *
 * @param name the input type name
 * @param description the input type description, may be null
 * @param inputFields the input fields
 * @see InputFieldDefinition
 */
public record InputTypeDefinition(
    @NotNull String name,
    @Nullable String description,
    @NotNull List<InputFieldDefinition> inputFields) {

  /**
   * Finds an input field by name.
   *
   * @param fieldName the field name to find
   * @return an Optional containing the field, or empty if not found
   */
  @NotNull
  public Optional<InputFieldDefinition> getField(@NotNull String fieldName) {
    return inputFields.stream().filter(f -> f.name().equals(fieldName)).findFirst();
  }

  /**
   * Returns the list of required fields.
   *
   * @return the required fields
   */
  @NotNull
  public List<InputFieldDefinition> getRequiredFields() {
    return inputFields.stream().filter(InputFieldDefinition::isRequired).toList();
  }
}
