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

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.FieldSpec;
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeSpec;
import io.github.graphite.codegen.CodegenConfiguration;
import io.github.graphite.codegen.schema.FieldDefinition;
import io.github.graphite.codegen.schema.SchemaModel;
import io.github.graphite.codegen.schema.TypeDefinition;
import io.github.graphite.codegen.schema.TypeReference;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import javax.lang.model.element.Modifier;
import org.jetbrains.annotations.NotNull;

/**
 * Generates projection classes for type-safe GraphQL field selection.
 *
 * <p>For each GraphQL object type, this generator produces a projection class with:
 *
 * <ul>
 *   <li>A fluent builder API for selecting fields
 *   <li>Nested projections for object type fields
 *   <li>A {@code toGraphQL()} method that generates the selection set
 * </ul>
 *
 * <p>Example output for a User type:
 *
 * <pre>{@code
 * public final class UserProjection {
 *     private final Set<String> selectedFields = new LinkedHashSet<>();
 *     private PostProjection postsProjection;
 *
 *     public String toGraphQL() {
 *         StringBuilder sb = new StringBuilder("{ ");
 *         for (String field : selectedFields) {
 *             sb.append(field).append(" ");
 *         }
 *         if (postsProjection != null) {
 *             sb.append("posts ").append(postsProjection.toGraphQL()).append(" ");
 *         }
 *         return sb.append("}").toString();
 *     }
 *
 *     public static Builder builder() { return new Builder(); }
 *
 *     public static final class Builder {
 *         private final UserProjection projection = new UserProjection();
 *
 *         public Builder id() { projection.selectedFields.add("id"); return this; }
 *         public Builder name() { projection.selectedFields.add("name"); return this; }
 *         public Builder posts(Consumer<PostProjection.Builder> config) { ... }
 *         public UserProjection build() { return projection; }
 *     }
 * }
 * }</pre>
 *
 * @see TypeDefinition
 * @see SchemaModel
 */
public final class ProjectionGenerator {

  private static final String TYPE_PACKAGE_SUFFIX = ".type";
  private static final String PROJECTION_SUFFIX = "Projection";
  private static final Set<String> ROOT_TYPE_NAMES = Set.of("Query", "Mutation", "Subscription");

  private final CodegenConfiguration configuration;
  private final SchemaModel schema;

  /**
   * Creates a new projection generator.
   *
   * @param configuration the codegen configuration
   * @param schema the parsed schema model
   */
  public ProjectionGenerator(
      @NotNull CodegenConfiguration configuration, @NotNull SchemaModel schema) {
    this.configuration = configuration;
    this.schema = schema;
  }

  /**
   * Generates projection classes for all object types in the schema.
   *
   * <p>Root types (Query, Mutation, Subscription) are excluded.
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

      files.add(generateProjection(type));
    }

    return files;
  }

  /**
   * Generates a projection class for a single type.
   *
   * @param type the type definition to generate a projection for
   * @return the generated Java file
   */
  @NotNull
  public JavaFile generateProjection(@NotNull TypeDefinition type) {
    String packageName = configuration.packageName() + TYPE_PACKAGE_SUFFIX;
    String className = type.name() + PROJECTION_SUFFIX;

    ClassName projectionClassName = ClassName.get(packageName, className);
    ClassName builderClassName = projectionClassName.nestedClass("Builder");

    TypeSpec.Builder classBuilder =
        TypeSpec.classBuilder(className).addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    // Add JavaDoc
    if (type.description() != null && !type.description().isBlank()) {
      classBuilder.addJavadoc(
          "Projection for selecting fields from {@code $L}.\n\n"
              + "<p>$L\n",
          type.name(),
          escapeJavadoc(type.description()));
    } else {
      classBuilder.addJavadoc("Projection for selecting fields from {@code $L}.\n", type.name());
    }

    // Add selectedFields field
    ParameterizedTypeName setType =
        ParameterizedTypeName.get(ClassName.get(Set.class), ClassName.get(String.class));
    classBuilder.addField(
        FieldSpec.builder(setType, "selectedFields", Modifier.PRIVATE, Modifier.FINAL)
            .initializer("new $T<>()", LinkedHashSet.class)
            .build());

    // Add projection fields for object type fields
    List<FieldDefinition> objectFields = getObjectTypeFields(type);
    for (FieldDefinition field : objectFields) {
      String fieldProjectionType = getProjectionTypeName(field);
      if (fieldProjectionType != null) {
        ClassName fieldProjectionClass = ClassName.get(packageName, fieldProjectionType);
        classBuilder.addField(
            FieldSpec.builder(fieldProjectionClass, field.name() + "Projection", Modifier.PRIVATE)
                .build());
      }
    }

    // Add toGraphQL method
    classBuilder.addMethod(generateToGraphQLMethod(type, objectFields));

    // Add static builder() method
    classBuilder.addMethod(
        MethodSpec.methodBuilder("builder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(builderClassName)
            .addStatement("return new Builder()")
            .addJavadoc("Creates a new builder for this projection.\n\n@return a new builder\n")
            .build());

    // Add Builder class
    classBuilder.addType(
        generateBuilder(type, projectionClassName, builderClassName, objectFields));

    TypeSpec classSpec = classBuilder.build();

    return JavaFile.builder(packageName, classSpec)
        .skipJavaLangImports(true)
        .indent("    ")
        .build();
  }

  private MethodSpec generateToGraphQLMethod(
      TypeDefinition type, List<FieldDefinition> objectFields) {
    MethodSpec.Builder method =
        MethodSpec.methodBuilder("toGraphQL")
            .addModifiers(Modifier.PUBLIC)
            .returns(String.class)
            .addJavadoc(
                "Generates the GraphQL selection set for this projection.\n\n"
                    + "@return the GraphQL selection set string\n");

    method.addStatement("$T sb = new $T(\"{ \")", StringBuilder.class, StringBuilder.class);

    // Add selected scalar fields
    method.beginControlFlow("for (String field : selectedFields)");
    method.addStatement("sb.append(field).append(\" \")");
    method.endControlFlow();

    // Add nested projections
    for (FieldDefinition field : objectFields) {
      method.beginControlFlow("if ($N != null)", field.name() + "Projection");
      method.addStatement(
          "sb.append($S).append($N.toGraphQL()).append(\" \")",
          field.name() + " ",
          field.name() + "Projection");
      method.endControlFlow();
    }

    method.addStatement("return sb.append(\"}\").toString()");

    return method.build();
  }

  private TypeSpec generateBuilder(
      TypeDefinition type,
      ClassName projectionClassName,
      ClassName builderClassName,
      List<FieldDefinition> objectFields) {
    TypeSpec.Builder builder =
        TypeSpec.classBuilder("Builder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);

    builder.addJavadoc("Builder for {@link $T}.\n", projectionClassName);

    // Add projection field
    builder.addField(
        FieldSpec.builder(projectionClassName, "projection", Modifier.PRIVATE, Modifier.FINAL)
            .initializer("new $T()", projectionClassName)
            .build());

    // Add fluent setter for each scalar field
    for (FieldDefinition field : type.fields()) {
      if (!isObjectType(field)) {
        builder.addMethod(generateScalarFieldMethod(field, builderClassName));
      }
    }

    // Add fluent setter for each object field with nested projection
    String packageName = configuration.packageName() + TYPE_PACKAGE_SUFFIX;
    for (FieldDefinition field : objectFields) {
      String fieldProjectionType = getProjectionTypeName(field);
      if (fieldProjectionType != null) {
        ClassName fieldProjectionClass = ClassName.get(packageName, fieldProjectionType);
        builder.addMethod(
            generateObjectFieldMethod(field, builderClassName, fieldProjectionClass));
      }
    }

    // Add build() method
    builder.addMethod(
        MethodSpec.methodBuilder("build")
            .addModifiers(Modifier.PUBLIC)
            .returns(projectionClassName)
            .addStatement("return projection")
            .addJavadoc(
                "Builds the projection.\n\n@return the built projection\n"
                    + "@throws IllegalStateException if no fields are selected\n")
            .build());

    return builder.build();
  }

  private MethodSpec generateScalarFieldMethod(FieldDefinition field, ClassName builderClassName) {
    MethodSpec.Builder method =
        MethodSpec.methodBuilder(field.name())
            .addModifiers(Modifier.PUBLIC)
            .returns(builderClassName)
            .addStatement("projection.selectedFields.add($S)", field.name())
            .addStatement("return this");

    if (field.description() != null && !field.description().isBlank()) {
      method.addJavadoc(
          escapeJavadoc(field.description()) + "\n\n@return this builder\n");
    } else {
      method.addJavadoc("Selects the $N field.\n\n@return this builder\n", field.name());
    }

    return method.build();
  }

  private MethodSpec generateObjectFieldMethod(
      FieldDefinition field, ClassName builderClassName, ClassName fieldProjectionClass) {
    ClassName fieldBuilderClass = fieldProjectionClass.nestedClass("Builder");
    ParameterizedTypeName consumerType =
        ParameterizedTypeName.get(ClassName.get(Consumer.class), fieldBuilderClass);

    MethodSpec.Builder method =
        MethodSpec.methodBuilder(field.name())
            .addModifiers(Modifier.PUBLIC)
            .addParameter(consumerType, "config")
            .returns(builderClassName)
            .addStatement("$T builder = $T.builder()", fieldBuilderClass, fieldProjectionClass)
            .addStatement("config.accept(builder)")
            .addStatement("projection.$N = builder.build()", field.name() + "Projection")
            .addStatement("return this");

    if (field.description() != null && !field.description().isBlank()) {
      method.addJavadoc(
          escapeJavadoc(field.description())
              + "\n\n@param config the nested projection configuration\n@return this builder\n");
    } else {
      method.addJavadoc(
          "Selects the $N field with nested projection.\n\n"
              + "@param config the nested projection configuration\n@return this builder\n",
          field.name());
    }

    return method.build();
  }

  private List<FieldDefinition> getObjectTypeFields(TypeDefinition type) {
    List<FieldDefinition> objectFields = new ArrayList<>();
    for (FieldDefinition field : type.fields()) {
      if (isObjectType(field)) {
        objectFields.add(field);
      }
    }
    return objectFields;
  }

  private boolean isObjectType(FieldDefinition field) {
    String baseTypeName = getBaseTypeName(field.type());
    return baseTypeName != null && schema.types().containsKey(baseTypeName);
  }

  private String getProjectionTypeName(FieldDefinition field) {
    String baseTypeName = getBaseTypeName(field.type());
    if (baseTypeName != null && schema.types().containsKey(baseTypeName)) {
      return baseTypeName + PROJECTION_SUFFIX;
    }
    return null;
  }

  private String getBaseTypeName(TypeReference typeRef) {
    return switch (typeRef) {
      case TypeReference.Named named -> named.name();
      case TypeReference.NonNull nonNull -> getBaseTypeName(nonNull.inner());
      case TypeReference.ListType listType -> getBaseTypeName(listType.inner());
    };
  }

  private String escapeJavadoc(String text) {
    return text.replace("$", "$$")
        .replace("@", "{@literal @}")
        .replace("<", "&lt;")
        .replace(">", "&gt;");
  }
}
