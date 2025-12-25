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

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.testfixtures.ProjectBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GraphitePlugin")
class GraphitePluginTest {

  private Project project;

  @BeforeEach
  void setUp() {
    project = ProjectBuilder.builder().build();
  }

  @Nested
  @DisplayName("apply")
  class Apply {

    @Test
    @DisplayName("should apply Java plugin")
    void shouldApplyJavaPlugin() {
      project.getPlugins().apply(GraphitePlugin.class);

      assertThat(project.getPlugins().hasPlugin(JavaPlugin.class)).isTrue();
    }

    @Test
    @DisplayName("should create graphite extension")
    void shouldCreateGraphiteExtension() {
      project.getPlugins().apply(GraphitePlugin.class);

      assertThat(project.getExtensions().findByName(GraphitePlugin.EXTENSION_NAME)).isNotNull();
      assertThat(project.getExtensions().findByType(GraphiteExtension.class)).isNotNull();
    }

    @Test
    @DisplayName("should register generateGraphiteCode task")
    void shouldRegisterGenerateTask() {
      project.getPlugins().apply(GraphitePlugin.class);

      assertThat(project.getTasks().findByName(GraphitePlugin.TASK_NAME))
          .isNotNull()
          .isInstanceOf(GraphiteGenerateTask.class);
    }

    @Test
    @DisplayName("should set task group")
    void shouldSetTaskGroup() {
      project.getPlugins().apply(GraphitePlugin.class);

      assertThat(project.getTasks().findByName(GraphitePlugin.TASK_NAME).getGroup())
          .isEqualTo(GraphitePlugin.TASK_GROUP);
    }

    @Test
    @DisplayName("should set task description")
    void shouldSetTaskDescription() {
      project.getPlugins().apply(GraphitePlugin.class);

      assertThat(project.getTasks().findByName(GraphitePlugin.TASK_NAME).getDescription())
          .isNotBlank();
    }
  }

  @Nested
  @DisplayName("extension defaults")
  class ExtensionDefaults {

    @Test
    @DisplayName("should have default output directory")
    void shouldHaveDefaultOutputDirectory() {
      project.getPlugins().apply(GraphitePlugin.class);
      GraphiteExtension extension = project.getExtensions().getByType(GraphiteExtension.class);

      assertThat(extension.getOutputDirectory().isPresent()).isTrue();
      assertThat(extension.getOutputDirectory().get().getAsFile().getPath())
          .contains(GraphiteExtension.DEFAULT_OUTPUT_PATH);
    }

    @Test
    @DisplayName("should have skipIfUpToDate true by default")
    void shouldHaveSkipIfUpToDateTrueByDefault() {
      project.getPlugins().apply(GraphitePlugin.class);
      GraphiteExtension extension = project.getExtensions().getByType(GraphiteExtension.class);

      assertThat(extension.getSkipIfUpToDate().get()).isTrue();
    }

    @Test
    @DisplayName("should have empty scalars by default")
    void shouldHaveEmptyScalarsByDefault() {
      project.getPlugins().apply(GraphitePlugin.class);
      GraphiteExtension extension = project.getExtensions().getByType(GraphiteExtension.class);

      assertThat(extension.getScalars().getOrElse(java.util.Map.of())).isEmpty();
    }
  }

  @Nested
  @DisplayName("task wiring")
  class TaskWiring {

    @Test
    @DisplayName("should wire extension schema file to task")
    void shouldWireSchemaFile() {
      project.getPlugins().apply(GraphitePlugin.class);
      GraphiteExtension extension = project.getExtensions().getByType(GraphiteExtension.class);
      GraphiteGenerateTask task =
          (GraphiteGenerateTask) project.getTasks().findByName(GraphitePlugin.TASK_NAME);

      java.io.File testFile = project.file("schema.json");
      extension.setSchemaFile(testFile);

      assertThat(task.getSchemaFile().get().getAsFile()).isEqualTo(testFile);
    }

    @Test
    @DisplayName("should wire extension package name to task")
    void shouldWirePackageName() {
      project.getPlugins().apply(GraphitePlugin.class);
      GraphiteExtension extension = project.getExtensions().getByType(GraphiteExtension.class);
      GraphiteGenerateTask task =
          (GraphiteGenerateTask) project.getTasks().findByName(GraphitePlugin.TASK_NAME);

      extension.setPackageName("com.example.test");

      assertThat(task.getPackageName().get()).isEqualTo("com.example.test");
    }

    @Test
    @DisplayName("should wire extension skipIfUpToDate to task")
    void shouldWireSkipIfUpToDate() {
      project.getPlugins().apply(GraphitePlugin.class);
      GraphiteExtension extension = project.getExtensions().getByType(GraphiteExtension.class);
      GraphiteGenerateTask task =
          (GraphiteGenerateTask) project.getTasks().findByName(GraphitePlugin.TASK_NAME);

      extension.setSkipIfUpToDate(false);

      assertThat(task.getSkipIfUpToDate().get()).isFalse();
    }
  }

  @Nested
  @DisplayName("constants")
  class Constants {

    @Test
    @DisplayName("should have correct extension name")
    void shouldHaveCorrectExtensionName() {
      assertThat(GraphitePlugin.EXTENSION_NAME).isEqualTo("graphite");
    }

    @Test
    @DisplayName("should have correct task name")
    void shouldHaveCorrectTaskName() {
      assertThat(GraphitePlugin.TASK_NAME).isEqualTo("generateGraphiteCode");
    }

    @Test
    @DisplayName("should have correct task group")
    void shouldHaveCorrectTaskGroup() {
      assertThat(GraphitePlugin.TASK_GROUP).isEqualTo("graphite");
    }
  }
}
