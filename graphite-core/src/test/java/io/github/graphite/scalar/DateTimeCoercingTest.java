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

import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DateTimeCoercing")
class DateTimeCoercingTest {

  private final DateTimeCoercing coercing = DateTimeCoercing.INSTANCE;

  @Nested
  @DisplayName("javaType()")
  class JavaType {

    @Test
    @DisplayName("should return Instant.class")
    void shouldReturnInstantClass() {
      assertThat(coercing.javaType()).isEqualTo(Instant.class);
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
    @DisplayName("should serialize Instant to ISO-8601 string")
    void shouldSerializeInstantToIsoString() {
      Instant instant = Instant.parse("2024-01-15T10:30:00Z");
      assertThat(coercing.serialize(instant)).isEqualTo("2024-01-15T10:30:00Z");
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
    @DisplayName("should return same Instant if already Instant")
    void shouldReturnSameInstantIfAlreadyInstant() {
      Instant instant = Instant.parse("2024-01-15T10:30:00Z");
      assertThat(coercing.deserialize(instant)).isSameAs(instant);
    }

    @Test
    @DisplayName("should parse ISO-8601 string")
    void shouldParseIsoString() {
      assertThat(coercing.deserialize("2024-01-15T10:30:00Z"))
          .isEqualTo(Instant.parse("2024-01-15T10:30:00Z"));
    }

    @Test
    @DisplayName("should handle epoch milliseconds")
    void shouldHandleEpochMillis() {
      long epochMillis = 1705314600000L;
      Instant result = coercing.deserialize(epochMillis);
      assertThat(result).isEqualTo(Instant.ofEpochMilli(epochMillis));
    }

    @Test
    @DisplayName("should throw for invalid string")
    void shouldThrowForInvalidString() {
      assertThatThrownBy(() -> coercing.deserialize("not-a-date"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Cannot parse DateTime value");
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
