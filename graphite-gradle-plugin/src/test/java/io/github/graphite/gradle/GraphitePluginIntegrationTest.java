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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("GraphitePlugin Integration Tests")
class GraphitePluginIntegrationTest {

  private static final String MINIMAL_SCHEMA =
      """
      {
        "__schema": {
          "queryType": {"name": "Query"},
          "mutationType": null,
          "subscriptionType": null,
          "types": [
            {
              "kind": "OBJECT",
              "name": "Query",
              "fields": [
                {
                  "name": "user",
                  "args": [
                    {
                      "name": "id",
                      "type": {"kind": "NON_NULL", "name": null, "ofType": {"kind": "SCALAR", "name": "ID", "ofType": null}}
                    }
                  ],
                  "type": {"kind": "OBJECT", "name": "User", "ofType": null}
                }
              ],
              "interfaces": []
            },
            {
              "kind": "OBJECT",
              "name": "User",
              "fields": [
                {"name": "id", "args": [], "type": {"kind": "NON_NULL", "name": null, "ofType": {"kind": "SCALAR", "name": "ID", "ofType": null}}},
                {"name": "name", "args": [], "type": {"kind": "NON_NULL", "name": null, "ofType": {"kind": "SCALAR", "name": "String", "ofType": null}}},
                {"name": "email", "args": [], "type": {"kind": "SCALAR", "name": "String", "ofType": null}}
              ],
              "interfaces": []
            },
            {"kind": "SCALAR", "name": "ID", "fields": null, "interfaces": null},
            {"kind": "SCALAR", "name": "String", "fields": null, "interfaces": null},
            {"kind": "SCALAR", "name": "Boolean", "fields": null, "interfaces": null}
          ],
          "directives": []
        }
      }
      """;

  private static final String SCHEMA_WITH_MUTATION =
      """
      {
        "__schema": {
          "queryType": {"name": "Query"},
          "mutationType": {"name": "Mutation"},
          "subscriptionType": null,
          "types": [
            {
              "kind": "OBJECT",
              "name": "Query",
              "fields": [
                {
                  "name": "user",
                  "args": [{"name": "id", "type": {"kind": "NON_NULL", "name": null, "ofType": {"kind": "SCALAR", "name": "ID", "ofType": null}}}],
                  "type": {"kind": "OBJECT", "name": "User", "ofType": null}
                }
              ],
              "interfaces": []
            },
            {
              "kind": "OBJECT",
              "name": "Mutation",
              "fields": [
                {
                  "name": "createUser",
                  "args": [{"name": "input", "type": {"kind": "NON_NULL", "name": null, "ofType": {"kind": "INPUT_OBJECT", "name": "CreateUserInput", "ofType": null}}}],
                  "type": {"kind": "NON_NULL", "name": null, "ofType": {"kind": "OBJECT", "name": "User", "ofType": null}}
                }
              ],
              "interfaces": []
            },
            {
              "kind": "OBJECT",
              "name": "User",
              "fields": [
                {"name": "id", "args": [], "type": {"kind": "NON_NULL", "name": null, "ofType": {"kind": "SCALAR", "name": "ID", "ofType": null}}},
                {"name": "name", "args": [], "type": {"kind": "NON_NULL", "name": null, "ofType": {"kind": "SCALAR", "name": "String", "ofType": null}}}
              ],
              "interfaces": []
            },
            {
              "kind": "INPUT_OBJECT",
              "name": "CreateUserInput",
              "fields": null,
              "inputFields": [
                {"name": "name", "type": {"kind": "NON_NULL", "name": null, "ofType": {"kind": "SCALAR", "name": "String", "ofType": null}}},
                {"name": "email", "type": {"kind": "SCALAR", "name": "String", "ofType": null}}
              ],
              "interfaces": null
            },
            {"kind": "SCALAR", "name": "ID", "fields": null, "interfaces": null},
            {"kind": "SCALAR", "name": "String", "fields": null, "interfaces": null},
            {"kind": "SCALAR", "name": "Boolean", "fields": null, "interfaces": null}
          ],
          "directives": []
        }
      }
      """;

  @TempDir Path projectDir;

  private GradleRunner runner;

  @BeforeEach
  void setup() {
    runner =
        GradleRunner.create()
            .withProjectDir(projectDir.toFile())
            .withPluginClasspath()
            .forwardOutput();
  }

  private void writeFile(String path, String content) throws IOException {
    Path filePath = projectDir.resolve(path);
    Files.createDirectories(filePath.getParent());
    Files.writeString(filePath, content);
  }

  private void writeBuildFile(String content) throws IOException {
    writeFile("build.gradle", content);
  }

  private void writeSettingsFile() throws IOException {
    writeFile("settings.gradle", "rootProject.name = 'test-project'\n");
  }

  private void writeMinimalSchema() throws IOException {
    writeFile("src/main/resources/schema.json", MINIMAL_SCHEMA);
  }

  @Nested
  @DisplayName("Basic generation")
  class BasicGeneration {

    @Test
    @DisplayName("should generate code with minimal config")
    void shouldGenerateCodeWithMinimalConfig() throws IOException {
      writeSettingsFile();
      writeBuildFile(
          """
          plugins {
              id 'io.github.graphite'
          }

          graphite {
              schemaFile = file('src/main/resources/schema.json')
              packageName = 'com.example.graphql'
          }
          """);
      writeMinimalSchema();

      BuildResult result = runner.withArguments("generateGraphiteCode", "--info").build();

      assertThat(result.task(":generateGraphiteCode").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);

      // Verify generated files exist
      Path outputDir =
          projectDir.resolve("build/generated/sources/graphite/main/java/com/example/graphql");
      assertThat(outputDir.resolve("type/UserDTO.java")).exists();
      assertThat(outputDir.resolve("query/UserQuery.java")).exists();
    }

    @Test
    @DisplayName("should generate mutations and input types")
    void shouldGenerateMutationsAndInputTypes() throws IOException {
      writeSettingsFile();
      writeBuildFile(
          """
          plugins {
              id 'io.github.graphite'
          }

          graphite {
              schemaFile = file('src/main/resources/schema.json')
              packageName = 'com.example.graphql'
          }
          """);
      writeFile("src/main/resources/schema.json", SCHEMA_WITH_MUTATION);

      BuildResult result = runner.withArguments("generateGraphiteCode", "--info").build();

      assertThat(result.task(":generateGraphiteCode").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);

      Path outputDir =
          projectDir.resolve("build/generated/sources/graphite/main/java/com/example/graphql");
      assertThat(outputDir.resolve("mutation/CreateUserMutation.java")).exists();
      assertThat(outputDir.resolve("input/CreateUserInput.java")).exists();
    }
  }

  @Nested
  @DisplayName("Custom configuration")
  class CustomConfiguration {

    @Test
    @DisplayName("should use custom package name")
    void shouldUseCustomPackageName() throws IOException {
      writeSettingsFile();
      writeBuildFile(
          """
          plugins {
              id 'io.github.graphite'
          }

          graphite {
              schemaFile = file('src/main/resources/schema.json')
              packageName = 'io.custom.api.generated'
          }
          """);
      writeMinimalSchema();

      BuildResult result = runner.withArguments("generateGraphiteCode", "--info").build();

      assertThat(result.task(":generateGraphiteCode").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);

      Path outputDir =
          projectDir.resolve("build/generated/sources/graphite/main/java/io/custom/api/generated");
      assertThat(outputDir.resolve("type/UserDTO.java")).exists();
    }

    @Test
    @DisplayName("should use custom output directory")
    void shouldUseCustomOutputDirectory() throws IOException {
      writeSettingsFile();
      writeBuildFile(
          """
          plugins {
              id 'io.github.graphite'
          }

          graphite {
              schemaFile = file('src/main/resources/schema.json')
              packageName = 'com.example.graphql'
              outputDirectory = layout.buildDirectory.dir('custom-output')
          }
          """);
      writeMinimalSchema();

      BuildResult result = runner.withArguments("generateGraphiteCode", "--info").build();

      assertThat(result.task(":generateGraphiteCode").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);

      Path outputDir = projectDir.resolve("build/custom-output/com/example/graphql");
      assertThat(outputDir.resolve("type/UserDTO.java")).exists();
    }

    @Test
    @DisplayName("should use custom scalar mappings")
    void shouldUseCustomScalarMappings() throws IOException {
      writeSettingsFile();
      writeBuildFile(
          """
          plugins {
              id 'io.github.graphite'
          }

          graphite {
              schemaFile = file('src/main/resources/schema.json')
              packageName = 'com.example.graphql'
              scalars = ['DateTime': 'java.time.Instant', 'Long': 'java.lang.Long']
          }
          """);
      writeMinimalSchema();

      BuildResult result = runner.withArguments("generateGraphiteCode", "--info").build();

      assertThat(result.task(":generateGraphiteCode").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
      assertThat(result.getOutput()).contains("Custom scalars: {DateTime=java.time.Instant");
    }
  }

  @Nested
  @DisplayName("Incremental builds")
  class IncrementalBuilds {

    @Test
    @DisplayName("should be up-to-date on second run")
    void shouldBeUpToDateOnSecondRun() throws IOException {
      writeSettingsFile();
      writeBuildFile(
          """
          plugins {
              id 'io.github.graphite'
          }

          graphite {
              schemaFile = file('src/main/resources/schema.json')
              packageName = 'com.example.graphql'
          }
          """);
      writeMinimalSchema();

      // First run
      BuildResult firstResult = runner.withArguments("generateGraphiteCode").build();
      assertThat(firstResult.task(":generateGraphiteCode").getOutcome())
          .isEqualTo(TaskOutcome.SUCCESS);

      // Second run should be UP-TO-DATE
      BuildResult secondResult = runner.withArguments("generateGraphiteCode").build();
      assertThat(secondResult.task(":generateGraphiteCode").getOutcome())
          .isEqualTo(TaskOutcome.UP_TO_DATE);
    }

    @Test
    @DisplayName("should regenerate when schema changes")
    void shouldRegenerateWhenSchemaChanges() throws IOException {
      writeSettingsFile();
      writeBuildFile(
          """
          plugins {
              id 'io.github.graphite'
          }

          graphite {
              schemaFile = file('src/main/resources/schema.json')
              packageName = 'com.example.graphql'
          }
          """);
      writeMinimalSchema();

      // First run
      BuildResult firstResult = runner.withArguments("generateGraphiteCode").build();
      assertThat(firstResult.task(":generateGraphiteCode").getOutcome())
          .isEqualTo(TaskOutcome.SUCCESS);

      // Modify schema
      writeFile("src/main/resources/schema.json", SCHEMA_WITH_MUTATION);

      // Second run should regenerate
      BuildResult secondResult = runner.withArguments("generateGraphiteCode").build();
      assertThat(secondResult.task(":generateGraphiteCode").getOutcome())
          .isEqualTo(TaskOutcome.SUCCESS);
    }
  }

  @Nested
  @DisplayName("Build cache")
  class BuildCache {

    @Test
    @DisplayName("should restore from build cache")
    void shouldRestoreFromBuildCache() throws IOException {
      writeSettingsFile();
      writeBuildFile(
          """
          plugins {
              id 'io.github.graphite'
          }

          graphite {
              schemaFile = file('src/main/resources/schema.json')
              packageName = 'com.example.graphql'
          }
          """);
      writeMinimalSchema();

      // First run with build cache - may be SUCCESS or FROM_CACHE if cache is shared
      BuildResult firstResult =
          runner.withArguments("generateGraphiteCode", "--build-cache").build();
      assertThat(firstResult.task(":generateGraphiteCode").getOutcome())
          .isIn(TaskOutcome.SUCCESS, TaskOutcome.FROM_CACHE);

      // Clean build directory
      runner.withArguments("clean").build();

      // Second run should restore from cache
      BuildResult secondResult =
          runner.withArguments("generateGraphiteCode", "--build-cache").build();
      assertThat(secondResult.task(":generateGraphiteCode").getOutcome())
          .isEqualTo(TaskOutcome.FROM_CACHE);
    }
  }

  @Nested
  @DisplayName("Error handling")
  class ErrorHandling {

    @Test
    @DisplayName("should fail when schema file is missing")
    void shouldFailWhenSchemaFileIsMissing() throws IOException {
      writeSettingsFile();
      writeBuildFile(
          """
          plugins {
              id 'io.github.graphite'
          }

          graphite {
              schemaFile = file('nonexistent.json')
              packageName = 'com.example.graphql'
          }
          """);

      BuildResult result = runner.withArguments("generateGraphiteCode").buildAndFail();

      assertThat(result.task(":generateGraphiteCode").getOutcome()).isEqualTo(TaskOutcome.FAILED);
    }

    @Test
    @DisplayName("should fail when schema is invalid JSON")
    void shouldFailWhenSchemaIsInvalidJson() throws IOException {
      writeSettingsFile();
      writeBuildFile(
          """
          plugins {
              id 'io.github.graphite'
          }

          graphite {
              schemaFile = file('src/main/resources/schema.json')
              packageName = 'com.example.graphql'
          }
          """);
      writeFile("src/main/resources/schema.json", "{ invalid json }");

      BuildResult result = runner.withArguments("generateGraphiteCode").buildAndFail();

      assertThat(result.task(":generateGraphiteCode").getOutcome()).isEqualTo(TaskOutcome.FAILED);
      assertThat(result.getOutput()).contains("Failed to generate GraphQL code");
    }

    @Test
    @DisplayName("should fail when schema is missing required fields")
    void shouldFailWhenSchemaIsMissingRequiredFields() throws IOException {
      writeSettingsFile();
      writeBuildFile(
          """
          plugins {
              id 'io.github.graphite'
          }

          graphite {
              schemaFile = file('src/main/resources/schema.json')
              packageName = 'com.example.graphql'
          }
          """);
      writeFile("src/main/resources/schema.json", "{ \"invalid\": true }");

      BuildResult result = runner.withArguments("generateGraphiteCode").buildAndFail();

      assertThat(result.task(":generateGraphiteCode").getOutcome()).isEqualTo(TaskOutcome.FAILED);
    }

    @Test
    @DisplayName("should fail when package name is not configured")
    void shouldFailWhenPackageNameNotConfigured() throws IOException {
      writeSettingsFile();
      writeBuildFile(
          """
          plugins {
              id 'io.github.graphite'
          }

          graphite {
              schemaFile = file('src/main/resources/schema.json')
          }
          """);
      writeMinimalSchema();

      BuildResult result = runner.withArguments("generateGraphiteCode").buildAndFail();

      // Gradle validates property configuration before task execution
      assertThat(result.getOutput()).contains("packageName");
    }
  }

  @Nested
  @DisplayName("Task dependencies")
  class TaskDependencies {

    @Test
    @DisplayName("should run before compileJava")
    void shouldRunBeforeCompileJava() throws IOException {
      writeSettingsFile();
      writeBuildFile(
          """
          plugins {
              id 'io.github.graphite'
          }

          graphite {
              schemaFile = file('src/main/resources/schema.json')
              packageName = 'com.example.graphql'
          }
          """);
      writeMinimalSchema();

      BuildResult result = runner.withArguments("compileJava", "--dry-run").build();

      assertThat(result.getOutput()).contains(":generateGraphiteCode");
      // The output should show generateGraphiteCode before compileJava
      int generateIndex = result.getOutput().indexOf(":generateGraphiteCode");
      int compileIndex = result.getOutput().indexOf(":compileJava");
      assertThat(generateIndex).isLessThan(compileIndex);
    }
  }

  @Nested
  @DisplayName("Generated file content")
  class GeneratedFileContent {

    @Test
    @DisplayName("should generate valid Java code")
    void shouldGenerateValidJavaCode() throws IOException {
      writeSettingsFile();
      writeBuildFile(
          """
          plugins {
              id 'io.github.graphite'
          }

          graphite {
              schemaFile = file('src/main/resources/schema.json')
              packageName = 'com.example.graphql'
          }
          """);
      writeMinimalSchema();

      runner.withArguments("generateGraphiteCode").build();

      Path userDto =
          projectDir.resolve(
              "build/generated/sources/graphite/main/java/com/example/graphql/type/UserDTO.java");
      String content = Files.readString(userDto);

      assertThat(content)
          .contains("package com.example.graphql.type;")
          .contains("public record UserDTO")
          .contains("String id")
          .contains("String name");
    }

    @Test
    @DisplayName("should generate query with correct structure")
    void shouldGenerateQueryWithCorrectStructure() throws IOException {
      writeSettingsFile();
      writeBuildFile(
          """
          plugins {
              id 'io.github.graphite'
          }

          graphite {
              schemaFile = file('src/main/resources/schema.json')
              packageName = 'com.example.graphql'
          }
          """);
      writeMinimalSchema();

      runner.withArguments("generateGraphiteCode").build();

      Path queryFile =
          projectDir.resolve(
              "build/generated/sources/graphite/main/java/com/example/graphql/query/UserQuery.java");
      String content = Files.readString(queryFile);

      assertThat(content)
          .contains("package com.example.graphql.query;")
          .contains("public final class UserQuery")
          .contains("GraphQLOperation");
    }
  }
}
