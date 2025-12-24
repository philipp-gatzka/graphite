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

import java.net.SocketTimeoutException;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

@DisplayName("GraphiteTimeoutException")
class GraphiteTimeoutExceptionTest {

  @Nested
  @DisplayName("constructor")
  class Constructor {

    @Test
    @DisplayName("should create exception with message and timeout type")
    void shouldCreateWithMessageAndTimeoutType() {
      var exception = new GraphiteTimeoutException("Request timed out", TimeoutType.REQUEST);

      assertThat(exception.getMessage()).isEqualTo("Request timed out");
      assertThat(exception.getTimeoutType()).isEqualTo(TimeoutType.REQUEST);
      assertThat(exception.getCause()).isNull();
      assertThat(exception.getConfiguredTimeout()).isNull();
      assertThat(exception.getElapsedTime()).isNull();
    }

    @Test
    @DisplayName("should create exception with message, type, and cause")
    void shouldCreateWithMessageTypeAndCause() {
      var cause = new SocketTimeoutException("Read timed out");
      var exception = new GraphiteTimeoutException("Read timed out", TimeoutType.READ, cause);

      assertThat(exception.getMessage()).isEqualTo("Read timed out");
      assertThat(exception.getTimeoutType()).isEqualTo(TimeoutType.READ);
      assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("should create exception with all parameters")
    void shouldCreateWithAllParameters() {
      var cause = new SocketTimeoutException("Connection timed out");
      var configuredTimeout = Duration.ofSeconds(10);
      var elapsedTime = Duration.ofMillis(10023);

      var exception =
          new GraphiteTimeoutException(
              "Connection timed out", TimeoutType.CONNECT, configuredTimeout, elapsedTime, cause);

      assertThat(exception.getMessage()).isEqualTo("Connection timed out");
      assertThat(exception.getTimeoutType()).isEqualTo(TimeoutType.CONNECT);
      assertThat(exception.getConfiguredTimeout()).isEqualTo(configuredTimeout);
      assertThat(exception.getElapsedTime()).isEqualTo(elapsedTime);
      assertThat(exception.getCause()).isSameAs(cause);
    }
  }

  @Nested
  @DisplayName("errorCode")
  class ErrorCode {

    @ParameterizedTest
    @EnumSource(TimeoutType.class)
    @DisplayName("should have error code based on timeout type")
    void shouldHaveErrorCodeBasedOnTimeoutType(TimeoutType type) {
      var exception = new GraphiteTimeoutException("Timeout", type);

      assertThat(exception.getErrorCode()).isEqualTo("TIMEOUT_" + type.name());
    }
  }

  @Nested
  @DisplayName("isSafeToRetry")
  class IsSafeToRetry {

    @Test
    @DisplayName("should return true for CONNECT timeout")
    void shouldReturnTrueForConnectTimeout() {
      var exception = new GraphiteTimeoutException("Timeout", TimeoutType.CONNECT);

      assertThat(exception.isSafeToRetry()).isTrue();
    }

    @Test
    @DisplayName("should return false for READ timeout")
    void shouldReturnFalseForReadTimeout() {
      var exception = new GraphiteTimeoutException("Timeout", TimeoutType.READ);

      assertThat(exception.isSafeToRetry()).isFalse();
    }

    @Test
    @DisplayName("should return false for REQUEST timeout")
    void shouldReturnFalseForRequestTimeout() {
      var exception = new GraphiteTimeoutException("Timeout", TimeoutType.REQUEST);

      assertThat(exception.isSafeToRetry()).isFalse();
    }
  }

  @Nested
  @DisplayName("inheritance")
  class Inheritance {

    @Test
    @DisplayName("should extend GraphiteClientException")
    void shouldExtendGraphiteClientException() {
      var exception = new GraphiteTimeoutException("Test", TimeoutType.CONNECT);

      assertThat(exception).isInstanceOf(GraphiteClientException.class);
    }

    @Test
    @DisplayName("should be catchable as GraphiteException")
    void shouldBeCatchableAsGraphiteException() {
      GraphiteException exception = new GraphiteTimeoutException("Test", TimeoutType.CONNECT);

      assertThat(exception).isNotNull();
    }
  }
}
