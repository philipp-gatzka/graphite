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
import com.palantir.javapoet.JavaFile;
import com.palantir.javapoet.MethodSpec;
import com.palantir.javapoet.TypeName;
import com.palantir.javapoet.TypeSpec;
import io.github.graphite.codegen.CodegenConfiguration;
import io.github.graphite.codegen.schema.FieldDefinition;
import io.github.graphite.codegen.schema.InterfaceDefinition;
import io.github.graphite.codegen.schema.SchemaModel;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Modifier;
import org.jetbrains.annotations.NotNull;

/**
 * Generates sealed Java interfaces from GraphQL interface types.
 *
 * <p>For each GraphQL interface, this generator produces a sealed Java interface with:
 *
 * <ul>
 *   <li>A permits clause listing all implementing types
 *   <li>Abstract accessor methods for each interface field
 *   <li>JavaDoc from GraphQL descriptions
 * </ul>
 *
 * <p>Example output for a GraphQL interface:
 *
 * <pre>{@code
 * /**
 *  * An object with a globally unique ID
 *  *&#47;
 * public sealed interface NodeDTO permits UserDTO, PostDTO {
 *     /**
 *      * The globally unique ID
 *      *
 *      * @return the id value
 *      *&#47;
 *     String id();
 * }
 * }</pre>
 *
 * <p>The generated DTOs must implement these interfaces, which is handled by {@link TypeGenerator}.
 *
 * @see InterfaceDefinition
 * @see TypeGenerator
 * @see SchemaModel
 */
public final class InterfaceGenerator {

  private static final String INTERFACE_PACKAGE_SUFFIX = ".type";
  private static final String DTO_SUFFIX = "DTO";

  private final CodegenConfiguration configuration;
  private final SchemaModel schema;
  private final TypeMapper typeMapper;

  /**
   * Creates a new interface generator.
   *
   * @param configuration the codegen configuration
   * @param schema the parsed schema model
   */
  public InterfaceGenerator(
      @NotNull CodegenConfiguration configuration, @NotNull SchemaModel schema) {
    this.configuration = configuration;
    this.schema = schema;
    this.typeMapper = new TypeMapper(configuration, schema);
  }

  /**
   * Generates sealed interfaces for all interface types in the schema.
   *
   * @return a list of generated Java files
   */
  @NotNull
  public List<JavaFile> generate() {
    List<JavaFile> files = new ArrayList<>();

    for (InterfaceDefinition interfaceDef : schema.interfaces().values()) {
      files.add(generateInterface(interfaceDef));
    }

    return files;
  }

  /**
   * Generates a single sealed interface.
   *
   * @param interfaceDef the interface definition to generate
   * @return the generated Java file
   */
  @NotNull
  public JavaFile generateInterface(@NotNull InterfaceDefinition interfaceDef) {
    String packageName = configuration.packageName() + INTERFACE_PACKAGE_SUFFIX;
    String interfaceName = interfaceDef.name() + DTO_SUFFIX;

    TypeSpec.Builder interfaceBuilder =
        TypeSpec.interfaceBuilder(interfaceName).addModifiers(Modifier.PUBLIC, Modifier.SEALED);

    // Add JavaDoc
    if (interfaceDef.description() != null && !interfaceDef.description().isBlank()) {
      interfaceBuilder.addJavadoc(escapeJavadoc(interfaceDef.description()) + "\n");
    }

    // Add permits clause for all implementing types
    for (String implementingType : interfaceDef.possibleTypes()) {
      ClassName permittedClass = ClassName.get(packageName, implementingType + DTO_SUFFIX);
      interfaceBuilder.addPermittedSubclass(permittedClass);
    }

    // Add abstract accessor methods for each field
    for (FieldDefinition field : interfaceDef.fields()) {
      interfaceBuilder.addMethod(generateAccessorMethod(field));
    }

    TypeSpec interfaceSpec = interfaceBuilder.build();

    return JavaFile.builder(packageName, interfaceSpec)
        .skipJavaLangImports(true)
        .indent("    ")
        .build();
  }

  private MethodSpec generateAccessorMethod(FieldDefinition field) {
    TypeName returnType = typeMapper.mapType(field.type());

    MethodSpec.Builder methodBuilder =
        MethodSpec.methodBuilder(field.name())
            .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
            .returns(returnType);

    // Add JavaDoc
    if (field.description() != null && !field.description().isBlank()) {
      methodBuilder.addJavadoc(
          escapeJavadoc(field.description()) + "\n\n@return the $N value\n", field.name());
    } else {
      methodBuilder.addJavadoc(
          "Returns the $N value.\n\n@return the $N value\n", field.name(), field.name());
    }

    return methodBuilder.build();
  }

  private String escapeJavadoc(String text) {
    return text.replace("$", "$$")
        .replace("@", "{@literal @}")
        .replace("<", "&lt;")
        .replace(">", "&gt;");
  }
}
