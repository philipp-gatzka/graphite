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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import java.time.Duration;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides methods for recording Graphite client metrics using Micrometer.
 *
 * <p>This class encapsulates the metric names and tags used for monitoring Graphite client
 * operations. The following metrics are recorded:
 *
 * <ul>
 *   <li>{@code graphite.client.requests} - Counter for total requests with operation and status
 *       tags
 *   <li>{@code graphite.client.request.duration} - Timer for request duration with operation tag
 *   <li>{@code graphite.client.errors} - Counter for errors with operation and error_type tags
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * GraphiteMetrics metrics = new GraphiteMetrics(meterRegistry);
 *
 * Timer.Sample sample = metrics.startTimer();
 * try {
 *     // Execute GraphQL request
 *     metrics.recordSuccess("GetUser", sample);
 * } catch (Exception e) {
 *     metrics.recordError("GetUser", e, sample);
 *     throw e;
 * }
 * }</pre>
 *
 * @see GraphiteMetricsInterceptor
 * @see io.github.graphite.spring.autoconfigure.GraphiteMetricsAutoConfiguration
 */
public class GraphiteMetrics {

  /** Metric name for request counter. */
  public static final String REQUESTS_METRIC = "graphite.client.requests";

  /** Metric name for request duration timer. */
  public static final String DURATION_METRIC = "graphite.client.request.duration";

  /** Metric name for error counter. */
  public static final String ERRORS_METRIC = "graphite.client.errors";

  /** Tag name for operation. */
  public static final String TAG_OPERATION = "operation";

  /** Tag name for status. */
  public static final String TAG_STATUS = "status";

  /** Tag name for error type. */
  public static final String TAG_ERROR_TYPE = "error_type";

  /** Status value for success. */
  public static final String STATUS_SUCCESS = "success";

  /** Status value for error. */
  public static final String STATUS_ERROR = "error";

  /** Default operation name when unknown. */
  public static final String UNKNOWN_OPERATION = "unknown";

  private final MeterRegistry registry;

  /**
   * Creates a new metrics instance with the given registry.
   *
   * @param registry the meter registry for recording metrics
   */
  public GraphiteMetrics(@NotNull MeterRegistry registry) {
    this.registry = registry;
  }

  /**
   * Starts a timer sample for measuring request duration.
   *
   * @return the timer sample
   */
  @NotNull
  public Timer.Sample startTimer() {
    return Timer.start(registry);
  }

  /**
   * Records a successful request.
   *
   * @param operation the operation name
   * @param sample the timer sample started before the request
   */
  public void recordSuccess(@Nullable String operation, @NotNull Timer.Sample sample) {
    String op = normalizeOperation(operation);

    // Record request counter
    Counter.builder(REQUESTS_METRIC)
        .tags(List.of(Tag.of(TAG_OPERATION, op), Tag.of(TAG_STATUS, STATUS_SUCCESS)))
        .description("Total number of GraphQL requests")
        .register(registry)
        .increment();

    // Record duration
    sample.stop(
        Timer.builder(DURATION_METRIC)
            .tags(List.of(Tag.of(TAG_OPERATION, op)))
            .description("GraphQL request duration")
            .register(registry));
  }

  /**
   * Records a failed request with error details.
   *
   * @param operation the operation name
   * @param error the exception that occurred
   * @param sample the timer sample started before the request
   */
  public void recordError(
      @Nullable String operation, @NotNull Throwable error, @NotNull Timer.Sample sample) {
    String op = normalizeOperation(operation);
    String errorType = error.getClass().getSimpleName();

    // Record request counter as error
    Counter.builder(REQUESTS_METRIC)
        .tags(List.of(Tag.of(TAG_OPERATION, op), Tag.of(TAG_STATUS, STATUS_ERROR)))
        .description("Total number of GraphQL requests")
        .register(registry)
        .increment();

    // Record error counter
    Counter.builder(ERRORS_METRIC)
        .tags(List.of(Tag.of(TAG_OPERATION, op), Tag.of(TAG_ERROR_TYPE, errorType)))
        .description("Total number of GraphQL errors")
        .register(registry)
        .increment();

    // Record duration
    sample.stop(
        Timer.builder(DURATION_METRIC)
            .tags(List.of(Tag.of(TAG_OPERATION, op)))
            .description("GraphQL request duration")
            .register(registry));
  }

  /**
   * Records request duration directly.
   *
   * @param operation the operation name
   * @param duration the request duration
   * @param success whether the request was successful
   */
  public void recordDuration(
      @Nullable String operation, @NotNull Duration duration, boolean success) {
    String op = normalizeOperation(operation);
    String status = success ? STATUS_SUCCESS : STATUS_ERROR;

    // Record request counter
    Counter.builder(REQUESTS_METRIC)
        .tags(List.of(Tag.of(TAG_OPERATION, op), Tag.of(TAG_STATUS, status)))
        .description("Total number of GraphQL requests")
        .register(registry)
        .increment();

    // Record duration
    Timer.builder(DURATION_METRIC)
        .tags(List.of(Tag.of(TAG_OPERATION, op)))
        .description("GraphQL request duration")
        .register(registry)
        .record(duration);
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

  private String normalizeOperation(@Nullable String operation) {
    return operation != null && !operation.isBlank() ? operation : UNKNOWN_OPERATION;
  }
}
