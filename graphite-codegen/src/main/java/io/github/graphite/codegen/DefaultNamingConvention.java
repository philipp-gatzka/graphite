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

import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Default implementation of {@link NamingConvention}.
 *
 * <p>This class provides the standard naming transformations for generated Java classes:
 *
 * <ul>
 *   <li>Object types get a configurable suffix (default: "DTO")
 *   <li>Input types preserve or add "Input" suffix
 *   <li>Query operations get "Query" suffix
 *   <li>Mutation operations get "Mutation" suffix
 *   <li>Enums, interfaces, and unions retain their original names
 *   <li>Projections get "Projection" suffix
 * </ul>
 *
 * @see NamingConvention
 */
final class DefaultNamingConvention implements NamingConvention {

  /** Singleton instance with default suffixes. */
  static final DefaultNamingConvention INSTANCE = new DefaultNamingConvention();

  private static final String DEFAULT_TYPE_SUFFIX = "DTO";
  private static final String DEFAULT_INPUT_SUFFIX = "Input";
  private static final String DEFAULT_QUERY_SUFFIX = "Query";
  private static final String DEFAULT_MUTATION_SUFFIX = "Mutation";
  private static final String DEFAULT_PROJECTION_SUFFIX = "Projection";
  private static final String GRAPHQL_NAME_NULL_MSG = "graphQLName must not be null";

  private final String typeSuffix;
  private final String inputSuffix;
  private final String querySuffix;
  private final String mutationSuffix;

  /** Creates a default naming convention instance. */
  DefaultNamingConvention() {
    this(DEFAULT_TYPE_SUFFIX, DEFAULT_INPUT_SUFFIX, DEFAULT_QUERY_SUFFIX, DEFAULT_MUTATION_SUFFIX);
  }

  /**
   * Creates a naming convention with custom suffixes.
   *
   * @param typeSuffix the suffix for object types
   * @param inputSuffix the suffix for input types
   * @param querySuffix the suffix for queries
   * @param mutationSuffix the suffix for mutations
   */
  DefaultNamingConvention(
      @NotNull String typeSuffix,
      @NotNull String inputSuffix,
      @NotNull String querySuffix,
      @NotNull String mutationSuffix) {
    this.typeSuffix = Objects.requireNonNull(typeSuffix, "typeSuffix must not be null");
    this.inputSuffix = Objects.requireNonNull(inputSuffix, "inputSuffix must not be null");
    this.querySuffix = Objects.requireNonNull(querySuffix, "querySuffix must not be null");
    this.mutationSuffix = Objects.requireNonNull(mutationSuffix, "mutationSuffix must not be null");
  }

  @Override
  @NotNull
  public String getTypeName(@NotNull String graphQLName) {
    Objects.requireNonNull(graphQLName, GRAPHQL_NAME_NULL_MSG);
    return capitalize(graphQLName) + typeSuffix;
  }

  @Override
  @NotNull
  public String getInputTypeName(@NotNull String graphQLName) {
    Objects.requireNonNull(graphQLName, GRAPHQL_NAME_NULL_MSG);
    String name = capitalize(graphQLName);
    // Don't double-add Input suffix
    if (name.endsWith(inputSuffix)) {
      return name;
    }
    return name + inputSuffix;
  }

  @Override
  @NotNull
  public String getQueryName(@NotNull String graphQLName) {
    Objects.requireNonNull(graphQLName, GRAPHQL_NAME_NULL_MSG);
    String name = capitalize(graphQLName);
    // Don't double-add Query suffix
    if (name.endsWith(querySuffix)) {
      return name;
    }
    return name + querySuffix;
  }

  @Override
  @NotNull
  public String getMutationName(@NotNull String graphQLName) {
    Objects.requireNonNull(graphQLName, GRAPHQL_NAME_NULL_MSG);
    String name = capitalize(graphQLName);
    // Don't double-add Mutation suffix
    if (name.endsWith(mutationSuffix)) {
      return name;
    }
    return name + mutationSuffix;
  }

  @Override
  @NotNull
  public String getEnumName(@NotNull String graphQLName) {
    return getCapitalizedName(graphQLName);
  }

  @Override
  @NotNull
  public String getInterfaceName(@NotNull String graphQLName) {
    return getCapitalizedName(graphQLName);
  }

  @Override
  @NotNull
  public String getUnionName(@NotNull String graphQLName) {
    return getCapitalizedName(graphQLName);
  }

  private String getCapitalizedName(String graphQLName) {
    Objects.requireNonNull(graphQLName, GRAPHQL_NAME_NULL_MSG);
    return capitalize(graphQLName);
  }

  @Override
  @NotNull
  public String getProjectionName(@NotNull String graphQLName) {
    Objects.requireNonNull(graphQLName, GRAPHQL_NAME_NULL_MSG);
    return capitalize(graphQLName) + DEFAULT_PROJECTION_SUFFIX;
  }

  /**
   * Capitalizes the first letter of a string.
   *
   * @param name the string to capitalize
   * @return the capitalized string
   */
  private String capitalize(String name) {
    if (name.isEmpty()) {
      return name;
    }
    return Character.toUpperCase(name.charAt(0)) + name.substring(1);
  }
}
