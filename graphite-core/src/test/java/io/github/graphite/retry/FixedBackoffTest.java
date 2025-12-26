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

@DisplayName("FixedBackoff")
class FixedBackoffTest {

  @Nested
  @DisplayName("of")
  class Of {

    @Test
    @DisplayName("should create backoff with specified delay")
    void shouldCreateBackoffWithSpecifiedDelay() {
      var backoff = FixedBackoff.of(Duration.ofMillis(500));

      assertThat(backoff.getDelay()).isEqualTo(Duration.ofMillis(500));
    }

    @Test
    @DisplayName("should allow zero delay")
    void shouldAllowZeroDelay() {
      var backoff = FixedBackoff.of(Duration.ZERO);

      assertThat(backoff.getDelay()).isEqualTo(Duration.ZERO);
    }

    @Test
    @DisplayName("should reject null delay")
    void shouldRejectNullDelay() {
      assertThatNullPointerException()
          .isThrownBy(() -> FixedBackoff.of(null))
          .withMessage("delay must not be null");
    }

    @Test
    @DisplayName("should reject negative delay")
    void shouldRejectNegativeDelay() {
      Duration negativeDelay = Duration.ofMillis(-1);
      assertThatThrownBy(() -> FixedBackoff.of(negativeDelay))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("delay must not be negative");
    }
  }

  @Nested
  @DisplayName("defaults")
  class Defaults {

    @Test
    @DisplayName("should return backoff with default delay")
    void shouldReturnBackoffWithDefaultDelay() {
      var backoff = FixedBackoff.defaults();

      assertThat(backoff.getDelay()).isEqualTo(Duration.ofSeconds(1));
    }

    @Test
    @DisplayName("should have expected default constant")
    void shouldHaveExpectedDefaultConstant() {
      assertThat(Duration.ofSeconds(1)).isEqualTo(FixedBackoff.DEFAULT_DELAY);
    }
  }

  @Nested
  @DisplayName("noDelay")
  class NoDelay {

    @Test
    @DisplayName("should return backoff with zero delay")
    void shouldReturnBackoffWithZeroDelay() {
      var backoff = FixedBackoff.noDelay();

      assertThat(backoff.getDelay()).isEqualTo(Duration.ZERO);
    }
  }

  @Nested
  @DisplayName("calculateDelay")
  class CalculateDelay {

    @Test
    @DisplayName("should return same delay for all attempts")
    void shouldReturnSameDelayForAllAttempts() {
      var backoff = FixedBackoff.of(Duration.ofMillis(250));

      assertThat(backoff.calculateDelay(1)).isEqualTo(Duration.ofMillis(250));
      assertThat(backoff.calculateDelay(2)).isEqualTo(Duration.ofMillis(250));
      assertThat(backoff.calculateDelay(5)).isEqualTo(Duration.ofMillis(250));
      assertThat(backoff.calculateDelay(100)).isEqualTo(Duration.ofMillis(250));
    }

    @Test
    @DisplayName("should reject attempt less than 1")
    void shouldRejectAttemptLessThan1() {
      var backoff = FixedBackoff.defaults();

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
    @DisplayName("should always return true")
    void shouldAlwaysReturnTrue() {
      var backoff = FixedBackoff.of(Duration.ofSeconds(1));

      assertThat(backoff.isAtMaxDelay(1)).isTrue();
      assertThat(backoff.isAtMaxDelay(5)).isTrue();
      assertThat(backoff.isAtMaxDelay(100)).isTrue();
    }
  }

  @Nested
  @DisplayName("equals and hashCode")
  class EqualsAndHashCode {

    @Test
    @DisplayName("should be equal for same delay")
    void shouldBeEqualForSameDelay() {
      var backoff1 = FixedBackoff.of(Duration.ofMillis(500));
      var backoff2 = FixedBackoff.of(Duration.ofMillis(500));

      assertThat(backoff1).isEqualTo(backoff2);
      assertThat(backoff1.hashCode()).isEqualTo(backoff2.hashCode());
    }

    @Test
    @DisplayName("should not be equal for different delays")
    void shouldNotBeEqualForDifferentDelays() {
      var backoff1 = FixedBackoff.of(Duration.ofMillis(500));
      var backoff2 = FixedBackoff.of(Duration.ofMillis(1000));

      assertThat(backoff1).isNotEqualTo(backoff2);
    }

    @Test
    @DisplayName("should not be equal to null")
    void shouldNotBeEqualToNull() {
      var backoff = FixedBackoff.defaults();

      assertThat(backoff).isNotEqualTo(null);
    }

    @Test
    @DisplayName("should not be equal to different type")
    void shouldNotBeEqualToDifferentType() {
      var backoff = FixedBackoff.defaults();

      assertThat(backoff).isNotEqualTo("not a backoff");
    }
  }

  @Nested
  @DisplayName("toString")
  class ToString {

    @Test
    @DisplayName("should include delay in toString")
    void shouldIncludeDelayInToString() {
      var backoff = FixedBackoff.of(Duration.ofMillis(500));

      assertThat(backoff.toString()).contains("PT0.5S");
    }
  }

  @Nested
  @DisplayName("implements BackoffStrategy")
  class ImplementsBackoffStrategy {

    @Test
    @DisplayName("should be usable as BackoffStrategy")
    void shouldBeUsableAsBackoffStrategy() {
      BackoffStrategy strategy = FixedBackoff.of(Duration.ofMillis(200));

      assertThat(strategy.calculateDelay(1)).isEqualTo(Duration.ofMillis(200));
    }

    @RepeatedTest(10)
    @DisplayName("should work with withJitter")
    void shouldWorkWithWithJitter() {
      var backoff = FixedBackoff.of(Duration.ofSeconds(1));
      var withJitter = backoff.withJitter(0.2);

      var delay = withJitter.calculateDelay(1);

      // Should be between 80% and 100% of 1 second
      assertThat(delay.toMillis()).isLessThanOrEqualTo(1000);
      assertThat(delay.toMillis()).isGreaterThanOrEqualTo(800);
    }
  }
}
