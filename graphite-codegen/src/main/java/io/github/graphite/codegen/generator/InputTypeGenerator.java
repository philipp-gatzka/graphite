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
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.github.graphite.codegen.CodegenConfiguration;
import io.github.graphite.codegen.schema.InputFieldDefinition;
import io.github.graphite.codegen.schema.InputTypeDefinition;
import io.github.graphite.codegen.schema.SchemaModel;
import io.github.graphite.codegen.util.GeneratorUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import javax.lang.model.element.Modifier;
import org.jetbrains.annotations.NotNull;

/**
 * Generates Java classes with builder pattern from GraphQL input types.
 *
 * <p>For each GraphQL input type, this generator produces a final class with:
 *
 * <ul>
 *   <li>Private final fields for each input field
 *   <li>A private constructor
 *   <li>A static builder() factory method
 *   <li>A nested Builder class with fluent setters
 *   <li>Required field validation in build()
 *   <li>Getter methods for all fields
 * </ul>
 *
 * <p>Example output:
 *
 * <pre>{@code
 * public final class CreateUserInput {
 *     private final String name;
 *     private final String email;
 *
 *     private CreateUserInput(Builder builder) {
 *         this.name = Objects.requireNonNull(builder.name, "name is required");
 *         this.email = builder.email;
 *     }
 *
 *     public static Builder builder() { return new Builder(); }
 *
 *     public String getName() { return name; }
 *     public String getEmail() { return email; }
 *
 *     public static final class Builder {
 *         private String name;
 *         private String email;
 *
 *         public Builder name(String name) { this.name = name; return this; }
 *         public Builder email(String email) { this.email = email; return this; }
 *         public CreateUserInput build() { return new CreateUserInput(this); }
 *     }
 * }
 * }</pre>
 *
 * @see TypeMapper
 * @see SchemaModel
 */
public final class InputTypeGenerator {

  private static final String INPUT_PACKAGE_SUFFIX = ".input";
  private static final String INPUT_SUFFIX = "Input";

  private final CodegenConfiguration configuration;
  private final SchemaModel schema;
  private final TypeMapper typeMapper;

  /**
   * Creates a new input type generator.
   *
   * @param configuration the codegen configuration
   * @param schema the parsed schema model
   */
  public InputTypeGenerator(
      @NotNull CodegenConfiguration configuration, @NotNull SchemaModel schema) {
    this.configuration = configuration;
    this.schema = schema;
    this.typeMapper = new TypeMapper(configuration, schema);
  }

  /**
   * Generates input type classes for all input types in the schema.
   *
   * @return a list of generated Java files
   */
  @NotNull
  public List<JavaFile> generate() {
    List<JavaFile> files = new ArrayList<>();

    for (InputTypeDefinition inputType : schema.inputTypes().values()) {
      files.add(generateInputType(inputType));
    }

    return files;
  }

  /**
   * Generates a single input type class.
   *
   * @param inputType the input type definition to generate
   * @return the generated Java file
   */
  @NotNull
  public JavaFile generateInputType(@NotNull InputTypeDefinition inputType) {
    String packageName = configuration.packageName() + INPUT_PACKAGE_SUFFIX;
    String className = inputType.name();

    // Ensure the class name ends with "Input"
    if (!className.endsWith(INPUT_SUFFIX)) {
      className = className + INPUT_SUFFIX;
    }

    ClassName inputClassName = ClassName.get(packageName, className);
    ClassName builderClassName = inputClassName.nestedClass("Builder");

    TypeSpec.Builder classBuilder =
        TypeSpec.classBuilder(className).addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    // Add JavaDoc
    if (inputType.description() != null && !inputType.description().isBlank()) {
      classBuilder.addJavadoc(GeneratorUtils.escapeJavadoc(inputType.description()) + "\n");
    }

    // Add fields
    for (InputFieldDefinition field : inputType.inputFields()) {
      classBuilder.addField(generateField(field));
    }

    // Add private constructor
    classBuilder.addMethod(generateConstructor(inputType, builderClassName));

    // Add static builder() method
    classBuilder.addMethod(
        MethodSpec.methodBuilder("builder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(builderClassName)
            .addStatement("return new Builder()")
            .addJavadoc("Creates a new builder for this input type.\n\n@return a new builder\n")
            .build());

    // Add getters
    for (InputFieldDefinition field : inputType.inputFields()) {
      classBuilder.addMethod(generateGetter(field));
    }

    // Add Builder class
    classBuilder.addType(generateBuilder(inputType, inputClassName, builderClassName));

    TypeSpec classSpec = classBuilder.build();

    return JavaFile.builder(packageName, classSpec)
        .skipJavaLangImports(true)
        .indent("    ")
        .build();
  }

  private FieldSpec generateField(InputFieldDefinition field) {
    TypeName javaType = typeMapper.mapType(field.type());

    return FieldSpec.builder(javaType, field.name(), Modifier.PRIVATE, Modifier.FINAL).build();
  }

  private MethodSpec generateConstructor(
      InputTypeDefinition inputType, ClassName builderClassName) {
    MethodSpec.Builder constructor =
        MethodSpec.constructorBuilder()
            .addModifiers(Modifier.PRIVATE)
            .addParameter(builderClassName, "builder");

    for (InputFieldDefinition field : inputType.inputFields()) {
      if (field.isRequired()) {
        constructor.addStatement(
            "this.$N = $T.requireNonNull(builder.$N, $S)",
            field.name(),
            Objects.class,
            field.name(),
            field.name() + " is required");
      } else {
        constructor.addStatement("this.$N = builder.$N", field.name(), field.name());
      }
    }

    return constructor.build();
  }

  private MethodSpec generateGetter(InputFieldDefinition field) {
    TypeName javaType = typeMapper.mapType(field.type());
    String methodName = "get" + GeneratorUtils.capitalize(field.name());

    MethodSpec.Builder getter =
        MethodSpec.methodBuilder(methodName)
            .addModifiers(Modifier.PUBLIC)
            .returns(javaType)
            .addStatement("return $N", field.name());

    if (field.description() != null && !field.description().isBlank()) {
      getter.addJavadoc(
          GeneratorUtils.escapeJavadoc(field.description()) + "\n\n@return the $N value\n",
          field.name());
    } else {
      getter.addJavadoc(
          "Returns the $N value.\n\n@return the $N value\n", field.name(), field.name());
    }

    return getter.build();
  }

  private TypeSpec generateBuilder(
      InputTypeDefinition inputType, ClassName inputClassName, ClassName builderClassName) {
    TypeSpec.Builder builder =
        TypeSpec.classBuilder("Builder")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL);

    builder.addJavadoc("Builder for {@link $T}.\n", inputClassName);

    // Add fields
    for (InputFieldDefinition field : inputType.inputFields()) {
      TypeName javaType = typeMapper.mapType(field.type());
      builder.addField(FieldSpec.builder(javaType, field.name(), Modifier.PRIVATE).build());
    }

    // Add setter methods
    for (InputFieldDefinition field : inputType.inputFields()) {
      builder.addMethod(generateBuilderSetter(field, builderClassName));
    }

    // Add build() method
    builder.addMethod(
        MethodSpec.methodBuilder("build")
            .addModifiers(Modifier.PUBLIC)
            .returns(inputClassName)
            .addStatement("return new $T(this)", inputClassName)
            .addJavadoc(
                """
                Builds the input type instance.

                @return the built instance
                @throws NullPointerException if required fields are not set
                """)
            .build());

    return builder.build();
  }

  private MethodSpec generateBuilderSetter(InputFieldDefinition field, ClassName builderClassName) {
    TypeName javaType = typeMapper.mapType(field.type());

    MethodSpec.Builder setter =
        MethodSpec.methodBuilder(field.name())
            .addModifiers(Modifier.PUBLIC)
            .addParameter(javaType, field.name())
            .returns(builderClassName)
            .addStatement("this.$N = $N", field.name(), field.name())
            .addStatement("return this");

    if (field.description() != null && !field.description().isBlank()) {
      setter.addJavadoc(
          GeneratorUtils.escapeJavadoc(field.description())
              + "\n\n@param $N the value to set\n@return this builder\n",
          field.name());
    } else {
      setter.addJavadoc(
          "Sets the $N value.\n\n@param $N the value to set\n@return this builder\n",
          field.name(),
          field.name());
    }

    return setter.build();
  }
}
