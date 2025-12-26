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
package io.github.graphite.http;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("HttpTransportConfig")
class HttpTransportConfigTest {

  @Nested
  @DisplayName("defaults")
  class Defaults {

    @Test
    @DisplayName("should return default timeouts")
    void shouldReturnDefaultTimeouts() {
      var config = HttpTransportConfig.defaults();

      assertThat(config.connectTimeout()).isEqualTo(Duration.ofSeconds(10));
      assertThat(config.readTimeout()).isEqualTo(Duration.ofSeconds(30));
      assertThat(config.requestTimeout()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("should have expected default constants")
    void shouldHaveExpectedDefaultConstants() {
      assertThat(Duration.ofSeconds(10)).isEqualTo(HttpTransportConfig.DEFAULT_CONNECT_TIMEOUT);
      assertThat(Duration.ofSeconds(30)).isEqualTo(HttpTransportConfig.DEFAULT_READ_TIMEOUT);
      assertThat(Duration.ofSeconds(60)).isEqualTo(HttpTransportConfig.DEFAULT_REQUEST_TIMEOUT);
    }
  }

  @Nested
  @DisplayName("constructor")
  class Constructor {

    @Test
    @DisplayName("should create config with specified timeouts")
    void shouldCreateConfigWithSpecifiedTimeouts() {
      var connectTimeout = Duration.ofSeconds(5);
      var readTimeout = Duration.ofSeconds(15);
      var requestTimeout = Duration.ofSeconds(30);

      var config = new HttpTransportConfig(connectTimeout, readTimeout, requestTimeout);

      assertThat(config.connectTimeout()).isEqualTo(connectTimeout);
      assertThat(config.readTimeout()).isEqualTo(readTimeout);
      assertThat(config.requestTimeout()).isEqualTo(requestTimeout);
    }

    @Test
    @DisplayName("should allow zero duration timeouts")
    void shouldAllowZeroDurationTimeouts() {
      var config = new HttpTransportConfig(Duration.ZERO, Duration.ZERO, Duration.ZERO);

      assertThat(config.connectTimeout()).isEqualTo(Duration.ZERO);
      assertThat(config.readTimeout()).isEqualTo(Duration.ZERO);
      assertThat(config.requestTimeout()).isEqualTo(Duration.ZERO);
    }

    @Test
    @DisplayName("should reject null connectTimeout")
    void shouldRejectNullConnectTimeout() {
      assertThatNullPointerException()
          .isThrownBy(
              () -> new HttpTransportConfig(null, Duration.ofSeconds(30), Duration.ofSeconds(60)))
          .withMessage("connectTimeout must not be null");
    }

    @Test
    @DisplayName("should reject null readTimeout")
    void shouldRejectNullReadTimeout() {
      assertThatNullPointerException()
          .isThrownBy(
              () -> new HttpTransportConfig(Duration.ofSeconds(10), null, Duration.ofSeconds(60)))
          .withMessage("readTimeout must not be null");
    }

    @Test
    @DisplayName("should reject null requestTimeout")
    void shouldRejectNullRequestTimeout() {
      assertThatNullPointerException()
          .isThrownBy(
              () -> new HttpTransportConfig(Duration.ofSeconds(10), Duration.ofSeconds(30), null))
          .withMessage("requestTimeout must not be null");
    }

    @Test
    @DisplayName("should reject negative connectTimeout")
    void shouldRejectNegativeConnectTimeout() {
      assertThatThrownBy(
              () ->
                  new HttpTransportConfig(
                      Duration.ofSeconds(-1), Duration.ofSeconds(30), Duration.ofSeconds(60)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("connectTimeout must not be negative");
    }

    @Test
    @DisplayName("should reject negative readTimeout")
    void shouldRejectNegativeReadTimeout() {
      assertThatThrownBy(
              () ->
                  new HttpTransportConfig(
                      Duration.ofSeconds(10), Duration.ofSeconds(-1), Duration.ofSeconds(60)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("readTimeout must not be negative");
    }

    @Test
    @DisplayName("should reject negative requestTimeout")
    void shouldRejectNegativeRequestTimeout() {
      assertThatThrownBy(
              () ->
                  new HttpTransportConfig(
                      Duration.ofSeconds(10), Duration.ofSeconds(30), Duration.ofSeconds(-1)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("requestTimeout must not be negative");
    }
  }

  @Nested
  @DisplayName("builder")
  class Builder {

    @Test
    @DisplayName("should create config with default values when nothing set")
    void shouldCreateConfigWithDefaultValuesWhenNothingSet() {
      var config = HttpTransportConfig.builder().build();

      assertThat(config.connectTimeout()).isEqualTo(HttpTransportConfig.DEFAULT_CONNECT_TIMEOUT);
      assertThat(config.readTimeout()).isEqualTo(HttpTransportConfig.DEFAULT_READ_TIMEOUT);
      assertThat(config.requestTimeout()).isEqualTo(HttpTransportConfig.DEFAULT_REQUEST_TIMEOUT);
    }

    @Test
    @DisplayName("should set connectTimeout")
    void shouldSetConnectTimeout() {
      var timeout = Duration.ofSeconds(5);
      var config = HttpTransportConfig.builder().connectTimeout(timeout).build();

      assertThat(config.connectTimeout()).isEqualTo(timeout);
    }

    @Test
    @DisplayName("should set readTimeout")
    void shouldSetReadTimeout() {
      var timeout = Duration.ofSeconds(15);
      var config = HttpTransportConfig.builder().readTimeout(timeout).build();

      assertThat(config.readTimeout()).isEqualTo(timeout);
    }

    @Test
    @DisplayName("should set requestTimeout")
    void shouldSetRequestTimeout() {
      var timeout = Duration.ofSeconds(45);
      var config = HttpTransportConfig.builder().requestTimeout(timeout).build();

      assertThat(config.requestTimeout()).isEqualTo(timeout);
    }

    @Test
    @DisplayName("should set all timeouts")
    void shouldSetAllTimeouts() {
      var config =
          HttpTransportConfig.builder()
              .connectTimeout(Duration.ofSeconds(5))
              .readTimeout(Duration.ofSeconds(15))
              .requestTimeout(Duration.ofSeconds(45))
              .build();

      assertThat(config.connectTimeout()).isEqualTo(Duration.ofSeconds(5));
      assertThat(config.readTimeout()).isEqualTo(Duration.ofSeconds(15));
      assertThat(config.requestTimeout()).isEqualTo(Duration.ofSeconds(45));
    }

    @Test
    @DisplayName("should reject null connectTimeout in builder")
    void shouldRejectNullConnectTimeoutInBuilder() {
      assertThatNullPointerException()
          .isThrownBy(() -> HttpTransportConfig.builder().connectTimeout(null))
          .withMessage("connectTimeout must not be null");
    }

    @Test
    @DisplayName("should reject null readTimeout in builder")
    void shouldRejectNullReadTimeoutInBuilder() {
      assertThatNullPointerException()
          .isThrownBy(() -> HttpTransportConfig.builder().readTimeout(null))
          .withMessage("readTimeout must not be null");
    }

    @Test
    @DisplayName("should reject null requestTimeout in builder")
    void shouldRejectNullRequestTimeoutInBuilder() {
      assertThatNullPointerException()
          .isThrownBy(() -> HttpTransportConfig.builder().requestTimeout(null))
          .withMessage("requestTimeout must not be null");
    }

    @Test
    @DisplayName("should reject negative timeout in builder")
    void shouldRejectNegativeTimeoutInBuilder() {
      var builder = HttpTransportConfig.builder().connectTimeout(Duration.ofSeconds(-1));

      assertThatThrownBy(builder::build)
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("connectTimeout must not be negative");
    }

    @Test
    @DisplayName("should allow method chaining")
    void shouldAllowMethodChaining() {
      var builder = HttpTransportConfig.builder();

      assertThat(builder.connectTimeout(Duration.ofSeconds(1))).isSameAs(builder);
      assertThat(builder.readTimeout(Duration.ofSeconds(1))).isSameAs(builder);
      assertThat(builder.requestTimeout(Duration.ofSeconds(1))).isSameAs(builder);
    }
  }
}
