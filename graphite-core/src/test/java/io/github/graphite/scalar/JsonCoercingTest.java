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
package io.github.graphite.scalar;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("JsonCoercing")
class JsonCoercingTest {

  private final JsonCoercing coercing = JsonCoercing.INSTANCE;
  private final ObjectMapper mapper = new ObjectMapper();

  @Nested
  @DisplayName("javaType()")
  class JavaType {

    @Test
    @DisplayName("should return JsonNode.class")
    void shouldReturnJsonNodeClass() {
      assertThat(coercing.javaType()).isEqualTo(JsonNode.class);
    }
  }

  @Nested
  @DisplayName("serialize()")
  class Serialize {

    @Test
    @DisplayName("should return null for null input")
    void shouldReturnNullForNullInput() {
      assertThat(coercing.serialize(null)).isNull();
    }

    @Test
    @DisplayName("should serialize JsonNode to raw value")
    void shouldSerializeJsonNodeToRawValue() {
      JsonNode node = mapper.valueToTree(Map.of("key", "value"));
      Object result = coercing.serialize(node);
      assertThat(result).isInstanceOf(Map.class);
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) result;
      assertThat(map).containsEntry("key", "value");
    }

    @Test
    @DisplayName("should serialize array JsonNode")
    void shouldSerializeArrayJsonNode() {
      JsonNode node = mapper.valueToTree(List.of(1, 2, 3));
      Object result = coercing.serialize(node);
      assertThat(result).isInstanceOf(List.class);
      @SuppressWarnings("unchecked")
      List<Integer> list = (List<Integer>) result;
      assertThat(list).containsExactly(1, 2, 3);
    }
  }

  @Nested
  @DisplayName("deserialize()")
  class Deserialize {

    @Test
    @DisplayName("should return null for null input")
    void shouldReturnNullForNullInput() {
      assertThat(coercing.deserialize(null)).isNull();
    }

    @Test
    @DisplayName("should return same JsonNode if already JsonNode")
    void shouldReturnSameJsonNodeIfAlreadyJsonNode() {
      JsonNode node = mapper.valueToTree(Map.of("key", "value"));
      assertThat(coercing.deserialize(node)).isSameAs(node);
    }

    @Test
    @DisplayName("should parse JSON string")
    void shouldParseJsonString() {
      JsonNode result = coercing.deserialize("{\"key\":\"value\"}");
      assertThat(result.isObject()).isTrue();
      assertThat(result.get("key").asText()).isEqualTo("value");
    }

    @Test
    @DisplayName("should convert Map to JsonNode")
    void shouldConvertMapToJsonNode() {
      JsonNode result = coercing.deserialize(Map.of("key", "value"));
      assertThat(result.isObject()).isTrue();
      assertThat(result.get("key").asText()).isEqualTo("value");
    }

    @Test
    @DisplayName("should convert List to JsonNode")
    void shouldConvertListToJsonNode() {
      JsonNode result = coercing.deserialize(List.of(1, 2, 3));
      assertThat(result.isArray()).isTrue();
      assertThat(result.size()).isEqualTo(3);
    }

    @Test
    @DisplayName("should throw for invalid JSON string")
    void shouldThrowForInvalidJsonString() {
      assertThatThrownBy(() -> coercing.deserialize("{invalid}"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Cannot parse JSON value");
    }
  }
}
