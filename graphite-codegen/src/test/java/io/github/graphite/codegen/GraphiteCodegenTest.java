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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("GraphiteCodegen")
class GraphiteCodegenTest {

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

  @Nested
  @DisplayName("constructor")
  class Constructor {

    @Test
    @DisplayName("should throw on null configuration")
    void shouldThrowOnNullConfiguration() {
      assertThatThrownBy(() -> new GraphiteCodegen(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("configuration");
    }

    @Test
    @DisplayName("should accept valid configuration")
    void shouldAcceptValidConfiguration() throws IOException {
      Path schemaFile = tempDir.resolve("schema.json");
      Files.writeString(schemaFile, SAMPLE_SCHEMA);

      CodegenConfiguration config =
          CodegenConfiguration.builder()
              .schemaFile(schemaFile)
              .outputDirectory(tempDir.resolve("output"))
              .packageName("com.example")
              .build();

      GraphiteCodegen codegen = new GraphiteCodegen(config);

      assertThat(codegen.getConfiguration()).isEqualTo(config);
    }
  }

  @Nested
  @DisplayName("generate")
  class Generate {

    @Test
    @DisplayName("should throw when schema file does not exist")
    void shouldThrowWhenSchemaFileDoesNotExist() {
      Path nonExistent = tempDir.resolve("nonexistent.json");
      CodegenConfiguration config =
          CodegenConfiguration.builder()
              .schemaFile(nonExistent)
              .outputDirectory(tempDir.resolve("output"))
              .packageName("com.example")
              .build();

      GraphiteCodegen codegen = new GraphiteCodegen(config);

      assertThatThrownBy(codegen::generate)
          .isInstanceOf(CodegenException.class)
          .hasMessageContaining("does not exist");
    }

    @Test
    @DisplayName("should throw when schema path is a directory")
    void shouldThrowWhenSchemaPathIsDirectory() throws IOException {
      Path directory = tempDir.resolve("schema-dir");
      Files.createDirectory(directory);

      CodegenConfiguration config =
          CodegenConfiguration.builder()
              .schemaFile(directory)
              .outputDirectory(tempDir.resolve("output"))
              .packageName("com.example")
              .build();

      GraphiteCodegen codegen = new GraphiteCodegen(config);

      assertThatThrownBy(codegen::generate)
          .isInstanceOf(CodegenException.class)
          .hasMessageContaining("not a file");
    }

    @Test
    @DisplayName("should throw when schema file is empty")
    void shouldThrowWhenSchemaFileIsEmpty() throws IOException {
      Path schemaFile = tempDir.resolve("empty.json");
      Files.writeString(schemaFile, "");

      CodegenConfiguration config =
          CodegenConfiguration.builder()
              .schemaFile(schemaFile)
              .outputDirectory(tempDir.resolve("output"))
              .packageName("com.example")
              .build();

      GraphiteCodegen codegen = new GraphiteCodegen(config);

      // SchemaParser will throw SchemaParseException wrapped in CodegenException
      assertThatThrownBy(codegen::generate).isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("should create output directory if it does not exist")
    void shouldCreateOutputDirectory() throws IOException {
      Path schemaFile = tempDir.resolve("schema.json");
      Files.writeString(schemaFile, SAMPLE_SCHEMA);
      Path outputDir = tempDir.resolve("nested/output/dir");

      CodegenConfiguration config =
          CodegenConfiguration.builder()
              .schemaFile(schemaFile)
              .outputDirectory(outputDir)
              .packageName("com.example")
              .build();

      GraphiteCodegen codegen = new GraphiteCodegen(config);
      codegen.generate();

      assertThat(outputDir).exists().isDirectory();
    }

    @Test
    @DisplayName("should return success result")
    void shouldReturnSuccessResult() throws IOException {
      Path schemaFile = tempDir.resolve("schema.json");
      Files.writeString(schemaFile, SAMPLE_SCHEMA);

      CodegenConfiguration config =
          CodegenConfiguration.builder()
              .schemaFile(schemaFile)
              .outputDirectory(tempDir.resolve("output"))
              .packageName("com.example")
              .build();

      GraphiteCodegen codegen = new GraphiteCodegen(config);
      CodegenResult result = codegen.generate();

      assertThat(result.wasSuccessful()).isTrue();
      assertThat(result.wasSkipped()).isFalse();
    }

    @Test
    @DisplayName("should write hash file after generation")
    void shouldWriteHashFile() throws IOException {
      Path schemaFile = tempDir.resolve("schema.json");
      Files.writeString(schemaFile, SAMPLE_SCHEMA);
      Path outputDir = tempDir.resolve("output");

      CodegenConfiguration config =
          CodegenConfiguration.builder()
              .schemaFile(schemaFile)
              .outputDirectory(outputDir)
              .packageName("com.example")
              .build();

      GraphiteCodegen codegen = new GraphiteCodegen(config);
      codegen.generate();

      Path hashFile = outputDir.resolve(".graphite-schema-hash");
      assertThat(hashFile).exists();
      assertThat(Files.readString(hashFile)).isNotBlank();
    }

    @Test
    @DisplayName("should skip generation when up-to-date")
    void shouldSkipGenerationWhenUpToDate() throws IOException {
      Path schemaFile = tempDir.resolve("schema.json");
      Files.writeString(schemaFile, SAMPLE_SCHEMA);

      CodegenConfiguration config =
          CodegenConfiguration.builder()
              .schemaFile(schemaFile)
              .outputDirectory(tempDir.resolve("output"))
              .packageName("com.example")
              .skipIfUpToDate(true)
              .build();

      GraphiteCodegen codegen = new GraphiteCodegen(config);

      // First generation should succeed
      CodegenResult first = codegen.generate();
      assertThat(first.wasSuccessful()).isTrue();

      // Second generation should be skipped
      CodegenResult second = codegen.generate();
      assertThat(second.wasSkipped()).isTrue();
    }

    @Test
    @DisplayName("should not skip when skipIfUpToDate is false")
    void shouldNotSkipWhenDisabled() throws IOException {
      Path schemaFile = tempDir.resolve("schema.json");
      Files.writeString(schemaFile, SAMPLE_SCHEMA);

      CodegenConfiguration config =
          CodegenConfiguration.builder()
              .schemaFile(schemaFile)
              .outputDirectory(tempDir.resolve("output"))
              .packageName("com.example")
              .skipIfUpToDate(false)
              .build();

      GraphiteCodegen codegen = new GraphiteCodegen(config);

      // First generation
      codegen.generate();

      // Second generation should NOT be skipped
      CodegenResult second = codegen.generate();
      assertThat(second.wasSuccessful()).isTrue();
      assertThat(second.wasSkipped()).isFalse();
    }

    @Test
    @DisplayName("should regenerate when schema changes")
    void shouldRegenerateWhenSchemaChanges() throws IOException {
      Path schemaFile = tempDir.resolve("schema.json");
      Files.writeString(schemaFile, SAMPLE_SCHEMA);

      CodegenConfiguration config =
          CodegenConfiguration.builder()
              .schemaFile(schemaFile)
              .outputDirectory(tempDir.resolve("output"))
              .packageName("com.example")
              .skipIfUpToDate(true)
              .build();

      GraphiteCodegen codegen = new GraphiteCodegen(config);

      // First generation
      CodegenResult first = codegen.generate();
      assertThat(first.wasSuccessful()).isTrue();

      // Modify schema
      Files.writeString(schemaFile, SAMPLE_SCHEMA + "  ");

      // Should regenerate
      CodegenResult second = codegen.generate();
      assertThat(second.wasSuccessful()).isTrue();
      assertThat(second.wasSkipped()).isFalse();
    }
  }

  @Nested
  @DisplayName("CodegenResult")
  class CodegenResultTest {

    @Test
    @DisplayName("success() should create success result")
    void successShouldCreateSuccessResult() {
      CodegenResult result = CodegenResult.success(5);

      assertThat(result.status()).isEqualTo(CodegenResult.Status.SUCCESS);
      assertThat(result.filesGenerated()).isEqualTo(5);
      assertThat(result.wasSuccessful()).isTrue();
      assertThat(result.wasSkipped()).isFalse();
    }

    @Test
    @DisplayName("skipped() should create skipped result")
    void skippedShouldCreateSkippedResult() {
      CodegenResult result = CodegenResult.skipped();

      assertThat(result.status()).isEqualTo(CodegenResult.Status.SKIPPED);
      assertThat(result.filesGenerated()).isZero();
      assertThat(result.wasSuccessful()).isFalse();
      assertThat(result.wasSkipped()).isTrue();
    }
  }

  @Nested
  @DisplayName("CodegenException")
  class CodegenExceptionTest {

    @Test
    @DisplayName("should create exception with message")
    void shouldCreateWithMessage() {
      CodegenException exception = new CodegenException("Test error");

      assertThat(exception.getMessage()).isEqualTo("Test error");
      assertThat(exception.getCause()).isNull();
    }

    @Test
    @DisplayName("should create exception with message and cause")
    void shouldCreateWithMessageAndCause() {
      IOException cause = new IOException("IO error");
      CodegenException exception = new CodegenException("Test error", cause);

      assertThat(exception.getMessage()).isEqualTo("Test error");
      assertThat(exception.getCause()).isSameAs(cause);
    }
  }
}
