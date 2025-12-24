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
package io.github.graphite.codegen.generator;

import static org.assertj.core.api.Assertions.assertThat;

import com.palantir.javapoet.JavaFile;
import io.github.graphite.codegen.CodegenConfiguration;
import io.github.graphite.codegen.schema.SchemaModel;
import io.github.graphite.codegen.schema.SchemaParser;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TypeGenerator")
class TypeGeneratorTest {

  private SchemaParser schemaParser;
  private CodegenConfiguration configuration;
  private SchemaModel schema;

  @BeforeEach
  void setUp() {
    schemaParser = new SchemaParser();
    Path schemaPath = Path.of("src/test/resources/schemas/complete-schema.json").toAbsolutePath();
    schema = schemaParser.parse(schemaPath);
    configuration =
        CodegenConfiguration.builder()
            .schemaFile(schemaPath)
            .outputDirectory(Path.of("build/generated"))
            .packageName("com.example.graphql")
            .build();
  }

  private JavaFile findFileByTypeName(List<JavaFile> files, String typeName) {
    return files.stream()
        .filter(f -> f.typeSpec().name().equals(typeName))
        .findFirst()
        .orElseThrow(() -> new AssertionError("No file found for type: " + typeName));
  }

  @Nested
  @DisplayName("generate()")
  class Generate {

    @Test
    @DisplayName("should generate DTO records for all non-root types")
    void shouldGenerateDTOsForAllTypes() {
      TypeGenerator generator = new TypeGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();

      // Should skip Query, Mutation, Subscription
      assertThat(files).hasSizeGreaterThanOrEqualTo(2);

      // Should have User and Post DTOs
      List<String> typeNames = files.stream().map(f -> f.typeSpec().name()).toList();
      assertThat(typeNames).contains("UserDTO", "PostDTO");
      assertThat(typeNames).doesNotContain("QueryDTO", "MutationDTO");
    }

    @Test
    @DisplayName("should use correct package for type DTOs")
    void shouldUseCorrectPackage() {
      TypeGenerator generator = new TypeGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();

      for (JavaFile file : files) {
        assertThat(file.packageName()).isEqualTo("com.example.graphql.type");
      }
    }
  }

  @Nested
  @DisplayName("Generated record structure")
  class GeneratedRecordStructure {

    @Test
    @DisplayName("should generate record with correct fields")
    void shouldGenerateRecordWithFields() {
      TypeGenerator generator = new TypeGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userFile = findFileByTypeName(files, "UserDTO");

      String source = userFile.toString();

      // Should be a record
      assertThat(source).contains("public record UserDTO(");

      // Should have expected fields
      assertThat(source).contains("String id");
      assertThat(source).contains("String name");
      assertThat(source).contains("String email");
      assertThat(source).contains("UserStatus status");
    }

    @Test
    @DisplayName("should add @NotNull annotation for non-nullable fields")
    void shouldAddNotNullAnnotation() {
      TypeGenerator generator = new TypeGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userFile = findFileByTypeName(files, "UserDTO");

      String source = userFile.toString();

      // id is non-null in schema
      assertThat(source).contains("@NotNull String id");
      assertThat(source).contains("@NotNull String name");
    }

    @Test
    @DisplayName("should add @Nullable annotation for nullable fields")
    void shouldAddNullableAnnotation() {
      TypeGenerator generator = new TypeGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userFile = findFileByTypeName(files, "UserDTO");

      String source = userFile.toString();

      // email is nullable in schema
      assertThat(source).contains("@Nullable String email");
    }

    @Test
    @DisplayName("should map DateTime scalar to Instant")
    void shouldMapDateTimeToInstant() {
      TypeGenerator generator = new TypeGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userFile = findFileByTypeName(files, "UserDTO");

      String source = userFile.toString();

      assertThat(source).contains("Instant createdAt");
      assertThat(source).contains("import java.time.Instant;");
    }

    @Test
    @DisplayName("should generate list types correctly")
    void shouldGenerateListTypes() {
      TypeGenerator generator = new TypeGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userFile = findFileByTypeName(files, "UserDTO");

      String source = userFile.toString();

      // posts is a list in schema: [Post!]!
      assertThat(source).contains("List<PostDTO> posts");
      assertThat(source).contains("import java.util.List;");
    }

    @Test
    @DisplayName("should reference enum types correctly")
    void shouldReferenceEnumTypes() {
      TypeGenerator generator = new TypeGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userFile = findFileByTypeName(files, "UserDTO");

      String source = userFile.toString();

      // status is UserStatus enum
      assertThat(source).contains("UserStatus status");
      assertThat(source).contains("import com.example.graphql.enumeration.UserStatus;");
    }

    @Test
    @DisplayName("should add JavaDoc from GraphQL description")
    void shouldAddJavaDoc() {
      TypeGenerator generator = new TypeGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userFile = findFileByTypeName(files, "UserDTO");

      String source = userFile.toString();

      // User has description "A user in the system"
      assertThat(source).contains("A user in the system");
    }
  }

  @Nested
  @DisplayName("Type references")
  class TypeReferences {

    @Test
    @DisplayName("should reference other object types as DTOs")
    void shouldReferenceOtherTypesAsDTOs() {
      TypeGenerator generator = new TypeGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile postFile = findFileByTypeName(files, "PostDTO");

      String source = postFile.toString();

      // author is User type
      assertThat(source).contains("UserDTO author");
    }
  }

  @Nested
  @DisplayName("Custom scalar mappings")
  class CustomScalarMappings {

    @Test
    @DisplayName("should use custom scalar mappings from configuration")
    void shouldUseCustomScalarMappings() {
      CodegenConfiguration customConfig =
          CodegenConfiguration.builder()
              .schemaFile(configuration.schemaFile())
              .outputDirectory(configuration.outputDirectory())
              .packageName(configuration.packageName())
              .customScalar("DateTime", "java.time.OffsetDateTime")
              .build();

      TypeGenerator generator = new TypeGenerator(customConfig, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userFile = findFileByTypeName(files, "UserDTO");

      String source = userFile.toString();

      assertThat(source).contains("OffsetDateTime createdAt");
      assertThat(source).contains("import java.time.OffsetDateTime;");
    }
  }

  @Nested
  @DisplayName("Deprecation handling")
  class DeprecationHandling {

    @Test
    @DisplayName("should add @Deprecated annotation for deprecated fields")
    void shouldAddDeprecatedAnnotation() {
      // Create a schema with a deprecated field
      String json =
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
                      "name": "test",
                      "args": [],
                      "type": { "kind": "OBJECT", "name": "TestType" },
                      "isDeprecated": false
                    }
                  ],
                  "interfaces": []
                },
                {
                  "kind": "OBJECT",
                  "name": "TestType",
                  "description": "A test type",
                  "fields": [
                    {
                      "name": "id",
                      "args": [],
                      "type": { "kind": "NON_NULL", "ofType": { "kind": "SCALAR", "name": "ID" } },
                      "isDeprecated": false
                    },
                    {
                      "name": "oldField",
                      "args": [],
                      "type": { "kind": "SCALAR", "name": "String" },
                      "isDeprecated": true,
                      "deprecationReason": "Use newField instead"
                    }
                  ],
                  "interfaces": []
                }
              ]
            }
          }
          """;

      SchemaModel testSchema = schemaParser.parse(json);
      TypeGenerator generator = new TypeGenerator(configuration, testSchema);

      List<JavaFile> files = generator.generate();
      JavaFile testFile = findFileByTypeName(files, "TestTypeDTO");

      String source = testFile.toString();

      assertThat(source).contains("@Deprecated");
    }
  }
}
