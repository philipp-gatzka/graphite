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
package io.github.graphite.spring.observability;

import io.github.graphite.retry.RetryListener;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import java.time.Duration;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Micrometer-based implementation of {@link RetryListener} for recording retry metrics.
 *
 * <p>This class records the following metrics:
 *
 * <ul>
 *   <li>{@code graphite.client.retry.attempts} - Counter for total retry attempts with
 *       exception_type tag
 *   <li>{@code graphite.client.retry.exhausted} - Counter for exhausted retries with exception_type
 *       tag
 *   <li>{@code graphite.client.retry.success} - Counter for successful retries after failures
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * MeterRegistry registry = new SimpleMeterRegistry();
 * GraphiteRetryMetrics retryMetrics = new GraphiteRetryMetrics(registry);
 *
 * GraphiteClient client = GraphiteClient.builder()
 *     .endpoint("https://api.example.com/graphql")
 *     .retryListener(retryMetrics)
 *     .build();
 * }</pre>
 *
 * @see RetryListener
 * @see io.github.graphite.spring.autoconfigure.GraphiteMetricsAutoConfiguration
 */
public class GraphiteRetryMetrics implements RetryListener {

  /** Metric name for retry attempts counter. */
  public static final String RETRY_ATTEMPTS_METRIC = "graphite.client.retry.attempts";

  /** Metric name for exhausted retries counter. */
  public static final String RETRY_EXHAUSTED_METRIC = "graphite.client.retry.exhausted";

  /** Metric name for successful retries counter. */
  public static final String RETRY_SUCCESS_METRIC = "graphite.client.retry.success";

  /** Tag name for exception type. */
  public static final String TAG_EXCEPTION_TYPE = "exception_type";

  private static final String RETRY_ATTEMPTS_DESCRIPTION = "Total number of retry attempts";
  private static final String RETRY_EXHAUSTED_DESCRIPTION = "Total number of exhausted retries";
  private static final String RETRY_SUCCESS_DESCRIPTION =
      "Total number of successful requests after retries";

  private final MeterRegistry registry;

  /**
   * Creates a new retry metrics instance with the given registry.
   *
   * @param registry the meter registry for recording metrics
   */
  public GraphiteRetryMetrics(@NotNull MeterRegistry registry) {
    this.registry = registry;
  }

  @Override
  public void onRetryAttempt(int attempt, @NotNull Exception exception, @NotNull Duration delay) {
    String exceptionType = exception.getClass().getSimpleName();

    Counter.builder(RETRY_ATTEMPTS_METRIC)
        .tags(List.of(Tag.of(TAG_EXCEPTION_TYPE, exceptionType)))
        .description(RETRY_ATTEMPTS_DESCRIPTION)
        .register(registry)
        .increment();
  }

  @Override
  public void onRetryExhausted(int totalAttempts, @NotNull Exception lastException) {
    String exceptionType = lastException.getClass().getSimpleName();

    Counter.builder(RETRY_EXHAUSTED_METRIC)
        .tags(List.of(Tag.of(TAG_EXCEPTION_TYPE, exceptionType)))
        .description(RETRY_EXHAUSTED_DESCRIPTION)
        .register(registry)
        .increment();
  }

  @Override
  public void onRetrySuccess(int attemptsTaken) {
    Counter.builder(RETRY_SUCCESS_METRIC)
        .description(RETRY_SUCCESS_DESCRIPTION)
        .register(registry)
        .increment();
  }

  /**
   * Returns the meter registry.
   *
   * @return the meter registry
   */
  @NotNull
  public MeterRegistry getRegistry() {
    return registry;
  }
}
