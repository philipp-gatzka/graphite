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
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.github.graphite.GraphQLOperation;
import io.github.graphite.codegen.CodegenConfiguration;
import io.github.graphite.codegen.schema.ArgumentDefinition;
import io.github.graphite.codegen.schema.FieldDefinition;
import io.github.graphite.codegen.schema.SchemaModel;
import io.github.graphite.codegen.schema.TypeDefinition;
import io.github.graphite.codegen.schema.TypeReference;
import io.github.graphite.codegen.util.GeneratorUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import javax.lang.model.element.Modifier;
import org.jetbrains.annotations.NotNull;

/**
 * Generates type-safe mutation classes from GraphQL Mutation type fields.
 *
 * <p>For each field in the Mutation type, this generator produces a class that:
 *
 * <ul>
 *   <li>Implements {@link GraphQLOperation}
 *   <li>Has type-safe argument setters via builder pattern
 *   <li>Integrates with projections for field selection
 *   <li>Generates valid GraphQL mutation strings
 * </ul>
 *
 * <p>Example output for a mutation field:
 *
 * <pre>{@code
 * public final class CreateUserMutation implements GraphQLOperation<UserDTO> {
 *     private final CreateUserInput input;
 *     private final UserProjection projection;
 *
 *     @Override
 *     public String operationName() { return "CreateUser"; }
 *
 *     @Override
 *     public String toGraphQL() {
 *         return "mutation CreateUser($input: CreateUserInput!) { createUser(input: $input) "
 *             + projection.toGraphQL() + " }";
 *     }
 *
 *     public static Builder builder() { return new Builder(); }
 * }
 * }</pre>
 *
 * @see GraphQLOperation
 * @see ProjectionGenerator
 */
public final class MutationGenerator {

  private static final String MUTATION_PACKAGE_SUFFIX = ".mutation";
  private static final String TYPE_PACKAGE_SUFFIX = ".type";
  private static final String MUTATION_SUFFIX = "Mutation";
  private static final String DTO_SUFFIX = "DTO";
  private static final String PROJECTION_SUFFIX = "Projection";
  private static final String BUILDER_CLASS_NAME = "Builder";
  private static final String INHERITDOC = "{@inheritDoc}\n";
  private static final String MUTATION_TYPE_NAME = "Mutation";

  private final CodegenConfiguration configuration;
  private final SchemaModel schema;
  private final TypeMapper typeMapper;

  /**
   * Creates a new mutation generator.
   *
   * @param configuration the codegen configuration
   * @param schema the parsed schema model
   */
  public MutationGenerator(
      @NotNull CodegenConfiguration configuration, @NotNull SchemaModel schema) {
    this.configuration = configuration;
    this.schema = schema;
    this.typeMapper = new TypeMapper(configuration, schema);
  }

  /**
   * Generates mutation classes for all fields in the Mutation type.
   *
   * @return a list of generated Java files
   */
  @NotNull
  public List<JavaFile> generate() {
    List<JavaFile> files = new ArrayList<>();

    TypeDefinition mutationType = schema.types().get(MUTATION_TYPE_NAME);
    if (mutationType == null) {
      return files;
    }

    for (FieldDefinition field : mutationType.fields()) {
      files.add(generateMutation(field));
    }

    return files;
  }

  /**
   * Generates a mutation class for a single mutation field.
   *
   * @param field the field definition from the Mutation type
   * @return the generated Java file
   */
  @NotNull
  public JavaFile generateMutation(@NotNull FieldDefinition field) {
    String packageName = configuration.packageName() + MUTATION_PACKAGE_SUFFIX;
    String typePackage = configuration.packageName() + TYPE_PACKAGE_SUFFIX;
    String className = GeneratorUtils.capitalize(field.name()) + MUTATION_SUFFIX;

    String returnTypeName = getBaseTypeName(field.type());
    boolean isScalarReturn = isScalarType(returnTypeName);
    boolean isList = isList(field.type());

    ClassName responseClass;
    if (isScalarReturn) {
      responseClass = getScalarClassName(returnTypeName);
    } else {
      responseClass = ClassName.get(typePackage, returnTypeName + DTO_SUFFIX);
    }

    TypeName actualResponseType;
    if (isList) {
      actualResponseType = ParameterizedTypeName.get(ClassName.get(List.class), responseClass);
    } else {
      actualResponseType = responseClass;
    }

    ParameterizedTypeName operationType =
        ParameterizedTypeName.get(ClassName.get(GraphQLOperation.class), actualResponseType);

    ClassName mutationClassName = ClassName.get(packageName, className);
    ClassName builderClassName = mutationClassName.nestedClass(BUILDER_CLASS_NAME);

    TypeSpec.Builder classBuilder =
        TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(operationType);

    // Add JavaDoc
    if (field.description() != null && !field.description().isBlank()) {
      classBuilder.addJavadoc(
          "Mutation for $N.\n\n<p>$L\n",
          field.name(),
          GeneratorUtils.escapeJavadoc(field.description()));
    } else {
      classBuilder.addJavadoc("Mutation for $N.\n", field.name());
    }

    // Add fields for arguments
    for (ArgumentDefinition arg : field.arguments()) {
      TypeName argType = typeMapper.mapType(arg.type());
      classBuilder.addField(
          FieldSpec.builder(argType, arg.name(), Modifier.PRIVATE, Modifier.FINAL).build());
    }

    // Add projection field if return type is object
    ClassName projectionClass = null;
    if (!isScalarReturn) {
      projectionClass = ClassName.get(typePackage, returnTypeName + PROJECTION_SUFFIX);
      classBuilder.addField(
          FieldSpec.builder(projectionClass, "projection", Modifier.PRIVATE, Modifier.FINAL)
              .build());
    }

    // Add private constructor
    classBuilder.addMethod(generateConstructor(field, builderClassName, !isScalarReturn));

    // Add operationName() method
    String operationName = GeneratorUtils.capitalize(field.name());
    classBuilder.addMethod(
        MethodSpec.methodBuilder("operationName")
            .addAnnotation(Override.class)
            .addAnnotation(NotNull.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(String.class)
            .addStatement("return $S", operationName)
            .addJavadoc(INHERITDOC)
            .build());

    // Add toGraphQL() method
    classBuilder.addMethod(generateToGraphQLMethod(field, operationName, !isScalarReturn));

    // Add variables() method
    classBuilder.addMethod(generateVariablesMethod(field));

    // Add responseType() method
    classBuilder.addMethod(generateResponseTypeMethod(actualResponseType, isList, responseClass));

    // Add static builder() method
    classBuilder.addMethod(
        MethodSpec.methodBuilder("builder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(builderClassName)
            .addStatement("return new Builder()")
            .addJavadoc("Creates a new builder for this mutation.\n\n@return a new builder\n")
            .build());

    // Add Builder class
    classBuilder.addType(
        generateBuilder(
            field, mutationClassName, builderClassName, projectionClass, !isScalarReturn));

    TypeSpec classSpec = classBuilder.build();

    return JavaFile.builder(packageName, classSpec)
        .skipJavaLangImports(true)
        .indent("    ")
        .build();
  }

  private MethodSpec generateConstructor(
      FieldDefinition field, ClassName builderClassName, boolean hasProjection) {
    MethodSpec.Builder constructor =
        MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PRIVATE)
            .addParameter(builderClassName, "builder");

    for (ArgumentDefinition arg : field.arguments()) {
      if (isRequired(arg.type())) {
        constructor.addStatement(
            "this.$N = java.util.Objects.requireNonNull(builder.$N, $S)",
            arg.name(),
            arg.name(),
            arg.name() + " is required");
      } else {
        constructor.addStatement("this.$N = builder.$N", arg.name(), arg.name());
      }
    }

    if (hasProjection) {
      constructor.addStatement(
          "this.projection = java.util.Objects.requireNonNull(builder.projection, \"projection is required\")");
    }

    return constructor.build();
  }

  private MethodSpec generateToGraphQLMethod(
      FieldDefinition field, String operationName, boolean hasProjection) {
    MethodSpec.Builder method =
        MethodSpec.methodBuilder("toGraphQL")
            .addAnnotation(Override.class)
            .addAnnotation(NotNull.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(String.class)
            .addJavadoc(INHERITDOC);

    // Build variable definitions
    StringBuilder varDefs = new StringBuilder();
    StringBuilder argRefs = new StringBuilder();
    for (int i = 0; i < field.arguments().size(); i++) {
      ArgumentDefinition arg = field.arguments().get(i);
      if (i > 0) {
        varDefs.append(", ");
        argRefs.append(", ");
      }
      varDefs.append("$").append(arg.name()).append(": ").append(formatGraphQLType(arg.type()));
      argRefs.append(arg.name()).append(": $").append(arg.name());
    }

    String mutationStart;
    if (field.arguments().isEmpty()) {
      mutationStart = "mutation " + operationName + " { " + field.name();
    } else {
      mutationStart =
          "mutation " + operationName + "(" + varDefs + ") { " + field.name() + "(" + argRefs + ")";
    }

    if (hasProjection) {
      method.addStatement("return $S + \" \" + projection.toGraphQL() + \" }\"", mutationStart);
    } else {
      method.addStatement("return $S + \" }\"", mutationStart);
    }

    return method.build();
  }

  private MethodSpec generateVariablesMethod(FieldDefinition field) {
    MethodSpec.Builder method =
        MethodSpec.methodBuilder("variables")
            .addAnnotation(Override.class)
            .addAnnotation(NotNull.class)
            .addModifiers(Modifier.PUBLIC)
            .returns(
                ParameterizedTypeName.get(
                    ClassName.get(Map.class),
                    ClassName.get(String.class),
                    ClassName.get(Object.class)))
            .addJavadoc(INHERITDOC);

    if (field.arguments().isEmpty()) {
      method.addStatement("return $T.emptyMap()", Collections.class);
    } else {
      method.addStatement("$T<String, Object> vars = new $T<>()", Map.class, HashMap.class);
      for (ArgumentDefinition arg : field.arguments()) {
        method.beginControlFlow("if ($N != null)", arg.name());
        method.addStatement("vars.put($S, $N)", arg.name(), arg.name());
        method.endControlFlow();
      }
      method.addStatement("return $T.unmodifiableMap(vars)", Collections.class);
    }

    return method.build();
  }

  private MethodSpec generateResponseTypeMethod(
      TypeName actualResponseType, boolean isList, ClassName responseClass) {
    MethodSpec.Builder method =
        MethodSpec.methodBuilder("responseType")
            .addAnnotation(Override.class)
            .addAnnotation(NotNull.class)
            .addModifiers(Modifier.PUBLIC)
            .addJavadoc(INHERITDOC);

    if (isList) {
      // For List types, we need to return a Class<List<T>> which requires casting
      method.returns(ParameterizedTypeName.get(ClassName.get(Class.class), actualResponseType));
      method.addStatement(
          "@SuppressWarnings(\"unchecked\") Class<$T> clazz = (Class<$T>) (Class<?>) $T.class",
          actualResponseType,
          actualResponseType,
          List.class);
      method.addStatement("return clazz");
    } else {
      method.returns(ParameterizedTypeName.get(ClassName.get(Class.class), actualResponseType));
      method.addStatement("return $T.class", responseClass);
    }

    return method.build();
  }

  private TypeSpec generateBuilder(
      FieldDefinition field,
      ClassName mutationClassName,
      ClassName builderClassName,
      ClassName projectionClass,
      boolean hasProjection) {
    TypeSpec.Builder builder =
        TypeSpec.classBuilder(BUILDER_CLASS_NAME)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);

    builder.addJavadoc("Builder for {@link $T}.\n", mutationClassName);

    // Add fields for arguments
    for (ArgumentDefinition arg : field.arguments()) {
      TypeName argType = typeMapper.mapType(arg.type());
      builder.addField(FieldSpec.builder(argType, arg.name(), Modifier.PRIVATE).build());
    }

    // Add projection field
    if (hasProjection) {
      builder.addField(FieldSpec.builder(projectionClass, "projection", Modifier.PRIVATE).build());
    }

    // Add setter methods for arguments
    for (ArgumentDefinition arg : field.arguments()) {
      builder.addMethod(generateBuilderSetter(arg, builderClassName));
    }

    // Add selecting() method for projection
    if (hasProjection) {
      ClassName projectionBuilderClass = projectionClass.nestedClass(BUILDER_CLASS_NAME);
      ParameterizedTypeName consumerType =
          ParameterizedTypeName.get(ClassName.get(Consumer.class), projectionBuilderClass);

      builder.addMethod(
          MethodSpec.methodBuilder("selecting")
              .addModifiers(Modifier.PUBLIC)
              .addParameter(consumerType, "config")
              .returns(builderClassName)
              .addStatement(
                  "$T projBuilder = $T.builder()", projectionBuilderClass, projectionClass)
              .addStatement("config.accept(projBuilder)")
              .addStatement("this.projection = projBuilder.build()")
              .addStatement("return this")
              .addJavadoc(
                  """
                  Configures the field selection for this mutation.

                  @param config the projection configuration
                  @return this builder
                  """)
              .build());
    }

    // Add build() method
    builder.addMethod(
        MethodSpec.methodBuilder("build")
            .addModifiers(Modifier.PUBLIC)
            .returns(mutationClassName)
            .addStatement("return new $T(this)", mutationClassName)
            .addJavadoc(
                """
                Builds the mutation.

                @return the built mutation
                @throws NullPointerException if required fields are not set
                """)
            .build());

    return builder.build();
  }

  private MethodSpec generateBuilderSetter(ArgumentDefinition arg, ClassName builderClassName) {
    TypeName argType = typeMapper.mapType(arg.type());

    MethodSpec.Builder setter =
        MethodSpec.methodBuilder(arg.name())
            .addModifiers(Modifier.PUBLIC)
            .addParameter(argType, arg.name())
            .returns(builderClassName)
            .addStatement("this.$N = $N", arg.name(), arg.name())
            .addStatement("return this");

    if (arg.description() != null && !arg.description().isBlank()) {
      setter.addJavadoc(
          GeneratorUtils.escapeJavadoc(arg.description())
              + "\n\n@param $N the value to set\n@return this builder\n",
          arg.name());
    } else {
      setter.addJavadoc(
          "Sets the $N argument.\n\n@param $N the value to set\n@return this builder\n",
          arg.name(),
          arg.name());
    }

    return setter.build();
  }

  private String getBaseTypeName(TypeReference type) {
    return type.getBaseName();
  }

  private boolean isList(TypeReference type) {
    return type.isList();
  }

  private boolean isRequired(TypeReference type) {
    return type.isNonNull();
  }

  private boolean isScalarType(String typeName) {
    return "String".equals(typeName)
        || "Int".equals(typeName)
        || "Float".equals(typeName)
        || "Boolean".equals(typeName)
        || "ID".equals(typeName)
        || schema.scalars().containsKey(typeName);
  }

  private ClassName getScalarClassName(String typeName) {
    return switch (typeName) {
      case "String", "ID" -> ClassName.get(String.class);
      case "Int" -> ClassName.get(Integer.class);
      case "Float" -> ClassName.get(Double.class);
      case "Boolean" -> ClassName.get(Boolean.class);
      default -> ClassName.get(Object.class);
    };
  }

  private String formatGraphQLType(TypeReference type) {
    return type.toGraphQL();
  }
}
