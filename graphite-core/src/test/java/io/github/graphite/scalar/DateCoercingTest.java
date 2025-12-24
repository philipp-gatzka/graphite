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

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DateCoercing")
class DateCoercingTest {

  private final DateCoercing coercing = DateCoercing.INSTANCE;

  @Nested
  @DisplayName("javaType()")
  class JavaType {

    @Test
    @DisplayName("should return LocalDate.class")
    void shouldReturnLocalDateClass() {
      assertThat(coercing.javaType()).isEqualTo(LocalDate.class);
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
    @DisplayName("should serialize LocalDate to ISO-8601 string")
    void shouldSerializeLocalDateToIsoString() {
      LocalDate date = LocalDate.of(2024, 1, 15);
      assertThat(coercing.serialize(date)).isEqualTo("2024-01-15");
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
    @DisplayName("should return same LocalDate if already LocalDate")
    void shouldReturnSameLocalDateIfAlreadyLocalDate() {
      LocalDate date = LocalDate.of(2024, 1, 15);
      assertThat(coercing.deserialize(date)).isSameAs(date);
    }

    @Test
    @DisplayName("should parse ISO-8601 string")
    void shouldParseIsoString() {
      assertThat(coercing.deserialize("2024-01-15")).isEqualTo(LocalDate.of(2024, 1, 15));
    }

    @Test
    @DisplayName("should throw for invalid string")
    void shouldThrowForInvalidString() {
      assertThatThrownBy(() -> coercing.deserialize("not-a-date"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Cannot parse Date value");
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
