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

@DisplayName("InputTypeGenerator")
class InputTypeGeneratorTest {

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
    @DisplayName("should generate input type classes for all input types")
    void shouldGenerateInputTypeClasses() {
      InputTypeGenerator generator = new InputTypeGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();

      assertThat(files).hasSizeGreaterThanOrEqualTo(2);

      List<String> typeNames = files.stream().map(f -> f.typeSpec().name()).toList();
      assertThat(typeNames).contains("CreateUserInput", "UpdateUserInput");
    }

    @Test
    @DisplayName("should use correct package for input types")
    void shouldUseCorrectPackage() {
      InputTypeGenerator generator = new InputTypeGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();

      for (JavaFile file : files) {
        assertThat(file.packageName()).isEqualTo("com.example.graphql.input");
      }
    }
  }

  @Nested
  @DisplayName("Generated class structure")
  class GeneratedClassStructure {

    static Stream<Arguments> classStructureTestCases() {
      return Stream.of(
          Arguments.of("final class", new String[] {"public final class CreateUserInput"}),
          Arguments.of(
              "private final fields",
              new String[] {"private final String name;", "private final String email;"}),
          Arguments.of(
              "private constructor", new String[] {"private CreateUserInput(Builder builder)"}),
          Arguments.of(
              "static builder method",
              new String[] {"public static Builder builder()", "return new Builder();"}),
          Arguments.of(
              "getters for all fields",
              new String[] {"public String getName()", "public String getEmail()"}),
          Arguments.of(
              "JavaDoc from GraphQL description", new String[] {"Input for creating a new user"}));
    }

    @ParameterizedTest(name = "should generate {0}")
    @MethodSource("classStructureTestCases")
    void shouldGenerateClassStructure(String description, String[] expectedContents) {
      InputTypeGenerator generator = new InputTypeGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile createUserFile = findFileByTypeName(files, "CreateUserInput");

      String source = createUserFile.toString();

      for (String expected : expectedContents) {
        assertThat(source).contains(expected);
      }
    }
  }

  @Nested
  @DisplayName("Builder class")
  class BuilderClass {

    @Test
    @DisplayName("should generate nested Builder class")
    void shouldGenerateNestedBuilderClass() {
      InputTypeGenerator generator = new InputTypeGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile createUserFile = findFileByTypeName(files, "CreateUserInput");

      String source = createUserFile.toString();

      assertThat(source).contains("public static final class Builder");
    }

    @Test
    @DisplayName("should generate fluent setter methods")
    void shouldGenerateFluentSetters() {
      InputTypeGenerator generator = new InputTypeGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile createUserFile = findFileByTypeName(files, "CreateUserInput");

      String source = createUserFile.toString();

      assertThat(source)
          .contains("public Builder name(String name)")
          .contains("public Builder email(String email)")
          .contains("return this;");
    }

    @Test
    @DisplayName("should generate build method")
    void shouldGenerateBuildMethod() {
      InputTypeGenerator generator = new InputTypeGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile createUserFile = findFileByTypeName(files, "CreateUserInput");

      String source = createUserFile.toString();

      assertThat(source).contains("public CreateUserInput build()");
      assertThat(source).contains("return new CreateUserInput(this);");
    }
  }

  @Nested
  @DisplayName("Required field validation")
  class RequiredFieldValidation {

    @Test
    @DisplayName("should validate required fields with Objects.requireNonNull")
    void shouldValidateRequiredFields() {
      InputTypeGenerator generator = new InputTypeGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile createUserFile = findFileByTypeName(files, "CreateUserInput");

      String source = createUserFile.toString();

      // name and email are required (non-null) in CreateUserInput
      assertThat(source)
          .contains("Objects.requireNonNull(builder.name")
          .contains("Objects.requireNonNull(builder.email");
    }

    @Test
    @DisplayName("should not validate optional fields")
    void shouldNotValidateOptionalFields() {
      InputTypeGenerator generator = new InputTypeGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile updateUserFile = findFileByTypeName(files, "UpdateUserInput");

      String source = updateUserFile.toString();

      // All fields in UpdateUserInput are optional (nullable)
      // The constructor should just assign without requireNonNull
      assertThat(source)
          .contains("this.name = builder.name;")
          .contains("this.email = builder.email;");
    }
  }

  @Nested
  @DisplayName("Type mapping")
  class TypeMapping {

    @Test
    @DisplayName("should reference enum types correctly")
    void shouldReferenceEnumTypes() {
      InputTypeGenerator generator = new InputTypeGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile createUserFile = findFileByTypeName(files, "CreateUserInput");

      String source = createUserFile.toString();

      // status is UserStatus enum
      assertThat(source)
          .contains("UserStatus status")
          .contains("import com.example.graphql.enumeration.UserStatus;");
    }
  }
}
