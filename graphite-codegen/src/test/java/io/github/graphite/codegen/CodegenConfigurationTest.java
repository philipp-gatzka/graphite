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

import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("CodegenConfiguration")
class CodegenConfigurationTest {

  private static final Path SCHEMA_FILE = Path.of("schema.json");
  private static final Path OUTPUT_DIR = Path.of("build/generated");
  private static final String PACKAGE_NAME = "com.example.graphql";

  @Nested
  @DisplayName("builder")
  class Builder {

    @Test
    @DisplayName("should build configuration with required fields")
    void shouldBuildWithRequiredFields() {
      CodegenConfiguration config =
          CodegenConfiguration.builder()
              .schemaFile(SCHEMA_FILE)
              .outputDirectory(OUTPUT_DIR)
              .packageName(PACKAGE_NAME)
              .build();

      assertThat(config.schemaFile()).isEqualTo(SCHEMA_FILE);
      assertThat(config.outputDirectory()).isEqualTo(OUTPUT_DIR);
      assertThat(config.packageName()).isEqualTo(PACKAGE_NAME);
    }

    @Test
    @DisplayName("should use default naming convention")
    void shouldUseDefaultNamingConvention() {
      CodegenConfiguration config =
          CodegenConfiguration.builder()
              .schemaFile(SCHEMA_FILE)
              .outputDirectory(OUTPUT_DIR)
              .packageName(PACKAGE_NAME)
              .build();

      assertThat(config.namingConvention()).isNotNull();
      assertThat(config.namingConvention().getTypeName("User")).isEqualTo("UserDTO");
    }

    @Test
    @DisplayName("should use default skipIfUpToDate as true")
    void shouldUseDefaultSkipIfUpToDate() {
      CodegenConfiguration config =
          CodegenConfiguration.builder()
              .schemaFile(SCHEMA_FILE)
              .outputDirectory(OUTPUT_DIR)
              .packageName(PACKAGE_NAME)
              .build();

      assertThat(config.skipIfUpToDate()).isTrue();
    }

    @Test
    @DisplayName("should use empty custom scalar mappings by default")
    void shouldUseEmptyCustomScalarMappingsByDefault() {
      CodegenConfiguration config =
          CodegenConfiguration.builder()
              .schemaFile(SCHEMA_FILE)
              .outputDirectory(OUTPUT_DIR)
              .packageName(PACKAGE_NAME)
              .build();

      assertThat(config.customScalarMappings()).isEmpty();
    }

    @Test
    @DisplayName("should allow adding custom scalar")
    void shouldAllowAddingCustomScalar() {
      CodegenConfiguration config =
          CodegenConfiguration.builder()
              .schemaFile(SCHEMA_FILE)
              .outputDirectory(OUTPUT_DIR)
              .packageName(PACKAGE_NAME)
              .customScalar("DateTime", "java.time.Instant")
              .customScalar("Money", "com.example.Money")
              .build();

      assertThat(config.customScalarMappings())
          .containsEntry("DateTime", "java.time.Instant")
          .containsEntry("Money", "com.example.Money");
    }

    @Test
    @DisplayName("should allow setting all custom scalar mappings")
    void shouldAllowSettingAllCustomScalarMappings() {
      Map<String, String> mappings =
          Map.of("DateTime", "java.time.Instant", "UUID", "java.util.UUID");

      CodegenConfiguration config =
          CodegenConfiguration.builder()
              .schemaFile(SCHEMA_FILE)
              .outputDirectory(OUTPUT_DIR)
              .packageName(PACKAGE_NAME)
              .customScalarMappings(mappings)
              .build();

      assertThat(config.customScalarMappings()).hasSize(2);
    }

    @Test
    @DisplayName("should allow custom naming convention")
    void shouldAllowCustomNamingConvention() {
      NamingConvention custom =
          NamingConvention.withSuffixes("Model", "Request", "Fetch", "Action");

      CodegenConfiguration config =
          CodegenConfiguration.builder()
              .schemaFile(SCHEMA_FILE)
              .outputDirectory(OUTPUT_DIR)
              .packageName(PACKAGE_NAME)
              .namingConvention(custom)
              .build();

      assertThat(config.namingConvention().getTypeName("User")).isEqualTo("UserModel");
    }

    @Test
    @DisplayName("should allow setting skipIfUpToDate to false")
    void shouldAllowSettingSkipIfUpToDateToFalse() {
      CodegenConfiguration config =
          CodegenConfiguration.builder()
              .schemaFile(SCHEMA_FILE)
              .outputDirectory(OUTPUT_DIR)
              .packageName(PACKAGE_NAME)
              .skipIfUpToDate(false)
              .build();

      assertThat(config.skipIfUpToDate()).isFalse();
    }

    @Test
    @DisplayName("should throw when schemaFile is not set")
    void shouldThrowWhenSchemaFileNotSet() {
      var builder =
          CodegenConfiguration.builder().outputDirectory(OUTPUT_DIR).packageName(PACKAGE_NAME);
      assertThatThrownBy(builder::build)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("schemaFile");
    }

    @Test
    @DisplayName("should throw when outputDirectory is not set")
    void shouldThrowWhenOutputDirectoryNotSet() {
      var builder =
          CodegenConfiguration.builder().schemaFile(SCHEMA_FILE).packageName(PACKAGE_NAME);
      assertThatThrownBy(builder::build)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("outputDirectory");
    }

    @Test
    @DisplayName("should throw when packageName is not set")
    void shouldThrowWhenPackageNameNotSet() {
      var builder =
          CodegenConfiguration.builder().schemaFile(SCHEMA_FILE).outputDirectory(OUTPUT_DIR);
      assertThatThrownBy(builder::build)
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("packageName");
    }
  }

  @Nested
  @DisplayName("record validation")
  class RecordValidation {

    private static final Map<String, String> EMPTY_MAPPINGS = Map.of();
    private static final NamingConvention DEFAULT_CONVENTION = NamingConvention.defaults();

    @Test
    @DisplayName("should throw on null schemaFile")
    void shouldThrowOnNullSchemaFile() {
      assertThatThrownBy(
              () ->
                  new CodegenConfiguration(
                      null, OUTPUT_DIR, PACKAGE_NAME, EMPTY_MAPPINGS, DEFAULT_CONVENTION, true))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("schemaFile");
    }

    @Test
    @DisplayName("should throw on null outputDirectory")
    void shouldThrowOnNullOutputDirectory() {
      assertThatThrownBy(
              () ->
                  new CodegenConfiguration(
                      SCHEMA_FILE, null, PACKAGE_NAME, EMPTY_MAPPINGS, DEFAULT_CONVENTION, true))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("outputDirectory");
    }

    @Test
    @DisplayName("should throw on null packageName")
    void shouldThrowOnNullPackageName() {
      assertThatThrownBy(
              () ->
                  new CodegenConfiguration(
                      SCHEMA_FILE, OUTPUT_DIR, null, EMPTY_MAPPINGS, DEFAULT_CONVENTION, true))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("packageName");
    }

    @Test
    @DisplayName("should throw on blank packageName")
    void shouldThrowOnBlankPackageName() {
      assertThatThrownBy(
              () ->
                  new CodegenConfiguration(
                      SCHEMA_FILE, OUTPUT_DIR, "  ", EMPTY_MAPPINGS, DEFAULT_CONVENTION, true))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("packageName");
    }

    @Test
    @DisplayName("should throw on null customScalarMappings")
    void shouldThrowOnNullCustomScalarMappings() {
      assertThatThrownBy(
              () ->
                  new CodegenConfiguration(
                      SCHEMA_FILE, OUTPUT_DIR, PACKAGE_NAME, null, DEFAULT_CONVENTION, true))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("customScalarMappings");
    }

    @Test
    @DisplayName("should throw on null namingConvention")
    void shouldThrowOnNullNamingConvention() {
      assertThatThrownBy(
              () ->
                  new CodegenConfiguration(
                      SCHEMA_FILE, OUTPUT_DIR, PACKAGE_NAME, EMPTY_MAPPINGS, null, true))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("namingConvention");
    }

    @Test
    @DisplayName("should make customScalarMappings unmodifiable")
    void shouldMakeCustomScalarMappingsUnmodifiable() {
      java.util.HashMap<String, String> mutableMap = new java.util.HashMap<>();
      mutableMap.put("DateTime", "java.time.Instant");

      CodegenConfiguration config =
          new CodegenConfiguration(
              SCHEMA_FILE, OUTPUT_DIR, PACKAGE_NAME, mutableMap, DEFAULT_CONVENTION, true);

      Map<String, String> mappings = config.customScalarMappings();
      assertThatThrownBy(() -> mappings.put("New", "Value"))
          .isInstanceOf(UnsupportedOperationException.class);
    }
  }

  @Nested
  @DisplayName("getPackageForCategory")
  class GetPackageForCategory {

    @Test
    @DisplayName("should return package for type category")
    void shouldReturnPackageForTypeCategory() {
      CodegenConfiguration config =
          CodegenConfiguration.builder()
              .schemaFile(SCHEMA_FILE)
              .outputDirectory(OUTPUT_DIR)
              .packageName("com.example.graphql")
              .build();

      assertThat(config.getPackageForCategory("type")).isEqualTo("com.example.graphql.type");
      assertThat(config.getPackageForCategory("input")).isEqualTo("com.example.graphql.input");
      assertThat(config.getPackageForCategory("query")).isEqualTo("com.example.graphql.query");
      assertThat(config.getPackageForCategory("mutation"))
          .isEqualTo("com.example.graphql.mutation");
    }
  }
}
