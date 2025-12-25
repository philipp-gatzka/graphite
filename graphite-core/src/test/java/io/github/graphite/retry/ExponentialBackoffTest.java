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
package io.github.graphite.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ExponentialBackoff")
class ExponentialBackoffTest {

  @Nested
  @DisplayName("defaults")
  class Defaults {

    @Test
    @DisplayName("should have expected default values")
    void shouldHaveExpectedDefaultValues() {
      var backoff = ExponentialBackoff.defaults();

      assertThat(backoff.getInitialDelay()).isEqualTo(Duration.ofMillis(100));
      assertThat(backoff.getMaxDelay()).isEqualTo(Duration.ofSeconds(30));
      assertThat(backoff.getMultiplier()).isEqualTo(2.0);
    }

    @Test
    @DisplayName("should have expected default constants")
    void shouldHaveExpectedDefaultConstants() {
      assertThat(ExponentialBackoff.DEFAULT_INITIAL_DELAY).isEqualTo(Duration.ofMillis(100));
      assertThat(ExponentialBackoff.DEFAULT_MAX_DELAY).isEqualTo(Duration.ofSeconds(30));
      assertThat(ExponentialBackoff.DEFAULT_MULTIPLIER).isEqualTo(2.0);
    }
  }

  @Nested
  @DisplayName("calculateDelay")
  class CalculateDelay {

    @Test
    @DisplayName("should return initial delay for first attempt")
    void shouldReturnInitialDelayForFirstAttempt() {
      var backoff = ExponentialBackoff.defaults();

      assertThat(backoff.calculateDelay(1)).isEqualTo(Duration.ofMillis(100));
    }

    @Test
    @DisplayName("should grow exponentially")
    void shouldGrowExponentially() {
      var backoff =
          ExponentialBackoff.builder()
              .initialDelay(Duration.ofMillis(100))
              .maxDelay(Duration.ofSeconds(60))
              .multiplier(2.0)
              .build();

      assertThat(backoff.calculateDelay(1)).isEqualTo(Duration.ofMillis(100));
      assertThat(backoff.calculateDelay(2)).isEqualTo(Duration.ofMillis(200));
      assertThat(backoff.calculateDelay(3)).isEqualTo(Duration.ofMillis(400));
      assertThat(backoff.calculateDelay(4)).isEqualTo(Duration.ofMillis(800));
      assertThat(backoff.calculateDelay(5)).isEqualTo(Duration.ofMillis(1600));
    }

    @Test
    @DisplayName("should cap at max delay")
    void shouldCapAtMaxDelay() {
      var backoff =
          ExponentialBackoff.builder()
              .initialDelay(Duration.ofMillis(100))
              .maxDelay(Duration.ofMillis(500))
              .multiplier(2.0)
              .build();

      assertThat(backoff.calculateDelay(1)).isEqualTo(Duration.ofMillis(100));
      assertThat(backoff.calculateDelay(2)).isEqualTo(Duration.ofMillis(200));
      assertThat(backoff.calculateDelay(3)).isEqualTo(Duration.ofMillis(400));
      assertThat(backoff.calculateDelay(4)).isEqualTo(Duration.ofMillis(500)); // capped
      assertThat(backoff.calculateDelay(5)).isEqualTo(Duration.ofMillis(500)); // still capped
      assertThat(backoff.calculateDelay(100)).isEqualTo(Duration.ofMillis(500)); // still capped
    }

    @Test
    @DisplayName("should work with different multipliers")
    void shouldWorkWithDifferentMultipliers() {
      var backoff =
          ExponentialBackoff.builder()
              .initialDelay(Duration.ofMillis(100))
              .maxDelay(Duration.ofSeconds(60))
              .multiplier(3.0)
              .build();

      assertThat(backoff.calculateDelay(1)).isEqualTo(Duration.ofMillis(100));
      assertThat(backoff.calculateDelay(2)).isEqualTo(Duration.ofMillis(300));
      assertThat(backoff.calculateDelay(3)).isEqualTo(Duration.ofMillis(900));
      assertThat(backoff.calculateDelay(4)).isEqualTo(Duration.ofMillis(2700));
    }

    @Test
    @DisplayName("should reject attempt less than 1")
    void shouldRejectAttemptLessThan1() {
      var backoff = ExponentialBackoff.defaults();

      assertThatThrownBy(() -> backoff.calculateDelay(0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("attempt must be at least 1");

      assertThatThrownBy(() -> backoff.calculateDelay(-1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("attempt must be at least 1");
    }
  }

  @Nested
  @DisplayName("isAtMaxDelay")
  class IsAtMaxDelay {

    @Test
    @DisplayName("should return false when not at max")
    void shouldReturnFalseWhenNotAtMax() {
      var backoff =
          ExponentialBackoff.builder()
              .initialDelay(Duration.ofMillis(100))
              .maxDelay(Duration.ofMillis(500))
              .build();

      assertThat(backoff.isAtMaxDelay(1)).isFalse();
      assertThat(backoff.isAtMaxDelay(2)).isFalse();
      assertThat(backoff.isAtMaxDelay(3)).isFalse();
    }

    @Test
    @DisplayName("should return true when at max")
    void shouldReturnTrueWhenAtMax() {
      var backoff =
          ExponentialBackoff.builder()
              .initialDelay(Duration.ofMillis(100))
              .maxDelay(Duration.ofMillis(500))
              .build();

      assertThat(backoff.isAtMaxDelay(4)).isTrue();
      assertThat(backoff.isAtMaxDelay(10)).isTrue();
    }
  }

  @Nested
  @DisplayName("builder")
  class Builder {

    @Test
    @DisplayName("should create with default values")
    void shouldCreateWithDefaultValues() {
      var backoff = ExponentialBackoff.builder().build();

      assertThat(backoff.getInitialDelay()).isEqualTo(ExponentialBackoff.DEFAULT_INITIAL_DELAY);
      assertThat(backoff.getMaxDelay()).isEqualTo(ExponentialBackoff.DEFAULT_MAX_DELAY);
      assertThat(backoff.getMultiplier()).isEqualTo(ExponentialBackoff.DEFAULT_MULTIPLIER);
    }

    @Test
    @DisplayName("should set initial delay")
    void shouldSetInitialDelay() {
      var backoff = ExponentialBackoff.builder().initialDelay(Duration.ofMillis(50)).build();

      assertThat(backoff.getInitialDelay()).isEqualTo(Duration.ofMillis(50));
    }

    @Test
    @DisplayName("should reject null initial delay")
    void shouldRejectNullInitialDelay() {
      assertThatNullPointerException()
          .isThrownBy(() -> ExponentialBackoff.builder().initialDelay(null))
          .withMessage("initialDelay must not be null");
    }

    @Test
    @DisplayName("should reject zero initial delay")
    void shouldRejectZeroInitialDelay() {
      var builder = ExponentialBackoff.builder();
      assertThatThrownBy(() -> builder.initialDelay(Duration.ZERO))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("initialDelay must be positive");
    }

    @Test
    @DisplayName("should reject negative initial delay")
    void shouldRejectNegativeInitialDelay() {
      var builder = ExponentialBackoff.builder();
      Duration negativeDelay = Duration.ofMillis(-1);
      assertThatThrownBy(() -> builder.initialDelay(negativeDelay))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("initialDelay must be positive");
    }

    @Test
    @DisplayName("should set max delay")
    void shouldSetMaxDelay() {
      var backoff = ExponentialBackoff.builder().maxDelay(Duration.ofSeconds(60)).build();

      assertThat(backoff.getMaxDelay()).isEqualTo(Duration.ofSeconds(60));
    }

    @Test
    @DisplayName("should reject null max delay")
    void shouldRejectNullMaxDelay() {
      assertThatNullPointerException()
          .isThrownBy(() -> ExponentialBackoff.builder().maxDelay(null))
          .withMessage("maxDelay must not be null");
    }

    @Test
    @DisplayName("should reject zero max delay")
    void shouldRejectZeroMaxDelay() {
      var builder = ExponentialBackoff.builder();
      assertThatThrownBy(() -> builder.maxDelay(Duration.ZERO))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("maxDelay must be positive");
    }

    @Test
    @DisplayName("should reject negative max delay")
    void shouldRejectNegativeMaxDelay() {
      var builder = ExponentialBackoff.builder();
      Duration negativeDelay = Duration.ofMillis(-1);
      assertThatThrownBy(() -> builder.maxDelay(negativeDelay))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("maxDelay must be positive");
    }

    @Test
    @DisplayName("should set multiplier")
    void shouldSetMultiplier() {
      var backoff = ExponentialBackoff.builder().multiplier(1.5).build();

      assertThat(backoff.getMultiplier()).isEqualTo(1.5);
    }

    @Test
    @DisplayName("should reject multiplier of 1.0")
    void shouldRejectMultiplierOf1() {
      var builder = ExponentialBackoff.builder();
      assertThatThrownBy(() -> builder.multiplier(1.0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("multiplier must be greater than 1.0");
    }

    @Test
    @DisplayName("should reject multiplier less than 1.0")
    void shouldRejectMultiplierLessThan1() {
      var builder = ExponentialBackoff.builder();
      assertThatThrownBy(() -> builder.multiplier(0.5))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("multiplier must be greater than 1.0");
    }

    @Test
    @DisplayName("should reject initial delay greater than max delay")
    void shouldRejectInitialDelayGreaterThanMaxDelay() {
      var builder =
          ExponentialBackoff.builder()
              .initialDelay(Duration.ofSeconds(10))
              .maxDelay(Duration.ofSeconds(5));

      assertThatThrownBy(builder::build)
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("initialDelay must not be greater than maxDelay");
    }

    @Test
    @DisplayName("should allow method chaining")
    void shouldAllowMethodChaining() {
      var builder = ExponentialBackoff.builder();

      assertThat(builder.initialDelay(Duration.ofMillis(100))).isSameAs(builder);
      assertThat(builder.maxDelay(Duration.ofSeconds(30))).isSameAs(builder);
      assertThat(builder.multiplier(2.0)).isSameAs(builder);
    }
  }

  @Nested
  @DisplayName("implements BackoffStrategy")
  class ImplementsBackoffStrategy {

    @Test
    @DisplayName("should be usable as BackoffStrategy")
    void shouldBeUsableAsBackoffStrategy() {
      BackoffStrategy strategy = ExponentialBackoff.defaults();

      assertThat(strategy.calculateDelay(1)).isEqualTo(Duration.ofMillis(100));
    }

    @Test
    @DisplayName("should work with withJitter")
    void shouldWorkWithWithJitter() {
      var backoff = ExponentialBackoff.defaults();
      var withJitter = backoff.withJitter(0.1);

      // Should still return reasonable values
      var delay = withJitter.calculateDelay(1);
      assertThat(delay.toMillis()).isLessThanOrEqualTo(100);
      assertThat(delay.toMillis()).isGreaterThanOrEqualTo(90);
    }
  }
}
