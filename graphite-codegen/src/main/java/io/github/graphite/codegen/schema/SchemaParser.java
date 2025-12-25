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
package io.github.graphite.codegen.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.jetbrains.annotations.NotNull;

/**
 * Parses GraphQL introspection JSON into a {@link SchemaModel}.
 *
 * <p>This parser handles the standard GraphQL introspection result format:
 *
 * <pre>{@code
 * {
 *   "__schema": {
 *     "types": [...],
 *     "queryType": { "name": "Query" },
 *     "mutationType": { "name": "Mutation" },
 *     "subscriptionType": null
 *   }
 * }
 * }</pre>
 *
 * <p>It also supports the "data" wrapper commonly returned by GraphQL endpoints:
 *
 * <pre>{@code
 * {
 *   "data": {
 *     "__schema": { ... }
 *   }
 * }
 * }</pre>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * SchemaParser parser = new SchemaParser();
 * SchemaModel schema = parser.parse(Path.of("schema.json"));
 *
 * // Or from a string
 * SchemaModel schema = parser.parse(jsonContent);
 * }</pre>
 *
 * @see SchemaModel
 * @see SchemaParseException
 */
public final class SchemaParser {

  private static final String SCHEMA_FIELD = "__schema";
  private static final String DATA_FIELD = "data";
  private static final String TYPES_FIELD = "types";
  private static final String QUERY_TYPE_FIELD = "queryType";
  private static final String MUTATION_TYPE_FIELD = "mutationType";
  private static final String SUBSCRIPTION_TYPE_FIELD = "subscriptionType";
  private static final String NAME_FIELD = "name";
  private static final String KIND_FIELD = "kind";
  private static final String DESCRIPTION_FIELD = "description";
  private static final String FIELDS_FIELD = "fields";
  private static final String INPUT_FIELDS_FIELD = "inputFields";
  private static final String ARGS_FIELD = "args";
  private static final String TYPE_FIELD = "type";
  private static final String DEFAULT_VALUE_FIELD = "defaultValue";
  private static final String OF_TYPE_FIELD = "ofType";
  private static final String ENUM_VALUES_FIELD = "enumValues";
  private static final String INTERFACES_FIELD = "interfaces";
  private static final String POSSIBLE_TYPES_FIELD = "possibleTypes";
  private static final String IS_DEPRECATED_FIELD = "isDeprecated";
  private static final String DEPRECATION_REASON_FIELD = "deprecationReason";

  private static final Set<String> INTRINSIC_TYPES =
      Set.of(
          "__Schema",
          "__Type",
          "__TypeKind",
          "__Field",
          "__InputValue",
          "__EnumValue",
          "__Directive",
          "__DirectiveLocation");

  private final ObjectMapper objectMapper;

  /** Creates a new schema parser with a default ObjectMapper. */
  public SchemaParser() {
    this(new ObjectMapper());
  }

  /**
   * Creates a new schema parser with the specified ObjectMapper.
   *
   * @param objectMapper the ObjectMapper to use for JSON parsing
   */
  public SchemaParser(@NotNull ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Parses a GraphQL introspection result from a file.
   *
   * @param schemaFile the path to the schema.json file
   * @return the parsed schema model
   * @throws SchemaParseException if parsing fails
   */
  @NotNull
  public SchemaModel parse(@NotNull Path schemaFile) {
    try (InputStream input = Files.newInputStream(schemaFile)) {
      return parse(input);
    } catch (IOException e) {
      throw new SchemaParseException("Failed to read schema file: " + schemaFile, e);
    }
  }

  /**
   * Parses a GraphQL introspection result from an input stream.
   *
   * @param input the input stream containing the schema JSON
   * @return the parsed schema model
   * @throws SchemaParseException if parsing fails
   */
  @NotNull
  public SchemaModel parse(@NotNull InputStream input) {
    try {
      JsonNode root = objectMapper.readTree(input);
      return parseRoot(root);
    } catch (IOException e) {
      throw new SchemaParseException("Failed to parse schema JSON", e);
    }
  }

  /**
   * Parses a GraphQL introspection result from a JSON string.
   *
   * @param json the JSON string
   * @return the parsed schema model
   * @throws SchemaParseException if parsing fails
   */
  @NotNull
  public SchemaModel parse(@NotNull String json) {
    try {
      JsonNode root = objectMapper.readTree(json);
      return parseRoot(root);
    } catch (IOException e) {
      throw new SchemaParseException("Failed to parse schema JSON", e);
    }
  }

  private SchemaModel parseRoot(JsonNode root) {
    // Handle optional "data" wrapper
    JsonNode schemaNode = root.get(SCHEMA_FIELD);
    if (schemaNode == null) {
      JsonNode dataNode = root.get(DATA_FIELD);
      if (dataNode != null) {
        schemaNode = dataNode.get(SCHEMA_FIELD);
      }
    }

    if (schemaNode == null) {
      throw new SchemaParseException("Missing '__schema' field in introspection result");
    }

    return parseSchema(schemaNode);
  }

  private SchemaModel parseSchema(JsonNode schemaNode) {
    String queryTypeName = parseQueryTypeName(schemaNode);
    String mutationTypeName = parseOptionalTypeName(schemaNode, MUTATION_TYPE_FIELD);
    String subscriptionTypeName = parseOptionalTypeName(schemaNode, SUBSCRIPTION_TYPE_FIELD);

    JsonNode typesNode = schemaNode.get(TYPES_FIELD);
    if (typesNode == null || !typesNode.isArray()) {
      throw new SchemaParseException("Missing or invalid 'types' array in schema");
    }

    Map<String, TypeDefinition> types = new LinkedHashMap<>();
    Map<String, EnumDefinition> enums = new LinkedHashMap<>();
    Map<String, InputTypeDefinition> inputTypes = new LinkedHashMap<>();
    Map<String, InterfaceDefinition> interfaces = new LinkedHashMap<>();
    Map<String, UnionDefinition> unions = new LinkedHashMap<>();
    Map<String, ScalarDefinition> scalars = new LinkedHashMap<>();

    for (JsonNode typeNode : typesNode) {
      String name = getRequiredString(typeNode, NAME_FIELD, "type");

      // Skip intrinsic types and built-in scalars
      if (isSkippableType(name)) {
        continue;
      }

      String kind = getRequiredString(typeNode, KIND_FIELD, "type '" + name + "'");

      switch (kind) {
        case "OBJECT" -> types.put(name, parseObjectType(typeNode));
        case "ENUM" -> enums.put(name, parseEnumType(typeNode));
        case "INPUT_OBJECT" -> inputTypes.put(name, parseInputType(typeNode));
        case "INTERFACE" -> interfaces.put(name, parseInterfaceType(typeNode));
        case "UNION" -> unions.put(name, parseUnionType(typeNode));
        case "SCALAR" -> scalars.put(name, parseScalarType(typeNode));
        default -> {
          // Ignore unknown kinds
        }
      }
    }

    // Get root types
    TypeDefinition queryType = types.get(queryTypeName);
    if (queryType == null) {
      throw new SchemaParseException("Query type '" + queryTypeName + "' not found in schema");
    }

    TypeDefinition mutationType = mutationTypeName != null ? types.get(mutationTypeName) : null;
    TypeDefinition subscriptionType =
        subscriptionTypeName != null ? types.get(subscriptionTypeName) : null;

    return new SchemaModel(
        queryType,
        mutationType,
        subscriptionType,
        types,
        enums,
        inputTypes,
        interfaces,
        unions,
        scalars);
  }

  private boolean isSkippableType(String name) {
    return INTRINSIC_TYPES.contains(name)
        || name.startsWith("__")
        || ScalarDefinition.BUILT_IN_SCALARS.contains(name);
  }

  private String parseQueryTypeName(JsonNode schemaNode) {
    JsonNode queryTypeNode = schemaNode.get(QUERY_TYPE_FIELD);
    if (queryTypeNode == null || queryTypeNode.isNull()) {
      throw new SchemaParseException("Missing 'queryType' field in schema");
    }
    return getRequiredString(queryTypeNode, NAME_FIELD, "queryType");
  }

  private String parseOptionalTypeName(JsonNode schemaNode, String fieldName) {
    JsonNode typeNode = schemaNode.get(fieldName);
    if (typeNode != null && !typeNode.isNull()) {
      return getString(typeNode, NAME_FIELD);
    }
    return null;
  }

  private TypeDefinition parseObjectType(JsonNode node) {
    String name = getRequiredString(node, NAME_FIELD, "type");
    String description = getString(node, DESCRIPTION_FIELD);
    List<FieldDefinition> fields = parseFields(node.get(FIELDS_FIELD), name);
    List<String> implementedInterfaces = parseInterfaceNames(node.get(INTERFACES_FIELD));

    return new TypeDefinition(name, description, fields, implementedInterfaces);
  }

  private EnumDefinition parseEnumType(JsonNode node) {
    String name = getRequiredString(node, NAME_FIELD, "enum");
    String description = getString(node, DESCRIPTION_FIELD);
    List<EnumValueDefinition> values = parseEnumValues(node.get(ENUM_VALUES_FIELD), name);

    return new EnumDefinition(name, description, values);
  }

  private InputTypeDefinition parseInputType(JsonNode node) {
    String name = getRequiredString(node, NAME_FIELD, "input type");
    String description = getString(node, DESCRIPTION_FIELD);
    List<InputFieldDefinition> inputFields = parseInputFields(node.get(INPUT_FIELDS_FIELD), name);

    return new InputTypeDefinition(name, description, inputFields);
  }

  private InterfaceDefinition parseInterfaceType(JsonNode node) {
    String name = getRequiredString(node, NAME_FIELD, "interface");
    String description = getString(node, DESCRIPTION_FIELD);
    List<FieldDefinition> fields = parseFields(node.get(FIELDS_FIELD), name);
    List<String> possibleTypes = parsePossibleTypes(node.get(POSSIBLE_TYPES_FIELD));

    return new InterfaceDefinition(name, description, fields, possibleTypes);
  }

  private UnionDefinition parseUnionType(JsonNode node) {
    String name = getRequiredString(node, NAME_FIELD, "union");
    String description = getString(node, DESCRIPTION_FIELD);
    List<String> possibleTypes = parsePossibleTypes(node.get(POSSIBLE_TYPES_FIELD));

    return new UnionDefinition(name, description, possibleTypes);
  }

  private ScalarDefinition parseScalarType(JsonNode node) {
    String name = getRequiredString(node, NAME_FIELD, "scalar");
    String description = getString(node, DESCRIPTION_FIELD);

    return new ScalarDefinition(name, description);
  }

  private List<FieldDefinition> parseFields(JsonNode fieldsNode, String typeName) {
    if (fieldsNode == null || !fieldsNode.isArray()) {
      return List.of();
    }

    List<FieldDefinition> fields = new ArrayList<>();
    for (JsonNode fieldNode : fieldsNode) {
      fields.add(parseField(fieldNode, typeName));
    }
    return List.copyOf(fields);
  }

  private FieldDefinition parseField(JsonNode node, String typeName) {
    String name = getRequiredString(node, NAME_FIELD, "field in " + typeName);
    String description = getString(node, DESCRIPTION_FIELD);
    TypeReference type = parseTypeReference(node.get(TYPE_FIELD), typeName + "." + name);
    List<ArgumentDefinition> args = parseArguments(node.get(ARGS_FIELD), typeName + "." + name);
    boolean isDeprecated = getBoolean(node, IS_DEPRECATED_FIELD);
    String deprecationReason = getString(node, DEPRECATION_REASON_FIELD);

    return new FieldDefinition(name, description, type, args, isDeprecated, deprecationReason);
  }

  private List<ArgumentDefinition> parseArguments(JsonNode argsNode, String context) {
    if (argsNode == null || !argsNode.isArray()) {
      return List.of();
    }

    List<ArgumentDefinition> args = new ArrayList<>();
    for (JsonNode argNode : argsNode) {
      args.add(parseArgument(argNode, context));
    }
    return List.copyOf(args);
  }

  private ArgumentDefinition parseArgument(JsonNode node, String context) {
    String name = getRequiredString(node, NAME_FIELD, "argument in " + context);
    String description = getString(node, DESCRIPTION_FIELD);
    TypeReference type = parseTypeReference(node.get(TYPE_FIELD), context + "." + name);
    String defaultValue = getString(node, DEFAULT_VALUE_FIELD);

    return new ArgumentDefinition(name, description, type, defaultValue);
  }

  private List<InputFieldDefinition> parseInputFields(JsonNode fieldsNode, String typeName) {
    if (fieldsNode == null || !fieldsNode.isArray()) {
      return List.of();
    }

    List<InputFieldDefinition> fields = new ArrayList<>();
    for (JsonNode fieldNode : fieldsNode) {
      fields.add(parseInputField(fieldNode, typeName));
    }
    return List.copyOf(fields);
  }

  private InputFieldDefinition parseInputField(JsonNode node, String typeName) {
    String name = getRequiredString(node, NAME_FIELD, "input field in " + typeName);
    String description = getString(node, DESCRIPTION_FIELD);
    TypeReference type = parseTypeReference(node.get(TYPE_FIELD), typeName + "." + name);
    String defaultValue = getString(node, DEFAULT_VALUE_FIELD);

    return new InputFieldDefinition(name, description, type, defaultValue);
  }

  private List<EnumValueDefinition> parseEnumValues(JsonNode valuesNode, String enumName) {
    if (valuesNode == null || !valuesNode.isArray()) {
      return List.of();
    }

    List<EnumValueDefinition> values = new ArrayList<>();
    for (JsonNode valueNode : valuesNode) {
      String name = getRequiredString(valueNode, NAME_FIELD, "enum value in " + enumName);
      String description = getString(valueNode, DESCRIPTION_FIELD);
      boolean isDeprecated = getBoolean(valueNode, IS_DEPRECATED_FIELD);
      String deprecationReason = getString(valueNode, DEPRECATION_REASON_FIELD);

      values.add(new EnumValueDefinition(name, description, isDeprecated, deprecationReason));
    }
    return List.copyOf(values);
  }

  private List<String> parseInterfaceNames(JsonNode interfacesNode) {
    if (interfacesNode == null || !interfacesNode.isArray()) {
      return List.of();
    }

    List<String> names = new ArrayList<>();
    for (JsonNode interfaceNode : interfacesNode) {
      String name = getString(interfaceNode, NAME_FIELD);
      if (name != null) {
        names.add(name);
      }
    }
    return List.copyOf(names);
  }

  private List<String> parsePossibleTypes(JsonNode possibleTypesNode) {
    if (possibleTypesNode == null || !possibleTypesNode.isArray()) {
      return List.of();
    }

    List<String> names = new ArrayList<>();
    for (JsonNode typeNode : possibleTypesNode) {
      String name = getString(typeNode, NAME_FIELD);
      if (name != null) {
        names.add(name);
      }
    }
    return List.copyOf(names);
  }

  private TypeReference parseTypeReference(JsonNode typeNode, String context) {
    if (typeNode == null || typeNode.isNull()) {
      throw new SchemaParseException("Missing type reference", context);
    }

    String kind = getString(typeNode, KIND_FIELD);
    if (kind == null) {
      throw new SchemaParseException("Missing 'kind' in type reference", context);
    }

    return switch (kind) {
      case "NON_NULL" -> {
        JsonNode ofType = typeNode.get(OF_TYPE_FIELD);
        yield new TypeReference.NonNull(parseTypeReference(ofType, context));
      }
      case "LIST" -> {
        JsonNode ofType = typeNode.get(OF_TYPE_FIELD);
        yield new TypeReference.ListType(parseTypeReference(ofType, context));
      }
      default -> {
        String name = getString(typeNode, NAME_FIELD);
        if (name == null) {
          throw new SchemaParseException("Missing 'name' in named type reference", context);
        }
        yield new TypeReference.Named(name);
      }
    };
  }

  private String getRequiredString(JsonNode node, String field, String context) {
    JsonNode fieldNode = node.get(field);
    if (fieldNode == null || fieldNode.isNull()) {
      throw new SchemaParseException("Missing required field '" + field + "'", context);
    }
    return fieldNode.asText();
  }

  private String getString(JsonNode node, String field) {
    JsonNode fieldNode = node.get(field);
    if (fieldNode == null || fieldNode.isNull()) {
      return null;
    }
    return fieldNode.asText();
  }

  private boolean getBoolean(JsonNode node, String field) {
    JsonNode fieldNode = node.get(field);
    return fieldNode != null && fieldNode.asBoolean();
  }
}
