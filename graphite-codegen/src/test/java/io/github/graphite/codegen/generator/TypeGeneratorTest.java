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
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

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
      assertThat(typeNames)
          .contains("UserDTO", "PostDTO")
          .doesNotContain("QueryDTO", "MutationDTO");
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
      assertThat(source)
          .contains("String id")
          .contains("String name")
          .contains("String email")
          .contains("UserStatus status");
    }

    static Stream<Arguments> fieldAnnotationTestCases() {
      return Stream.of(
          Arguments.of(
              "@NotNull annotation for non-nullable fields",
              new String[] {"@NotNull String id", "@NotNull String name"}),
          Arguments.of(
              "@Nullable annotation for nullable fields", new String[] {"@Nullable String email"}),
          Arguments.of(
              "DateTime scalar mapped to Instant",
              new String[] {"Instant createdAt", "import java.time.Instant;"}),
          Arguments.of(
              "list types correctly",
              new String[] {"List<PostDTO> posts", "import java.util.List;"}));
    }

    @ParameterizedTest(name = "should generate {0}")
    @MethodSource("fieldAnnotationTestCases")
    void shouldGenerateFieldAnnotations(String description, String[] expectedContents) {
      TypeGenerator generator = new TypeGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userFile = findFileByTypeName(files, "UserDTO");

      String source = userFile.toString();

      for (String expected : expectedContents) {
        assertThat(source).contains(expected);
      }
    }

    @Test
    @DisplayName("should reference enum types correctly")
    void shouldReferenceEnumTypes() {
      TypeGenerator generator = new TypeGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userFile = findFileByTypeName(files, "UserDTO");

      String source = userFile.toString();

      // status is UserStatus enum
      assertThat(source)
          .contains("UserStatus status")
          .contains("import com.example.graphql.enumeration.UserStatus;");
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

      assertThat(source)
          .contains("OffsetDateTime createdAt")
          .contains("import java.time.OffsetDateTime;");
    }
  }

  @Nested
  @DisplayName("Interface implementation")
  class InterfaceImplementation {

    @Test
    @DisplayName("should implement interfaces from schema")
    void shouldImplementInterfaces() {
      TypeGenerator generator = new TypeGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userFile = findFileByTypeName(files, "UserDTO");

      String source = userFile.toString();

      // User implements Node and Timestamped interfaces
      assertThat(source).contains("implements NodeDTO, TimestampedDTO");
    }

    @Test
    @DisplayName("should implement multiple interfaces")
    void shouldImplementMultipleInterfaces() {
      TypeGenerator generator = new TypeGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile postFile = findFileByTypeName(files, "PostDTO");

      String source = postFile.toString();

      // Post implements Node and Timestamped interfaces
      assertThat(source).contains("implements NodeDTO, TimestampedDTO");
    }

    @Test
    @DisplayName("should implement union interfaces")
    void shouldImplementUnionInterfaces() {
      TypeGenerator generator = new TypeGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userFile = findFileByTypeName(files, "UserDTO");

      String source = userFile.toString();

      // User is part of SearchResult union
      assertThat(source)
          .contains("SearchResultUnion")
          .contains("import com.example.graphql.union.SearchResultUnion;");
    }

    @Test
    @DisplayName("should implement both interfaces and unions")
    void shouldImplementBothInterfacesAndUnions() {
      TypeGenerator generator = new TypeGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile postFile = findFileByTypeName(files, "PostDTO");

      String source = postFile.toString();

      // Post implements NodeDTO, TimestampedDTO, and SearchResultUnion
      assertThat(source)
          .contains("NodeDTO")
          .contains("TimestampedDTO")
          .contains("SearchResultUnion");
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
