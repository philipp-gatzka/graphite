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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("GraphiteServerException")
class GraphiteServerExceptionTest {

  @Nested
  @DisplayName("constructor")
  class Constructor {

    @Test
    @DisplayName("should create exception with message and status code")
    void shouldCreateWithMessageAndStatusCode() {
      var exception = new GraphiteServerException("Not Found", 404);

      assertThat(exception.getMessage()).isEqualTo("Not Found");
      assertThat(exception.getStatusCode()).isEqualTo(404);
      assertThat(exception.getCause()).isNull();
      assertThat(exception.getErrorCode()).isNull();
    }

    @Test
    @DisplayName("should create exception with message, status code, and cause")
    void shouldCreateWithMessageStatusCodeAndCause() {
      var cause = new RuntimeException("Upstream error");
      var exception = new GraphiteServerException("Internal Server Error", 500, cause);

      assertThat(exception.getMessage()).isEqualTo("Internal Server Error");
      assertThat(exception.getStatusCode()).isEqualTo(500);
      assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("should create exception with all parameters")
    void shouldCreateWithAllParameters() {
      var cause = new RuntimeException("Upstream error");
      var exception = new GraphiteServerException("Internal Server Error", 500, cause, "SRV_ERR");

      assertThat(exception.getMessage()).isEqualTo("Internal Server Error");
      assertThat(exception.getStatusCode()).isEqualTo(500);
      assertThat(exception.getCause()).isSameAs(cause);
      assertThat(exception.getErrorCode()).isEqualTo("SRV_ERR");
    }
  }

  @Nested
  @DisplayName("isClientError")
  class IsClientError {

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404, 422, 429, 499})
    @DisplayName("should return true for 4xx status codes")
    void shouldReturnTrueFor4xxStatusCodes(int statusCode) {
      var exception = new GraphiteServerException("Error", statusCode);

      assertThat(exception.isClientError()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 201, 301, 302, 500, 502, 503})
    @DisplayName("should return false for non-4xx status codes")
    void shouldReturnFalseForNon4xxStatusCodes(int statusCode) {
      var exception = new GraphiteServerException("Error", statusCode);

      assertThat(exception.isClientError()).isFalse();
    }
  }

  @Nested
  @DisplayName("isServerError")
  class IsServerError {

    @ParameterizedTest
    @ValueSource(ints = {500, 501, 502, 503, 504, 599})
    @DisplayName("should return true for 5xx status codes")
    void shouldReturnTrueFor5xxStatusCodes(int statusCode) {
      var exception = new GraphiteServerException("Error", statusCode);

      assertThat(exception.isServerError()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(ints = {200, 201, 301, 302, 400, 401, 404})
    @DisplayName("should return false for non-5xx status codes")
    void shouldReturnFalseForNon5xxStatusCodes(int statusCode) {
      var exception = new GraphiteServerException("Error", statusCode);

      assertThat(exception.isServerError()).isFalse();
    }
  }

  @Nested
  @DisplayName("inheritance")
  class Inheritance {

    @Test
    @DisplayName("should extend GraphiteException")
    void shouldExtendGraphiteException() {
      var exception = new GraphiteServerException("Test", 500);

      assertThat(exception).isInstanceOf(GraphiteException.class);
    }

    @Test
    @DisplayName("should be catchable as GraphiteException")
    void shouldBeCatchableAsGraphiteException() {
      GraphiteException exception = new GraphiteServerException("Test", 500);

      assertThat(exception).isNotNull();
    }
  }
}
