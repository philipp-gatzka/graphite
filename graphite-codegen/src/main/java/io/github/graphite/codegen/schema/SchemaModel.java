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

import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a complete GraphQL schema model.
 *
 * <p>This is the top-level container for all type definitions parsed from a GraphQL introspection
 * result. It provides access to:
 *
 * <ul>
 *   <li>Root operation types (Query, Mutation, Subscription)
 *   <li>Object types
 *   <li>Enum types
 *   <li>Input object types
 *   <li>Interface types
 *   <li>Union types
 *   <li>Custom scalar types
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * SchemaParser parser = new SchemaParser();
 * SchemaModel schema = parser.parse(schemaFile);
 *
 * // Access the Query type
 * TypeDefinition queryType = schema.queryType();
 *
 * // Find a specific type
 * Optional<TypeDefinition> userType = schema.getType("User");
 * }</pre>
 *
 * @param queryType the root Query type (required)
 * @param mutationType the root Mutation type (optional)
 * @param subscriptionType the root Subscription type (optional)
 * @param types all object type definitions indexed by name
 * @param enums all enum type definitions indexed by name
 * @param inputTypes all input object type definitions indexed by name
 * @param interfaces all interface type definitions indexed by name
 * @param unions all union type definitions indexed by name
 * @param scalars all custom scalar type definitions indexed by name
 * @see SchemaParser
 */
public record SchemaModel(
    @NotNull TypeDefinition queryType,
    @Nullable TypeDefinition mutationType,
    @Nullable TypeDefinition subscriptionType,
    @NotNull Map<String, TypeDefinition> types,
    @NotNull Map<String, EnumDefinition> enums,
    @NotNull Map<String, InputTypeDefinition> inputTypes,
    @NotNull Map<String, InterfaceDefinition> interfaces,
    @NotNull Map<String, UnionDefinition> unions,
    @NotNull Map<String, ScalarDefinition> scalars) {

  /**
   * Returns whether this schema has a Mutation type.
   *
   * @return true if mutations are defined
   */
  public boolean hasMutationType() {
    return mutationType != null;
  }

  /**
   * Returns whether this schema has a Subscription type.
   *
   * @return true if subscriptions are defined
   */
  public boolean hasSubscriptionType() {
    return subscriptionType != null;
  }

  /**
   * Finds an object type by name.
   *
   * @param name the type name
   * @return an Optional containing the type, or empty if not found
   */
  @NotNull
  public Optional<TypeDefinition> getType(@NotNull String name) {
    return Optional.ofNullable(types.get(name));
  }

  /**
   * Finds an enum type by name.
   *
   * @param name the enum name
   * @return an Optional containing the enum, or empty if not found
   */
  @NotNull
  public Optional<EnumDefinition> getEnum(@NotNull String name) {
    return Optional.ofNullable(enums.get(name));
  }

  /**
   * Finds an input type by name.
   *
   * @param name the input type name
   * @return an Optional containing the input type, or empty if not found
   */
  @NotNull
  public Optional<InputTypeDefinition> getInputType(@NotNull String name) {
    return Optional.ofNullable(inputTypes.get(name));
  }

  /**
   * Finds an interface by name.
   *
   * @param name the interface name
   * @return an Optional containing the interface, or empty if not found
   */
  @NotNull
  public Optional<InterfaceDefinition> getInterface(@NotNull String name) {
    return Optional.ofNullable(interfaces.get(name));
  }

  /**
   * Finds a union by name.
   *
   * @param name the union name
   * @return an Optional containing the union, or empty if not found
   */
  @NotNull
  public Optional<UnionDefinition> getUnion(@NotNull String name) {
    return Optional.ofNullable(unions.get(name));
  }

  /**
   * Finds a scalar by name.
   *
   * @param name the scalar name
   * @return an Optional containing the scalar, or empty if not found
   */
  @NotNull
  public Optional<ScalarDefinition> getScalar(@NotNull String name) {
    return Optional.ofNullable(scalars.get(name));
  }

  /**
   * Returns whether a type with the given name is a scalar type.
   *
   * @param name the type name
   * @return true if the name refers to a scalar (built-in or custom)
   */
  public boolean isScalar(@NotNull String name) {
    return ScalarDefinition.BUILT_IN_SCALARS.contains(name) || scalars.containsKey(name);
  }

  /**
   * Returns whether a type with the given name is an enum type.
   *
   * @param name the type name
   * @return true if the name refers to an enum
   */
  public boolean isEnum(@NotNull String name) {
    return enums.containsKey(name);
  }

  /**
   * Returns whether a type with the given name is an input type.
   *
   * @param name the type name
   * @return true if the name refers to an input type
   */
  public boolean isInputType(@NotNull String name) {
    return inputTypes.containsKey(name);
  }

  /**
   * Returns whether a type with the given name is an object type.
   *
   * @param name the type name
   * @return true if the name refers to an object type
   */
  public boolean isObjectType(@NotNull String name) {
    return types.containsKey(name);
  }

  /**
   * Returns whether a type with the given name is an interface.
   *
   * @param name the type name
   * @return true if the name refers to an interface
   */
  public boolean isInterface(@NotNull String name) {
    return interfaces.containsKey(name);
  }

  /**
   * Returns whether a type with the given name is a union.
   *
   * @param name the type name
   * @return true if the name refers to a union
   */
  public boolean isUnion(@NotNull String name) {
    return unions.containsKey(name);
  }
}
