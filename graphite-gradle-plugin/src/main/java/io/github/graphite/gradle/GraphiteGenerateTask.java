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
package io.github.graphite.gradle;

import io.github.graphite.codegen.CodegenConfiguration;
import io.github.graphite.codegen.CodegenResult;
import io.github.graphite.codegen.GraphiteCodegen;
import io.github.graphite.codegen.NamingConvention;
import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.CacheableTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.Optional;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.gradle.api.tasks.TaskAction;

/**
 * Gradle task for generating Java code from a GraphQL schema.
 *
 * <p>This task:
 *
 * <ul>
 *   <li>Reads the GraphQL schema from the configured schema file
 *   <li>Generates type-safe Java code for queries, mutations, types, and inputs
 *   <li>Supports incremental builds by tracking schema changes
 *   <li>Is fully cacheable for build performance
 * </ul>
 *
 * <p>The task is registered by {@link GraphitePlugin} with the name {@code generateGraphiteCode}.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ./gradlew generateGraphiteCode
 * }</pre>
 *
 * @see GraphitePlugin
 * @see GraphiteExtension
 */
@CacheableTask
public abstract class GraphiteGenerateTask extends DefaultTask {

  /**
   * The GraphQL schema file to process.
   *
   * @return the schema file property
   */
  @InputFile
  @PathSensitive(PathSensitivity.RELATIVE)
  public abstract RegularFileProperty getSchemaFile();

  /**
   * The output directory for generated Java sources.
   *
   * @return the output directory property
   */
  @OutputDirectory
  public abstract DirectoryProperty getOutputDirectory();

  /**
   * The Java package name for generated classes.
   *
   * @return the package name property
   */
  @Input
  public abstract Property<String> getPackageName();

  /**
   * Whether to skip generation if the schema is unchanged.
   *
   * @return the skip if up-to-date property
   */
  @Input
  @Optional
  public abstract Property<Boolean> getSkipIfUpToDate();

  /**
   * Custom scalar type mappings.
   *
   * @return the scalars map property
   */
  @Input
  @Optional
  public abstract MapProperty<String, String> getScalars();

  /**
   * Executes the code generation task.
   *
   * <p>This method:
   *
   * <ol>
   *   <li>Validates that required inputs are configured
   *   <li>Creates a {@link CodegenConfiguration} from the task properties
   *   <li>Invokes {@link GraphiteCodegen} to generate the code
   *   <li>Reports the result to the logger
   * </ol>
   *
   * @throws GradleException if code generation fails or required inputs are missing
   */
  @TaskAction
  public void generate() {
    // Validate required inputs
    if (!getSchemaFile().isPresent()) {
      throw new GradleException("schemaFile must be configured in the graphite extension");
    }

    if (!getPackageName().isPresent()) {
      throw new GradleException("packageName must be configured in the graphite extension");
    }

    File schemaFile = getSchemaFile().get().getAsFile();
    Path outputPath = getOutputDirectory().get().getAsFile().toPath();
    String packageName = getPackageName().get();
    boolean skipIfUpToDate = getSkipIfUpToDate().getOrElse(true);
    Map<String, String> scalars = getScalars().getOrElse(Map.of());

    // Log configuration
    getLogger().info("Graphite Code Generation:");
    getLogger().info("  Schema file: {}", schemaFile);
    getLogger().info("  Output directory: {}", outputPath);
    getLogger().info("  Package name: {}", packageName);
    getLogger().info("  Skip if up-to-date: {}", skipIfUpToDate);
    if (!scalars.isEmpty()) {
      getLogger().info("  Custom scalars: {}", scalars);
    }

    // Build configuration
    CodegenConfiguration.Builder configBuilder =
        CodegenConfiguration.builder()
            .schemaFile(schemaFile.toPath())
            .outputDirectory(outputPath)
            .packageName(packageName)
            .skipIfUpToDate(skipIfUpToDate)
            .namingConvention(NamingConvention.defaults());

    // Add custom scalar mappings
    for (Map.Entry<String, String> entry : scalars.entrySet()) {
      configBuilder.customScalar(entry.getKey(), entry.getValue());
    }

    CodegenConfiguration config = configBuilder.build();

    try {
      // Execute code generation
      GraphiteCodegen codegen = new GraphiteCodegen(config);
      CodegenResult result = codegen.generate();

      // Report result
      if (result.wasSkipped()) {
        getLogger().lifecycle("Graphite: Code generation skipped (schema unchanged)");
      } else {
        getLogger()
            .lifecycle("Graphite: Generated {} files in {}", result.filesGenerated(), outputPath);
      }

    } catch (Exception e) {
      throw new GradleException("Failed to generate GraphQL code: " + e.getMessage(), e);
    }
  }
}
