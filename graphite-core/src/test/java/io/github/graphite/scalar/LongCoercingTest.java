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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("LongCoercing")
class LongCoercingTest {

  private final LongCoercing coercing = LongCoercing.INSTANCE;

  @Nested
  @DisplayName("javaType()")
  class JavaType {

    @Test
    @DisplayName("should return Long.class")
    void shouldReturnLongClass() {
      assertThat(coercing.javaType()).isEqualTo(Long.class);
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
    @DisplayName("should serialize Long to string")
    void shouldSerializeLongToString() {
      assertThat(coercing.serialize(9223372036854775807L)).isEqualTo("9223372036854775807");
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
    @DisplayName("should return same Long if already Long")
    void shouldReturnSameLongIfAlreadyLong() {
      Long value = 12345L;
      assertThat(coercing.deserialize(value)).isSameAs(value);
    }

    @Test
    @DisplayName("should convert Number to Long")
    void shouldConvertNumberToLong() {
      assertThat(coercing.deserialize(12345)).isEqualTo(12345L);
      assertThat(coercing.deserialize(12345.67)).isEqualTo(12345L);
    }

    @Test
    @DisplayName("should parse string to Long")
    void shouldParseStringToLong() {
      assertThat(coercing.deserialize("9223372036854775807")).isEqualTo(9223372036854775807L);
      assertThat(coercing.deserialize("  12345  ")).isEqualTo(12345L);
    }

    @Test
    @DisplayName("should throw for invalid string")
    void shouldThrowForInvalidString() {
      assertThatThrownBy(() -> coercing.deserialize("not-a-number"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Cannot parse Long value");
    }

    @Test
    @DisplayName("should throw for unsupported type")
    void shouldThrowForUnsupportedType() {
      assertThatThrownBy(() -> coercing.deserialize(new Object()))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Cannot coerce");
    }
  }
}
