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

import java.io.File;
import javax.inject.Inject;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.provider.MapProperty;
import org.gradle.api.provider.Property;

/**
 * Extension for configuring GraphQL code generation.
 *
 * <p>This extension provides the following configuration options:
 *
 * <ul>
 *   <li>{@code schemaFile} - Path to the GraphQL schema file (required)
 *   <li>{@code outputDirectory} - Directory for generated code
 *   <li>{@code packageName} - Java package for generated classes (required)
 *   <li>{@code skipIfUpToDate} - Whether to skip generation if schema unchanged
 *   <li>{@code scalars} - Custom scalar type mappings
 * </ul>
 *
 * <p>Example Kotlin DSL:
 *
 * <pre>{@code
 * graphite {
 *     schemaFile = file("src/main/resources/schema.json")
 *     packageName = "com.example.graphql"
 *     outputDirectory = layout.buildDirectory.dir("generated/sources/graphite/main/java")
 *     skipIfUpToDate = true
 *     scalars {
 *         register("DateTime", "java.time.Instant")
 *         register("Money", "com.example.Money")
 *     }
 * }
 * }</pre>
 *
 * @see GraphitePlugin
 */
public class GraphiteExtension {

  /** Default path for generated sources within build directory. */
  public static final String DEFAULT_OUTPUT_PATH = "generated/sources/graphite/main/java";

  private final RegularFileProperty schemaFile;
  private final DirectoryProperty outputDirectory;
  private final Property<String> packageName;
  private final Property<Boolean> skipIfUpToDate;
  private final MapProperty<String, String> scalars;

  /**
   * Creates a new extension instance.
   *
   * @param project the Gradle project
   */
  @Inject
  public GraphiteExtension(Project project) {
    ObjectFactory objects = project.getObjects();

    this.schemaFile = objects.fileProperty();
    this.outputDirectory = objects.directoryProperty();
    this.packageName = objects.property(String.class);
    this.skipIfUpToDate = objects.property(Boolean.class);
    this.scalars = objects.mapProperty(String.class, String.class);

    // Set defaults
    this.outputDirectory.convention(
        project.getLayout().getBuildDirectory().dir(DEFAULT_OUTPUT_PATH));
    this.skipIfUpToDate.convention(true);
  }

  /**
   * The path to the GraphQL schema file.
   *
   * <p>This file should be in GraphQL introspection JSON format (schema.json).
   *
   * @return the schema file property
   */
  public RegularFileProperty getSchemaFile() {
    return schemaFile;
  }

  /**
   * The output directory for generated Java source files.
   *
   * <p>Defaults to {@code build/generated/sources/graphite/main/java}.
   *
   * @return the output directory property
   */
  public DirectoryProperty getOutputDirectory() {
    return outputDirectory;
  }

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
   *
   * @return the package name property
   */
  public Property<String> getPackageName() {
    return packageName;
  }

  /**
   * Whether to skip code generation if the schema hasn't changed.
   *
   * <p>When enabled, a hash of the schema file is stored and compared on subsequent builds. If the
   * hash matches, code generation is skipped.
   *
   * <p>Defaults to {@code true}.
   *
   * @return the skip if up-to-date property
   */
  public Property<Boolean> getSkipIfUpToDate() {
    return skipIfUpToDate;
  }

  /**
   * Custom scalar type mappings.
   *
   * <p>Maps GraphQL scalar names to fully qualified Java type names.
   *
   * @return the scalars map property
   */
  public MapProperty<String, String> getScalars() {
    return scalars;
  }

  /**
   * Configures custom scalar mappings using an action.
   *
   * <p>Example:
   *
   * <pre>{@code
   * scalars {
   *     register("DateTime", "java.time.Instant")
   * }
   * }</pre>
   *
   * @param action the configuration action
   */
  public void scalars(Action<ScalarsHandler> action) {
    action.execute(new ScalarsHandler(scalars));
  }

  /**
   * Convenience method to set schema file from a File.
   *
   * @param file the schema file
   */
  public void setSchemaFile(File file) {
    schemaFile.set(file);
  }

  /**
   * Convenience method to set output directory from a File.
   *
   * @param directory the output directory
   */
  public void setOutputDirectory(File directory) {
    outputDirectory.set(directory);
  }

  /**
   * Convenience method to set package name from a String.
   *
   * @param name the package name
   */
  public void setPackageName(String name) {
    packageName.set(name);
  }

  /**
   * Convenience method to set skipIfUpToDate.
   *
   * @param skip whether to skip if up-to-date
   */
  public void setSkipIfUpToDate(boolean skip) {
    skipIfUpToDate.set(skip);
  }

  /**
   * Handler for configuring scalar mappings.
   *
   * @see #scalars(Action)
   */
  public static class ScalarsHandler {

    private final MapProperty<String, String> scalars;

    ScalarsHandler(MapProperty<String, String> scalars) {
      this.scalars = scalars;
    }

    /**
     * Registers a custom scalar mapping.
     *
     * @param scalarName the GraphQL scalar name
     * @param javaType the fully qualified Java type name
     */
    public void register(String scalarName, String javaType) {
      scalars.put(scalarName, javaType);
    }
  }
}
