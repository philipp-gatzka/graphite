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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SchemaParser")
class SchemaParserTest {

  private SchemaParser parser;

  @BeforeEach
  void setUp() {
    parser = new SchemaParser();
  }

  @Nested
  @DisplayName("parse(Path)")
  class ParseFromPath {

    @Test
    @DisplayName("should parse complete schema from file")
    void shouldParseCompleteSchema() {
      Path schemaPath = Path.of("src/test/resources/schemas/complete-schema.json").toAbsolutePath();

      SchemaModel schema = parser.parse(schemaPath);

      assertThat(schema).isNotNull();
      assertThat(schema.queryType()).isNotNull();
      assertThat(schema.queryType().name()).isEqualTo("Query");
      assertThat(schema.hasMutationType()).isTrue();
      assertThat(schema.mutationType().name()).isEqualTo("Mutation");
      assertThat(schema.hasSubscriptionType()).isFalse();
    }

    @Test
    @DisplayName("should throw exception for non-existent file")
    void shouldThrowForNonExistentFile() {
      Path schemaPath = Path.of("non-existent-schema.json");

      assertThatThrownBy(() -> parser.parse(schemaPath))
          .isInstanceOf(SchemaParseException.class)
          .hasMessageContaining("Failed to read schema file");
    }
  }

  @Nested
  @DisplayName("parse(String)")
  class ParseFromString {

    @Test
    @DisplayName("should parse minimal schema")
    void shouldParseMinimalSchema() {
      String json =
          """
          {
            "__schema": {
              "queryType": { "name": "Query" },
              "mutationType": null,
              "subscriptionType": null,
              "types": [
                {
                  "kind": "OBJECT",
                  "name": "Query",
                  "fields": [
                    {
                      "name": "hello",
                      "args": [],
                      "type": { "kind": "SCALAR", "name": "String" },
                      "isDeprecated": false
                    }
                  ],
                  "interfaces": []
                }
              ]
            }
          }
          """;

      SchemaModel schema = parser.parse(json);

      assertThat(schema).isNotNull();
      assertThat(schema.queryType().name()).isEqualTo("Query");
      assertThat(schema.queryType().fields()).hasSize(1);
      assertThat(schema.queryType().fields().getFirst().name()).isEqualTo("hello");
    }

    @Test
    @DisplayName("should parse schema with data wrapper")
    void shouldParseSchemaWithDataWrapper() {
      String json =
          """
          {
            "data": {
              "__schema": {
                "queryType": { "name": "Query" },
                "mutationType": null,
                "subscriptionType": null,
                "types": [
                  {
                    "kind": "OBJECT",
                    "name": "Query",
                    "fields": [],
                    "interfaces": []
                  }
                ]
              }
            }
          }
          """;

      SchemaModel schema = parser.parse(json);

      assertThat(schema).isNotNull();
      assertThat(schema.queryType().name()).isEqualTo("Query");
    }

    @Test
    @DisplayName("should throw exception for missing __schema field")
    void shouldThrowForMissingSchemaField() {
      String json =
          """
          { "types": [] }
          """;

      assertThatThrownBy(() -> parser.parse(json))
          .isInstanceOf(SchemaParseException.class)
          .hasMessageContaining("Missing '__schema' field");
    }

    @Test
    @DisplayName("should throw exception for missing queryType")
    void shouldThrowForMissingQueryType() {
      String json =
          """
          {
            "__schema": {
              "mutationType": null,
              "types": []
            }
          }
          """;

      assertThatThrownBy(() -> parser.parse(json))
          .isInstanceOf(SchemaParseException.class)
          .hasMessageContaining("Missing 'queryType' field");
    }

    @Test
    @DisplayName("should throw exception for invalid JSON")
    void shouldThrowForInvalidJson() {
      String json = "not valid json";

      assertThatThrownBy(() -> parser.parse(json))
          .isInstanceOf(SchemaParseException.class)
          .hasMessageContaining("Failed to parse schema JSON");
    }

    @Test
    @DisplayName("should throw exception when query type not found in types")
    void shouldThrowWhenQueryTypeNotFound() {
      String json =
          """
          {
            "__schema": {
              "queryType": { "name": "Query" },
              "mutationType": null,
              "subscriptionType": null,
              "types": []
            }
          }
          """;

      assertThatThrownBy(() -> parser.parse(json))
          .isInstanceOf(SchemaParseException.class)
          .hasMessageContaining("Query type 'Query' not found");
    }
  }

  @Nested
  @DisplayName("parse(InputStream)")
  class ParseFromInputStream {

    @Test
    @DisplayName("should parse schema from input stream")
    void shouldParseFromInputStream() {
      String json =
          """
          {
            "__schema": {
              "queryType": { "name": "Query" },
              "mutationType": null,
              "subscriptionType": null,
              "types": [
                {
                  "kind": "OBJECT",
                  "name": "Query",
                  "fields": [],
                  "interfaces": []
                }
              ]
            }
          }
          """;
      InputStream input = new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8));

      SchemaModel schema = parser.parse(input);

      assertThat(schema).isNotNull();
      assertThat(schema.queryType().name()).isEqualTo("Query");
    }
  }

  @Nested
  @DisplayName("Type parsing")
  class TypeParsing {

    @Test
    @DisplayName("should parse object types with fields")
    void shouldParseObjectTypes() {
      Path schemaPath = Path.of("src/test/resources/schemas/complete-schema.json").toAbsolutePath();

      SchemaModel schema = parser.parse(schemaPath);

      assertThat(schema.types()).containsKey("User");
      TypeDefinition user = schema.types().get("User");
      assertThat(user.description()).isEqualTo("A user in the system");
      assertThat(user.fields()).hasSizeGreaterThan(0);

      Optional<FieldDefinition> idField = user.getField("id");
      assertThat(idField).isPresent();
      assertThat(idField.get().type().isNonNull()).isTrue();
      assertThat(idField.get().type().getBaseName()).isEqualTo("ID");
    }

    @Test
    @DisplayName("should parse enum types with values")
    void shouldParseEnumTypes() {
      Path schemaPath = Path.of("src/test/resources/schemas/complete-schema.json").toAbsolutePath();

      SchemaModel schema = parser.parse(schemaPath);

      assertThat(schema.enums()).containsKey("UserStatus");
      EnumDefinition userStatus = schema.enums().get("UserStatus");
      assertThat(userStatus.values()).hasSize(4);
      assertThat(userStatus.values().stream().map(EnumValueDefinition::name))
          .containsExactly("ACTIVE", "INACTIVE", "SUSPENDED", "DELETED");

      // Check deprecated value
      EnumValueDefinition inactive =
          userStatus.values().stream()
              .filter(v -> v.name().equals("INACTIVE"))
              .findFirst()
              .orElseThrow();
      assertThat(inactive.isDeprecated()).isTrue();
      assertThat(inactive.deprecationReason()).isEqualTo("Use SUSPENDED instead");
    }

    @Test
    @DisplayName("should parse input types with fields")
    void shouldParseInputTypes() {
      Path schemaPath = Path.of("src/test/resources/schemas/complete-schema.json").toAbsolutePath();

      SchemaModel schema = parser.parse(schemaPath);

      assertThat(schema.inputTypes()).containsKey("CreateUserInput");
      InputTypeDefinition createUser = schema.inputTypes().get("CreateUserInput");
      assertThat(createUser.inputFields()).hasSize(3);

      Optional<InputFieldDefinition> nameField = createUser.getField("name");
      assertThat(nameField).isPresent();
      assertThat(nameField.get().isRequired()).isTrue();

      Optional<InputFieldDefinition> statusField = createUser.getField("status");
      assertThat(statusField).isPresent();
      assertThat(statusField.get().hasDefaultValue()).isTrue();
      assertThat(statusField.get().defaultValue()).isEqualTo("ACTIVE");
    }

    @Test
    @DisplayName("should parse interface types")
    void shouldParseInterfaceTypes() {
      Path schemaPath = Path.of("src/test/resources/schemas/complete-schema.json").toAbsolutePath();

      SchemaModel schema = parser.parse(schemaPath);

      assertThat(schema.interfaces()).containsKey("Node");
      InterfaceDefinition node = schema.interfaces().get("Node");
      assertThat(node.description()).isEqualTo("An object with a globally unique ID");
      assertThat(node.fields()).hasSize(1);
      assertThat(node.possibleTypes()).containsExactlyInAnyOrder("User", "Post");
    }

    @Test
    @DisplayName("should parse union types")
    void shouldParseUnionTypes() {
      Path schemaPath = Path.of("src/test/resources/schemas/complete-schema.json").toAbsolutePath();

      SchemaModel schema = parser.parse(schemaPath);

      assertThat(schema.unions()).containsKey("SearchResult");
      UnionDefinition searchResult = schema.unions().get("SearchResult");
      assertThat(searchResult.description())
          .isEqualTo("A search result that can be a user or post");
      assertThat(searchResult.possibleTypes()).containsExactlyInAnyOrder("User", "Post");
    }

    @Test
    @DisplayName("should parse custom scalar types")
    void shouldParseCustomScalars() {
      Path schemaPath = Path.of("src/test/resources/schemas/complete-schema.json").toAbsolutePath();

      SchemaModel schema = parser.parse(schemaPath);

      assertThat(schema.scalars()).containsKey("DateTime");
      ScalarDefinition dateTime = schema.scalars().get("DateTime");
      assertThat(dateTime.description()).isEqualTo("An ISO-8601 encoded UTC date time string");
      assertThat(dateTime.isBuiltIn()).isFalse();
    }

    @Test
    @DisplayName("should skip built-in scalar types")
    void shouldSkipBuiltInScalars() {
      Path schemaPath = Path.of("src/test/resources/schemas/complete-schema.json").toAbsolutePath();

      SchemaModel schema = parser.parse(schemaPath);

      // Built-in scalars should not be in the custom scalars map
      assertThat(schema.scalars()).doesNotContainKey("String");
      assertThat(schema.scalars()).doesNotContainKey("Int");
      assertThat(schema.scalars()).doesNotContainKey("Float");
      assertThat(schema.scalars()).doesNotContainKey("Boolean");
      assertThat(schema.scalars()).doesNotContainKey("ID");
    }

    @Test
    @DisplayName("should parse types implementing interfaces")
    void shouldParseTypesImplementingInterfaces() {
      Path schemaPath = Path.of("src/test/resources/schemas/complete-schema.json").toAbsolutePath();

      SchemaModel schema = parser.parse(schemaPath);

      TypeDefinition user = schema.types().get("User");
      assertThat(user.hasInterfaces()).isTrue();
      assertThat(user.interfaces()).containsExactlyInAnyOrder("Node", "Timestamped");
    }
  }

  @Nested
  @DisplayName("Field argument parsing")
  class ArgumentParsing {

    @Test
    @DisplayName("should parse field arguments")
    void shouldParseFieldArguments() {
      Path schemaPath = Path.of("src/test/resources/schemas/complete-schema.json").toAbsolutePath();

      SchemaModel schema = parser.parse(schemaPath);

      FieldDefinition userField = schema.queryType().getField("user").orElseThrow();
      assertThat(userField.hasArguments()).isTrue();
      assertThat(userField.arguments()).hasSize(1);

      ArgumentDefinition idArg = userField.arguments().getFirst();
      assertThat(idArg.name()).isEqualTo("id");
      assertThat(idArg.isRequired()).isTrue();
      assertThat(idArg.type().isNonNull()).isTrue();
      assertThat(idArg.type().getBaseName()).isEqualTo("ID");
    }

    @Test
    @DisplayName("should parse arguments with default values")
    void shouldParseArgumentsWithDefaults() {
      Path schemaPath = Path.of("src/test/resources/schemas/complete-schema.json").toAbsolutePath();

      SchemaModel schema = parser.parse(schemaPath);

      FieldDefinition usersField = schema.queryType().getField("users").orElseThrow();
      assertThat(usersField.arguments()).hasSize(2);

      ArgumentDefinition limitArg =
          usersField.arguments().stream()
              .filter(a -> a.name().equals("limit"))
              .findFirst()
              .orElseThrow();
      assertThat(limitArg.hasDefaultValue()).isTrue();
      assertThat(limitArg.defaultValue()).isEqualTo("10");
      assertThat(limitArg.isRequired()).isFalse();
    }
  }

  @Nested
  @DisplayName("TypeReference parsing")
  class TypeReferenceParsing {

    @Test
    @DisplayName("should parse non-null type reference")
    void shouldParseNonNullType() {
      Path schemaPath = Path.of("src/test/resources/schemas/complete-schema.json").toAbsolutePath();

      SchemaModel schema = parser.parse(schemaPath);

      FieldDefinition idField = schema.types().get("User").getField("id").orElseThrow();
      TypeReference typeRef = idField.type();

      assertThat(typeRef.isNonNull()).isTrue();
      assertThat(typeRef.isList()).isFalse();
      assertThat(typeRef.getBaseName()).isEqualTo("ID");
      assertThat(typeRef.toGraphQL()).isEqualTo("ID!");
    }

    @Test
    @DisplayName("should parse list type reference")
    void shouldParseListType() {
      Path schemaPath = Path.of("src/test/resources/schemas/complete-schema.json").toAbsolutePath();

      SchemaModel schema = parser.parse(schemaPath);

      FieldDefinition usersField = schema.queryType().getField("users").orElseThrow();
      TypeReference typeRef = usersField.type();

      assertThat(typeRef.isNonNull()).isTrue();
      assertThat(typeRef.isList()).isTrue();
      assertThat(typeRef.getBaseName()).isEqualTo("User");
      assertThat(typeRef.toGraphQL()).isEqualTo("[User!]!");
    }

    @Test
    @DisplayName("should parse nullable type reference")
    void shouldParseNullableType() {
      Path schemaPath = Path.of("src/test/resources/schemas/complete-schema.json").toAbsolutePath();

      SchemaModel schema = parser.parse(schemaPath);

      FieldDefinition emailField = schema.types().get("User").getField("email").orElseThrow();
      TypeReference typeRef = emailField.type();

      assertThat(typeRef.isNonNull()).isFalse();
      assertThat(typeRef.isList()).isFalse();
      assertThat(typeRef.getBaseName()).isEqualTo("String");
      assertThat(typeRef.toGraphQL()).isEqualTo("String");
    }
  }

  @Nested
  @DisplayName("Deprecation handling")
  class DeprecationHandling {

    @Test
    @DisplayName("should parse deprecated fields")
    void shouldParseDeprecatedFields() {
      Path schemaPath = Path.of("src/test/resources/schemas/complete-schema.json").toAbsolutePath();

      SchemaModel schema = parser.parse(schemaPath);

      FieldDefinition deleteUser = schema.mutationType().getField("deleteUser").orElseThrow();
      assertThat(deleteUser.isDeprecated()).isTrue();
      assertThat(deleteUser.deprecationReason()).isEqualTo("Use deactivateUser instead");
    }

    @Test
    @DisplayName("should parse non-deprecated fields")
    void shouldParseNonDeprecatedFields() {
      Path schemaPath = Path.of("src/test/resources/schemas/complete-schema.json").toAbsolutePath();

      SchemaModel schema = parser.parse(schemaPath);

      FieldDefinition createUser = schema.mutationType().getField("createUser").orElseThrow();
      assertThat(createUser.isDeprecated()).isFalse();
      assertThat(createUser.deprecationReason()).isNull();
    }
  }

  @Nested
  @DisplayName("SchemaModel queries")
  class SchemaModelQueries {

    private SchemaModel schema;

    @BeforeEach
    void setUp() {
      Path schemaPath = Path.of("src/test/resources/schemas/complete-schema.json").toAbsolutePath();
      schema = parser.parse(schemaPath);
    }

    @Test
    @DisplayName("should identify scalar types")
    void shouldIdentifyScalars() {
      assertThat(schema.isScalar("String")).isTrue();
      assertThat(schema.isScalar("Int")).isTrue();
      assertThat(schema.isScalar("DateTime")).isTrue();
      assertThat(schema.isScalar("User")).isFalse();
    }

    @Test
    @DisplayName("should identify enum types")
    void shouldIdentifyEnums() {
      assertThat(schema.isEnum("UserStatus")).isTrue();
      assertThat(schema.isEnum("User")).isFalse();
    }

    @Test
    @DisplayName("should identify input types")
    void shouldIdentifyInputTypes() {
      assertThat(schema.isInputType("CreateUserInput")).isTrue();
      assertThat(schema.isInputType("User")).isFalse();
    }

    @Test
    @DisplayName("should identify object types")
    void shouldIdentifyObjectTypes() {
      assertThat(schema.isObjectType("User")).isTrue();
      assertThat(schema.isObjectType("CreateUserInput")).isFalse();
    }

    @Test
    @DisplayName("should identify interface types")
    void shouldIdentifyInterfaces() {
      assertThat(schema.isInterface("Node")).isTrue();
      assertThat(schema.isInterface("User")).isFalse();
    }

    @Test
    @DisplayName("should identify union types")
    void shouldIdentifyUnions() {
      assertThat(schema.isUnion("SearchResult")).isTrue();
      assertThat(schema.isUnion("User")).isFalse();
    }
  }
}
