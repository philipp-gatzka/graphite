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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("VoidCoercing")
class VoidCoercingTest {

  private final VoidCoercing coercing = VoidCoercing.INSTANCE;

  @Nested
  @DisplayName("javaType()")
  class JavaType {

    @Test
    @DisplayName("should return Void.class")
    void shouldReturnVoidClass() {
      assertThat(coercing.javaType()).isEqualTo(Void.class);
    }
  }

  @Nested
  @DisplayName("serialize()")
  class Serialize {

    @Test
    @DisplayName("should always return null")
    void shouldAlwaysReturnNull() {
      assertThat(coercing.serialize(null)).isNull();
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
    @DisplayName("should return null for any input")
    void shouldReturnNullForAnyInput() {
      assertThat(coercing.deserialize("ignored")).isNull();
      assertThat(coercing.deserialize(12345)).isNull();
      assertThat(coercing.deserialize(new Object())).isNull();
    }
  }
}
