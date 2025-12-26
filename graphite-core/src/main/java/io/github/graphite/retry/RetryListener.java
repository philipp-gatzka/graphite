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

import java.time.Duration;
import org.jetbrains.annotations.NotNull;

/**
 * Listener interface for retry events.
 *
 * <p>Implementations of this interface can be registered with the Graphite client to receive
 * notifications about retry attempts. This is useful for metrics collection, logging, and
 * monitoring purposes.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * RetryListener listener = new RetryListener() {
 *     @Override
 *     public void onRetryAttempt(int attempt, Exception exception, Duration delay) {
 *         log.warn("Retry attempt {} after error: {}", attempt, exception.getMessage());
 *     }
 *
 *     @Override
 *     public void onRetryExhausted(int totalAttempts, Exception lastException) {
 *         log.error("Retries exhausted after {} attempts", totalAttempts);
 *     }
 *
 *     @Override
 *     public void onRetrySuccess(int attemptsTaken) {
 *         if (attemptsTaken > 1) {
 *             log.info("Request succeeded after {} attempts", attemptsTaken);
 *         }
 *     }
 * };
 *
 * GraphiteClient client = GraphiteClient.builder()
 *     .endpoint("https://api.example.com/graphql")
 *     .retryListener(listener)
 *     .build();
 * }</pre>
 *
 * @see RetryPolicy
 */
public interface RetryListener {

  /**
   * Called when a retry attempt is about to be made.
   *
   * <p>This method is invoked before the delay is applied and before the next attempt is made.
   *
   * @param attempt the retry attempt number (1-based, where 1 is the first retry)
   * @param exception the exception that triggered the retry
   * @param delay the delay that will be applied before the next attempt
   */
  void onRetryAttempt(int attempt, @NotNull Exception exception, @NotNull Duration delay);

  /**
   * Called when all retry attempts have been exhausted.
   *
   * <p>This method is invoked when the retry policy determines that no more retries should be
   * attempted and the exception will be thrown.
   *
   * @param totalAttempts the total number of attempts made (including the initial attempt)
   * @param lastException the exception from the last attempt
   */
  void onRetryExhausted(int totalAttempts, @NotNull Exception lastException);

  /**
   * Called when a request eventually succeeds after retries.
   *
   * <p>This method is only invoked if the request required retries to succeed. If the request
   * succeeds on the first attempt, this method is not called.
   *
   * @param attemptsTaken the total number of attempts that were made (always >= 2 when called)
   */
  void onRetrySuccess(int attemptsTaken);
}
