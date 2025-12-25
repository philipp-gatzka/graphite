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

import java.math.BigInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BigIntegerCoercing")
class BigIntegerCoercingTest {

  private final BigIntegerCoercing coercing = BigIntegerCoercing.INSTANCE;

  @Nested
  @DisplayName("javaType()")
  class JavaType {

    @Test
    @DisplayName("should return BigInteger.class")
    void shouldReturnBigIntegerClass() {
      assertThat(coercing.javaType()).isEqualTo(BigInteger.class);
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
    @DisplayName("should serialize BigInteger to string")
    void shouldSerializeBigIntegerToString() {
      assertThat(coercing.serialize(new BigInteger("12345678901234567890")))
          .isEqualTo("12345678901234567890");
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
    @DisplayName("should return same BigInteger if already BigInteger")
    void shouldReturnSameBigIntegerIfAlreadyBigInteger() {
      BigInteger value = new BigInteger("12345");
      assertThat(coercing.deserialize(value)).isSameAs(value);
    }

    @Test
    @DisplayName("should parse string to BigInteger")
    void shouldParseStringToBigInteger() {
      assertThat(coercing.deserialize("12345678901234567890"))
          .isEqualTo(new BigInteger("12345678901234567890"));
      assertThat(coercing.deserialize("  12345  ")).isEqualTo(new BigInteger("12345"));
    }

    @Test
    @DisplayName("should convert Number to BigInteger")
    void shouldConvertNumberToBigInteger() {
      assertThat(coercing.deserialize(12345)).isEqualTo(new BigInteger("12345"));
    }

    @Test
    @DisplayName("should throw for invalid string")
    void shouldThrowForInvalidString() {
      assertThatThrownBy(() -> coercing.deserialize("not-a-number"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Cannot parse BigInteger value");
    }

    @Test
    @DisplayName("should throw for unsupported type")
    void shouldThrowForUnsupportedType() {
      Object unsupportedType = new Object();
      assertThatThrownBy(() -> coercing.deserialize(unsupportedType))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Cannot coerce");
    }
  }
}
