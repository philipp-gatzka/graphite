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
package io.github.graphite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import io.github.graphite.ratelimit.RateLimiter;
import io.github.graphite.retry.RetryPolicy;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GraphiteConfiguration")
class GraphiteConfigurationTest {

  private static final URI TEST_ENDPOINT = URI.create("https://api.example.com/graphql");

  @Nested
  @DisplayName("constructor")
  class Constructor {

    @Test
    @DisplayName("should create configuration with all parameters")
    void shouldCreateConfigurationWithAllParameters() {
      var rateLimiter = RateLimiter.create(100);
      var retryPolicy = RetryPolicy.defaults();
      var config =
          new GraphiteConfiguration(
              TEST_ENDPOINT,
              Map.of("Authorization", "Bearer token"),
              Duration.ofSeconds(10),
              Duration.ofSeconds(30),
              Duration.ofSeconds(60),
              retryPolicy,
              rateLimiter);

      assertThat(config.endpoint()).isEqualTo(TEST_ENDPOINT);
      assertThat(config.headers()).containsEntry("Authorization", "Bearer token");
      assertThat(config.connectTimeout()).isEqualTo(Duration.ofSeconds(10));
      assertThat(config.readTimeout()).isEqualTo(Duration.ofSeconds(30));
      assertThat(config.requestTimeout()).isEqualTo(Duration.ofSeconds(60));
      assertThat(config.retryPolicy()).isSameAs(retryPolicy);
      assertThat(config.rateLimiter()).isEqualTo(rateLimiter);
    }

    @Test
    @DisplayName("should reject null endpoint")
    void shouldRejectNullEndpoint() {
      assertThatNullPointerException()
          .isThrownBy(
              () ->
                  new GraphiteConfiguration(
                      null,
                      Map.of(),
                      Duration.ofSeconds(10),
                      Duration.ofSeconds(30),
                      Duration.ofSeconds(60),
                      RetryPolicy.defaults(),
                      null))
          .withMessageContaining("endpoint must not be null");
    }

    @Test
    @DisplayName("should reject null headers")
    void shouldRejectNullHeaders() {
      assertThatNullPointerException()
          .isThrownBy(
              () ->
                  new GraphiteConfiguration(
                      TEST_ENDPOINT,
                      null,
                      Duration.ofSeconds(10),
                      Duration.ofSeconds(30),
                      Duration.ofSeconds(60),
                      RetryPolicy.defaults(),
                      null))
          .withMessageContaining("headers must not be null");
    }

    @Test
    @DisplayName("should reject null connectTimeout")
    void shouldRejectNullConnectTimeout() {
      assertThatNullPointerException()
          .isThrownBy(
              () ->
                  new GraphiteConfiguration(
                      TEST_ENDPOINT,
                      Map.of(),
                      null,
                      Duration.ofSeconds(30),
                      Duration.ofSeconds(60),
                      RetryPolicy.defaults(),
                      null))
          .withMessageContaining("connectTimeout must not be null");
    }

    @Test
    @DisplayName("should reject null readTimeout")
    void shouldRejectNullReadTimeout() {
      assertThatNullPointerException()
          .isThrownBy(
              () ->
                  new GraphiteConfiguration(
                      TEST_ENDPOINT,
                      Map.of(),
                      Duration.ofSeconds(10),
                      null,
                      Duration.ofSeconds(60),
                      RetryPolicy.defaults(),
                      null))
          .withMessageContaining("readTimeout must not be null");
    }

    @Test
    @DisplayName("should reject null requestTimeout")
    void shouldRejectNullRequestTimeout() {
      assertThatNullPointerException()
          .isThrownBy(
              () ->
                  new GraphiteConfiguration(
                      TEST_ENDPOINT,
                      Map.of(),
                      Duration.ofSeconds(10),
                      Duration.ofSeconds(30),
                      null,
                      RetryPolicy.defaults(),
                      null))
          .withMessageContaining("requestTimeout must not be null");
    }

    @Test
    @DisplayName("should reject null retryPolicy")
    void shouldRejectNullRetryPolicy() {
      assertThatNullPointerException()
          .isThrownBy(
              () ->
                  new GraphiteConfiguration(
                      TEST_ENDPOINT,
                      Map.of(),
                      Duration.ofSeconds(10),
                      Duration.ofSeconds(30),
                      Duration.ofSeconds(60),
                      null,
                      null))
          .withMessageContaining("retryPolicy must not be null");
    }

    @Test
    @DisplayName("should allow null rateLimiter")
    void shouldAllowNullRateLimiter() {
      var config =
          new GraphiteConfiguration(
              TEST_ENDPOINT,
              Map.of(),
              Duration.ofSeconds(10),
              Duration.ofSeconds(30),
              Duration.ofSeconds(60),
              RetryPolicy.defaults(),
              null);

      assertThat(config.rateLimiter()).isNull();
    }

    @Test
    @DisplayName("should reject zero connectTimeout")
    void shouldRejectZeroConnectTimeout() {
      assertThatIllegalArgumentException()
          .isThrownBy(
              () ->
                  new GraphiteConfiguration(
                      TEST_ENDPOINT,
                      Map.of(),
                      Duration.ZERO,
                      Duration.ofSeconds(30),
                      Duration.ofSeconds(60),
                      RetryPolicy.defaults(),
                      null))
          .withMessageContaining("connectTimeout must be positive");
    }

    @Test
    @DisplayName("should reject negative connectTimeout")
    void shouldRejectNegativeConnectTimeout() {
      assertThatIllegalArgumentException()
          .isThrownBy(
              () ->
                  new GraphiteConfiguration(
                      TEST_ENDPOINT,
                      Map.of(),
                      Duration.ofSeconds(-1),
                      Duration.ofSeconds(30),
                      Duration.ofSeconds(60),
                      RetryPolicy.defaults(),
                      null))
          .withMessageContaining("connectTimeout must be positive");
    }

    @Test
    @DisplayName("should reject zero readTimeout")
    void shouldRejectZeroReadTimeout() {
      assertThatIllegalArgumentException()
          .isThrownBy(
              () ->
                  new GraphiteConfiguration(
                      TEST_ENDPOINT,
                      Map.of(),
                      Duration.ofSeconds(10),
                      Duration.ZERO,
                      Duration.ofSeconds(60),
                      RetryPolicy.defaults(),
                      null))
          .withMessageContaining("readTimeout must be positive");
    }

    @Test
    @DisplayName("should reject negative readTimeout")
    void shouldRejectNegativeReadTimeout() {
      assertThatIllegalArgumentException()
          .isThrownBy(
              () ->
                  new GraphiteConfiguration(
                      TEST_ENDPOINT,
                      Map.of(),
                      Duration.ofSeconds(10),
                      Duration.ofSeconds(-1),
                      Duration.ofSeconds(60),
                      RetryPolicy.defaults(),
                      null))
          .withMessageContaining("readTimeout must be positive");
    }

    @Test
    @DisplayName("should reject zero requestTimeout")
    void shouldRejectZeroRequestTimeout() {
      assertThatIllegalArgumentException()
          .isThrownBy(
              () ->
                  new GraphiteConfiguration(
                      TEST_ENDPOINT,
                      Map.of(),
                      Duration.ofSeconds(10),
                      Duration.ofSeconds(30),
                      Duration.ZERO,
                      RetryPolicy.defaults(),
                      null))
          .withMessageContaining("requestTimeout must be positive");
    }

    @Test
    @DisplayName("should reject negative requestTimeout")
    void shouldRejectNegativeRequestTimeout() {
      assertThatIllegalArgumentException()
          .isThrownBy(
              () ->
                  new GraphiteConfiguration(
                      TEST_ENDPOINT,
                      Map.of(),
                      Duration.ofSeconds(10),
                      Duration.ofSeconds(30),
                      Duration.ofSeconds(-1),
                      RetryPolicy.defaults(),
                      null))
          .withMessageContaining("requestTimeout must be positive");
    }

    @Test
    @DisplayName("should make headers immutable")
    void shouldMakeHeadersImmutable() {
      var mutableHeaders = new java.util.HashMap<String, String>();
      mutableHeaders.put("Key", "Value");

      var config =
          new GraphiteConfiguration(
              TEST_ENDPOINT,
              mutableHeaders,
              Duration.ofSeconds(10),
              Duration.ofSeconds(30),
              Duration.ofSeconds(60),
              RetryPolicy.defaults(),
              null);

      assertThat(config.headers()).isUnmodifiable();
    }
  }

  @Nested
  @DisplayName("withEndpoint factory")
  class WithEndpointFactory {

    @Test
    @DisplayName("should create configuration with string endpoint")
    void shouldCreateConfigurationWithStringEndpoint() {
      var config = GraphiteConfiguration.withEndpoint("https://api.example.com/graphql");

      assertThat(config.endpoint()).isEqualTo(TEST_ENDPOINT);
      assertThat(config.headers()).isEmpty();
      assertThat(config.connectTimeout()).isEqualTo(GraphiteConfiguration.DEFAULT_CONNECT_TIMEOUT);
      assertThat(config.readTimeout()).isEqualTo(GraphiteConfiguration.DEFAULT_READ_TIMEOUT);
      assertThat(config.requestTimeout()).isEqualTo(GraphiteConfiguration.DEFAULT_REQUEST_TIMEOUT);
      assertThat(config.retryPolicy().maxAttempts()).isEqualTo(RetryPolicy.DEFAULT_MAX_ATTEMPTS);
      assertThat(config.rateLimiter()).isNull();
    }

    @Test
    @DisplayName("should create configuration with URI endpoint")
    void shouldCreateConfigurationWithURIEndpoint() {
      var config = GraphiteConfiguration.withEndpoint(TEST_ENDPOINT);

      assertThat(config.endpoint()).isEqualTo(TEST_ENDPOINT);
      assertThat(config.headers()).isEmpty();
    }
  }

  @Nested
  @DisplayName("hasRateLimiter")
  class HasRateLimiter {

    @Test
    @DisplayName("should return true when rate limiter is set")
    void shouldReturnTrueWhenRateLimiterSet() {
      var config =
          GraphiteConfiguration.withEndpoint(TEST_ENDPOINT)
              .withRateLimiter(RateLimiter.create(100));

      assertThat(config.hasRateLimiter()).isTrue();
    }

    @Test
    @DisplayName("should return false when rate limiter is null")
    void shouldReturnFalseWhenRateLimiterNull() {
      var config = GraphiteConfiguration.withEndpoint(TEST_ENDPOINT);

      assertThat(config.hasRateLimiter()).isFalse();
    }
  }

  @Nested
  @DisplayName("hasRetry")
  class HasRetry {

    @Test
    @DisplayName("should return true when retry policy has attempts")
    void shouldReturnTrueWhenRetryPolicyHasAttempts() {
      var config = GraphiteConfiguration.withEndpoint(TEST_ENDPOINT);

      assertThat(config.hasRetry()).isTrue();
    }

    @Test
    @DisplayName("should return false when retry policy has zero attempts")
    void shouldReturnFalseWhenRetryPolicyHasZeroAttempts() {
      var config =
          GraphiteConfiguration.withEndpoint(TEST_ENDPOINT).withRetryPolicy(RetryPolicy.disabled());

      assertThat(config.hasRetry()).isFalse();
    }
  }

  @Nested
  @DisplayName("withHeader")
  class WithHeader {

    @Test
    @DisplayName("should add header to new configuration")
    void shouldAddHeaderToNewConfiguration() {
      var original = GraphiteConfiguration.withEndpoint(TEST_ENDPOINT);
      var modified = original.withHeader("Authorization", "Bearer token");

      assertThat(modified.headers()).containsEntry("Authorization", "Bearer token");
      assertThat(original.headers()).isEmpty();
    }

    @Test
    @DisplayName("should preserve existing headers")
    void shouldPreserveExistingHeaders() {
      var original =
          GraphiteConfiguration.withEndpoint(TEST_ENDPOINT).withHeader("X-Custom", "value");
      var modified = original.withHeader("Authorization", "Bearer token");

      assertThat(modified.headers()).containsEntry("X-Custom", "value");
      assertThat(modified.headers()).containsEntry("Authorization", "Bearer token");
    }
  }

  @Nested
  @DisplayName("withConnectTimeout")
  class WithConnectTimeout {

    @Test
    @DisplayName("should create new configuration with timeout")
    void shouldCreateNewConfigurationWithTimeout() {
      var original = GraphiteConfiguration.withEndpoint(TEST_ENDPOINT);
      var modified = original.withConnectTimeout(Duration.ofSeconds(5));

      assertThat(modified.connectTimeout()).isEqualTo(Duration.ofSeconds(5));
      assertThat(original.connectTimeout())
          .isEqualTo(GraphiteConfiguration.DEFAULT_CONNECT_TIMEOUT);
    }
  }

  @Nested
  @DisplayName("withReadTimeout")
  class WithReadTimeout {

    @Test
    @DisplayName("should create new configuration with timeout")
    void shouldCreateNewConfigurationWithTimeout() {
      var original = GraphiteConfiguration.withEndpoint(TEST_ENDPOINT);
      var modified = original.withReadTimeout(Duration.ofSeconds(45));

      assertThat(modified.readTimeout()).isEqualTo(Duration.ofSeconds(45));
      assertThat(original.readTimeout()).isEqualTo(GraphiteConfiguration.DEFAULT_READ_TIMEOUT);
    }
  }

  @Nested
  @DisplayName("withRequestTimeout")
  class WithRequestTimeout {

    @Test
    @DisplayName("should create new configuration with timeout")
    void shouldCreateNewConfigurationWithTimeout() {
      var original = GraphiteConfiguration.withEndpoint(TEST_ENDPOINT);
      var modified = original.withRequestTimeout(Duration.ofSeconds(120));

      assertThat(modified.requestTimeout()).isEqualTo(Duration.ofSeconds(120));
      assertThat(original.requestTimeout())
          .isEqualTo(GraphiteConfiguration.DEFAULT_REQUEST_TIMEOUT);
    }
  }

  @Nested
  @DisplayName("withRetryPolicy")
  class WithRetryPolicy {

    @Test
    @DisplayName("should create new configuration with retry policy")
    void shouldCreateNewConfigurationWithRetryPolicy() {
      var original = GraphiteConfiguration.withEndpoint(TEST_ENDPOINT);
      var disabled = RetryPolicy.disabled();
      var modified = original.withRetryPolicy(disabled);

      assertThat(modified.retryPolicy()).isSameAs(disabled);
      assertThat(original.retryPolicy().maxAttempts()).isEqualTo(RetryPolicy.DEFAULT_MAX_ATTEMPTS);
    }
  }

  @Nested
  @DisplayName("withRateLimiter")
  class WithRateLimiter {

    @Test
    @DisplayName("should create new configuration with rate limiter")
    void shouldCreateNewConfigurationWithRateLimiter() {
      var rateLimiter = RateLimiter.create(100);
      var original = GraphiteConfiguration.withEndpoint(TEST_ENDPOINT);
      var modified = original.withRateLimiter(rateLimiter);

      assertThat(modified.rateLimiter()).isEqualTo(rateLimiter);
      assertThat(original.rateLimiter()).isNull();
    }

    @Test
    @DisplayName("should allow setting rate limiter to null")
    void shouldAllowSettingRateLimiterToNull() {
      var original =
          GraphiteConfiguration.withEndpoint(TEST_ENDPOINT)
              .withRateLimiter(RateLimiter.create(100));
      var modified = original.withRateLimiter(null);

      assertThat(modified.rateLimiter()).isNull();
    }
  }

  @Nested
  @DisplayName("default timeouts")
  class DefaultTimeouts {

    @Test
    @DisplayName("should have correct default connect timeout")
    void shouldHaveCorrectDefaultConnectTimeout() {
      assertThat(Duration.ofSeconds(10)).isEqualTo(GraphiteConfiguration.DEFAULT_CONNECT_TIMEOUT);
    }

    @Test
    @DisplayName("should have correct default read timeout")
    void shouldHaveCorrectDefaultReadTimeout() {
      assertThat(Duration.ofSeconds(30)).isEqualTo(GraphiteConfiguration.DEFAULT_READ_TIMEOUT);
    }

    @Test
    @DisplayName("should have correct default request timeout")
    void shouldHaveCorrectDefaultRequestTimeout() {
      assertThat(Duration.ofSeconds(60)).isEqualTo(GraphiteConfiguration.DEFAULT_REQUEST_TIMEOUT);
    }
  }

  @Nested
  @DisplayName("record behavior")
  class RecordBehavior {

    @Test
    @DisplayName("should be equal for same values")
    void shouldBeEqualForSameValues() {
      var retryPolicy = RetryPolicy.defaults();
      var config1 =
          new GraphiteConfiguration(
              TEST_ENDPOINT,
              Map.of(),
              Duration.ofSeconds(10),
              Duration.ofSeconds(30),
              Duration.ofSeconds(60),
              retryPolicy,
              null);
      var config2 =
          new GraphiteConfiguration(
              TEST_ENDPOINT,
              Map.of(),
              Duration.ofSeconds(10),
              Duration.ofSeconds(30),
              Duration.ofSeconds(60),
              retryPolicy,
              null);

      assertThat(config1).isEqualTo(config2).hasSameHashCodeAs(config2);
    }

    @Test
    @DisplayName("should not be equal for different endpoints")
    void shouldNotBeEqualForDifferentEndpoints() {
      var retryPolicy = RetryPolicy.defaults();
      var config1 =
          new GraphiteConfiguration(
              TEST_ENDPOINT,
              Map.of(),
              Duration.ofSeconds(10),
              Duration.ofSeconds(30),
              Duration.ofSeconds(60),
              retryPolicy,
              null);
      var config2 =
          new GraphiteConfiguration(
              URI.create("https://other.example.com/graphql"),
              Map.of(),
              Duration.ofSeconds(10),
              Duration.ofSeconds(30),
              Duration.ofSeconds(60),
              retryPolicy,
              null);

      assertThat(config1).isNotEqualTo(config2);
    }
  }
}
