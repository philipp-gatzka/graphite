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

import java.io.File;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GraphiteExtension")
class GraphiteExtensionTest {

  private Project project;
  private GraphiteExtension extension;

  @BeforeEach
  void setUp() {
    project = ProjectBuilder.builder().build();
    project.getPlugins().apply(GraphitePlugin.class);
    extension = project.getExtensions().getByType(GraphiteExtension.class);
  }

  @Nested
  @DisplayName("schemaFile")
  class SchemaFile {

    @Test
    @DisplayName("should accept file via property")
    void shouldAcceptFileViaProperty() {
      File schemaFile = project.file("src/main/resources/schema.json");
      extension.getSchemaFile().set(schemaFile);

      assertThat(extension.getSchemaFile().get().getAsFile()).isEqualTo(schemaFile);
    }

    @Test
    @DisplayName("should accept file via setter")
    void shouldAcceptFileViaSetter() {
      File schemaFile = project.file("schema.json");
      extension.setSchemaFile(schemaFile);

      assertThat(extension.getSchemaFile().get().getAsFile()).isEqualTo(schemaFile);
    }
  }

  @Nested
  @DisplayName("outputDirectory")
  class OutputDirectory {

    @Test
    @DisplayName("should accept directory via property")
    void shouldAcceptDirectoryViaProperty() {
      File outputDir = project.file("build/custom/output");
      extension.getOutputDirectory().set(outputDir);

      assertThat(extension.getOutputDirectory().get().getAsFile()).isEqualTo(outputDir);
    }

    @Test
    @DisplayName("should accept directory via setter")
    void shouldAcceptDirectoryViaSetter() {
      File outputDir = project.file("custom/output");
      extension.setOutputDirectory(outputDir);

      assertThat(extension.getOutputDirectory().get().getAsFile()).isEqualTo(outputDir);
    }
  }

  @Nested
  @DisplayName("packageName")
  class PackageName {

    @Test
    @DisplayName("should accept package name via property")
    void shouldAcceptPackageNameViaProperty() {
      extension.getPackageName().set("com.example.graphql");

      assertThat(extension.getPackageName().get()).isEqualTo("com.example.graphql");
    }

    @Test
    @DisplayName("should accept package name via setter")
    void shouldAcceptPackageNameViaSetter() {
      extension.setPackageName("com.example.api");

      assertThat(extension.getPackageName().get()).isEqualTo("com.example.api");
    }
  }

  @Nested
  @DisplayName("skipIfUpToDate")
  class SkipIfUpToDate {

    @Test
    @DisplayName("should accept boolean via property")
    void shouldAcceptBooleanViaProperty() {
      extension.getSkipIfUpToDate().set(false);

      assertThat(extension.getSkipIfUpToDate().get()).isFalse();
    }

    @Test
    @DisplayName("should accept boolean via setter")
    void shouldAcceptBooleanViaSetter() {
      extension.setSkipIfUpToDate(false);

      assertThat(extension.getSkipIfUpToDate().get()).isFalse();
    }
  }

  @Nested
  @DisplayName("scalars")
  class Scalars {

    @Test
    @DisplayName("should register scalar via action")
    void shouldRegisterScalarViaAction() {
      extension.scalars(handler -> handler.register("DateTime", "java.time.Instant"));

      assertThat(extension.getScalars().get()).containsEntry("DateTime", "java.time.Instant");
    }

    @Test
    @DisplayName("should register multiple scalars")
    void shouldRegisterMultipleScalars() {
      extension.scalars(
          handler -> {
            handler.register("DateTime", "java.time.Instant");
            handler.register("UUID", "java.util.UUID");
            handler.register("Money", "com.example.Money");
          });

      assertThat(extension.getScalars().get())
          .hasSize(3)
          .containsEntry("DateTime", "java.time.Instant")
          .containsEntry("UUID", "java.util.UUID")
          .containsEntry("Money", "com.example.Money");
    }

    @Test
    @DisplayName("should add scalar via map property")
    void shouldAddScalarViaMapProperty() {
      extension.getScalars().put("Date", "java.time.LocalDate");

      assertThat(extension.getScalars().get()).containsEntry("Date", "java.time.LocalDate");
    }
  }

  @Nested
  @DisplayName("constants")
  class Constants {

    @Test
    @DisplayName("should have correct default output path")
    void shouldHaveCorrectDefaultOutputPath() {
      assertThat(GraphiteExtension.DEFAULT_OUTPUT_PATH)
          .isEqualTo("generated/sources/graphite/main/java");
    }
  }
}
