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

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.jetbrains.annotations.NotNull;

/**
 * Gradle plugin for generating type-safe Java code from GraphQL schema.
 *
 * <p>This plugin provides:
 *
 * <ul>
 *   <li>A {@code graphite} extension for configuration
 *   <li>A {@code generateGraphiteCode} task for code generation
 *   <li>Automatic integration with the Java source sets
 * </ul>
 *
 * <p>Example usage in Kotlin DSL:
 *
 * <pre>{@code
 * plugins {
 *     id("io.github.graphite") version "1.0.0"
 * }
 *
 * graphite {
 *     schemaFile = file("src/main/resources/schema.json")
 *     packageName = "com.example.graphql"
 *     scalars {
 *         register("DateTime", "java.time.Instant")
 *     }
 * }
 * }</pre>
 *
 * <p>Example usage in Groovy DSL:
 *
 * <pre>{@code
 * plugins {
 *     id 'io.github.graphite' version '1.0.0'
 * }
 *
 * graphite {
 *     schemaFile = file('src/main/resources/schema.json')
 *     packageName = 'com.example.graphql'
 *     scalars {
 *         register('DateTime', 'java.time.Instant')
 *     }
 * }
 * }</pre>
 *
 * @see GraphiteExtension
 * @see GraphiteGenerateTask
 */
public class GraphitePlugin implements Plugin<Project> {

  /** The name of the extension. */
  public static final String EXTENSION_NAME = "graphite";

  /** The name of the code generation task. */
  public static final String TASK_NAME = "generateGraphiteCode";

  /** The task group for Graphite tasks. */
  public static final String TASK_GROUP = "graphite";

  @Override
  public void apply(@NotNull Project project) {
    // Apply Java plugin if not already applied
    project.getPluginManager().apply(JavaPlugin.class);

    // Create the extension (defaults are set in its constructor)
    GraphiteExtension extension =
        project.getExtensions().create(EXTENSION_NAME, GraphiteExtension.class, project);

    // Register the generation task
    project
        .getTasks()
        .register(
            TASK_NAME,
            GraphiteGenerateTask.class,
            task -> {
              task.setGroup(TASK_GROUP);
              task.setDescription("Generates Java code from GraphQL schema");

              // Wire extension properties to task properties
              task.getSchemaFile().set(extension.getSchemaFile());
              task.getOutputDirectory().set(extension.getOutputDirectory());
              task.getPackageName().set(extension.getPackageName());
              task.getSkipIfUpToDate().set(extension.getSkipIfUpToDate());
              task.getScalars().set(extension.getScalars());
            });

    // Add generated sources to the main source set
    project.afterEvaluate(
        p -> {
          SourceSetContainer sourceSets = p.getExtensions().getByType(SourceSetContainer.class);

          SourceSet mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
          mainSourceSet.getJava().srcDir(extension.getOutputDirectory());

          // Make compileJava depend on code generation
          p.getTasks()
              .named(JavaPlugin.COMPILE_JAVA_TASK_NAME)
              .configure(task -> task.dependsOn(TASK_NAME));
        });
  }
}
