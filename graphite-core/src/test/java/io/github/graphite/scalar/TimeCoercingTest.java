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

import java.time.LocalTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("TimeCoercing")
class TimeCoercingTest {

  private final TimeCoercing coercing = TimeCoercing.INSTANCE;

  @Nested
  @DisplayName("javaType()")
  class JavaType {

    @Test
    @DisplayName("should return LocalTime.class")
    void shouldReturnLocalTimeClass() {
      assertThat(coercing.javaType()).isEqualTo(LocalTime.class);
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
    @DisplayName("should serialize LocalTime to ISO-8601 string")
    void shouldSerializeLocalTimeToIsoString() {
      LocalTime time = LocalTime.of(10, 30, 0);
      assertThat(coercing.serialize(time)).isEqualTo("10:30");
    }

    @Test
    @DisplayName("should serialize LocalTime with seconds")
    void shouldSerializeLocalTimeWithSeconds() {
      LocalTime time = LocalTime.of(10, 30, 45);
      assertThat(coercing.serialize(time)).isEqualTo("10:30:45");
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
    @DisplayName("should return same LocalTime if already LocalTime")
    void shouldReturnSameLocalTimeIfAlreadyLocalTime() {
      LocalTime time = LocalTime.of(10, 30, 0);
      assertThat(coercing.deserialize(time)).isSameAs(time);
    }

    @Test
    @DisplayName("should parse ISO-8601 string")
    void shouldParseIsoString() {
      assertThat(coercing.deserialize("10:30:00")).isEqualTo(LocalTime.of(10, 30, 0));
    }

    @Test
    @DisplayName("should parse time without seconds")
    void shouldParseTimeWithoutSeconds() {
      assertThat(coercing.deserialize("10:30")).isEqualTo(LocalTime.of(10, 30, 0));
    }

    @Test
    @DisplayName("should throw for invalid string")
    void shouldThrowForInvalidString() {
      assertThatThrownBy(() -> coercing.deserialize("not-a-time"))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("Cannot parse Time value");
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
