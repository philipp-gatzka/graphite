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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Map;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("GraphiteGenerateMojo")
class GraphiteGenerateMojoTest {

  private static final String SAMPLE_SCHEMA =
      """
      {
        "__schema": {
          "queryType": { "name": "Query" },
          "mutationType": null,
          "subscriptionType": null,
          "types": [
            {
              "kind": "OBJECT",
              "name": "Query",
              "fields": [
                {
                  "name": "hello",
                  "args": [],
                  "type": { "kind": "SCALAR", "name": "String" },
                  "isDeprecated": false
                }
              ],
              "interfaces": []
            }
          ]
        }
      }
      """;

  @TempDir Path tempDir;

  private GraphiteGenerateMojo mojo;
  private MavenProject mockProject;

  @BeforeEach
  void setUp() {
    mojo = new GraphiteGenerateMojo();
    mockProject = mock(MavenProject.class);
    when(mockProject.getCompileSourceRoots()).thenReturn(new ArrayList<>());
    mojo.setProject(mockProject);
  }

  @Nested
  @DisplayName("validation")
  class Validation {

    @Test
    @DisplayName("should throw when schemaFile is null")
    void shouldThrowWhenSchemaFileIsNull() {
      mojo.setPackageName("com.example");
      mojo.setOutputDirectory(tempDir.resolve("output").toFile());

      assertThatThrownBy(() -> mojo.execute())
          .isInstanceOf(MojoExecutionException.class)
          .hasMessageContaining("schemaFile must be configured");
    }

    @Test
    @DisplayName("should throw when schemaFile does not exist")
    void shouldThrowWhenSchemaFileDoesNotExist() {
      mojo.setSchemaFile(tempDir.resolve("nonexistent.json").toFile());
      mojo.setPackageName("com.example");
      mojo.setOutputDirectory(tempDir.resolve("output").toFile());

      assertThatThrownBy(() -> mojo.execute())
          .isInstanceOf(MojoExecutionException.class)
          .hasMessageContaining("does not exist");
    }

    @Test
    @DisplayName("should throw when schemaFile is a directory")
    void shouldThrowWhenSchemaFileIsDirectory() throws IOException {
      Path dir = tempDir.resolve("schema-dir");
      Files.createDirectory(dir);

      mojo.setSchemaFile(dir.toFile());
      mojo.setPackageName("com.example");
      mojo.setOutputDirectory(tempDir.resolve("output").toFile());

      assertThatThrownBy(() -> mojo.execute())
          .isInstanceOf(MojoExecutionException.class)
          .hasMessageContaining("not a file");
    }

    @Test
    @DisplayName("should throw when packageName is null")
    void shouldThrowWhenPackageNameIsNull() throws IOException {
      Path schemaFile = tempDir.resolve("schema.json");
      Files.writeString(schemaFile, SAMPLE_SCHEMA);

      mojo.setSchemaFile(schemaFile.toFile());
      mojo.setOutputDirectory(tempDir.resolve("output").toFile());

      assertThatThrownBy(() -> mojo.execute())
          .isInstanceOf(MojoExecutionException.class)
          .hasMessageContaining("packageName must be configured");
    }

    @Test
    @DisplayName("should throw when packageName is blank")
    void shouldThrowWhenPackageNameIsBlank() throws IOException {
      Path schemaFile = tempDir.resolve("schema.json");
      Files.writeString(schemaFile, SAMPLE_SCHEMA);

      mojo.setSchemaFile(schemaFile.toFile());
      mojo.setPackageName("   ");
      mojo.setOutputDirectory(tempDir.resolve("output").toFile());

      assertThatThrownBy(() -> mojo.execute())
          .isInstanceOf(MojoExecutionException.class)
          .hasMessageContaining("packageName must be configured");
    }
  }

  @Nested
  @DisplayName("skip")
  class Skip {

    @Test
    @DisplayName("should skip execution when skip is true")
    void shouldSkipWhenSkipIsTrue() throws MojoExecutionException {
      mojo.setSkip(true);

      mojo.execute();

      // Should not throw even without required configuration
      verify(mockProject, never()).addCompileSourceRoot(org.mockito.ArgumentMatchers.anyString());
    }
  }

  @Nested
  @DisplayName("execution")
  class Execution {

    @Test
    @DisplayName("should execute successfully with valid configuration")
    void shouldExecuteSuccessfully() throws IOException, MojoExecutionException {
      Path schemaFile = tempDir.resolve("schema.json");
      Files.writeString(schemaFile, SAMPLE_SCHEMA);
      Path outputDir = tempDir.resolve("output");

      mojo.setSchemaFile(schemaFile.toFile());
      mojo.setOutputDirectory(outputDir.toFile());
      mojo.setPackageName("com.example.graphql");

      mojo.execute();

      assertThat(outputDir).exists();
    }

    @Test
    @DisplayName("should add compile source root when enabled")
    void shouldAddCompileSourceRoot() throws IOException, MojoExecutionException {
      Path schemaFile = tempDir.resolve("schema.json");
      Files.writeString(schemaFile, SAMPLE_SCHEMA);
      Path outputDir = tempDir.resolve("output");

      mojo.setSchemaFile(schemaFile.toFile());
      mojo.setOutputDirectory(outputDir.toFile());
      mojo.setPackageName("com.example");
      mojo.setAddCompileSourceRoot(true);

      mojo.execute();

      verify(mockProject).addCompileSourceRoot(outputDir.toAbsolutePath().toString());
    }

    @Test
    @DisplayName("should not add compile source root when disabled")
    void shouldNotAddCompileSourceRootWhenDisabled() throws IOException, MojoExecutionException {
      Path schemaFile = tempDir.resolve("schema.json");
      Files.writeString(schemaFile, SAMPLE_SCHEMA);
      Path outputDir = tempDir.resolve("output");

      mojo.setSchemaFile(schemaFile.toFile());
      mojo.setOutputDirectory(outputDir.toFile());
      mojo.setPackageName("com.example");
      mojo.setAddCompileSourceRoot(false);

      mojo.execute();

      verify(mockProject, never()).addCompileSourceRoot(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    @DisplayName("should support custom scalars")
    void shouldSupportCustomScalars() throws IOException, MojoExecutionException {
      Path schemaFile = tempDir.resolve("schema.json");
      Files.writeString(schemaFile, SAMPLE_SCHEMA);
      Path outputDir = tempDir.resolve("output");

      mojo.setSchemaFile(schemaFile.toFile());
      mojo.setOutputDirectory(outputDir.toFile());
      mojo.setPackageName("com.example");
      mojo.setScalars(Map.of("DateTime", "java.time.Instant", "UUID", "java.util.UUID"));

      // Should execute without error
      mojo.execute();

      assertThat(outputDir).exists();
    }

    @Test
    @DisplayName("should skip generation when up-to-date")
    void shouldSkipWhenUpToDate() throws IOException, MojoExecutionException {
      Path schemaFile = tempDir.resolve("schema.json");
      Files.writeString(schemaFile, SAMPLE_SCHEMA);
      Path outputDir = tempDir.resolve("output");

      mojo.setSchemaFile(schemaFile.toFile());
      mojo.setOutputDirectory(outputDir.toFile());
      mojo.setPackageName("com.example");
      mojo.setSkipIfUpToDate(true);

      // First execution
      mojo.execute();

      // Second execution should be skipped (no exception)
      mojo.execute();

      assertThat(outputDir).exists();
    }
  }
}
