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
import com.palantir.javapoet.ParameterizedTypeName;
import com.palantir.javapoet.TypeName;
import io.github.graphite.codegen.CodegenConfiguration;
import io.github.graphite.codegen.schema.SchemaModel;
import io.github.graphite.codegen.schema.TypeReference;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jetbrains.annotations.NotNull;

/**
 * Maps GraphQL types to Java types.
 *
 * <p>This class handles:
 *
 * <ul>
 *   <li>Built-in GraphQL scalars (String, Int, Float, Boolean, ID)
 *   <li>Common custom scalars (DateTime, Date, Time, UUID, etc.)
 *   <li>Custom scalar mappings from configuration
 *   <li>Object types, enums, input types, interfaces, and unions
 *   <li>List and NonNull type modifiers
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * TypeMapper mapper = new TypeMapper(config, schema);
 * TypeName javaType = mapper.mapType(field.type());
 * }</pre>
 *
 * @see CodegenConfiguration
 * @see SchemaModel
 */
public final class TypeMapper {

  /** Default scalar mappings for built-in and common scalars. */
  private static final Map<String, TypeName> DEFAULT_SCALAR_MAPPINGS = new HashMap<>();

  static {
    // Built-in GraphQL scalars
    DEFAULT_SCALAR_MAPPINGS.put("String", TypeName.get(String.class));
    DEFAULT_SCALAR_MAPPINGS.put("Int", TypeName.INT.box());
    DEFAULT_SCALAR_MAPPINGS.put("Float", TypeName.DOUBLE.box());
    DEFAULT_SCALAR_MAPPINGS.put("Boolean", TypeName.BOOLEAN.box());
    DEFAULT_SCALAR_MAPPINGS.put("ID", TypeName.get(String.class));

    // Common custom scalars
    DEFAULT_SCALAR_MAPPINGS.put("DateTime", TypeName.get(Instant.class));
    DEFAULT_SCALAR_MAPPINGS.put("Date", TypeName.get(LocalDate.class));
    DEFAULT_SCALAR_MAPPINGS.put("Time", TypeName.get(LocalTime.class));
    DEFAULT_SCALAR_MAPPINGS.put("UUID", TypeName.get(UUID.class));
    DEFAULT_SCALAR_MAPPINGS.put("Long", TypeName.LONG.box());
    DEFAULT_SCALAR_MAPPINGS.put("BigDecimal", TypeName.get(BigDecimal.class));
    DEFAULT_SCALAR_MAPPINGS.put("BigInteger", TypeName.get(BigInteger.class));
  }

  private final CodegenConfiguration configuration;
  private final SchemaModel schema;
  private final Map<String, TypeName> scalarMappings;

  /**
   * Creates a new type mapper.
   *
   * @param configuration the codegen configuration
   * @param schema the parsed schema model
   */
  public TypeMapper(@NotNull CodegenConfiguration configuration, @NotNull SchemaModel schema) {
    this.configuration = configuration;
    this.schema = schema;
    this.scalarMappings = new HashMap<>(DEFAULT_SCALAR_MAPPINGS);

    // Add custom scalar mappings from configuration
    configuration
        .customScalarMappings()
        .forEach((name, className) -> scalarMappings.put(name, ClassName.bestGuess(className)));
  }

  /**
   * Maps a GraphQL type reference to a Java type.
   *
   * @param typeRef the GraphQL type reference
   * @return the corresponding Java type
   */
  @NotNull
  public TypeName mapType(@NotNull TypeReference typeRef) {
    return switch (typeRef) {
      case TypeReference.NonNull(var inner) -> mapType(inner);
      case TypeReference.ListType(var inner) ->
          ParameterizedTypeName.get(ClassName.get(List.class), mapType(inner));
      case TypeReference.Named(var name) -> mapNamedType(name);
    };
  }

  /**
   * Maps a named GraphQL type to a Java type.
   *
   * @param typeName the GraphQL type name
   * @return the corresponding Java type
   */
  @NotNull
  public TypeName mapNamedType(@NotNull String typeName) {
    // Check scalar mappings first
    TypeName scalarType = scalarMappings.get(typeName);
    if (scalarType != null) {
      return scalarType;
    }

    // Check if it's an enum
    if (schema.isEnum(typeName)) {
      return ClassName.get(configuration.packageName() + ".enumeration", typeName);
    }

    // Check if it's an input type
    if (schema.isInputType(typeName)) {
      // Only add Input suffix if not already present
      String inputClassName = typeName.endsWith("Input") ? typeName : typeName + "Input";
      return ClassName.get(configuration.packageName() + ".input", inputClassName);
    }

    // Check if it's an interface
    if (schema.isInterface(typeName)) {
      return ClassName.get(configuration.packageName() + ".type", typeName);
    }

    // Check if it's a union
    if (schema.isUnion(typeName)) {
      return ClassName.get(configuration.packageName() + ".union", typeName);
    }

    // Default to object type (DTO)
    return ClassName.get(configuration.packageName() + ".type", typeName + "DTO");
  }

  /**
   * Returns whether a type reference is nullable (not wrapped in NonNull).
   *
   * @param typeRef the type reference to check
   * @return true if the type is nullable
   */
  public boolean isNullable(@NotNull TypeReference typeRef) {
    return !typeRef.isNonNull();
  }

  /**
   * Returns whether a type reference is a list type.
   *
   * @param typeRef the type reference to check
   * @return true if the type is a list
   */
  public boolean isList(@NotNull TypeReference typeRef) {
    return typeRef.isList();
  }

  /**
   * Gets the base type name from a type reference.
   *
   * @param typeRef the type reference
   * @return the base type name
   */
  @NotNull
  public String getBaseName(@NotNull TypeReference typeRef) {
    return typeRef.getBaseName();
  }
}
