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

import com.palantir.javapoet.AnnotationSpec;
import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.github.graphite.codegen.CodegenConfiguration;
import io.github.graphite.codegen.schema.FieldDefinition;
import io.github.graphite.codegen.schema.SchemaModel;
import io.github.graphite.codegen.schema.TypeDefinition;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.lang.model.element.Modifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Generates Java record DTOs from GraphQL object types.
 *
 * <p>For each GraphQL object type, this generator produces a Java record with:
 *
 * <ul>
 *   <li>A component for each field
 *   <li>Appropriate nullability annotations (@NotNull/@Nullable)
 *   <li>Correct type mappings for scalars, enums, and other types
 *   <li>JavaDoc from the GraphQL description
 * </ul>
 *
 * <p>Example output:
 *
 * <pre>{@code
 * public record UserDTO(
 *     @NotNull String id,
 *     @NotNull String name,
 *     @Nullable String email,
 *     @NotNull Instant createdAt,
 *     @NotNull UserStatus status
 * ) {}
 * }</pre>
 *
 * @see TypeMapper
 * @see SchemaModel
 */
public final class TypeGenerator {

  private static final String TYPE_PACKAGE_SUFFIX = ".type";
  private static final String DTO_SUFFIX = "DTO";
  private static final Set<String> ROOT_TYPE_NAMES = Set.of("Query", "Mutation", "Subscription");

  private final CodegenConfiguration configuration;
  private final SchemaModel schema;
  private final TypeMapper typeMapper;

  /**
   * Creates a new type generator.
   *
   * @param configuration the codegen configuration
   * @param schema the parsed schema model
   */
  public TypeGenerator(@NotNull CodegenConfiguration configuration, @NotNull SchemaModel schema) {
    this.configuration = configuration;
    this.schema = schema;
    this.typeMapper = new TypeMapper(configuration, schema);
  }

  /**
   * Generates DTO records for all object types in the schema.
   *
   * <p>Root types (Query, Mutation, Subscription) are excluded as they are not DTOs.
   *
   * @return a list of generated Java files
   */
  @NotNull
  public List<JavaFile> generate() {
    List<JavaFile> files = new ArrayList<>();

    for (TypeDefinition type : schema.types().values()) {
      // Skip root types
      if (ROOT_TYPE_NAMES.contains(type.name())) {
        continue;
      }

      files.add(generateType(type));
    }

    return files;
  }

  /**
   * Generates a single DTO record for a GraphQL object type.
   *
   * @param type the type definition to generate
   * @return the generated Java file
   */
  @NotNull
  public JavaFile generateType(@NotNull TypeDefinition type) {
    String packageName = configuration.packageName() + TYPE_PACKAGE_SUFFIX;
    String className = type.name() + DTO_SUFFIX;

    TypeSpec.Builder recordBuilder =
        TypeSpec.recordBuilder(className).addModifiers(Modifier.PUBLIC);

    // Add JavaDoc
    if (type.description() != null && !type.description().isBlank()) {
      recordBuilder.addJavadoc(escapeJavadoc(type.description()) + "\n");
    }

    // Generate record components via record constructor
    MethodSpec.Builder constructorBuilder = MethodSpec.constructorBuilder();
    for (FieldDefinition field : type.fields()) {
      constructorBuilder.addParameter(generateComponent(field));
    }
    recordBuilder.recordConstructor(constructorBuilder.build());

    // Add interface implementations if any
    for (String interfaceName : type.interfaces()) {
      ClassName interfaceType = ClassName.get(packageName, interfaceName);
      recordBuilder.addSuperinterface(interfaceType);
    }

    TypeSpec recordSpec = recordBuilder.build();

    return JavaFile.builder(packageName, recordSpec)
        .skipJavaLangImports(true)
        .indent("    ")
        .build();
  }

  /**
   * Generates a record component for a field.
   *
   * @param field the field definition
   * @return the parameter spec for the record component
   */
  private ParameterSpec generateComponent(FieldDefinition field) {
    TypeName javaType = typeMapper.mapType(field.type());
    boolean isNullable = typeMapper.isNullable(field.type());

    ParameterSpec.Builder paramBuilder = ParameterSpec.builder(javaType, field.name());

    // Add nullability annotation
    if (isNullable) {
      paramBuilder.addAnnotation(Nullable.class);
    } else {
      paramBuilder.addAnnotation(NotNull.class);
    }

    // Add JavaDoc as annotation (JavaPoet doesn't support javadoc on record components directly)
    // Instead, we'll add deprecation annotation if applicable
    if (field.isDeprecated()) {
      AnnotationSpec.Builder deprecatedBuilder = AnnotationSpec.builder(Deprecated.class);
      if (field.deprecationReason() != null && !field.deprecationReason().isBlank()) {
        deprecatedBuilder.addMember("since", "$S", "Deprecated in GraphQL schema");
      }
      paramBuilder.addAnnotation(deprecatedBuilder.build());
    }

    return paramBuilder.build();
  }

  /**
   * Escapes special characters in JavaDoc content.
   *
   * @param text the text to escape
   * @return the escaped text
   */
  private String escapeJavadoc(String text) {
    return text.replace("$", "$$")
        .replace("@", "{@literal @}")
        .replace("<", "&lt;")
        .replace(">", "&gt;");
  }
}
