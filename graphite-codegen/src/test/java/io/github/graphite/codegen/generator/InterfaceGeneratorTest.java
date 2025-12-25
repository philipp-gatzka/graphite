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

@DisplayName("InterfaceGenerator")
class InterfaceGeneratorTest {

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
    @DisplayName("should generate sealed interfaces for all interface types")
    void shouldGenerateSealedInterfaces() {
      InterfaceGenerator generator = new InterfaceGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();

      assertThat(files).hasSizeGreaterThanOrEqualTo(2);

      List<String> typeNames = files.stream().map(f -> f.typeSpec().name()).toList();
      assertThat(typeNames).contains("NodeDTO", "TimestampedDTO");
    }

    @Test
    @DisplayName("should use correct package for interfaces")
    void shouldUseCorrectPackage() {
      InterfaceGenerator generator = new InterfaceGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();

      for (JavaFile file : files) {
        assertThat(file.packageName()).isEqualTo("com.example.graphql.type");
      }
    }
  }

  @Nested
  @DisplayName("Generated interface structure")
  class GeneratedInterfaceStructure {

    static Stream<Arguments> interfaceStructureTestCases() {
      return Stream.of(
          Arguments.of(
              "public sealed interface",
              "NodeDTO",
              new String[] {"public sealed interface NodeDTO"}),
          Arguments.of(
              "permits clause with implementing types",
              "NodeDTO",
              new String[] {"permits", "UserDTO", "PostDTO"}),
          Arguments.of("abstract accessor methods", "NodeDTO", new String[] {"String id();"}),
          Arguments.of(
              "multiple accessor methods for interface with multiple fields",
              "TimestampedDTO",
              new String[] {"Instant createdAt();", "Instant updatedAt();"}));
    }

    @ParameterizedTest(name = "should generate {0}")
    @MethodSource("interfaceStructureTestCases")
    void shouldGenerateInterfaceStructure(
        String description, String typeName, String[] expectedContents) {
      InterfaceGenerator generator = new InterfaceGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile file = findFileByTypeName(files, typeName);

      String source = file.toString();

      for (String expected : expectedContents) {
        assertThat(source).contains(expected);
      }
    }

    @Test
    @DisplayName("should add JavaDoc from GraphQL description")
    void shouldAddJavaDoc() {
      InterfaceGenerator generator = new InterfaceGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile nodeFile = findFileByTypeName(files, "NodeDTO");

      String source = nodeFile.toString();

      // Node has description "An object with a globally unique ID"
      assertThat(source).contains("An object with a globally unique ID");
    }

    @Test
    @DisplayName("should add JavaDoc for accessor methods from field descriptions")
    void shouldAddJavaDocForAccessorMethods() {
      InterfaceGenerator generator = new InterfaceGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile nodeFile = findFileByTypeName(files, "NodeDTO");

      String source = nodeFile.toString();

      // id field has description "The globally unique ID"
      assertThat(source).contains("The globally unique ID");
    }
  }

  @Nested
  @DisplayName("Type mapping")
  class TypeMapping {

    @Test
    @DisplayName("should map ID scalar to String")
    void shouldMapIdToString() {
      InterfaceGenerator generator = new InterfaceGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile nodeFile = findFileByTypeName(files, "NodeDTO");

      String source = nodeFile.toString();

      assertThat(source).contains("String id();");
    }

    @Test
    @DisplayName("should map DateTime scalar to Instant")
    void shouldMapDateTimeToInstant() {
      InterfaceGenerator generator = new InterfaceGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile timestampedFile = findFileByTypeName(files, "TimestampedDTO");

      String source = timestampedFile.toString();

      assertThat(source).contains("Instant createdAt();").contains("import java.time.Instant;");
    }
  }

  @Nested
  @DisplayName("Edge cases")
  class EdgeCases {

    @Test
    @DisplayName("should handle interface with no implementing types")
    void shouldHandleInterfaceWithNoImplementingTypes() {
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
                      "type": { "kind": "SCALAR", "name": "String" },
                      "isDeprecated": false
                    }
                  ],
                  "interfaces": []
                },
                {
                  "kind": "INTERFACE",
                  "name": "OrphanInterface",
                  "description": "An interface with no implementations",
                  "fields": [
                    {
                      "name": "id",
                      "args": [],
                      "type": { "kind": "NON_NULL", "ofType": { "kind": "SCALAR", "name": "ID" } },
                      "isDeprecated": false
                    }
                  ],
                  "possibleTypes": []
                }
              ]
            }
          }
          """;

      SchemaModel testSchema = schemaParser.parse(json);
      InterfaceGenerator generator = new InterfaceGenerator(configuration, testSchema);

      List<JavaFile> files = generator.generate();

      assertThat(files).hasSize(1);
      JavaFile file = files.get(0);
      String source = file.toString();

      // Should still be a sealed interface, just with no permits
      assertThat(source)
          .contains("public sealed interface OrphanInterfaceDTO")
          .contains("String id();");
    }

    @Test
    @DisplayName("should handle interface with nullable fields")
    void shouldHandleInterfaceWithNullableFields() {
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
                      "type": { "kind": "SCALAR", "name": "String" },
                      "isDeprecated": false
                    }
                  ],
                  "interfaces": []
                },
                {
                  "kind": "INTERFACE",
                  "name": "HasOptional",
                  "fields": [
                    {
                      "name": "optionalField",
                      "args": [],
                      "type": { "kind": "SCALAR", "name": "String" },
                      "isDeprecated": false
                    }
                  ],
                  "possibleTypes": []
                }
              ]
            }
          }
          """;

      SchemaModel testSchema = schemaParser.parse(json);
      InterfaceGenerator generator = new InterfaceGenerator(configuration, testSchema);

      List<JavaFile> files = generator.generate();
      JavaFile file = files.get(0);
      String source = file.toString();

      // Nullable field should still generate accessor
      assertThat(source).contains("String optionalField();");
    }
  }
}
