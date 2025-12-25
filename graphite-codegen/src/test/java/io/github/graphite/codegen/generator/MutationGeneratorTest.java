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

@DisplayName("MutationGenerator")
class MutationGeneratorTest {

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
    @DisplayName("should generate mutation classes for all Mutation fields")
    void shouldGenerateMutationClasses() {
      MutationGenerator generator = new MutationGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();

      // Mutation has createUser, updateUser, deleteUser fields
      assertThat(files).hasSizeGreaterThanOrEqualTo(3);

      List<String> typeNames = files.stream().map(f -> f.typeSpec().name()).toList();
      assertThat(typeNames)
          .contains("CreateUserMutation", "UpdateUserMutation", "DeleteUserMutation");
    }

    @Test
    @DisplayName("should use correct package for mutations")
    void shouldUseCorrectPackage() {
      MutationGenerator generator = new MutationGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();

      for (JavaFile file : files) {
        assertThat(file.packageName()).isEqualTo("com.example.graphql.mutation");
      }
    }
  }

  @Nested
  @DisplayName("Generated mutation structure")
  class GeneratedMutationStructure {

    @Test
    @DisplayName("should generate public final class implementing GraphQLOperation")
    void shouldGeneratePublicFinalClassImplementingGraphQLOperation() {
      MutationGenerator generator = new MutationGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile createUserMutation = findFileByTypeName(files, "CreateUserMutation");

      String source = createUserMutation.toString();

      assertThat(source).contains("public final class CreateUserMutation");
      assertThat(source).contains("implements GraphQLOperation<UserDTO>");
    }

    @Test
    @DisplayName("should generate private final fields for arguments")
    void shouldGeneratePrivateFinalFieldsForArguments() {
      MutationGenerator generator = new MutationGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile createUserMutation = findFileByTypeName(files, "CreateUserMutation");

      String source = createUserMutation.toString();

      // createUser(input: CreateUserInput!) mutation has input argument
      assertThat(source).contains("private final CreateUserInput input;");
    }

    @Test
    @DisplayName("should generate private final field for projection")
    void shouldGeneratePrivateFinalFieldForProjection() {
      MutationGenerator generator = new MutationGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile createUserMutation = findFileByTypeName(files, "CreateUserMutation");

      String source = createUserMutation.toString();

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
              new String[] {"public String operationName()", "return \"CreateUser\""}),
          Arguments.of(
              "toGraphQL method with mutation keyword",
              new String[] {
                "public String toGraphQL()",
                "mutation CreateUser",
                "$input: CreateUserInput!",
                "createUser(input: $input)",
                "projection.toGraphQL()"
              }),
          Arguments.of(
              "variables method",
              new String[] {
                "public Map<String, Object> variables()", "vars.put(\"input\", input)"
              }));
    }

    @ParameterizedTest(name = "should generate {0}")
    @MethodSource("operationMethodsTestCases")
    void shouldGenerateOperationMethods(String description, String[] expectedContents) {
      MutationGenerator generator = new MutationGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile createUserMutation = findFileByTypeName(files, "CreateUserMutation");

      String source = createUserMutation.toString();

      for (String expected : expectedContents) {
        assertThat(source).contains(expected);
      }
    }

    @Test
    @DisplayName("should generate responseType method")
    void shouldGenerateResponseTypeMethod() {
      MutationGenerator generator = new MutationGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile createUserMutation = findFileByTypeName(files, "CreateUserMutation");

      String source = createUserMutation.toString();

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
      MutationGenerator generator = new MutationGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile createUserMutation = findFileByTypeName(files, "CreateUserMutation");

      String source = createUserMutation.toString();

      assertThat(source).contains("public static final class Builder");
    }

    @Test
    @DisplayName("should generate static builder method")
    void shouldGenerateStaticBuilderMethod() {
      MutationGenerator generator = new MutationGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile createUserMutation = findFileByTypeName(files, "CreateUserMutation");

      String source = createUserMutation.toString();

      assertThat(source).contains("public static Builder builder()");
      assertThat(source).contains("return new Builder()");
    }

    @Test
    @DisplayName("should generate fluent setters for arguments")
    void shouldGenerateFluentSettersForArguments() {
      MutationGenerator generator = new MutationGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile createUserMutation = findFileByTypeName(files, "CreateUserMutation");

      String source = createUserMutation.toString();

      assertThat(source).contains("public Builder input(CreateUserInput input)");
      assertThat(source).contains("this.input = input");
      assertThat(source).contains("return this;");
    }

    @Test
    @DisplayName("should generate selecting method for projection")
    void shouldGenerateSelectingMethodForProjection() {
      MutationGenerator generator = new MutationGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile createUserMutation = findFileByTypeName(files, "CreateUserMutation");

      String source = createUserMutation.toString();

      assertThat(source)
          .contains("public Builder selecting(Consumer<UserProjection.Builder> config)");
    }

    @Test
    @DisplayName("should generate build method")
    void shouldGenerateBuildMethod() {
      MutationGenerator generator = new MutationGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile createUserMutation = findFileByTypeName(files, "CreateUserMutation");

      String source = createUserMutation.toString();

      assertThat(source).contains("public CreateUserMutation build()");
      assertThat(source).contains("return new CreateUserMutation(this)");
    }
  }

  @Nested
  @DisplayName("Return type handling")
  class ReturnTypeHandling {

    @Test
    @DisplayName("should handle scalar return type")
    void shouldHandleScalarReturnType() {
      MutationGenerator generator = new MutationGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile deleteUserMutation = findFileByTypeName(files, "DeleteUserMutation");

      String source = deleteUserMutation.toString();

      // deleteUser returns Boolean
      assertThat(source).contains("implements GraphQLOperation<Boolean>");
      assertThat(source).doesNotContain("projection");
      assertThat(source).doesNotContain("selecting");
    }

    @Test
    @DisplayName("should handle nullable return type")
    void shouldHandleNullableReturnType() {
      MutationGenerator generator = new MutationGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile updateUserMutation = findFileByTypeName(files, "UpdateUserMutation");

      String source = updateUserMutation.toString();

      // updateUser returns User (nullable)
      assertThat(source).contains("implements GraphQLOperation<UserDTO>");
    }
  }

  @Nested
  @DisplayName("JavaDoc generation")
  class JavaDocGeneration {

    @Test
    @DisplayName("should add JavaDoc from field description")
    void shouldAddJavaDocFromFieldDescription() {
      MutationGenerator generator = new MutationGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile createUserMutation = findFileByTypeName(files, "CreateUserMutation");

      String source = createUserMutation.toString();

      // createUser field has description "Create a new user"
      assertThat(source).contains("Create a new user");
    }
  }

  @Nested
  @DisplayName("Edge cases")
  class EdgeCases {

    @Test
    @DisplayName("should handle schema with no Mutation type")
    void shouldHandleSchemaWithNoMutationType() {
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
      MutationGenerator generator = new MutationGenerator(configuration, testSchema);

      List<JavaFile> files = generator.generate();

      assertThat(files).isEmpty();
    }

    @Test
    @DisplayName("should handle mutation with multiple arguments")
    void shouldHandleMutationWithMultipleArguments() {
      MutationGenerator generator = new MutationGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile updateUserMutation = findFileByTypeName(files, "UpdateUserMutation");

      String source = updateUserMutation.toString();

      // updateUser(id: ID!, input: UpdateUserInput!) has two arguments
      assertThat(source).contains("private final String id;");
      assertThat(source).contains("private final UpdateUserInput input;");
      assertThat(source).contains("$id: ID!, $input: UpdateUserInput!");
    }

    @Test
    @DisplayName("should handle deprecated mutation")
    void shouldHandleDeprecatedMutation() {
      MutationGenerator generator = new MutationGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile deleteUserMutation = findFileByTypeName(files, "DeleteUserMutation");

      String source = deleteUserMutation.toString();

      // deleteUser is deprecated with reason "Use deactivateUser instead"
      assertThat(source).contains("Delete a user");
    }
  }
}
