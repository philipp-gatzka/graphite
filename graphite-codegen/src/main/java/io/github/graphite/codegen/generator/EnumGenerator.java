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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeSpec;
import io.github.graphite.codegen.CodegenConfiguration;
import io.github.graphite.codegen.schema.EnumDefinition;
import io.github.graphite.codegen.schema.EnumValueDefinition;
import io.github.graphite.codegen.schema.SchemaModel;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Modifier;
import org.jetbrains.annotations.NotNull;

/**
 * Generates Java enums from GraphQL enum types.
 *
 * <p>For each GraphQL enum type, this generator produces a Java enum with:
 *
 * <ul>
 *   <li>Enum constants for each GraphQL enum value
 *   <li>Jackson annotations for JSON serialization
 *   <li>Deprecation annotations where applicable
 *   <li>JavaDoc from GraphQL descriptions
 * </ul>
 *
 * <p>Example output:
 *
 * <pre>{@code
 * public enum UserStatus {
 *     ACTIVE,
 *     INACTIVE,
 *     PENDING;
 *
 *     @JsonCreator
 *     public static UserStatus fromValue(String value) {
 *         return valueOf(value);
 *     }
 *
 *     @JsonValue
 *     public String toValue() {
 *         return name();
 *     }
 * }
 * }</pre>
 *
 * @see SchemaModel
 */
public final class EnumGenerator {

  private static final String ENUM_PACKAGE_SUFFIX = ".enumeration";

  private final CodegenConfiguration configuration;
  private final SchemaModel schema;

  /**
   * Creates a new enum generator.
   *
   * @param configuration the codegen configuration
   * @param schema the parsed schema model
   */
  public EnumGenerator(@NotNull CodegenConfiguration configuration, @NotNull SchemaModel schema) {
    this.configuration = configuration;
    this.schema = schema;
  }

  /**
   * Generates Java enums for all enum types in the schema.
   *
   * @return a list of generated Java files
   */
  @NotNull
  public List<JavaFile> generate() {
    List<JavaFile> files = new ArrayList<>();

    for (EnumDefinition enumDef : schema.enums().values()) {
      files.add(generateEnum(enumDef));
    }

    return files;
  }

  /**
   * Generates a single Java enum.
   *
   * @param enumDef the enum definition to generate
   * @return the generated Java file
   */
  @NotNull
  public JavaFile generateEnum(@NotNull EnumDefinition enumDef) {
    String packageName = configuration.packageName() + ENUM_PACKAGE_SUFFIX;

    TypeSpec.Builder enumBuilder =
        TypeSpec.enumBuilder(enumDef.name()).addModifiers(Modifier.PUBLIC);

    // Add JavaDoc
    if (enumDef.description() != null && !enumDef.description().isBlank()) {
      enumBuilder.addJavadoc(escapeJavadoc(enumDef.description()) + "\n");
    }

    // Add enum constants
    for (EnumValueDefinition value : enumDef.values()) {
      enumBuilder.addEnumConstant(value.name(), generateEnumConstant(value));
    }

    // Add fromValue method with @JsonCreator
    enumBuilder.addMethod(generateFromValueMethod(enumDef.name()));

    // Add toValue method with @JsonValue
    enumBuilder.addMethod(generateToValueMethod());

    TypeSpec enumSpec = enumBuilder.build();

    return JavaFile.builder(packageName, enumSpec).skipJavaLangImports(true).indent("    ").build();
  }

  private TypeSpec generateEnumConstant(EnumValueDefinition value) {
    TypeSpec.Builder constantBuilder = TypeSpec.anonymousClassBuilder("");

    // Add JavaDoc if present
    if (value.description() != null && !value.description().isBlank()) {
      constantBuilder.addJavadoc(escapeJavadoc(value.description()) + "\n");
    }

    // Add deprecation annotation if deprecated
    if (value.isDeprecated()) {
      AnnotationSpec.Builder deprecatedBuilder = AnnotationSpec.builder(Deprecated.class);
      if (value.deprecationReason() != null && !value.deprecationReason().isBlank()) {
        deprecatedBuilder.addMember("since", "$S", "Deprecated in GraphQL schema");
      }
      constantBuilder.addAnnotation(deprecatedBuilder.build());
    }

    return constantBuilder.build();
  }

  private MethodSpec generateFromValueMethod(String enumName) {
    return MethodSpec.methodBuilder("fromValue")
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .addAnnotation(JsonCreator.class)
        .addParameter(String.class, "value")
        .returns(com.palantir.javapoet.ClassName.bestGuess(enumName))
        .addStatement("return valueOf(value)")
        .addJavadoc(
            """
            Creates an enum instance from a string value.

            @param value the string value
            @return the enum instance
            @throws IllegalArgumentException if no enum constant matches the value
            """)
        .build();
  }

  private MethodSpec generateToValueMethod() {
    return MethodSpec.methodBuilder("toValue")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(JsonValue.class)
        .returns(String.class)
        .addStatement("return name()")
        .addJavadoc(
            """
            Returns the string value of this enum constant.

            @return the enum constant name
            """)
        .build();
  }

  private String escapeJavadoc(String text) {
    return text.replace("$", "$$")
        .replace("@", "{@literal @}")
        .replace("<", "&lt;")
        .replace(">", "&gt;");
  }
}
