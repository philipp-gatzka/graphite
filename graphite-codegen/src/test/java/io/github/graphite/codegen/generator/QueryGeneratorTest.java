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

@DisplayName("QueryGenerator")
class QueryGeneratorTest {

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
    @DisplayName("should generate query classes for all Query fields")
    void shouldGenerateQueryClasses() {
      QueryGenerator generator = new QueryGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();

      // Query has user, users, and search fields
      assertThat(files).hasSizeGreaterThanOrEqualTo(3);

      List<String> typeNames = files.stream().map(f -> f.typeSpec().name()).toList();
      assertThat(typeNames).contains("UserQuery", "UsersQuery", "SearchQuery");
    }

    @Test
    @DisplayName("should use correct package for queries")
    void shouldUseCorrectPackage() {
      QueryGenerator generator = new QueryGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();

      for (JavaFile file : files) {
        assertThat(file.packageName()).isEqualTo("com.example.graphql.query");
      }
    }
  }

  @Nested
  @DisplayName("Generated query structure")
  class GeneratedQueryStructure {

    @Test
    @DisplayName("should generate public final class implementing GraphQLOperation")
    void shouldGeneratePublicFinalClassImplementingGraphQLOperation() {
      QueryGenerator generator = new QueryGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userQuery = findFileByTypeName(files, "UserQuery");

      String source = userQuery.toString();

      assertThat(source).contains("public final class UserQuery");
      assertThat(source).contains("implements GraphQLOperation<UserDTO>");
    }

    @Test
    @DisplayName("should generate private final fields for arguments")
    void shouldGeneratePrivateFinalFieldsForArguments() {
      QueryGenerator generator = new QueryGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userQuery = findFileByTypeName(files, "UserQuery");

      String source = userQuery.toString();

      // user(id: ID!) query has id argument
      assertThat(source).contains("private final String id;");
    }

    @Test
    @DisplayName("should generate private final field for projection")
    void shouldGeneratePrivateFinalFieldForProjection() {
      QueryGenerator generator = new QueryGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userQuery = findFileByTypeName(files, "UserQuery");

      String source = userQuery.toString();

      assertThat(source).contains("private final UserProjection projection;");
    }
  }

  @Nested
  @DisplayName("GraphQLOperation interface methods")
  class GraphQLOperationMethods {

    static Stream<Arguments> operationMethodsTestCases() {
      return Stream.of(
          Arguments.of(
              "operationName method",
              "UserQuery",
              new String[] {"public String operationName()", "return \"User\""}),
          Arguments.of(
              "toGraphQL method with arguments",
              "UserQuery",
              new String[] {
                "public String toGraphQL()",
                "query User($id: ID!)",
                "user(id: $id)",
                "projection.toGraphQL()"
              }),
          Arguments.of(
              "toGraphQL method with multiple arguments",
              "UsersQuery",
              new String[] {"query Users", "$limit: Int", "$offset: Int"}));
    }

    @ParameterizedTest(name = "should generate {0}")
    @MethodSource("operationMethodsTestCases")
    void shouldGenerateOperationMethods(
        String description, String queryTypeName, String[] expectedContents) {
      QueryGenerator generator = new QueryGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile queryFile = findFileByTypeName(files, queryTypeName);

      String source = queryFile.toString();

      for (String expected : expectedContents) {
        assertThat(source).contains(expected);
      }
    }

    @Test
    @DisplayName("should generate variables method")
    void shouldGenerateVariablesMethod() {
      QueryGenerator generator = new QueryGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userQuery = findFileByTypeName(files, "UserQuery");

      String source = userQuery.toString();

      assertThat(source).contains("public Map<String, Object> variables()");
      assertThat(source).contains("vars.put(\"id\", id)");
    }

    @Test
    @DisplayName("should generate responseType method")
    void shouldGenerateResponseTypeMethod() {
      QueryGenerator generator = new QueryGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userQuery = findFileByTypeName(files, "UserQuery");

      String source = userQuery.toString();

      assertThat(source).contains("public Class<UserDTO> responseType()");
      assertThat(source).contains("return UserDTO.class");
    }
  }

  @Nested
  @DisplayName("Builder class")
  class BuilderClass {

    @Test
    @DisplayName("should generate nested Builder class")
    void shouldGenerateNestedBuilderClass() {
      QueryGenerator generator = new QueryGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userQuery = findFileByTypeName(files, "UserQuery");

      String source = userQuery.toString();

      assertThat(source).contains("public static final class Builder");
    }

    @Test
    @DisplayName("should generate static builder method")
    void shouldGenerateStaticBuilderMethod() {
      QueryGenerator generator = new QueryGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userQuery = findFileByTypeName(files, "UserQuery");

      String source = userQuery.toString();

      assertThat(source).contains("public static Builder builder()");
      assertThat(source).contains("return new Builder()");
    }

    @Test
    @DisplayName("should generate fluent setters for arguments")
    void shouldGenerateFluentSettersForArguments() {
      QueryGenerator generator = new QueryGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userQuery = findFileByTypeName(files, "UserQuery");

      String source = userQuery.toString();

      assertThat(source).contains("public Builder id(String id)");
      assertThat(source).contains("this.id = id");
      assertThat(source).contains("return this;");
    }

    @Test
    @DisplayName("should generate selecting method for projection")
    void shouldGenerateSelectingMethodForProjection() {
      QueryGenerator generator = new QueryGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userQuery = findFileByTypeName(files, "UserQuery");

      String source = userQuery.toString();

      assertThat(source)
          .contains("public Builder selecting(Consumer<UserProjection.Builder> config)");
    }

    @Test
    @DisplayName("should generate build method")
    void shouldGenerateBuildMethod() {
      QueryGenerator generator = new QueryGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userQuery = findFileByTypeName(files, "UserQuery");

      String source = userQuery.toString();

      assertThat(source).contains("public UserQuery build()");
      assertThat(source).contains("return new UserQuery(this)");
    }
  }

  @Nested
  @DisplayName("Return type handling")
  class ReturnTypeHandling {

    @Test
    @DisplayName("should handle list return type")
    void shouldHandleListReturnType() {
      QueryGenerator generator = new QueryGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile usersQuery = findFileByTypeName(files, "UsersQuery");

      String source = usersQuery.toString();

      // users returns [User!]!
      assertThat(source).contains("implements GraphQLOperation<List<UserDTO>>");
    }
  }

  @Nested
  @DisplayName("JavaDoc generation")
  class JavaDocGeneration {

    @Test
    @DisplayName("should add JavaDoc from field description")
    void shouldAddJavaDocFromFieldDescription() {
      QueryGenerator generator = new QueryGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userQuery = findFileByTypeName(files, "UserQuery");

      String source = userQuery.toString();

      // user field has description "Get a user by ID"
      assertThat(source).contains("Get a user by ID");
    }

    @Test
    @DisplayName("should add JavaDoc for argument setters from argument descriptions")
    void shouldAddJavaDocForArgumentSetters() {
      QueryGenerator generator = new QueryGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userQuery = findFileByTypeName(files, "UserQuery");

      String source = userQuery.toString();

      // id argument has description "The user ID"
      assertThat(source).contains("The user ID");
    }
  }

  @Nested
  @DisplayName("Edge cases")
  class EdgeCases {

    @Test
    @DisplayName("should handle query with no arguments")
    void shouldHandleQueryWithNoArguments() {
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
                      "name": "currentUser",
                      "description": "Get the current user",
                      "args": [],
                      "type": { "kind": "OBJECT", "name": "User" },
                      "isDeprecated": false
                    }
                  ],
                  "interfaces": []
                },
                {
                  "kind": "OBJECT",
                  "name": "User",
                  "fields": [
                    {
                      "name": "id",
                      "args": [],
                      "type": { "kind": "NON_NULL", "ofType": { "kind": "SCALAR", "name": "ID" } },
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
      QueryGenerator generator = new QueryGenerator(configuration, testSchema);

      List<JavaFile> files = generator.generate();
      JavaFile file = files.get(0);
      String source = file.toString();

      assertThat(source).contains("query CurrentUser { currentUser");
      assertThat(source).contains("return Collections.emptyMap()");
    }

    @Test
    @DisplayName("should handle schema with no Query type")
    void shouldHandleSchemaWithNoQueryType() {
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
                  "fields": [],
                  "interfaces": []
                }
              ]
            }
          }
          """;

      SchemaModel testSchema = schemaParser.parse(json);
      QueryGenerator generator = new QueryGenerator(configuration, testSchema);

      List<JavaFile> files = generator.generate();

      assertThat(files).isEmpty();
    }
  }
}
