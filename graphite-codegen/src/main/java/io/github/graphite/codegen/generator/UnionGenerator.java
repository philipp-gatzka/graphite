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
import com.palantir.javapoet.TypeSpec;
import io.github.graphite.codegen.CodegenConfiguration;
import io.github.graphite.codegen.schema.SchemaModel;
import io.github.graphite.codegen.schema.UnionDefinition;
import java.util.ArrayList;
import java.util.List;
import javax.lang.model.element.Modifier;
import org.jetbrains.annotations.NotNull;

/**
 * Generates sealed marker interfaces from GraphQL union types.
 *
 * <p>For each GraphQL union, this generator produces a sealed marker interface with:
 *
 * <ul>
 *   <li>A permits clause listing all member types
 *   <li>No methods (marker interface only)
 *   <li>JavaDoc from GraphQL descriptions
 * </ul>
 *
 * <p>Example output for a GraphQL union:
 *
 * <pre>{@code
 * /**
 *  * A search result that can be a user or post
 *  *&#47;
 * public sealed interface SearchResultUnion permits UserDTO, PostDTO {}
 * }</pre>
 *
 * <p>The generated DTOs must implement these union interfaces, which is handled by {@link
 * TypeGenerator}.
 *
 * @see UnionDefinition
 * @see TypeGenerator
 * @see SchemaModel
 */
public final class UnionGenerator {

  private static final String UNION_PACKAGE_SUFFIX = ".union";
  private static final String TYPE_PACKAGE_SUFFIX = ".type";
  private static final String UNION_SUFFIX = "Union";
  private static final String DTO_SUFFIX = "DTO";

  private final CodegenConfiguration configuration;
  private final SchemaModel schema;

  /**
   * Creates a new union generator.
   *
   * @param configuration the codegen configuration
   * @param schema the parsed schema model
   */
  public UnionGenerator(@NotNull CodegenConfiguration configuration, @NotNull SchemaModel schema) {
    this.configuration = configuration;
    this.schema = schema;
  }

  /**
   * Generates sealed marker interfaces for all union types in the schema.
   *
   * @return a list of generated Java files
   */
  @NotNull
  public List<JavaFile> generate() {
    List<JavaFile> files = new ArrayList<>();

    for (UnionDefinition unionDef : schema.unions().values()) {
      files.add(generateUnion(unionDef));
    }

    return files;
  }

  /**
   * Generates a single sealed marker interface for a union.
   *
   * @param unionDef the union definition to generate
   * @return the generated Java file
   */
  @NotNull
  public JavaFile generateUnion(@NotNull UnionDefinition unionDef) {
    String packageName = configuration.packageName() + UNION_PACKAGE_SUFFIX;
    String typePackageName = configuration.packageName() + TYPE_PACKAGE_SUFFIX;
    String unionName = unionDef.name() + UNION_SUFFIX;

    TypeSpec.Builder unionBuilder =
        TypeSpec.interfaceBuilder(unionName).addModifiers(Modifier.PUBLIC, Modifier.SEALED);

    // Add JavaDoc
    if (unionDef.description() != null && !unionDef.description().isBlank()) {
      unionBuilder.addJavadoc(escapeJavadoc(unionDef.description()) + "\n");
    }

    // Add permits clause for all member types
    for (String memberType : unionDef.possibleTypes()) {
      // Member types are DTOs in the type package
      ClassName permittedClass = ClassName.get(typePackageName, memberType + DTO_SUFFIX);
      unionBuilder.addPermittedSubclass(permittedClass);
    }

    TypeSpec unionSpec = unionBuilder.build();

    return JavaFile.builder(packageName, unionSpec)
        .skipJavaLangImports(true)
        .indent("    ")
        .build();
  }

  private String escapeJavadoc(String text) {
    return text.replace("$", "$$")
        .replace("@", "{@literal @}")
        .replace("<", "&lt;")
        .replace(">", "&gt;");
  }
}
