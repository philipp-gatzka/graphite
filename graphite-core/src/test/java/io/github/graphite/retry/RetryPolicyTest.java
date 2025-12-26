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

import io.github.graphite.exception.GraphiteConnectionException;
import io.github.graphite.exception.GraphiteException;
import io.github.graphite.exception.GraphiteServerException;
import io.github.graphite.exception.GraphiteTimeoutException;
import io.github.graphite.exception.TimeoutType;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RetryPolicy")
class RetryPolicyTest {

  @Nested
  @DisplayName("constructor")
  class Constructor {

    @Test
    @DisplayName("should create policy with valid parameters")
    void shouldCreatePolicyWithValidParameters() {
      var policy = new RetryPolicy(3, BackoffStrategy.fixed(Duration.ofMillis(100)), t -> true);

      assertThat(policy.maxAttempts()).isEqualTo(3);
      assertThat(policy.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("should allow zero max attempts")
    void shouldAllowZeroMaxAttempts() {
      var policy = new RetryPolicy(0, BackoffStrategy.fixed(Duration.ofMillis(100)), t -> true);

      assertThat(policy.maxAttempts()).isEqualTo(0);
      assertThat(policy.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("should reject null backoff strategy")
    void shouldRejectNullBackoffStrategy() {
      assertThatNullPointerException()
          .isThrownBy(() -> new RetryPolicy(3, null, t -> true))
          .withMessage("backoffStrategy must not be null");
    }

    @Test
    @DisplayName("should reject null retry predicate")
    void shouldRejectNullRetryPredicate() {
      assertThatNullPointerException()
          .isThrownBy(() -> new RetryPolicy(3, BackoffStrategy.fixed(Duration.ofMillis(100)), null))
          .withMessage("retryPredicate must not be null");
    }

    @Test
    @DisplayName("should reject negative max attempts")
    void shouldRejectNegativeMaxAttempts() {
      var backoff = BackoffStrategy.fixed(Duration.ofMillis(100));

      assertThatThrownBy(() -> new RetryPolicy(-1, backoff, t -> true))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("maxAttempts must not be negative");
    }
  }

  @Nested
  @DisplayName("disabled")
  class Disabled {

    @Test
    @DisplayName("should return policy with no retries")
    void shouldReturnPolicyWithNoRetries() {
      var policy = RetryPolicy.disabled();

      assertThat(policy.maxAttempts()).isEqualTo(0);
      assertThat(policy.isEnabled()).isFalse();
      assertThat(policy.shouldRetry(new RuntimeException(), 1)).isFalse();
    }
  }

  @Nested
  @DisplayName("defaults")
  class Defaults {

    @Test
    @DisplayName("should return policy with default settings")
    void shouldReturnPolicyWithDefaultSettings() {
      var policy = RetryPolicy.defaults();

      assertThat(policy.maxAttempts()).isEqualTo(RetryPolicy.DEFAULT_MAX_ATTEMPTS);
      assertThat(policy.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("should retry connection exceptions by default")
    void shouldRetryConnectionExceptionsByDefault() {
      var policy = RetryPolicy.defaults();
      var exception = new GraphiteConnectionException("Connection failed", null, "localhost", 443);

      assertThat(policy.shouldRetry(exception, 1)).isTrue();
    }

    @Test
    @DisplayName("should retry timeout exceptions by default")
    void shouldRetryTimeoutExceptionsByDefault() {
      var policy = RetryPolicy.defaults();
      var exception = new GraphiteTimeoutException("Timeout", TimeoutType.REQUEST);

      assertThat(policy.shouldRetry(exception, 1)).isTrue();
    }

    @Test
    @DisplayName("should not retry generic exceptions by default")
    void shouldNotRetryGenericExceptionsByDefault() {
      var policy = RetryPolicy.defaults();

      assertThat(policy.shouldRetry(new RuntimeException(), 1)).isFalse();
    }
  }

  @Nested
  @DisplayName("shouldRetry")
  class ShouldRetry {

    @Test
    @DisplayName("should return false when attempt exceeds max attempts")
    void shouldReturnFalseWhenAttemptExceedsMaxAttempts() {
      var policy = RetryPolicy.builder().maxAttempts(3).retryOnAllErrors().build();

      assertThat(policy.shouldRetry(new GraphiteException("Error"), 1)).isTrue();
      assertThat(policy.shouldRetry(new GraphiteException("Error"), 2)).isTrue();
      assertThat(policy.shouldRetry(new GraphiteException("Error"), 3)).isTrue();
      assertThat(policy.shouldRetry(new GraphiteException("Error"), 4)).isFalse();
    }

    @Test
    @DisplayName("should return false when exception does not match predicate")
    void shouldReturnFalseWhenExceptionDoesNotMatchPredicate() {
      var policy =
          RetryPolicy.builder()
              .maxAttempts(3)
              .retryPredicate(t -> t instanceof GraphiteConnectionException)
              .build();

      assertThat(policy.shouldRetry(new GraphiteConnectionException("Error", null, "host", 80), 1))
          .isTrue();
      assertThat(
              policy.shouldRetry(new GraphiteTimeoutException("Timeout", TimeoutType.CONNECT), 1))
          .isFalse();
    }
  }

  @Nested
  @DisplayName("getDelay")
  class GetDelay {

    @Test
    @DisplayName("should return delay from backoff strategy")
    void shouldReturnDelayFromBackoffStrategy() {
      var expectedDelay = Duration.ofMillis(250);
      var policy =
          RetryPolicy.builder().backoffStrategy(BackoffStrategy.fixed(expectedDelay)).build();

      assertThat(policy.getDelay(1)).isEqualTo(expectedDelay);
      assertThat(policy.getDelay(5)).isEqualTo(expectedDelay);
    }
  }

  @Nested
  @DisplayName("builder")
  class Builder {

    @Test
    @DisplayName("should set max attempts")
    void shouldSetMaxAttempts() {
      var policy = RetryPolicy.builder().maxAttempts(5).build();

      assertThat(policy.maxAttempts()).isEqualTo(5);
    }

    @Test
    @DisplayName("should reject negative max attempts in builder")
    void shouldRejectNegativeMaxAttemptsInBuilder() {
      var builder = RetryPolicy.builder();

      assertThatThrownBy(() -> builder.maxAttempts(-1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessage("maxAttempts must not be negative");
    }

    @Test
    @DisplayName("should set backoff strategy")
    void shouldSetBackoffStrategy() {
      var strategy = BackoffStrategy.fixed(Duration.ofSeconds(1));
      var policy = RetryPolicy.builder().backoffStrategy(strategy).build();

      assertThat(policy.getDelay(1)).isEqualTo(Duration.ofSeconds(1));
    }

    @Test
    @DisplayName("should reject null backoff strategy in builder")
    void shouldRejectNullBackoffStrategyInBuilder() {
      assertThatNullPointerException()
          .isThrownBy(() -> RetryPolicy.builder().backoffStrategy(null))
          .withMessage("backoffStrategy must not be null");
    }

    @Test
    @DisplayName("should add exception type with retryOn")
    void shouldAddExceptionTypeWithRetryOn() {
      var policy =
          RetryPolicy.builder()
              .retryPredicate(t -> false)
              .retryOn(IllegalStateException.class)
              .build();

      assertThat(policy.shouldRetry(new IllegalStateException(), 1)).isTrue();
      assertThat(policy.shouldRetry(new RuntimeException(), 1)).isFalse();
    }

    @Test
    @DisplayName("should reject null exception type in retryOn")
    void shouldRejectNullExceptionTypeInRetryOn() {
      assertThatNullPointerException()
          .isThrownBy(() -> RetryPolicy.builder().retryOn(null))
          .withMessage("exceptionType must not be null");
    }

    @Test
    @DisplayName("should set custom retry predicate")
    void shouldSetCustomRetryPredicate() {
      var policy =
          RetryPolicy.builder()
              .retryPredicate(t -> t.getMessage() != null && t.getMessage().contains("retry"))
              .build();

      assertThat(policy.shouldRetry(new RuntimeException("please retry"), 1)).isTrue();
      assertThat(policy.shouldRetry(new RuntimeException("fail"), 1)).isFalse();
    }

    @Test
    @DisplayName("should reject null retry predicate in builder")
    void shouldRejectNullRetryPredicateInBuilder() {
      assertThatNullPointerException()
          .isThrownBy(() -> RetryPolicy.builder().retryPredicate(null))
          .withMessage("retryPredicate must not be null");
    }

    @Test
    @DisplayName("should configure retry on server errors")
    void shouldConfigureRetryOnServerErrors() {
      var policy = RetryPolicy.builder().retryOnServerErrors().build();

      assertThat(policy.shouldRetry(new GraphiteServerException("Error", 500), 1)).isTrue();
      assertThat(policy.shouldRetry(new GraphiteServerException("Error", 400), 1)).isFalse();
    }

    @Test
    @DisplayName("should configure retry on all errors")
    void shouldConfigureRetryOnAllErrors() {
      var policy = RetryPolicy.builder().retryOnAllErrors().build();

      assertThat(policy.shouldRetry(new GraphiteException("Error"), 1)).isTrue();
      assertThat(policy.shouldRetry(new GraphiteConnectionException("Error", null, "host", 80), 1))
          .isTrue();
      assertThat(policy.shouldRetry(new RuntimeException(), 1)).isFalse();
    }

    @Test
    @DisplayName("should configure no retry")
    void shouldConfigureNoRetry() {
      var policy = RetryPolicy.builder().noRetry().build();

      assertThat(policy.maxAttempts()).isEqualTo(0);
      assertThat(policy.isEnabled()).isFalse();
      assertThat(policy.shouldRetry(new GraphiteException("Error"), 1)).isFalse();
    }
  }
}
