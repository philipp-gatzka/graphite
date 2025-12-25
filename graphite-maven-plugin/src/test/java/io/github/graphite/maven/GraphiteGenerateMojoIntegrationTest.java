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
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Map;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration tests for GraphiteGenerateMojo that verify actual code generation output.
 *
 * <p>These tests mirror the maven-invoker-plugin style integration tests, testing:
 *
 * <ul>
 *   <li>basic-generation - Minimal configuration
 *   <li>custom-package - Custom package name
 *   <li>custom-scalars - Custom scalar mappings
 *   <li>validation-error - Invalid schema handling
 * </ul>
 */
@DisplayName("GraphiteGenerateMojo Integration Tests")
class GraphiteGenerateMojoIntegrationTest {

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
                {"name": "email", "args": [], "type": {"kind": "SCALAR", "name": "String", "ofType": null}},
                {"name": "createdAt", "args": [], "type": {"kind": "SCALAR", "name": "DateTime", "ofType": null}}
              ],
              "interfaces": []
            },
            {"kind": "SCALAR", "name": "ID", "fields": null, "interfaces": null},
            {"kind": "SCALAR", "name": "String", "fields": null, "interfaces": null},
            {"kind": "SCALAR", "name": "DateTime", "fields": null, "interfaces": null},
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

  private static final String SCHEMA_WITH_ENUM =
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
                  "name": "usersByStatus",
                  "args": [{"name": "status", "type": {"kind": "NON_NULL", "name": null, "ofType": {"kind": "ENUM", "name": "UserStatus", "ofType": null}}}],
                  "type": {"kind": "LIST", "name": null, "ofType": {"kind": "OBJECT", "name": "User", "ofType": null}}
                }
              ],
              "interfaces": []
            },
            {
              "kind": "OBJECT",
              "name": "User",
              "fields": [
                {"name": "id", "args": [], "type": {"kind": "NON_NULL", "name": null, "ofType": {"kind": "SCALAR", "name": "ID", "ofType": null}}},
                {"name": "status", "args": [], "type": {"kind": "NON_NULL", "name": null, "ofType": {"kind": "ENUM", "name": "UserStatus", "ofType": null}}}
              ],
              "interfaces": []
            },
            {
              "kind": "ENUM",
              "name": "UserStatus",
              "fields": null,
              "enumValues": [
                {"name": "ACTIVE", "isDeprecated": false},
                {"name": "INACTIVE", "isDeprecated": false},
                {"name": "SUSPENDED", "isDeprecated": true, "deprecationReason": "Use INACTIVE instead"}
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
  @DisplayName("basic-generation")
  class BasicGeneration {

    @Test
    @DisplayName("should generate DTOs with correct structure")
    void shouldGenerateDtosWithCorrectStructure() throws Exception {
      Path schemaFile = writeSchema(MINIMAL_SCHEMA);
      Path outputDir = tempDir.resolve("generated-sources/graphite");

      mojo.setSchemaFile(schemaFile.toFile());
      mojo.setOutputDirectory(outputDir.toFile());
      mojo.setPackageName("com.example.graphql");

      mojo.execute();

      // Verify UserDTO
      Path userDto = outputDir.resolve("com/example/graphql/type/UserDTO.java");
      assertThat(userDto).exists();
      String content = Files.readString(userDto);
      assertThat(content).contains("public record UserDTO");
      assertThat(content).contains("String id");
      assertThat(content).contains("String name");
      assertThat(content).contains("String email");
    }

    @Test
    @DisplayName("should generate query classes")
    void shouldGenerateQueryClasses() throws Exception {
      Path schemaFile = writeSchema(MINIMAL_SCHEMA);
      Path outputDir = tempDir.resolve("generated-sources/graphite");

      mojo.setSchemaFile(schemaFile.toFile());
      mojo.setOutputDirectory(outputDir.toFile());
      mojo.setPackageName("com.example.graphql");

      mojo.execute();

      // Verify UserQuery
      Path userQuery = outputDir.resolve("com/example/graphql/query/UserQuery.java");
      assertThat(userQuery).exists();
      String content = Files.readString(userQuery);
      assertThat(content).contains("public final class UserQuery");
      assertThat(content).contains("implements GraphQLOperation");
    }

    @Test
    @DisplayName("should generate projections")
    void shouldGenerateProjections() throws Exception {
      Path schemaFile = writeSchema(MINIMAL_SCHEMA);
      Path outputDir = tempDir.resolve("generated-sources/graphite");

      mojo.setSchemaFile(schemaFile.toFile());
      mojo.setOutputDirectory(outputDir.toFile());
      mojo.setPackageName("com.example.graphql");

      mojo.execute();

      // Verify UserProjection
      Path userProjection = outputDir.resolve("com/example/graphql/type/UserProjection.java");
      assertThat(userProjection).exists();
      String content = Files.readString(userProjection);
      assertThat(content).contains("public final class UserProjection");
    }
  }

  @Nested
  @DisplayName("custom-package")
  class CustomPackage {

    @Test
    @DisplayName("should use custom package name")
    void shouldUseCustomPackageName() throws Exception {
      Path schemaFile = writeSchema(MINIMAL_SCHEMA);
      Path outputDir = tempDir.resolve("generated-sources/graphite");

      mojo.setSchemaFile(schemaFile.toFile());
      mojo.setOutputDirectory(outputDir.toFile());
      mojo.setPackageName("io.custom.api.generated");

      mojo.execute();

      // Verify files in custom package
      Path userDto = outputDir.resolve("io/custom/api/generated/type/UserDTO.java");
      assertThat(userDto).exists();
      String content = Files.readString(userDto);
      assertThat(content).contains("package io.custom.api.generated.type;");
    }

    @Test
    @DisplayName("should create correct directory structure")
    void shouldCreateCorrectDirectoryStructure() throws Exception {
      Path schemaFile = writeSchema(MINIMAL_SCHEMA);
      Path outputDir = tempDir.resolve("generated-sources/graphite");

      mojo.setSchemaFile(schemaFile.toFile());
      mojo.setOutputDirectory(outputDir.toFile());
      mojo.setPackageName("com.deep.nested.package.graphql");

      mojo.execute();

      // Verify directory structure
      assertThat(outputDir.resolve("com/deep/nested/package/graphql/type")).isDirectory();
      assertThat(outputDir.resolve("com/deep/nested/package/graphql/query")).isDirectory();
    }
  }

  @Nested
  @DisplayName("custom-scalars")
  class CustomScalars {

    @Test
    @DisplayName("should map custom DateTime scalar")
    void shouldMapCustomDateTimeScalar() throws Exception {
      Path schemaFile = writeSchema(MINIMAL_SCHEMA);
      Path outputDir = tempDir.resolve("generated-sources/graphite");

      mojo.setSchemaFile(schemaFile.toFile());
      mojo.setOutputDirectory(outputDir.toFile());
      mojo.setPackageName("com.example.graphql");
      mojo.setScalars(Map.of("DateTime", "java.time.Instant"));

      mojo.execute();

      // Verify UserDTO uses Instant for createdAt
      Path userDto = outputDir.resolve("com/example/graphql/type/UserDTO.java");
      assertThat(userDto).exists();
      String content = Files.readString(userDto);
      assertThat(content).contains("java.time.Instant");
      assertThat(content).contains("Instant createdAt");
    }

    @Test
    @DisplayName("should support multiple custom scalars")
    void shouldSupportMultipleCustomScalars() throws Exception {
      Path schemaFile = writeSchema(MINIMAL_SCHEMA);
      Path outputDir = tempDir.resolve("generated-sources/graphite");

      mojo.setSchemaFile(schemaFile.toFile());
      mojo.setOutputDirectory(outputDir.toFile());
      mojo.setPackageName("com.example.graphql");
      mojo.setScalars(
          Map.of(
              "DateTime", "java.time.Instant",
              "ID", "java.util.UUID"));

      mojo.execute();

      // Verify UserDTO uses mapped types
      Path userDto = outputDir.resolve("com/example/graphql/type/UserDTO.java");
      assertThat(userDto).exists();
      String content = Files.readString(userDto);
      assertThat(content).contains("java.time.Instant");
      assertThat(content).contains("java.util.UUID");
    }
  }

  @Nested
  @DisplayName("mutations-and-inputs")
  class MutationsAndInputs {

    @Test
    @DisplayName("should generate mutation classes")
    void shouldGenerateMutationClasses() throws Exception {
      Path schemaFile = writeSchema(SCHEMA_WITH_MUTATION);
      Path outputDir = tempDir.resolve("generated-sources/graphite");

      mojo.setSchemaFile(schemaFile.toFile());
      mojo.setOutputDirectory(outputDir.toFile());
      mojo.setPackageName("com.example.graphql");

      mojo.execute();

      // Verify CreateUserMutation
      Path mutation = outputDir.resolve("com/example/graphql/mutation/CreateUserMutation.java");
      assertThat(mutation).exists();
      String content = Files.readString(mutation);
      assertThat(content).contains("public final class CreateUserMutation");
      assertThat(content).contains("implements GraphQLOperation");
    }

    @Test
    @DisplayName("should generate input type classes")
    void shouldGenerateInputTypeClasses() throws Exception {
      Path schemaFile = writeSchema(SCHEMA_WITH_MUTATION);
      Path outputDir = tempDir.resolve("generated-sources/graphite");

      mojo.setSchemaFile(schemaFile.toFile());
      mojo.setOutputDirectory(outputDir.toFile());
      mojo.setPackageName("com.example.graphql");

      mojo.execute();

      // Verify CreateUserInput
      Path input = outputDir.resolve("com/example/graphql/input/CreateUserInput.java");
      assertThat(input).exists();
      String content = Files.readString(input);
      assertThat(content).contains("public final class CreateUserInput");
      assertThat(content).contains("String name");
      assertThat(content).contains("String email");
    }
  }

  @Nested
  @DisplayName("enums")
  class Enums {

    @Test
    @DisplayName("should generate enum types")
    void shouldGenerateEnumTypes() throws Exception {
      Path schemaFile = writeSchema(SCHEMA_WITH_ENUM);
      Path outputDir = tempDir.resolve("generated-sources/graphite");

      mojo.setSchemaFile(schemaFile.toFile());
      mojo.setOutputDirectory(outputDir.toFile());
      mojo.setPackageName("com.example.graphql");

      mojo.execute();

      // Verify UserStatus enum
      Path enumFile = outputDir.resolve("com/example/graphql/enumeration/UserStatus.java");
      assertThat(enumFile).exists();
      String content = Files.readString(enumFile);
      assertThat(content).contains("public enum UserStatus");
      assertThat(content).contains("ACTIVE");
      assertThat(content).contains("INACTIVE");
      assertThat(content).contains("SUSPENDED");
    }

    @Test
    @DisplayName("should include deprecation annotation for deprecated enum values")
    void shouldIncludeDeprecationAnnotation() throws Exception {
      Path schemaFile = writeSchema(SCHEMA_WITH_ENUM);
      Path outputDir = tempDir.resolve("generated-sources/graphite");

      mojo.setSchemaFile(schemaFile.toFile());
      mojo.setOutputDirectory(outputDir.toFile());
      mojo.setPackageName("com.example.graphql");

      mojo.execute();

      Path enumFile = outputDir.resolve("com/example/graphql/enumeration/UserStatus.java");
      String content = Files.readString(enumFile);
      assertThat(content).contains("@Deprecated");
    }
  }

  @Nested
  @DisplayName("validation-error")
  class ValidationError {

    @Test
    @DisplayName("should fail on invalid JSON")
    void shouldFailOnInvalidJson() throws Exception {
      Path schemaFile = tempDir.resolve("schema.json");
      Files.writeString(schemaFile, "{ invalid json }");
      Path outputDir = tempDir.resolve("generated-sources/graphite");

      mojo.setSchemaFile(schemaFile.toFile());
      mojo.setOutputDirectory(outputDir.toFile());
      mojo.setPackageName("com.example.graphql");

      assertThatThrownBy(() -> mojo.execute())
          .isInstanceOf(MojoExecutionException.class)
          .hasMessageContaining("Failed to generate GraphQL code");
    }

    @Test
    @DisplayName("should fail on missing __schema field")
    void shouldFailOnMissingSchemaField() throws Exception {
      Path schemaFile = tempDir.resolve("schema.json");
      Files.writeString(schemaFile, "{ \"data\": {} }");
      Path outputDir = tempDir.resolve("generated-sources/graphite");

      mojo.setSchemaFile(schemaFile.toFile());
      mojo.setOutputDirectory(outputDir.toFile());
      mojo.setPackageName("com.example.graphql");

      assertThatThrownBy(() -> mojo.execute())
          .isInstanceOf(MojoExecutionException.class)
          .hasMessageContaining("Failed to generate GraphQL code");
    }

    @Test
    @DisplayName("should fail on empty types array")
    void shouldFailOnEmptyTypesArray() throws Exception {
      Path schemaFile = tempDir.resolve("schema.json");
      String emptySchema =
          """
          {
            "__schema": {
              "queryType": {"name": "Query"},
              "mutationType": null,
              "subscriptionType": null,
              "types": []
            }
          }
          """;
      Files.writeString(schemaFile, emptySchema);
      Path outputDir = tempDir.resolve("generated-sources/graphite");

      mojo.setSchemaFile(schemaFile.toFile());
      mojo.setOutputDirectory(outputDir.toFile());
      mojo.setPackageName("com.example.graphql");

      assertThatThrownBy(() -> mojo.execute())
          .isInstanceOf(MojoExecutionException.class)
          .hasMessageContaining("Failed to generate GraphQL code");
    }
  }

  @Nested
  @DisplayName("incremental-build")
  class IncrementalBuild {

    @Test
    @DisplayName("should skip generation when up-to-date")
    void shouldSkipWhenUpToDate() throws Exception {
      Path schemaFile = writeSchema(MINIMAL_SCHEMA);
      Path outputDir = tempDir.resolve("generated-sources/graphite");

      mojo.setSchemaFile(schemaFile.toFile());
      mojo.setOutputDirectory(outputDir.toFile());
      mojo.setPackageName("com.example.graphql");
      mojo.setSkipIfUpToDate(true);

      // First execution generates files
      mojo.execute();
      Path userDto = outputDir.resolve("com/example/graphql/type/UserDTO.java");
      assertThat(userDto).exists();

      // Set file timestamp to a known past value to detect any regeneration
      FileTime pastTime = FileTime.from(Instant.now().minusSeconds(60));
      Files.setLastModifiedTime(userDto, pastTime);
      long firstModTime = Files.getLastModifiedTime(userDto).toMillis();

      // Second execution should be skipped
      mojo.execute();

      // File should not be modified
      long secondModTime = Files.getLastModifiedTime(userDto).toMillis();
      assertThat(secondModTime).isEqualTo(firstModTime);
    }

    @Test
    @DisplayName("should regenerate when schema changes")
    void shouldRegenerateWhenSchemaChanges() throws Exception {
      Path schemaFile = writeSchema(MINIMAL_SCHEMA);
      Path outputDir = tempDir.resolve("generated-sources/graphite");

      mojo.setSchemaFile(schemaFile.toFile());
      mojo.setOutputDirectory(outputDir.toFile());
      mojo.setPackageName("com.example.graphql");
      mojo.setSkipIfUpToDate(true);

      // First execution
      mojo.execute();
      Path userDto = outputDir.resolve("com/example/graphql/type/UserDTO.java");

      // Set generated file timestamp to the past to ensure schema appears newer
      FileTime pastTime = FileTime.from(Instant.now().minusSeconds(60));
      Files.setLastModifiedTime(userDto, pastTime);
      long firstModTime = Files.getLastModifiedTime(userDto).toMillis();

      // Modify schema (will have current timestamp, which is newer than generated file)
      Files.writeString(schemaFile, SCHEMA_WITH_MUTATION);

      // Second execution should regenerate
      mojo.execute();

      // File should be modified (regenerated)
      long secondModTime = Files.getLastModifiedTime(userDto).toMillis();
      assertThat(secondModTime).isGreaterThan(firstModTime);
    }
  }

  private Path writeSchema(String content) throws IOException {
    Path schemaFile = tempDir.resolve("schema.json");
    Files.writeString(schemaFile, content);
    return schemaFile;
  }
}
