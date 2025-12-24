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

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BigDecimalCoercing")
class BigDecimalCoercingTest {

  private final BigDecimalCoercing coercing = BigDecimalCoercing.INSTANCE;

  @Nested
  @DisplayName("javaType()")
  class JavaType {

    @Test
    @DisplayName("should return BigDecimal.class")
    void shouldReturnBigDecimalClass() {
      assertThat(coercing.javaType()).isEqualTo(BigDecimal.class);
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
    @DisplayName("should serialize BigDecimal to plain string")
    void shouldSerializeBigDecimalToPlainString() {
      assertThat(coercing.serialize(new BigDecimal("123.456"))).isEqualTo("123.456");
    }

    @Test
    @DisplayName("should not use scientific notation")
    void shouldNotUseScientificNotation() {
      assertThat(coercing.serialize(new BigDecimal("1E+10"))).isEqualTo("10000000000");
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
    @DisplayName("should return same BigDecimal if already BigDecimal")
    void shouldReturnSameBigDecimalIfAlreadyBigDecimal() {
      BigDecimal value = new BigDecimal("123.456");
      assertThat(coercing.deserialize(value)).isSameAs(value);
    }

    @Test
    @DisplayName("should parse string to BigDecimal")
    void shouldParseStringToBigDecimal() {
      assertThat(coercing.deserialize("123.456")).isEqualTo(new BigDecimal("123.456"));
      assertThat(coercing.deserialize("  99.99  ")).isEqualTo(new BigDecimal("99.99"));
    }

    @Test
    @DisplayName("should convert Number to BigDecimal")
    void shouldConvertNumberToBigDecimal() {
      BigDecimal result = coercing.deserialize(123.456);
      assertThat(result).isEqualByComparingTo(new BigDecimal("123.456"));
    }

    @Test
    @DisplayName("should throw for invalid string")
    void shouldThrowForInvalidString() {
      assertThatThrownBy(() -> coercing.deserialize("not-a-number"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Cannot parse BigDecimal value");
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
