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

@DisplayName("UnionGenerator")
class UnionGeneratorTest {

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
    @DisplayName("should generate sealed marker interfaces for all union types")
    void shouldGenerateSealedMarkerInterfaces() {
      UnionGenerator generator = new UnionGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();

      assertThat(files).hasSize(1);

      List<String> typeNames = files.stream().map(f -> f.typeSpec().name()).toList();
      assertThat(typeNames).contains("SearchResultUnion");
    }

    @Test
    @DisplayName("should use correct package for unions")
    void shouldUseCorrectPackage() {
      UnionGenerator generator = new UnionGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();

      for (JavaFile file : files) {
        assertThat(file.packageName()).isEqualTo("com.example.graphql.union");
      }
    }
  }

  @Nested
  @DisplayName("Generated union structure")
  class GeneratedUnionStructure {

    @Test
    @DisplayName("should generate public sealed interface")
    void shouldGeneratePublicSealedInterface() {
      UnionGenerator generator = new UnionGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile searchResultFile = findFileByTypeName(files, "SearchResultUnion");

      String source = searchResultFile.toString();

      assertThat(source).contains("public sealed interface SearchResultUnion");
    }

    @Test
    @DisplayName("should include permits clause with member types")
    void shouldIncludePermitsClause() {
      UnionGenerator generator = new UnionGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile searchResultFile = findFileByTypeName(files, "SearchResultUnion");

      String source = searchResultFile.toString();

      // SearchResult union includes User and Post
      assertThat(source).contains("permits");
      assertThat(source).contains("UserDTO");
      assertThat(source).contains("PostDTO");
    }

    @Test
    @DisplayName("should be a marker interface with no methods")
    void shouldBeMarkerInterfaceWithNoMethods() {
      UnionGenerator generator = new UnionGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile searchResultFile = findFileByTypeName(files, "SearchResultUnion");

      String source = searchResultFile.toString();

      // Should have empty body (just {})
      // Check that there are no method signatures
      assertThat(source).doesNotContain("void ");
      assertThat(source).doesNotContain("String ");
      assertThat(source).doesNotContain("()");
    }

    @Test
    @DisplayName("should add JavaDoc from GraphQL description")
    void shouldAddJavaDoc() {
      UnionGenerator generator = new UnionGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile searchResultFile = findFileByTypeName(files, "SearchResultUnion");

      String source = searchResultFile.toString();

      // SearchResult has description "A search result that can be a user or post"
      assertThat(source).contains("A search result that can be a user or post");
    }

    @Test
    @DisplayName("should import member types from type package")
    void shouldImportMemberTypesFromTypePackage() {
      UnionGenerator generator = new UnionGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile searchResultFile = findFileByTypeName(files, "SearchResultUnion");

      String source = searchResultFile.toString();

      // Should import member types from type package
      assertThat(source).contains("import com.example.graphql.type.UserDTO;");
      assertThat(source).contains("import com.example.graphql.type.PostDTO;");
    }
  }

  @Nested
  @DisplayName("Edge cases")
  class EdgeCases {

    @Test
    @DisplayName("should handle union with single member type")
    void shouldHandleUnionWithSingleMemberType() {
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
                      "name": "result",
                      "args": [],
                      "type": { "kind": "UNION", "name": "SingleResult" },
                      "isDeprecated": false
                    }
                  ],
                  "interfaces": []
                },
                {
                  "kind": "OBJECT",
                  "name": "Item",
                  "fields": [
                    {
                      "name": "id",
                      "args": [],
                      "type": { "kind": "NON_NULL", "ofType": { "kind": "SCALAR", "name": "ID" } },
                      "isDeprecated": false
                    }
                  ],
                  "interfaces": []
                },
                {
                  "kind": "UNION",
                  "name": "SingleResult",
                  "description": "A result with only one possible type",
                  "possibleTypes": [
                    { "name": "Item" }
                  ]
                }
              ]
            }
          }
          """;

      SchemaModel testSchema = schemaParser.parse(json);
      UnionGenerator generator = new UnionGenerator(configuration, testSchema);

      List<JavaFile> files = generator.generate();

      assertThat(files).hasSize(1);
      JavaFile file = files.get(0);
      String source = file.toString();

      assertThat(source).contains("public sealed interface SingleResultUnion");
      assertThat(source).contains("permits ItemDTO");
    }

    @Test
    @DisplayName("should handle union with many member types")
    void shouldHandleUnionWithManyMemberTypes() {
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
                      "name": "content",
                      "args": [],
                      "type": { "kind": "UNION", "name": "Content" },
                      "isDeprecated": false
                    }
                  ],
                  "interfaces": []
                },
                {
                  "kind": "OBJECT",
                  "name": "Article",
                  "fields": [
                    { "name": "id", "args": [], "type": { "kind": "SCALAR", "name": "ID" }, "isDeprecated": false }
                  ],
                  "interfaces": []
                },
                {
                  "kind": "OBJECT",
                  "name": "Video",
                  "fields": [
                    { "name": "id", "args": [], "type": { "kind": "SCALAR", "name": "ID" }, "isDeprecated": false }
                  ],
                  "interfaces": []
                },
                {
                  "kind": "OBJECT",
                  "name": "Podcast",
                  "fields": [
                    { "name": "id", "args": [], "type": { "kind": "SCALAR", "name": "ID" }, "isDeprecated": false }
                  ],
                  "interfaces": []
                },
                {
                  "kind": "OBJECT",
                  "name": "Image",
                  "fields": [
                    { "name": "id", "args": [], "type": { "kind": "SCALAR", "name": "ID" }, "isDeprecated": false }
                  ],
                  "interfaces": []
                },
                {
                  "kind": "UNION",
                  "name": "Content",
                  "description": "Various content types",
                  "possibleTypes": [
                    { "name": "Article" },
                    { "name": "Video" },
                    { "name": "Podcast" },
                    { "name": "Image" }
                  ]
                }
              ]
            }
          }
          """;

      SchemaModel testSchema = schemaParser.parse(json);
      UnionGenerator generator = new UnionGenerator(configuration, testSchema);

      List<JavaFile> files = generator.generate();
      JavaFile file = files.get(0);
      String source = file.toString();

      assertThat(source).contains("ArticleDTO");
      assertThat(source).contains("VideoDTO");
      assertThat(source).contains("PodcastDTO");
      assertThat(source).contains("ImageDTO");
    }

    @Test
    @DisplayName("should handle schema with no unions")
    void shouldHandleSchemaWithNoUnions() {
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
                }
              ]
            }
          }
          """;

      SchemaModel testSchema = schemaParser.parse(json);
      UnionGenerator generator = new UnionGenerator(configuration, testSchema);

      List<JavaFile> files = generator.generate();

      assertThat(files).isEmpty();
    }
  }
}
