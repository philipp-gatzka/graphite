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

@DisplayName("ProjectionGenerator")
class ProjectionGeneratorTest {

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
    @DisplayName("should generate projection classes for all non-root types")
    void shouldGenerateProjectionClasses() {
      ProjectionGenerator generator = new ProjectionGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();

      // Should skip Query, Mutation, Subscription
      assertThat(files).hasSizeGreaterThanOrEqualTo(2);

      List<String> typeNames = files.stream().map(f -> f.typeSpec().name()).toList();
      assertThat(typeNames).contains("UserProjection", "PostProjection");
      assertThat(typeNames).doesNotContain("QueryProjection", "MutationProjection");
    }

    @Test
    @DisplayName("should use correct package for projections")
    void shouldUseCorrectPackage() {
      ProjectionGenerator generator = new ProjectionGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();

      for (JavaFile file : files) {
        assertThat(file.packageName()).isEqualTo("com.example.graphql.type");
      }
    }
  }

  @Nested
  @DisplayName("Generated projection structure")
  class GeneratedProjectionStructure {

    @Test
    @DisplayName("should generate public final class")
    void shouldGeneratePublicFinalClass() {
      ProjectionGenerator generator = new ProjectionGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userProjection = findFileByTypeName(files, "UserProjection");

      String source = userProjection.toString();

      assertThat(source).contains("public final class UserProjection");
    }

    @Test
    @DisplayName("should generate selectedFields set")
    void shouldGenerateSelectedFieldsSet() {
      ProjectionGenerator generator = new ProjectionGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userProjection = findFileByTypeName(files, "UserProjection");

      String source = userProjection.toString();

      assertThat(source).contains("private final Set<String> selectedFields");
      assertThat(source).contains("new LinkedHashSet<>()");
    }

    @Test
    @DisplayName("should generate static builder method")
    void shouldGenerateStaticBuilderMethod() {
      ProjectionGenerator generator = new ProjectionGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userProjection = findFileByTypeName(files, "UserProjection");

      String source = userProjection.toString();

      assertThat(source).contains("public static Builder builder()");
      assertThat(source).contains("return new Builder()");
    }

    @Test
    @DisplayName("should generate toGraphQL method")
    void shouldGenerateToGraphQLMethod() {
      ProjectionGenerator generator = new ProjectionGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userProjection = findFileByTypeName(files, "UserProjection");

      String source = userProjection.toString();

      assertThat(source).contains("public String toGraphQL()");
      assertThat(source).contains("StringBuilder");
    }
  }

  @Nested
  @DisplayName("Builder class")
  class BuilderClass {

    @Test
    @DisplayName("should generate nested Builder class")
    void shouldGenerateNestedBuilderClass() {
      ProjectionGenerator generator = new ProjectionGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userProjection = findFileByTypeName(files, "UserProjection");

      String source = userProjection.toString();

      assertThat(source).contains("public static final class Builder");
    }

    @Test
    @DisplayName("should generate fluent setters for scalar fields")
    void shouldGenerateFluentSettersForScalarFields() {
      ProjectionGenerator generator = new ProjectionGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userProjection = findFileByTypeName(files, "UserProjection");

      String source = userProjection.toString();

      // User has id, name, email fields that are scalars
      assertThat(source).contains("public Builder id()");
      assertThat(source).contains("public Builder name()");
      assertThat(source).contains("public Builder email()");
      assertThat(source).contains("projection.selectedFields.add(");
      assertThat(source).contains("return this;");
    }

    @Test
    @DisplayName("should generate fluent setters with Consumer for object fields")
    void shouldGenerateFluentSettersWithConsumerForObjectFields() {
      ProjectionGenerator generator = new ProjectionGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userProjection = findFileByTypeName(files, "UserProjection");

      String source = userProjection.toString();

      // User has posts field which is a list of Post objects
      assertThat(source).contains("public Builder posts(Consumer<PostProjection.Builder> config)");
    }

    @Test
    @DisplayName("should generate build method")
    void shouldGenerateBuildMethod() {
      ProjectionGenerator generator = new ProjectionGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userProjection = findFileByTypeName(files, "UserProjection");

      String source = userProjection.toString();

      assertThat(source).contains("public UserProjection build()");
      assertThat(source).contains("return projection;");
    }
  }

  @Nested
  @DisplayName("Nested projections")
  class NestedProjections {

    @Test
    @DisplayName("should generate projection field for object type fields")
    void shouldGenerateProjectionFieldForObjectTypeFields() {
      ProjectionGenerator generator = new ProjectionGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userProjection = findFileByTypeName(files, "UserProjection");

      String source = userProjection.toString();

      assertThat(source).contains("private PostProjection postsProjection;");
    }

    @Test
    @DisplayName("should include nested projection in toGraphQL output")
    void shouldIncludeNestedProjectionInToGraphQL() {
      ProjectionGenerator generator = new ProjectionGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userProjection = findFileByTypeName(files, "UserProjection");

      String source = userProjection.toString();

      assertThat(source).contains("if (postsProjection != null)");
      assertThat(source).contains("postsProjection.toGraphQL()");
    }

    @Test
    @DisplayName("should handle type with no object fields")
    void shouldHandleTypeWithNoObjectFields() {
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
                      "name": "item",
                      "args": [],
                      "type": { "kind": "OBJECT", "name": "SimpleItem" },
                      "isDeprecated": false
                    }
                  ],
                  "interfaces": []
                },
                {
                  "kind": "OBJECT",
                  "name": "SimpleItem",
                  "description": "A simple item with only scalar fields",
                  "fields": [
                    {
                      "name": "id",
                      "args": [],
                      "type": { "kind": "NON_NULL", "ofType": { "kind": "SCALAR", "name": "ID" } },
                      "isDeprecated": false
                    },
                    {
                      "name": "name",
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
      ProjectionGenerator generator = new ProjectionGenerator(configuration, testSchema);

      List<JavaFile> files = generator.generate();
      JavaFile file = files.get(0);
      String source = file.toString();

      assertThat(source).contains("public Builder id()");
      assertThat(source).contains("public Builder name()");
      assertThat(source).doesNotContain("Consumer<");
    }
  }

  @Nested
  @DisplayName("JavaDoc generation")
  class JavaDocGeneration {

    @Test
    @DisplayName("should add JavaDoc from type description")
    void shouldAddJavaDocFromTypeDescription() {
      ProjectionGenerator generator = new ProjectionGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userProjection = findFileByTypeName(files, "UserProjection");

      String source = userProjection.toString();

      // User has description "A user in the system"
      assertThat(source).contains("A user in the system");
    }

    @Test
    @DisplayName("should add JavaDoc for field methods from field descriptions")
    void shouldAddJavaDocForFieldMethods() {
      ProjectionGenerator generator = new ProjectionGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userProjection = findFileByTypeName(files, "UserProjection");

      String source = userProjection.toString();

      // name field has description "The user's display name"
      assertThat(source).contains("display name");
    }
  }

  @Nested
  @DisplayName("Edge cases")
  class EdgeCases {

    @Test
    @DisplayName("should handle circular references")
    void shouldHandleCircularReferences() {
      // Post has author (User), User has posts (Post) - circular
      ProjectionGenerator generator = new ProjectionGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile postProjection = findFileByTypeName(files, "PostProjection");

      String source = postProjection.toString();

      assertThat(source).contains("public Builder author(Consumer<UserProjection.Builder> config)");
    }

    @Test
    @DisplayName("should handle list of object types")
    void shouldHandleListOfObjectTypes() {
      ProjectionGenerator generator = new ProjectionGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userProjection = findFileByTypeName(files, "UserProjection");

      String source = userProjection.toString();

      // posts is [Post!]! - list of non-null Post
      assertThat(source).contains("PostProjection postsProjection");
      assertThat(source).contains("posts(Consumer<PostProjection.Builder>");
    }
  }
}
