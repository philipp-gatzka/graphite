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
package io.github.graphite.maven;

import io.github.graphite.codegen.CodegenConfiguration;
import io.github.graphite.codegen.CodegenException;
import io.github.graphite.codegen.CodegenResult;
import io.github.graphite.codegen.GraphiteCodegen;
import io.github.graphite.codegen.NamingConvention;
import java.io.File;
import java.nio.file.Path;
import java.util.Map;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

/**
 * Maven Mojo for generating Java code from a GraphQL schema.
 *
 * <p>This Mojo generates type-safe Java code from a GraphQL introspection schema file
 * (schema.json). It binds to the {@code generate-sources} lifecycle phase by default.
 *
 * <p>Example configuration:
 *
 * <pre>{@code
 * <plugin>
 *     <groupId>io.github.graphite</groupId>
 *     <artifactId>graphite-maven-plugin</artifactId>
 *     <version>${graphite.version}</version>
 *     <executions>
 *         <execution>
 *             <goals>
 *                 <goal>generate</goal>
 *             </goals>
 *         </execution>
 *     </executions>
 *     <configuration>
 *         <schemaFile>${project.basedir}/src/main/resources/schema.json</schemaFile>
 *         <packageName>com.example.graphql</packageName>
 *         <scalars>
 *             <DateTime>java.time.Instant</DateTime>
 *             <UUID>java.util.UUID</UUID>
 *         </scalars>
 *     </configuration>
 * </plugin>
 * }</pre>
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, threadSafe = true)
public class GraphiteGenerateMojo extends AbstractMojo {

  /** The current Maven project. */
  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  /**
   * Path to the GraphQL schema file (schema.json).
   *
   * <p>This file should be in GraphQL introspection JSON format.
   */
  @Parameter(property = "graphite.schemaFile", required = true)
  private File schemaFile;

  /**
   * Output directory for generated Java sources.
   *
   * <p>Defaults to {@code ${project.build.directory}/generated-sources/graphite}.
   */
  @Parameter(
      property = "graphite.outputDirectory",
      defaultValue = "${project.build.directory}/generated-sources/graphite")
  private File outputDirectory;

  /**
   * The Java package name for generated classes.
   *
   * <p>Generated classes will be organized in sub-packages:
   *
   * <ul>
   *   <li>{@code <package>.type} - DTO records
   *   <li>{@code <package>.input} - Input types
   *   <li>{@code <package>.query} - Query operations
   *   <li>{@code <package>.mutation} - Mutation operations
   *   <li>{@code <package>.enumeration} - Enums
   * </ul>
   */
  @Parameter(property = "graphite.packageName", required = true)
  private String packageName;

  /**
   * Whether to skip code generation if the schema hasn't changed.
   *
   * <p>When enabled, a hash of the schema file is stored and compared on subsequent builds. If the
   * hash matches, code generation is skipped.
   *
   * <p>Defaults to {@code true}.
   */
  @Parameter(property = "graphite.skipIfUpToDate", defaultValue = "true")
  private boolean skipIfUpToDate;

  /**
   * Custom scalar type mappings.
   *
   * <p>Maps GraphQL scalar names to fully qualified Java type names.
   *
   * <p>Example:
   *
   * <pre>{@code
   * <scalars>
   *     <DateTime>java.time.Instant</DateTime>
   *     <UUID>java.util.UUID</UUID>
   * </scalars>
   * }</pre>
   */
  @Parameter private Map<String, String> scalars;

  /**
   * Whether to skip plugin execution entirely.
   *
   * <p>Use this to temporarily disable code generation without removing the plugin configuration.
   */
  @Parameter(property = "graphite.skip", defaultValue = "false")
  private boolean skip;

  /**
   * Whether to add the generated sources to the project's compile source roots.
   *
   * <p>When enabled (default), the output directory is automatically added to the project's source
   * directories so generated code is included in compilation.
   */
  @Parameter(property = "graphite.addCompileSourceRoot", defaultValue = "true")
  private boolean addCompileSourceRoot;

  @Override
  public void execute() throws MojoExecutionException {
    if (skip) {
      getLog().info("Graphite code generation is skipped");
      return;
    }

    validateConfiguration();
    logConfiguration();

    try {
      CodegenResult result = runCodeGeneration();
      reportResult(result);
      addSourceRootIfEnabled();
    } catch (CodegenException e) {
      throw new MojoExecutionException("Failed to generate GraphQL code: " + e.getMessage(), e);
    }
  }

  private void validateConfiguration() throws MojoExecutionException {
    if (schemaFile == null) {
      throw new MojoExecutionException("schemaFile must be configured");
    }

    if (!schemaFile.exists()) {
      throw new MojoExecutionException("Schema file does not exist: " + schemaFile);
    }

    if (!schemaFile.isFile()) {
      throw new MojoExecutionException("Schema path is not a file: " + schemaFile);
    }

    if (packageName == null || packageName.isBlank()) {
      throw new MojoExecutionException("packageName must be configured");
    }
  }

  private void logConfiguration() {
    getLog().info("Graphite Code Generation:");
    getLog().info("  Schema file: " + schemaFile);
    getLog().info("  Output directory: " + outputDirectory);
    getLog().info("  Package name: " + packageName);
    getLog().info("  Skip if up-to-date: " + skipIfUpToDate);
    if (scalars != null && !scalars.isEmpty()) {
      getLog().info("  Custom scalars: " + scalars);
    }
  }

  private CodegenResult runCodeGeneration() {
    Path schemaPath = schemaFile.toPath();
    Path outputPath = outputDirectory.toPath();

    CodegenConfiguration.Builder configBuilder =
        CodegenConfiguration.builder()
            .schemaFile(schemaPath)
            .outputDirectory(outputPath)
            .packageName(packageName)
            .skipIfUpToDate(skipIfUpToDate)
            .namingConvention(NamingConvention.defaults());

    if (scalars != null) {
      for (Map.Entry<String, String> entry : scalars.entrySet()) {
        configBuilder.customScalar(entry.getKey(), entry.getValue());
      }
    }

    CodegenConfiguration config = configBuilder.build();
    GraphiteCodegen codegen = new GraphiteCodegen(config);

    return codegen.generate();
  }

  private void reportResult(CodegenResult result) {
    if (result.wasSkipped()) {
      getLog().info("Graphite: Code generation skipped (schema unchanged)");
    } else {
      getLog()
          .info("Graphite: Generated " + result.filesGenerated() + " files in " + outputDirectory);
    }
  }

  private void addSourceRootIfEnabled() {
    if (addCompileSourceRoot) {
      String path = outputDirectory.getAbsolutePath();
      if (!project.getCompileSourceRoots().contains(path)) {
        project.addCompileSourceRoot(path);
        getLog().debug("Added source root: " + path);
      }
    }
  }

  // Setters for testing

  void setProject(MavenProject project) {
    this.project = project;
  }

  void setSchemaFile(File schemaFile) {
    this.schemaFile = schemaFile;
  }

  void setOutputDirectory(File outputDirectory) {
    this.outputDirectory = outputDirectory;
  }

  void setPackageName(String packageName) {
    this.packageName = packageName;
  }

  void setSkipIfUpToDate(boolean skipIfUpToDate) {
    this.skipIfUpToDate = skipIfUpToDate;
  }

  void setScalars(Map<String, String> scalars) {
    this.scalars = scalars;
  }

  void setSkip(boolean skip) {
    this.skip = skip;
  }

  void setAddCompileSourceRoot(boolean addCompileSourceRoot) {
    this.addCompileSourceRoot = addCompileSourceRoot;
  }
}
