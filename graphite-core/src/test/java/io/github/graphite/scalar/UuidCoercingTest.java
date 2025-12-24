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

import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("UuidCoercing")
class UuidCoercingTest {

  private final UuidCoercing coercing = UuidCoercing.INSTANCE;

  @Nested
  @DisplayName("javaType()")
  class JavaType {

    @Test
    @DisplayName("should return UUID.class")
    void shouldReturnUuidClass() {
      assertThat(coercing.javaType()).isEqualTo(UUID.class);
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
    @DisplayName("should serialize UUID to string")
    void shouldSerializeUuidToString() {
      UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
      assertThat(coercing.serialize(uuid)).isEqualTo("550e8400-e29b-41d4-a716-446655440000");
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
    @DisplayName("should return same UUID if already UUID")
    void shouldReturnSameUuidIfAlreadyUuid() {
      UUID uuid = UUID.fromString("550e8400-e29b-41d4-a716-446655440000");
      assertThat(coercing.deserialize(uuid)).isSameAs(uuid);
    }

    @Test
    @DisplayName("should parse UUID string")
    void shouldParseUuidString() {
      assertThat(coercing.deserialize("550e8400-e29b-41d4-a716-446655440000"))
          .isEqualTo(UUID.fromString("550e8400-e29b-41d4-a716-446655440000"));
    }

    @Test
    @DisplayName("should throw for invalid string")
    void shouldThrowForInvalidString() {
      assertThatThrownBy(() -> coercing.deserialize("not-a-uuid"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Cannot parse UUID value");
    }

    @Test
    @DisplayName("should throw for unsupported type")
    void shouldThrowForUnsupportedType() {
      assertThatThrownBy(() -> coercing.deserialize(12345))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Cannot coerce");
    }
  }
}
