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
package io.github.graphite.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RateLimitConfig")
class RateLimitConfigTest {

  @Nested
  @DisplayName("constructor")
  class Constructor {

    @Test
    @DisplayName("should create config with valid parameters")
    void shouldCreateConfigWithValidParameters() {
      var config = new RateLimitConfig(100.0, 150);

      assertThat(config.requestsPerSecond()).isEqualTo(100.0);
      assertThat(config.burstCapacity()).isEqualTo(150);
    }

    @Test
    @DisplayName("should reject zero requestsPerSecond")
    void shouldRejectZeroRequestsPerSecond() {
      assertThatThrownBy(() -> new RateLimitConfig(0, 100))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("requestsPerSecond must be positive");
    }

    @Test
    @DisplayName("should reject negative requestsPerSecond")
    void shouldRejectNegativeRequestsPerSecond() {
      assertThatThrownBy(() -> new RateLimitConfig(-1, 100))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("requestsPerSecond must be positive");
    }

    @Test
    @DisplayName("should reject zero burstCapacity")
    void shouldRejectZeroBurstCapacity() {
      assertThatThrownBy(() -> new RateLimitConfig(100, 0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("burstCapacity must be positive");
    }

    @Test
    @DisplayName("should reject negative burstCapacity")
    void shouldRejectNegativeBurstCapacity() {
      assertThatThrownBy(() -> new RateLimitConfig(100, -1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("burstCapacity must be positive");
    }
  }

  @Nested
  @DisplayName("of with two parameters")
  class OfTwoParams {

    @Test
    @DisplayName("should create config with specified values")
    void shouldCreateConfigWithSpecifiedValues() {
      var config = RateLimitConfig.of(50.0, 75);

      assertThat(config.requestsPerSecond()).isEqualTo(50.0);
      assertThat(config.burstCapacity()).isEqualTo(75);
    }
  }

  @Nested
  @DisplayName("of with one parameter")
  class OfOneParam {

    @Test
    @DisplayName("should use rate as burst capacity")
    void shouldUseRateAsBurstCapacity() {
      var config = RateLimitConfig.of(100.0);

      assertThat(config.requestsPerSecond()).isEqualTo(100.0);
      assertThat(config.burstCapacity()).isEqualTo(100);
    }

    @Test
    @DisplayName("should ceil fractional rate for burst capacity")
    void shouldCeilFractionalRateForBurstCapacity() {
      var config = RateLimitConfig.of(50.5);

      assertThat(config.requestsPerSecond()).isEqualTo(50.5);
      assertThat(config.burstCapacity()).isEqualTo(51);
    }
  }

  @Nested
  @DisplayName("defaults")
  class Defaults {

    @Test
    @DisplayName("should return config with default values")
    void shouldReturnConfigWithDefaultValues() {
      var config = RateLimitConfig.defaults();

      assertThat(config.requestsPerSecond()).isEqualTo(100.0);
      assertThat(config.burstCapacity()).isEqualTo(150);
    }

    @Test
    @DisplayName("should have expected default constants")
    void shouldHaveExpectedDefaultConstants() {
      assertThat(RateLimitConfig.DEFAULT_REQUESTS_PER_SECOND).isEqualTo(100.0);
      assertThat(RateLimitConfig.DEFAULT_BURST_CAPACITY).isEqualTo(150);
    }
  }

  @Nested
  @DisplayName("unlimited")
  class Unlimited {

    @Test
    @DisplayName("should return config with maximum values")
    void shouldReturnConfigWithMaximumValues() {
      var config = RateLimitConfig.unlimited();

      assertThat(config.requestsPerSecond()).isEqualTo(Double.MAX_VALUE);
      assertThat(config.burstCapacity()).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    @DisplayName("should be unlimited")
    void shouldBeUnlimited() {
      var config = RateLimitConfig.unlimited();

      assertThat(config.isUnlimited()).isTrue();
    }
  }

  @Nested
  @DisplayName("createLimiter")
  class CreateLimiter {

    @Test
    @DisplayName("should create limiter with config values")
    void shouldCreateLimiterWithConfigValues() {
      var config = RateLimitConfig.of(200.0, 300);

      var limiter = config.createLimiter();

      assertThat(limiter.getRequestsPerSecond()).isEqualTo(200.0);
      assertThat(limiter.getBurstCapacity()).isEqualTo(300);
    }
  }

  @Nested
  @DisplayName("isUnlimited")
  class IsUnlimited {

    @Test
    @DisplayName("should return false for normal config")
    void shouldReturnFalseForNormalConfig() {
      var config = RateLimitConfig.of(100.0, 150);

      assertThat(config.isUnlimited()).isFalse();
    }

    @Test
    @DisplayName("should return true for very high rate")
    void shouldReturnTrueForVeryHighRate() {
      var config = new RateLimitConfig(Double.MAX_VALUE / 2, 1000);

      assertThat(config.isUnlimited()).isTrue();
    }

    @Test
    @DisplayName("should return false for high but limited rate")
    void shouldReturnFalseForHighButLimitedRate() {
      var config = new RateLimitConfig(1_000_000.0, 1000);

      assertThat(config.isUnlimited()).isFalse();
    }
  }

  @Nested
  @DisplayName("record behavior")
  class RecordBehavior {

    @Test
    @DisplayName("should be equal for same values")
    void shouldBeEqualForSameValues() {
      var config1 = RateLimitConfig.of(100.0, 150);
      var config2 = RateLimitConfig.of(100.0, 150);

      assertThat(config1).isEqualTo(config2);
      assertThat(config1.hashCode()).isEqualTo(config2.hashCode());
    }

    @Test
    @DisplayName("should not be equal for different values")
    void shouldNotBeEqualForDifferentValues() {
      var config1 = RateLimitConfig.of(100.0, 150);
      var config2 = RateLimitConfig.of(200.0, 150);

      assertThat(config1).isNotEqualTo(config2);
    }

    @Test
    @DisplayName("should have meaningful toString")
    void shouldHaveMeaningfulToString() {
      var config = RateLimitConfig.of(100.0, 150);

      assertThat(config.toString()).contains("100.0");
      assertThat(config.toString()).contains("150");
    }
  }
}
