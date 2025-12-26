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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RetryListener")
class RetryListenerTest {

  @Nested
  @DisplayName("interface implementation")
  class InterfaceImplementation {

    @Test
    @DisplayName("should allow implementation of all methods")
    void shouldAllowImplementationOfAllMethods() {
      List<String> events = new ArrayList<>();

      RetryListener listener =
          new RetryListener() {
            @Override
            public void onRetryAttempt(int attempt, Exception exception, Duration delay) {
              events.add("attempt:" + attempt + ":" + exception.getMessage());
            }

            @Override
            public void onRetryExhausted(int totalAttempts, Exception lastException) {
              events.add("exhausted:" + totalAttempts + ":" + lastException.getMessage());
            }

            @Override
            public void onRetrySuccess(int attemptsTaken) {
              events.add("success:" + attemptsTaken);
            }
          };

      Exception testException = new RuntimeException("Test error");

      listener.onRetryAttempt(1, testException, Duration.ofMillis(100));
      listener.onRetryAttempt(2, testException, Duration.ofMillis(200));
      listener.onRetryExhausted(3, testException);

      assertThat(events)
          .containsExactly(
              "attempt:1:Test error", "attempt:2:Test error", "exhausted:3:Test error");
    }

    @Test
    @DisplayName("should record success after retries")
    void shouldRecordSuccessAfterRetries() {
      List<String> events = new ArrayList<>();

      RetryListener listener =
          new RetryListener() {
            @Override
            public void onRetryAttempt(int attempt, Exception exception, Duration delay) {
              events.add("attempt:" + attempt);
            }

            @Override
            public void onRetryExhausted(int totalAttempts, Exception lastException) {
              events.add("exhausted:" + totalAttempts);
            }

            @Override
            public void onRetrySuccess(int attemptsTaken) {
              events.add("success:" + attemptsTaken);
            }
          };

      Exception testException = new RuntimeException("Test error");

      listener.onRetryAttempt(1, testException, Duration.ofMillis(100));
      listener.onRetrySuccess(2);

      assertThat(events).containsExactly("attempt:1", "success:2");
    }
  }
}
