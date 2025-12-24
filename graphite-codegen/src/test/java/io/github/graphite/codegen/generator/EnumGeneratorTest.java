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

@DisplayName("EnumGenerator")
class EnumGeneratorTest {

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
    @DisplayName("should generate Java enums for all enum types")
    void shouldGenerateEnums() {
      EnumGenerator generator = new EnumGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();

      assertThat(files).hasSizeGreaterThanOrEqualTo(1);

      List<String> typeNames = files.stream().map(f -> f.typeSpec().name()).toList();
      assertThat(typeNames).contains("UserStatus");
    }

    @Test
    @DisplayName("should use correct package for enums")
    void shouldUseCorrectPackage() {
      EnumGenerator generator = new EnumGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();

      for (JavaFile file : files) {
        assertThat(file.packageName()).isEqualTo("com.example.graphql.enumeration");
      }
    }
  }

  @Nested
  @DisplayName("Generated enum structure")
  class GeneratedEnumStructure {

    @Test
    @DisplayName("should generate public enum")
    void shouldGeneratePublicEnum() {
      EnumGenerator generator = new EnumGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userStatusFile = findFileByTypeName(files, "UserStatus");

      String source = userStatusFile.toString();

      assertThat(source).contains("public enum UserStatus");
    }

    @Test
    @DisplayName("should generate enum constants")
    void shouldGenerateEnumConstants() {
      EnumGenerator generator = new EnumGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userStatusFile = findFileByTypeName(files, "UserStatus");

      String source = userStatusFile.toString();

      assertThat(source).contains("ACTIVE");
      assertThat(source).contains("INACTIVE");
      assertThat(source).contains("SUSPENDED");
      assertThat(source).contains("DELETED");
    }

    @Test
    @DisplayName("should add JavaDoc from GraphQL description")
    void shouldAddJavaDoc() {
      EnumGenerator generator = new EnumGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userStatusFile = findFileByTypeName(files, "UserStatus");

      String source = userStatusFile.toString();

      // UserStatus has description "The status of a user account"
      assertThat(source).contains("The status of a user account");
    }
  }

  @Nested
  @DisplayName("Jackson annotations")
  class JacksonAnnotations {

    @Test
    @DisplayName("should add @JsonCreator on fromValue method")
    void shouldAddJsonCreator() {
      EnumGenerator generator = new EnumGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userStatusFile = findFileByTypeName(files, "UserStatus");

      String source = userStatusFile.toString();

      assertThat(source).contains("@JsonCreator");
      assertThat(source).contains("public static UserStatus fromValue(String value)");
    }

    @Test
    @DisplayName("should add @JsonValue on toValue method")
    void shouldAddJsonValue() {
      EnumGenerator generator = new EnumGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userStatusFile = findFileByTypeName(files, "UserStatus");

      String source = userStatusFile.toString();

      assertThat(source).contains("@JsonValue");
      assertThat(source).contains("public String toValue()");
      assertThat(source).contains("return name();");
    }

    @Test
    @DisplayName("should import Jackson annotations")
    void shouldImportJacksonAnnotations() {
      EnumGenerator generator = new EnumGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userStatusFile = findFileByTypeName(files, "UserStatus");

      String source = userStatusFile.toString();

      assertThat(source).contains("import com.fasterxml.jackson.annotation.JsonCreator;");
      assertThat(source).contains("import com.fasterxml.jackson.annotation.JsonValue;");
    }
  }

  @Nested
  @DisplayName("Deprecation handling")
  class DeprecationHandling {

    @Test
    @DisplayName("should add @Deprecated annotation for deprecated enum values")
    void shouldAddDeprecatedAnnotation() {
      EnumGenerator generator = new EnumGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userStatusFile = findFileByTypeName(files, "UserStatus");

      String source = userStatusFile.toString();

      // INACTIVE is deprecated in the schema
      assertThat(source).contains("@Deprecated");
    }
  }

  @Nested
  @DisplayName("fromValue method")
  class FromValueMethod {

    @Test
    @DisplayName("should generate fromValue method that calls valueOf")
    void shouldGenerateFromValueMethod() {
      EnumGenerator generator = new EnumGenerator(configuration, schema);

      List<JavaFile> files = generator.generate();
      JavaFile userStatusFile = findFileByTypeName(files, "UserStatus");

      String source = userStatusFile.toString();

      assertThat(source).contains("return valueOf(value);");
    }
  }
}
