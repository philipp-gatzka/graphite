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

import java.net.ConnectException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GraphiteConnectionException")
class GraphiteConnectionExceptionTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("should create exception with message only")
        void shouldCreateWithMessageOnly() {
            var exception = new GraphiteConnectionException("Connection refused");

            assertThat(exception.getMessage()).isEqualTo("Connection refused");
            assertThat(exception.getCause()).isNull();
            assertThat(exception.getHost()).isNull();
            assertThat(exception.getPort()).isNull();
        }

        @Test
        @DisplayName("should create exception with message and cause")
        void shouldCreateWithMessageAndCause() {
            var cause = new ConnectException("Connection refused");
            var exception = new GraphiteConnectionException("Connection refused", cause);

            assertThat(exception.getMessage()).isEqualTo("Connection refused");
            assertThat(exception.getCause()).isSameAs(cause);
        }

        @Test
        @DisplayName("should create exception with all parameters")
        void shouldCreateWithAllParameters() {
            var cause = new ConnectException("Connection refused");
            var exception =
                    new GraphiteConnectionException(
                            "Connection refused", cause, "api.example.com", 443);

            assertThat(exception.getMessage()).isEqualTo("Connection refused");
            assertThat(exception.getCause()).isSameAs(cause);
            assertThat(exception.getHost()).isEqualTo("api.example.com");
            assertThat(exception.getPort()).isEqualTo(443);
        }
    }

    @Nested
    @DisplayName("errorCode")
    class ErrorCode {

        @Test
        @DisplayName("should have CONNECTION_FAILED error code")
        void shouldHaveConnectionFailedErrorCode() {
            var exception = new GraphiteConnectionException("Connection failed");

            assertThat(exception.getErrorCode()).isEqualTo("CONNECTION_FAILED");
        }
    }

    @Nested
    @DisplayName("inheritance")
    class Inheritance {

        @Test
        @DisplayName("should extend GraphiteClientException")
        void shouldExtendGraphiteClientException() {
            var exception = new GraphiteConnectionException("Test");

            assertThat(exception).isInstanceOf(GraphiteClientException.class);
        }

        @Test
        @DisplayName("should be catchable as GraphiteException")
        void shouldBeCatchableAsGraphiteException() {
            GraphiteException exception = new GraphiteConnectionException("Test");

            assertThat(exception).isNotNull();
        }
    }
}
