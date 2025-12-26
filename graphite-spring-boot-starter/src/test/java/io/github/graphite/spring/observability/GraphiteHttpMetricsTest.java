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

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GraphiteHttpMetrics")
class GraphiteHttpMetricsTest {

  private MeterRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new SimpleMeterRegistry();
  }

  @Nested
  @DisplayName("constructor")
  class Constructor {

    @Test
    @DisplayName("should create metrics with default client name")
    void shouldCreateMetricsWithDefaultClientName() {
      var metrics = new GraphiteHttpMetrics(registry);

      assertThat(metrics.getClientName()).isEqualTo("default");
      assertThat(metrics.getMaxConnections()).isEqualTo(0);
    }

    @Test
    @DisplayName("should create metrics with custom client name")
    void shouldCreateMetricsWithCustomClientName() {
      var metrics = new GraphiteHttpMetrics(registry, "my-client");

      assertThat(metrics.getClientName()).isEqualTo("my-client");
    }

    @Test
    @DisplayName("should create metrics with max connections")
    void shouldCreateMetricsWithMaxConnections() {
      var metrics = new GraphiteHttpMetrics(registry, "my-client", 100);

      assertThat(metrics.getMaxConnections()).isEqualTo(100);
    }
  }

  @Nested
  @DisplayName("metrics registration")
  class MetricsRegistration {

    @Test
    @DisplayName("should register active connections gauge")
    void shouldRegisterActiveConnectionsGauge() {
      new GraphiteHttpMetrics(registry, "test");

      var gauge = registry.find(GraphiteHttpMetrics.ACTIVE_METRIC).gauge();
      assertThat(gauge).isNotNull();
      assertThat(gauge.value()).isZero();
    }

    @Test
    @DisplayName("should register pending connections gauge")
    void shouldRegisterPendingConnectionsGauge() {
      new GraphiteHttpMetrics(registry, "test");

      var gauge = registry.find(GraphiteHttpMetrics.PENDING_METRIC).gauge();
      assertThat(gauge).isNotNull();
      assertThat(gauge.value()).isZero();
    }

    @Test
    @DisplayName("should register max connections gauge")
    void shouldRegisterMaxConnectionsGauge() {
      new GraphiteHttpMetrics(registry, "test", 50);

      var gauge = registry.find(GraphiteHttpMetrics.MAX_METRIC).gauge();
      assertThat(gauge).isNotNull();
      assertThat(gauge.value()).isEqualTo(50);
    }

    @Test
    @DisplayName("should register total connections counter")
    void shouldRegisterTotalConnectionsCounter() {
      new GraphiteHttpMetrics(registry, "test");

      var counter = registry.find(GraphiteHttpMetrics.TOTAL_METRIC).counter();
      assertThat(counter).isNotNull();
      assertThat(counter.count()).isZero();
    }

    @Test
    @DisplayName("should register connection acquisition timer")
    void shouldRegisterConnectionAcquisitionTimer() {
      new GraphiteHttpMetrics(registry, "test");

      var timer = registry.find(GraphiteHttpMetrics.ACQUIRED_METRIC).timer();
      assertThat(timer).isNotNull();
      assertThat(timer.count()).isZero();
    }
  }

  @Nested
  @DisplayName("connection tracking")
  class ConnectionTracking {

    @Test
    @DisplayName("should track connection acquired")
    void shouldTrackConnectionAcquired() {
      var metrics = new GraphiteHttpMetrics(registry, "test");

      metrics.connectionAcquired();

      assertThat(metrics.getActiveConnections()).isEqualTo(1);
      var counter = registry.find(GraphiteHttpMetrics.TOTAL_METRIC).counter();
      assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("should track connection released")
    void shouldTrackConnectionReleased() {
      var metrics = new GraphiteHttpMetrics(registry, "test");
      metrics.connectionAcquired();
      metrics.connectionAcquired();

      metrics.connectionReleased();

      assertThat(metrics.getActiveConnections()).isEqualTo(1);
    }

    @Test
    @DisplayName("should track connection with timing")
    void shouldTrackConnectionWithTiming() {
      var metrics = new GraphiteHttpMetrics(registry, "test");

      Timer.Sample sample = metrics.startAcquisitionTimer();
      assertThat(metrics.getPendingConnections()).isEqualTo(1);

      metrics.connectionAcquired(sample);

      assertThat(metrics.getPendingConnections()).isZero();
      assertThat(metrics.getActiveConnections()).isEqualTo(1);

      var timer = registry.find(GraphiteHttpMetrics.ACQUIRED_METRIC).timer();
      assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("should track connection cancelled")
    void shouldTrackConnectionCancelled() {
      var metrics = new GraphiteHttpMetrics(registry, "test");
      metrics.startAcquisitionTimer();

      assertThat(metrics.getPendingConnections()).isEqualTo(1);

      metrics.connectionCancelled();

      assertThat(metrics.getPendingConnections()).isZero();
    }
  }

  @Nested
  @DisplayName("metric tags")
  class MetricTags {

    @Test
    @DisplayName("should tag metrics with client name")
    void shouldTagMetricsWithClientName() {
      new GraphiteHttpMetrics(registry, "my-client");

      var gauge =
          registry
              .find(GraphiteHttpMetrics.ACTIVE_METRIC)
              .tag(GraphiteHttpMetrics.TAG_CLIENT, "my-client")
              .gauge();

      assertThat(gauge).isNotNull();
    }
  }
}
