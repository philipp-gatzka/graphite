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

import static org.assertj.core.api.Assertions.assertThat;

import io.github.graphite.exception.GraphiteConnectionException;
import io.github.graphite.exception.GraphiteServerException;
import io.github.graphite.exception.GraphiteTimeoutException;
import io.github.graphite.exception.TimeoutType;
import io.github.graphite.retry.RetryListener;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GraphiteRetryMetrics")
class GraphiteRetryMetricsTest {

  private MeterRegistry registry;
  private GraphiteRetryMetrics retryMetrics;

  @BeforeEach
  void setUp() {
    registry = new SimpleMeterRegistry();
    retryMetrics = new GraphiteRetryMetrics(registry);
  }

  @Nested
  @DisplayName("constructor")
  class Constructor {

    @Test
    @DisplayName("should store registry")
    void shouldStoreRegistry() {
      assertThat(retryMetrics.getRegistry()).isSameAs(registry);
    }
  }

  @Nested
  @DisplayName("implements RetryListener")
  class ImplementsRetryListener {

    @Test
    @DisplayName("should implement RetryListener interface")
    void shouldImplementRetryListenerInterface() {
      assertThat(retryMetrics).isInstanceOf(RetryListener.class);
    }
  }

  @Nested
  @DisplayName("onRetryAttempt")
  class OnRetryAttempt {

    @Test
    @DisplayName("should record retry attempt metric")
    void shouldRecordRetryAttemptMetric() {
      var exception = new GraphiteConnectionException("Connection refused");

      retryMetrics.onRetryAttempt(1, exception, Duration.ofMillis(100));

      Counter counter = registry.find(GraphiteRetryMetrics.RETRY_ATTEMPTS_METRIC).counter();
      assertThat(counter).isNotNull();
      assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should tag with exception type")
    void shouldTagWithExceptionType() {
      var exception = new GraphiteConnectionException("Connection refused");

      retryMetrics.onRetryAttempt(1, exception, Duration.ofMillis(100));

      Counter counter =
          registry
              .find(GraphiteRetryMetrics.RETRY_ATTEMPTS_METRIC)
              .tag(GraphiteRetryMetrics.TAG_EXCEPTION_TYPE, "GraphiteConnectionException")
              .counter();
      assertThat(counter).isNotNull();
      assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should increment on multiple attempts")
    void shouldIncrementOnMultipleAttempts() {
      var exception = new GraphiteConnectionException("Connection refused");

      retryMetrics.onRetryAttempt(1, exception, Duration.ofMillis(100));
      retryMetrics.onRetryAttempt(2, exception, Duration.ofMillis(200));
      retryMetrics.onRetryAttempt(3, exception, Duration.ofMillis(400));

      Counter counter =
          registry
              .find(GraphiteRetryMetrics.RETRY_ATTEMPTS_METRIC)
              .tag(GraphiteRetryMetrics.TAG_EXCEPTION_TYPE, "GraphiteConnectionException")
              .counter();
      assertThat(counter).isNotNull();
      assertThat(counter.count()).isEqualTo(3.0);
    }

    @Test
    @DisplayName("should track different exception types separately")
    void shouldTrackDifferentExceptionTypesSeparately() {
      retryMetrics.onRetryAttempt(
          1, new GraphiteConnectionException("Connection refused"), Duration.ofMillis(100));
      retryMetrics.onRetryAttempt(
          1, new GraphiteTimeoutException("Timeout", TimeoutType.REQUEST), Duration.ofMillis(100));
      retryMetrics.onRetryAttempt(
          1, new GraphiteServerException("Server error", 500), Duration.ofMillis(100));

      Counter connectionCounter =
          registry
              .find(GraphiteRetryMetrics.RETRY_ATTEMPTS_METRIC)
              .tag(GraphiteRetryMetrics.TAG_EXCEPTION_TYPE, "GraphiteConnectionException")
              .counter();
      Counter timeoutCounter =
          registry
              .find(GraphiteRetryMetrics.RETRY_ATTEMPTS_METRIC)
              .tag(GraphiteRetryMetrics.TAG_EXCEPTION_TYPE, "GraphiteTimeoutException")
              .counter();
      Counter serverCounter =
          registry
              .find(GraphiteRetryMetrics.RETRY_ATTEMPTS_METRIC)
              .tag(GraphiteRetryMetrics.TAG_EXCEPTION_TYPE, "GraphiteServerException")
              .counter();

      assertThat(connectionCounter.count()).isEqualTo(1.0);
      assertThat(timeoutCounter.count()).isEqualTo(1.0);
      assertThat(serverCounter.count()).isEqualTo(1.0);
    }
  }

  @Nested
  @DisplayName("onRetryExhausted")
  class OnRetryExhausted {

    @Test
    @DisplayName("should record exhausted metric")
    void shouldRecordExhaustedMetric() {
      var exception = new GraphiteConnectionException("Connection refused");

      retryMetrics.onRetryExhausted(3, exception);

      Counter counter = registry.find(GraphiteRetryMetrics.RETRY_EXHAUSTED_METRIC).counter();
      assertThat(counter).isNotNull();
      assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should tag with exception type")
    void shouldTagWithExceptionType() {
      var exception = new GraphiteServerException("Server error", 503);

      retryMetrics.onRetryExhausted(3, exception);

      Counter counter =
          registry
              .find(GraphiteRetryMetrics.RETRY_EXHAUSTED_METRIC)
              .tag(GraphiteRetryMetrics.TAG_EXCEPTION_TYPE, "GraphiteServerException")
              .counter();
      assertThat(counter).isNotNull();
      assertThat(counter.count()).isEqualTo(1.0);
    }
  }

  @Nested
  @DisplayName("onRetrySuccess")
  class OnRetrySuccess {

    @Test
    @DisplayName("should record success metric")
    void shouldRecordSuccessMetric() {
      retryMetrics.onRetrySuccess(2);

      Counter counter = registry.find(GraphiteRetryMetrics.RETRY_SUCCESS_METRIC).counter();
      assertThat(counter).isNotNull();
      assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should increment on multiple successes")
    void shouldIncrementOnMultipleSuccesses() {
      retryMetrics.onRetrySuccess(2);
      retryMetrics.onRetrySuccess(3);
      retryMetrics.onRetrySuccess(4);

      Counter counter = registry.find(GraphiteRetryMetrics.RETRY_SUCCESS_METRIC).counter();
      assertThat(counter).isNotNull();
      assertThat(counter.count()).isEqualTo(3.0);
    }
  }

  @Nested
  @DisplayName("metric constants")
  class MetricConstants {

    @Test
    @DisplayName("should have expected metric names")
    void shouldHaveExpectedMetricNames() {
      assertThat(GraphiteRetryMetrics.RETRY_ATTEMPTS_METRIC)
          .isEqualTo("graphite.client.retry.attempts");
      assertThat(GraphiteRetryMetrics.RETRY_EXHAUSTED_METRIC)
          .isEqualTo("graphite.client.retry.exhausted");
      assertThat(GraphiteRetryMetrics.RETRY_SUCCESS_METRIC)
          .isEqualTo("graphite.client.retry.success");
    }

    @Test
    @DisplayName("should have expected tag names")
    void shouldHaveExpectedTagNames() {
      assertThat(GraphiteRetryMetrics.TAG_EXCEPTION_TYPE).isEqualTo("exception_type");
    }
  }
}
