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

package io.github.graphite.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GraphiteException")
class GraphiteExceptionTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("should create exception with message only")
        void shouldCreateWithMessageOnly() {
            var exception = new GraphiteException("Test error");

            assertThat(exception.getMessage()).isEqualTo("Test error");
            assertThat(exception.getCause()).isNull();
            assertThat(exception.getErrorCode()).isNull();
        }

        @Test
        @DisplayName("should create exception with message and cause")
        void shouldCreateWithMessageAndCause() {
            var cause = new RuntimeException("Original error");
            var exception = new GraphiteException("Test error", cause);

            assertThat(exception.getMessage()).isEqualTo("Test error");
            assertThat(exception.getCause()).isSameAs(cause);
            assertThat(exception.getErrorCode()).isNull();
        }

        @Test
        @DisplayName("should create exception with message and error code")
        void shouldCreateWithMessageAndErrorCode() {
            var exception = new GraphiteException("Test error", "ERR_001");

            assertThat(exception.getMessage()).isEqualTo("Test error");
            assertThat(exception.getCause()).isNull();
            assertThat(exception.getErrorCode()).isEqualTo("ERR_001");
        }

        @Test
        @DisplayName("should create exception with all parameters")
        void shouldCreateWithAllParameters() {
            var cause = new RuntimeException("Original error");
            var exception = new GraphiteException("Test error", cause, "ERR_001");

            assertThat(exception.getMessage()).isEqualTo("Test error");
            assertThat(exception.getCause()).isSameAs(cause);
            assertThat(exception.getErrorCode()).isEqualTo("ERR_001");
        }

        @Test
        @DisplayName("should allow null cause")
        void shouldAllowNullCause() {
            var exception = new GraphiteException("Test error", (Throwable) null);

            assertThat(exception.getCause()).isNull();
        }

        @Test
        @DisplayName("should allow null error code")
        void shouldAllowNullErrorCode() {
            var exception = new GraphiteException("Test error", (String) null);

            assertThat(exception.getErrorCode()).isNull();
        }
    }

    @Nested
    @DisplayName("inheritance")
    class Inheritance {

        @Test
        @DisplayName("should be a RuntimeException")
        void shouldBeRuntimeException() {
            var exception = new GraphiteException("Test error");

            assertThat(exception).isInstanceOf(RuntimeException.class);
        }

        @Test
        @DisplayName("should be catchable as Exception")
        void shouldBeCatchableAsException() {
            var exception = new GraphiteException("Test error");

            assertThat(exception).isInstanceOf(Exception.class);
        }
    }
}
