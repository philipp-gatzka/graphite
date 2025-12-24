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

@DisplayName("GraphiteClientException")
class GraphiteClientExceptionTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("should create exception with message only")
        void shouldCreateWithMessageOnly() {
            var exception = new GraphiteClientException("Connection failed");

            assertThat(exception.getMessage()).isEqualTo("Connection failed");
            assertThat(exception.getCause()).isNull();
            assertThat(exception.getErrorCode()).isNull();
        }

        @Test
        @DisplayName("should create exception with message and cause")
        void shouldCreateWithMessageAndCause() {
            var cause = new java.net.ConnectException("Connection refused");
            var exception = new GraphiteClientException("Connection failed", cause);

            assertThat(exception.getMessage()).isEqualTo("Connection failed");
            assertThat(exception.getCause()).isSameAs(cause);
        }

        @Test
        @DisplayName("should create exception with message and error code")
        void shouldCreateWithMessageAndErrorCode() {
            var exception = new GraphiteClientException("Connection failed", "CONN_ERR");

            assertThat(exception.getMessage()).isEqualTo("Connection failed");
            assertThat(exception.getErrorCode()).isEqualTo("CONN_ERR");
        }

        @Test
        @DisplayName("should create exception with all parameters")
        void shouldCreateWithAllParameters() {
            var cause = new java.net.ConnectException("Connection refused");
            var exception = new GraphiteClientException("Connection failed", cause, "CONN_ERR");

            assertThat(exception.getMessage()).isEqualTo("Connection failed");
            assertThat(exception.getCause()).isSameAs(cause);
            assertThat(exception.getErrorCode()).isEqualTo("CONN_ERR");
        }
    }

    @Nested
    @DisplayName("inheritance")
    class Inheritance {

        @Test
        @DisplayName("should extend GraphiteException")
        void shouldExtendGraphiteException() {
            var exception = new GraphiteClientException("Test");

            assertThat(exception).isInstanceOf(GraphiteException.class);
        }

        @Test
        @DisplayName("should be catchable as GraphiteException")
        void shouldBeCatchableAsGraphiteException() {
            GraphiteException exception = new GraphiteClientException("Test");

            assertThat(exception).isNotNull();
        }
    }
}
