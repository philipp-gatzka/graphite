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
package io.github.graphite.spring.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GraphiteProperties")
class GraphitePropertiesTest {

  @Nested
  @DisplayName("defaults")
  class Defaults {

    private final GraphiteProperties properties = new GraphiteProperties();

    @Test
    @DisplayName("should have null url by default")
    void shouldHaveNullUrlByDefault() {
      assertThat(properties.getUrl()).isNull();
    }

    @Test
    @DisplayName("should have empty headers by default")
    void shouldHaveEmptyHeadersByDefault() {
      assertThat(properties.getHeaders()).isEmpty();
    }

    @Test
    @DisplayName("should be enabled by default")
    void shouldBeEnabledByDefault() {
      assertThat(properties.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("should have default timeout values")
    void shouldHaveDefaultTimeoutValues() {
      GraphiteProperties.Timeout timeout = properties.getTimeout();

      assertThat(timeout.getConnect()).isEqualTo(Duration.ofSeconds(10));
      assertThat(timeout.getRead()).isEqualTo(Duration.ofSeconds(30));
      assertThat(timeout.getRequest()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("should have default retry values")
    void shouldHaveDefaultRetryValues() {
      GraphiteProperties.Retry retry = properties.getRetry();

      assertThat(retry.getMaxAttempts()).isEqualTo(3);
      assertThat(retry.getInitialDelay()).isEqualTo(Duration.ofMillis(100));
      assertThat(retry.getMultiplier()).isEqualTo(2.0);
      assertThat(retry.getMaxDelay()).isEqualTo(Duration.ofSeconds(5));
      assertThat(retry.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("should have default rate limit values")
    void shouldHaveDefaultRateLimitValues() {
      GraphiteProperties.RateLimit rateLimit = properties.getRateLimit();

      assertThat(rateLimit.getRequestsPerSecond()).isEqualTo(100.0);
      assertThat(rateLimit.getBurstCapacity()).isEqualTo(150);
      assertThat(rateLimit.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("should have default connection pool values")
    void shouldHaveDefaultConnectionPoolValues() {
      GraphiteProperties.ConnectionPool pool = properties.getConnectionPool();

      assertThat(pool.getMaxConnections()).isEqualTo(50);
      assertThat(pool.getIdleTimeout()).isEqualTo(Duration.ofSeconds(30));
    }
  }

  @Nested
  @DisplayName("setters")
  class Setters {

    @Test
    @DisplayName("should set url")
    void shouldSetUrl() {
      GraphiteProperties properties = new GraphiteProperties();
      properties.setUrl("https://api.example.com/graphql");

      assertThat(properties.getUrl()).isEqualTo("https://api.example.com/graphql");
    }

    @Test
    @DisplayName("should set headers")
    void shouldSetHeaders() {
      GraphiteProperties properties = new GraphiteProperties();
      properties.setHeaders(Map.of("Authorization", "Bearer token"));

      assertThat(properties.getHeaders()).containsEntry("Authorization", "Bearer token");
    }

    @Test
    @DisplayName("should set enabled")
    void shouldSetEnabled() {
      GraphiteProperties properties = new GraphiteProperties();
      properties.setEnabled(false);

      assertThat(properties.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("should set timeout values")
    void shouldSetTimeoutValues() {
      GraphiteProperties.Timeout timeout = new GraphiteProperties.Timeout();
      timeout.setConnect(Duration.ofSeconds(5));
      timeout.setRead(Duration.ofSeconds(15));
      timeout.setRequest(Duration.ofSeconds(30));

      assertThat(timeout.getConnect()).isEqualTo(Duration.ofSeconds(5));
      assertThat(timeout.getRead()).isEqualTo(Duration.ofSeconds(15));
      assertThat(timeout.getRequest()).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    @DisplayName("should set retry values")
    void shouldSetRetryValues() {
      GraphiteProperties.Retry retry = new GraphiteProperties.Retry();
      retry.setMaxAttempts(5);
      retry.setInitialDelay(Duration.ofMillis(200));
      retry.setMultiplier(1.5);
      retry.setMaxDelay(Duration.ofSeconds(10));
      retry.setEnabled(false);

      assertThat(retry.getMaxAttempts()).isEqualTo(5);
      assertThat(retry.getInitialDelay()).isEqualTo(Duration.ofMillis(200));
      assertThat(retry.getMultiplier()).isEqualTo(1.5);
      assertThat(retry.getMaxDelay()).isEqualTo(Duration.ofSeconds(10));
      assertThat(retry.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("should set rate limit values")
    void shouldSetRateLimitValues() {
      GraphiteProperties.RateLimit rateLimit = new GraphiteProperties.RateLimit();
      rateLimit.setRequestsPerSecond(50.0);
      rateLimit.setBurstCapacity(75);
      rateLimit.setEnabled(true);

      assertThat(rateLimit.getRequestsPerSecond()).isEqualTo(50.0);
      assertThat(rateLimit.getBurstCapacity()).isEqualTo(75);
      assertThat(rateLimit.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("should set connection pool values")
    void shouldSetConnectionPoolValues() {
      GraphiteProperties.ConnectionPool pool = new GraphiteProperties.ConnectionPool();
      pool.setMaxConnections(100);
      pool.setIdleTimeout(Duration.ofMinutes(1));

      assertThat(pool.getMaxConnections()).isEqualTo(100);
      assertThat(pool.getIdleTimeout()).isEqualTo(Duration.ofMinutes(1));
    }
  }
}
