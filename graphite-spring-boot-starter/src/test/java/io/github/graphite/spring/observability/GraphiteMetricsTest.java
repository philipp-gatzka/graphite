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

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("GraphiteMetrics")
class GraphiteMetricsTest {

  private MeterRegistry registry;
  private GraphiteMetrics metrics;

  @BeforeEach
  void setUp() {
    registry = new SimpleMeterRegistry();
    metrics = new GraphiteMetrics(registry);
  }

  @Nested
  @DisplayName("recordSuccess")
  class RecordSuccess {

    static Stream<Arguments> recordSuccessTestCases() {
      return Stream.of(
          Arguments.of("success counter", GraphiteMetrics.REQUESTS_METRIC, "operation", "GetUser"),
          Arguments.of("success status tag", GraphiteMetrics.REQUESTS_METRIC, "status", "success"),
          Arguments.of("duration timer", GraphiteMetrics.DURATION_METRIC, "operation", "GetUser"));
    }

    @ParameterizedTest(name = "should record {0}")
    @MethodSource("recordSuccessTestCases")
    void shouldRecordSuccess(
        String description, String metricName, String tagKey, String tagValue) {
      Timer.Sample sample = metrics.startTimer();
      metrics.recordSuccess("GetUser", sample);

      if (metricName.equals(GraphiteMetrics.DURATION_METRIC)) {
        Timer timer = registry.find(metricName).tag(tagKey, tagValue).timer();
        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
      } else {
        Counter counter = registry.find(metricName).tag(tagKey, tagValue).counter();
        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
      }
    }

    @Test
    @DisplayName("should use unknown for null operation")
    void shouldUseUnknownForNullOperation() {
      Timer.Sample sample = metrics.startTimer();
      metrics.recordSuccess(null, sample);

      Counter counter =
          registry.find(GraphiteMetrics.REQUESTS_METRIC).tag("operation", "unknown").counter();
      assertThat(counter).isNotNull();
      assertThat(counter.count()).isEqualTo(1.0);
    }
  }

  @Nested
  @DisplayName("recordError")
  class RecordError {

    @Test
    @DisplayName("should record error counter")
    void shouldRecordErrorCounter() {
      Timer.Sample sample = metrics.startTimer();
      metrics.recordError("GetUser", new RuntimeException("test"), sample);

      Counter counter =
          registry.find(GraphiteMetrics.REQUESTS_METRIC).tag("status", "error").counter();
      assertThat(counter).isNotNull();
      assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should record error type")
    void shouldRecordErrorType() {
      Timer.Sample sample = metrics.startTimer();
      metrics.recordError("GetUser", new IllegalStateException("test"), sample);

      Counter counter =
          registry
              .find(GraphiteMetrics.ERRORS_METRIC)
              .tag("error_type", "IllegalStateException")
              .counter();
      assertThat(counter).isNotNull();
      assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should record operation in error counter")
    void shouldRecordOperationInErrorCounter() {
      Timer.Sample sample = metrics.startTimer();
      metrics.recordError("CreateUser", new RuntimeException("test"), sample);

      Counter counter =
          registry.find(GraphiteMetrics.ERRORS_METRIC).tag("operation", "CreateUser").counter();
      assertThat(counter).isNotNull();
      assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should record duration for errors")
    void shouldRecordDurationForErrors() {
      Timer.Sample sample = metrics.startTimer();
      metrics.recordError("GetUser", new RuntimeException("test"), sample);

      Timer timer =
          registry.find(GraphiteMetrics.DURATION_METRIC).tag("operation", "GetUser").timer();
      assertThat(timer).isNotNull();
      assertThat(timer.count()).isEqualTo(1);
    }
  }

  @Nested
  @DisplayName("recordDuration")
  class RecordDuration {

    @Test
    @DisplayName("should record duration directly")
    void shouldRecordDurationDirectly() {
      metrics.recordDuration("GetUser", Duration.ofMillis(100), true);

      Timer timer =
          registry.find(GraphiteMetrics.DURATION_METRIC).tag("operation", "GetUser").timer();
      assertThat(timer).isNotNull();
      assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("should record success status for direct duration")
    void shouldRecordSuccessStatusForDirectDuration() {
      metrics.recordDuration("GetUser", Duration.ofMillis(100), true);

      Counter counter =
          registry.find(GraphiteMetrics.REQUESTS_METRIC).tag("status", "success").counter();
      assertThat(counter).isNotNull();
    }

    @Test
    @DisplayName("should record error status for direct duration")
    void shouldRecordErrorStatusForDirectDuration() {
      metrics.recordDuration("GetUser", Duration.ofMillis(100), false);

      Counter counter =
          registry.find(GraphiteMetrics.REQUESTS_METRIC).tag("status", "error").counter();
      assertThat(counter).isNotNull();
    }
  }

  @Nested
  @DisplayName("getRegistry")
  class GetRegistry {

    @Test
    @DisplayName("should return the meter registry")
    void shouldReturnMeterRegistry() {
      assertThat(metrics.getRegistry()).isSameAs(registry);
    }
  }
}
