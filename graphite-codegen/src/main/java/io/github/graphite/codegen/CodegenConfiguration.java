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

import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Configuration for GraphQL code generation.
 *
 * <p>This record encapsulates all configuration options needed to generate Java code from a GraphQL
 * schema:
 *
 * <ul>
 *   <li>{@code schemaFile} - Path to the GraphQL schema file (schema.json)
 *   <li>{@code outputDirectory} - Directory where generated code will be written
 *   <li>{@code packageName} - Java package name for generated classes
 *   <li>{@code customScalarMappings} - Custom scalar type to Java class mappings
 *   <li>{@code namingConvention} - Strategy for naming generated classes
 *   <li>{@code skipIfUpToDate} - Whether to skip generation if schema hasn't changed
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * CodegenConfiguration config = CodegenConfiguration.builder()
 *     .schemaFile(Path.of("src/main/resources/schema.json"))
 *     .outputDirectory(Path.of("build/generated/graphite"))
 *     .packageName("com.example.graphql")
 *     .customScalar("DateTime", "java.time.Instant")
 *     .build();
 * }</pre>
 *
 * @param schemaFile the path to the GraphQL schema file
 * @param outputDirectory the directory for generated source files
 * @param packageName the Java package for generated classes
 * @param customScalarMappings custom scalar to Java type mappings
 * @param namingConvention the naming convention for generated classes
 * @param skipIfUpToDate whether to skip generation if up-to-date
 * @see GraphiteCodegen
 * @see NamingConvention
 */
public record CodegenConfiguration(
    @NotNull Path schemaFile,
    @NotNull Path outputDirectory,
    @NotNull String packageName,
    @NotNull Map<String, String> customScalarMappings,
    @NotNull NamingConvention namingConvention,
    boolean skipIfUpToDate) {

  /**
   * Creates a new configuration with validation.
   *
   * @param schemaFile the path to the GraphQL schema file
   * @param outputDirectory the directory for generated source files
   * @param packageName the Java package for generated classes
   * @param customScalarMappings custom scalar to Java type mappings
   * @param namingConvention the naming convention for generated classes
   * @param skipIfUpToDate whether to skip generation if up-to-date
   * @throws NullPointerException if any required parameter is null
   * @throws IllegalArgumentException if packageName is empty
   */
  public CodegenConfiguration {
    Objects.requireNonNull(schemaFile, "schemaFile must not be null");
    Objects.requireNonNull(outputDirectory, "outputDirectory must not be null");
    Objects.requireNonNull(packageName, "packageName must not be null");
    Objects.requireNonNull(customScalarMappings, "customScalarMappings must not be null");
    Objects.requireNonNull(namingConvention, "namingConvention must not be null");

    if (packageName.isBlank()) {
      throw new IllegalArgumentException("packageName must not be empty");
    }

    customScalarMappings = Collections.unmodifiableMap(customScalarMappings);
  }

  /**
   * Creates a new builder for constructing configurations.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Returns the package name for a specific type category.
   *
   * @param category the type category (e.g., "type", "input", "query")
   * @return the full package name for the category
   */
  public String getPackageForCategory(String category) {
    return packageName + "." + category;
  }

  /** Builder for creating {@link CodegenConfiguration} instances. */
  public static final class Builder {

    private Path schemaFile;
    private Path outputDirectory;
    private String packageName;
    private final java.util.HashMap<String, String> customScalarMappings =
        new java.util.HashMap<>();
    private NamingConvention namingConvention = NamingConvention.defaults();
    private boolean skipIfUpToDate = true;

    private Builder() {}

    /**
     * Sets the schema file path.
     *
     * @param schemaFile the path to the GraphQL schema file
     * @return this builder
     */
    public Builder schemaFile(Path schemaFile) {
      this.schemaFile = schemaFile;
      return this;
    }

    /**
     * Sets the output directory for generated code.
     *
     * @param outputDirectory the output directory
     * @return this builder
     */
    public Builder outputDirectory(Path outputDirectory) {
      this.outputDirectory = outputDirectory;
      return this;
    }

    /**
     * Sets the package name for generated classes.
     *
     * @param packageName the Java package name
     * @return this builder
     */
    public Builder packageName(String packageName) {
      this.packageName = packageName;
      return this;
    }

    /**
     * Adds a custom scalar mapping.
     *
     * @param scalarName the GraphQL scalar name
     * @param javaType the fully qualified Java type name
     * @return this builder
     */
    public Builder customScalar(String scalarName, String javaType) {
      this.customScalarMappings.put(scalarName, javaType);
      return this;
    }

    /**
     * Sets all custom scalar mappings.
     *
     * @param mappings the scalar mappings
     * @return this builder
     */
    public Builder customScalarMappings(Map<String, String> mappings) {
      this.customScalarMappings.clear();
      this.customScalarMappings.putAll(mappings);
      return this;
    }

    /**
     * Sets the naming convention.
     *
     * @param namingConvention the naming convention to use
     * @return this builder
     */
    public Builder namingConvention(NamingConvention namingConvention) {
      this.namingConvention = namingConvention;
      return this;
    }

    /**
     * Sets whether to skip generation if the schema is up-to-date.
     *
     * @param skipIfUpToDate true to enable incremental builds
     * @return this builder
     */
    public Builder skipIfUpToDate(boolean skipIfUpToDate) {
      this.skipIfUpToDate = skipIfUpToDate;
      return this;
    }

    /**
     * Builds the configuration.
     *
     * @return the configured {@link CodegenConfiguration}
     * @throws IllegalStateException if required fields are not set
     */
    public CodegenConfiguration build() {
      if (schemaFile == null) {
        throw new IllegalStateException("schemaFile is required");
      }
      if (outputDirectory == null) {
        throw new IllegalStateException("outputDirectory is required");
      }
      if (packageName == null) {
        throw new IllegalStateException("packageName is required");
      }

      return new CodegenConfiguration(
          schemaFile,
          outputDirectory,
          packageName,
          customScalarMappings,
          namingConvention,
          skipIfUpToDate);
    }
  }
}
