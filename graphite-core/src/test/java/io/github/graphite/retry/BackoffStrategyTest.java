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
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

@DisplayName("BackoffStrategy")
class BackoffStrategyTest {

  @Nested
  @DisplayName("fixed")
  class Fixed {

    @Test
    @DisplayName("should return constant delay")
    void shouldReturnConstantDelay() {
      var delay = Duration.ofMillis(500);
      var strategy = BackoffStrategy.fixed(delay);

      assertThat(strategy.calculateDelay(1)).isEqualTo(delay);
      assertThat(strategy.calculateDelay(2)).isEqualTo(delay);
      assertThat(strategy.calculateDelay(5)).isEqualTo(delay);
      assertThat(strategy.calculateDelay(100)).isEqualTo(delay);
    }

    @Test
    @DisplayName("should reject null delay")
    void shouldRejectNullDelay() {
      assertThatNullPointerException()
          .isThrownBy(() -> BackoffStrategy.fixed(null))
          .withMessage("delay must not be null");
    }
  }

  @Nested
  @DisplayName("none")
  class None {

    @Test
    @DisplayName("should return zero delay")
    void shouldReturnZeroDelay() {
      var strategy = BackoffStrategy.none();

      assertThat(strategy.calculateDelay(1)).isEqualTo(Duration.ZERO);
      assertThat(strategy.calculateDelay(5)).isEqualTo(Duration.ZERO);
    }
  }

  @Nested
  @DisplayName("withJitter")
  class WithJitter {

    @Test
    @DisplayName("should return same strategy when jitter is zero")
    void shouldReturnSameStrategyWhenJitterIsZero() {
      var base = BackoffStrategy.fixed(Duration.ofSeconds(1));
      var withJitter = base.withJitter(0.0);

      assertThat(withJitter).isSameAs(base);
    }

    @Test
    @DisplayName("should reject negative jitter factor")
    void shouldRejectNegativeJitterFactor() {
      var base = BackoffStrategy.fixed(Duration.ofSeconds(1));

      assertThatThrownBy(() -> base.withJitter(-0.1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("jitterFactor must be between 0.0 and 1.0");
    }

    @Test
    @DisplayName("should reject jitter factor greater than 1")
    void shouldRejectJitterFactorGreaterThanOne() {
      var base = BackoffStrategy.fixed(Duration.ofSeconds(1));

      assertThatThrownBy(() -> base.withJitter(1.1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("jitterFactor must be between 0.0 and 1.0");
    }

    @RepeatedTest(10)
    @DisplayName("should apply jitter within bounds")
    void shouldApplyJitterWithinBounds() {
      var baseDelay = Duration.ofSeconds(1);
      var jitterFactor = 0.25;
      var strategy = BackoffStrategy.fixed(baseDelay).withJitter(jitterFactor);

      var delay = strategy.calculateDelay(1);

      // Delay should be between 75% and 100% of base delay
      long minMillis = (long) (baseDelay.toMillis() * (1.0 - jitterFactor));
      long maxMillis = baseDelay.toMillis();

      assertThat(delay.toMillis()).isGreaterThanOrEqualTo(minMillis).isLessThanOrEqualTo(maxMillis);
    }
  }

  @Nested
  @DisplayName("isAtMaxDelay")
  class IsAtMaxDelay {

    @Test
    @DisplayName("should return false by default")
    void shouldReturnFalseByDefault() {
      var strategy = BackoffStrategy.fixed(Duration.ofSeconds(1));

      assertThat(strategy.isAtMaxDelay(1)).isFalse();
      assertThat(strategy.isAtMaxDelay(100)).isFalse();
    }
  }

  @Nested
  @DisplayName("custom implementation")
  class CustomImplementation {

    @Test
    @DisplayName("should support lambda implementation")
    void shouldSupportLambdaImplementation() {
      BackoffStrategy linear = attempt -> Duration.ofMillis(attempt * 100L);

      assertThat(linear.calculateDelay(1)).isEqualTo(Duration.ofMillis(100));
      assertThat(linear.calculateDelay(2)).isEqualTo(Duration.ofMillis(200));
      assertThat(linear.calculateDelay(5)).isEqualTo(Duration.ofMillis(500));
    }
  }
}
