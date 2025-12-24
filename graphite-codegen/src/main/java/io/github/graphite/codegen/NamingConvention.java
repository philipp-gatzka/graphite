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
package io.github.graphite.codegen;

import org.jetbrains.annotations.NotNull;

/**
 * Defines the naming convention for generated Java classes.
 *
 * <p>This interface allows customization of how GraphQL type names are transformed into Java class
 * names. Different suffixes can be applied to different categories of types.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * NamingConvention convention = NamingConvention.defaults();
 * String dtoName = convention.getTypeName("User"); // Returns "UserDTO"
 * String inputName = convention.getInputTypeName("CreateUser"); // Returns "CreateUserInput"
 * }</pre>
 *
 * @see CodegenConfiguration
 */
public interface NamingConvention {

  /**
   * Returns the Java class name for a GraphQL object type.
   *
   * @param graphQLName the GraphQL type name
   * @return the Java class name (e.g., "UserDTO")
   */
  @NotNull
  String getTypeName(@NotNull String graphQLName);

  /**
   * Returns the Java class name for a GraphQL input type.
   *
   * @param graphQLName the GraphQL input type name
   * @return the Java class name (e.g., "CreateUserInput")
   */
  @NotNull
  String getInputTypeName(@NotNull String graphQLName);

  /**
   * Returns the Java class name for a GraphQL query operation.
   *
   * @param graphQLName the GraphQL query name
   * @return the Java class name (e.g., "GetUserQuery")
   */
  @NotNull
  String getQueryName(@NotNull String graphQLName);

  /**
   * Returns the Java class name for a GraphQL mutation operation.
   *
   * @param graphQLName the GraphQL mutation name
   * @return the Java class name (e.g., "CreateUserMutation")
   */
  @NotNull
  String getMutationName(@NotNull String graphQLName);

  /**
   * Returns the Java enum name for a GraphQL enum type.
   *
   * @param graphQLName the GraphQL enum name
   * @return the Java enum name (typically unchanged)
   */
  @NotNull
  String getEnumName(@NotNull String graphQLName);

  /**
   * Returns the Java interface name for a GraphQL interface type.
   *
   * @param graphQLName the GraphQL interface name
   * @return the Java interface name
   */
  @NotNull
  String getInterfaceName(@NotNull String graphQLName);

  /**
   * Returns the Java interface name for a GraphQL union type.
   *
   * @param graphQLName the GraphQL union name
   * @return the Java interface name
   */
  @NotNull
  String getUnionName(@NotNull String graphQLName);

  /**
   * Returns the Java class name for a projection (field selection) builder.
   *
   * @param graphQLName the GraphQL type name
   * @return the Java class name (e.g., "UserProjection")
   */
  @NotNull
  String getProjectionName(@NotNull String graphQLName);

  /**
   * Creates the default naming convention.
   *
   * <p>The default convention applies the following suffixes:
   *
   * <ul>
   *   <li>Types: "DTO" (e.g., User → UserDTO)
   *   <li>Inputs: "Input" (preserved if already present)
   *   <li>Queries: "Query" (e.g., getUser → GetUserQuery)
   *   <li>Mutations: "Mutation" (e.g., createUser → CreateUserMutation)
   *   <li>Enums: unchanged
   *   <li>Interfaces: unchanged
   *   <li>Unions: unchanged
   *   <li>Projections: "Projection" (e.g., User → UserProjection)
   * </ul>
   *
   * @return the default naming convention
   */
  @NotNull
  static NamingConvention defaults() {
    return DefaultNamingConvention.INSTANCE;
  }

  /**
   * Creates a custom naming convention with specified suffixes.
   *
   * @param typeSuffix the suffix for object types (e.g., "DTO")
   * @param inputSuffix the suffix for input types (e.g., "Input")
   * @param querySuffix the suffix for queries (e.g., "Query")
   * @param mutationSuffix the suffix for mutations (e.g., "Mutation")
   * @return a custom naming convention
   */
  @NotNull
  static NamingConvention withSuffixes(
      @NotNull String typeSuffix,
      @NotNull String inputSuffix,
      @NotNull String querySuffix,
      @NotNull String mutationSuffix) {
    return new DefaultNamingConvention(typeSuffix, inputSuffix, querySuffix, mutationSuffix);
  }
}
