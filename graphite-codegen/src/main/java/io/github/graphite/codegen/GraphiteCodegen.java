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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

/**
 * Main orchestrator for GraphQL code generation.
 *
 * <p>This class coordinates the entire code generation process:
 *
 * <ul>
 *   <li>Validates the configuration
 *   <li>Checks if generation can be skipped (incremental builds)
 *   <li>Parses the GraphQL schema from schema.json
 *   <li>Invokes appropriate generators for each type category
 *   <li>Writes generated Java source files
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * CodegenConfiguration config = CodegenConfiguration.builder()
 *     .schemaFile(Path.of("src/main/resources/schema.json"))
 *     .outputDirectory(Path.of("build/generated/sources/graphite"))
 *     .packageName("com.example.graphql")
 *     .build();
 *
 * GraphiteCodegen codegen = new GraphiteCodegen(config);
 * CodegenResult result = codegen.generate();
 *
 * System.out.println("Generated " + result.filesGenerated() + " files");
 * }</pre>
 *
 * @see CodegenConfiguration
 * @see CodegenResult
 */
public final class GraphiteCodegen {

  private static final String HASH_FILE_NAME = ".graphite-schema-hash";

  private final CodegenConfiguration configuration;

  /**
   * Creates a new code generator with the specified configuration.
   *
   * @param configuration the code generation configuration
   * @throws NullPointerException if configuration is null
   */
  public GraphiteCodegen(@NotNull CodegenConfiguration configuration) {
    this.configuration = Objects.requireNonNull(configuration, "configuration must not be null");
  }

  /**
   * Executes the code generation process.
   *
   * <p>This method will:
   *
   * <ol>
   *   <li>Validate that the schema file exists
   *   <li>Check if generation can be skipped (if skipIfUpToDate is enabled)
   *   <li>Parse the schema
   *   <li>Generate Java source files for all types
   *   <li>Update the schema hash for incremental builds
   * </ol>
   *
   * @return the result of code generation
   * @throws CodegenException if code generation fails
   */
  @NotNull
  public CodegenResult generate() {
    validateConfiguration();

    // Check if we can skip generation
    if (configuration.skipIfUpToDate() && isUpToDate()) {
      return CodegenResult.skipped();
    }

    try {
      // Ensure output directory exists
      Files.createDirectories(configuration.outputDirectory());

      // Parse the schema
      String schemaContent = Files.readString(configuration.schemaFile());

      // TODO: Implement schema parsing and code generation
      // This will be done in subsequent issues for SchemaParser and generators
      int filesGenerated = generateFromSchema(schemaContent);

      // Update the hash file for incremental builds
      updateSchemaHash();

      return CodegenResult.success(filesGenerated);

    } catch (IOException e) {
      throw new CodegenException("Failed to generate code", e);
    }
  }

  /**
   * Validates that the configuration is valid for code generation.
   *
   * @throws CodegenException if the configuration is invalid
   */
  private void validateConfiguration() {
    Path schemaFile = configuration.schemaFile();

    if (!Files.exists(schemaFile)) {
      throw new CodegenException("Schema file does not exist: " + schemaFile);
    }

    if (!Files.isRegularFile(schemaFile)) {
      throw new CodegenException("Schema path is not a file: " + schemaFile);
    }

    if (!Files.isReadable(schemaFile)) {
      throw new CodegenException("Schema file is not readable: " + schemaFile);
    }
  }

  /**
   * Checks if the schema has changed since the last generation.
   *
   * @return true if the generated code is up-to-date
   */
  private boolean isUpToDate() {
    Path hashFile = configuration.outputDirectory().resolve(HASH_FILE_NAME);

    if (!Files.exists(hashFile)) {
      return false;
    }

    try {
      String storedHash = Files.readString(hashFile).trim();
      String currentHash = computeSchemaHash();
      return storedHash.equals(currentHash);
    } catch (IOException e) {
      // If we can't read the hash, assume we need to regenerate
      return false;
    }
  }

  /**
   * Updates the stored schema hash after successful generation.
   *
   * @throws IOException if the hash file cannot be written
   */
  private void updateSchemaHash() throws IOException {
    Path hashFile = configuration.outputDirectory().resolve(HASH_FILE_NAME);
    String hash = computeSchemaHash();
    Files.writeString(hashFile, hash);
  }

  /**
   * Computes the SHA-256 hash of the schema file.
   *
   * @return the hex-encoded hash
   */
  private String computeSchemaHash() {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] content = Files.readAllBytes(configuration.schemaFile());
      byte[] hash = digest.digest(content);
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException e) {
      throw new CodegenException("SHA-256 algorithm not available", e);
    } catch (IOException e) {
      throw new CodegenException("Failed to read schema file for hashing", e);
    }
  }

  /**
   * Generates Java source files from the parsed schema.
   *
   * @param schemaContent the raw schema JSON content
   * @return the number of files generated
   */
  private int generateFromSchema(String schemaContent) {
    // TODO: This will be implemented in subsequent issues:
    // - Issue #39: SchemaParser for parsing schema.json
    // - Issue #40: TypeGenerator for DTOs
    // - Issue #41: InputTypeGenerator for input types
    // - Issue #42: EnumGenerator for enums
    // - Issue #43: QueryGenerator for queries
    // - Issue #44: MutationGenerator for mutations
    // - Issue #45: ProjectionGenerator for field selections

    // For now, we just validate the schema can be read
    if (schemaContent == null || schemaContent.isBlank()) {
      throw new CodegenException("Schema file is empty");
    }

    // Placeholder: actual generation will be implemented in subsequent issues
    return 0;
  }

  /**
   * Returns the configuration for this code generator.
   *
   * @return the configuration
   */
  @NotNull
  public CodegenConfiguration getConfiguration() {
    return configuration;
  }
}
