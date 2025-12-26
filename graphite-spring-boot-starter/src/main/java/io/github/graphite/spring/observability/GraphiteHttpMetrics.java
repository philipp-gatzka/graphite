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
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.jetbrains.annotations.NotNull;

/**
 * Provides HTTP connection pool metrics for Graphite client using Micrometer.
 *
 * <p>This class provides metrics for monitoring HTTP connection behavior:
 *
 * <ul>
 *   <li>{@code graphite.http.connections.active} - Gauge for currently active connections
 *   <li>{@code graphite.http.connections.pending} - Gauge for pending connection requests
 *   <li>{@code graphite.http.connections.max} - Gauge for maximum allowed connections
 *   <li>{@code graphite.http.connections.total} - Counter for total connection attempts
 *   <li>{@code graphite.http.connections.acquired} - Timer for connection acquisition time
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * GraphiteHttpMetrics httpMetrics = new GraphiteHttpMetrics(meterRegistry, "my-client");
 * httpMetrics.bindTo(meterRegistry);
 *
 * // When making a request
 * httpMetrics.connectionAcquired();
 * try {
 *     // ... execute request
 * } finally {
 *     httpMetrics.connectionReleased();
 * }
 * }</pre>
 *
 * @see GraphiteMetrics
 */
public final class GraphiteHttpMetrics implements MeterBinder {

  /** Metric name prefix for all HTTP connection metrics. */
  public static final String METRIC_PREFIX = "graphite.http.connections";

  /** Metric name for active connections gauge. */
  public static final String ACTIVE_METRIC = METRIC_PREFIX + ".active";

  /** Metric name for pending connections gauge. */
  public static final String PENDING_METRIC = METRIC_PREFIX + ".pending";

  /** Metric name for max connections gauge. */
  public static final String MAX_METRIC = METRIC_PREFIX + ".max";

  /** Metric name for total connections counter. */
  public static final String TOTAL_METRIC = METRIC_PREFIX + ".total";

  /** Metric name for connection acquisition timer. */
  public static final String ACQUIRED_METRIC = METRIC_PREFIX + ".acquired";

  /** Tag name for client name. */
  public static final String TAG_CLIENT = "client";

  private static final String ACTIVE_DESCRIPTION = "Number of currently active HTTP connections";
  private static final String PENDING_DESCRIPTION = "Number of pending connection requests";
  private static final String MAX_DESCRIPTION = "Maximum allowed concurrent connections";
  private static final String TOTAL_DESCRIPTION = "Total number of connection attempts";
  private static final String ACQUIRED_DESCRIPTION = "Time taken to acquire a connection";

  private final MeterRegistry registry;
  private final String clientName;
  private final int maxConnections;

  private final AtomicInteger activeConnections = new AtomicInteger(0);
  private final AtomicInteger pendingConnections = new AtomicInteger(0);

  private Counter totalConnectionsCounter;
  private Timer connectionAcquisitionTimer;

  /**
   * Creates a new HTTP metrics instance with default client name.
   *
   * <p>Call {@link #bindTo(MeterRegistry)} to register metrics with the registry.
   *
   * @param registry the meter registry
   */
  public GraphiteHttpMetrics(@NotNull MeterRegistry registry) {
    this(registry, "default", 0);
  }

  /**
   * Creates a new HTTP metrics instance.
   *
   * <p>Call {@link #bindTo(MeterRegistry)} to register metrics with the registry.
   *
   * @param registry the meter registry
   * @param clientName the client name for tagging metrics
   */
  public GraphiteHttpMetrics(@NotNull MeterRegistry registry, @NotNull String clientName) {
    this(registry, clientName, 0);
  }

  /**
   * Creates a new HTTP metrics instance with max connections configuration.
   *
   * <p>Call {@link #bindTo(MeterRegistry)} to register metrics with the registry.
   *
   * @param registry the meter registry
   * @param clientName the client name for tagging metrics
   * @param maxConnections the maximum number of concurrent connections (0 = unlimited)
   */
  public GraphiteHttpMetrics(
      @NotNull MeterRegistry registry, @NotNull String clientName, int maxConnections) {
    this.registry = registry;
    this.clientName = clientName;
    this.maxConnections = maxConnections;
    doBindTo(registry);
  }

  private void doBindTo(@NotNull MeterRegistry registry) {
    List<Tag> tags = List.of(Tag.of(TAG_CLIENT, clientName));

    // Active connections gauge
    Gauge.builder(ACTIVE_METRIC, activeConnections, AtomicInteger::get)
        .tags(tags)
        .description(ACTIVE_DESCRIPTION)
        .register(registry);

    // Pending connections gauge
    Gauge.builder(PENDING_METRIC, pendingConnections, AtomicInteger::get)
        .tags(tags)
        .description(PENDING_DESCRIPTION)
        .register(registry);

    // Max connections gauge (0 means unlimited)
    Gauge.builder(MAX_METRIC, () -> maxConnections)
        .tags(tags)
        .description(MAX_DESCRIPTION)
        .register(registry);

    // Total connections counter
    totalConnectionsCounter =
        Counter.builder(TOTAL_METRIC).tags(tags).description(TOTAL_DESCRIPTION).register(registry);

    // Connection acquisition timer
    connectionAcquisitionTimer =
        Timer.builder(ACQUIRED_METRIC)
            .tags(tags)
            .description(ACQUIRED_DESCRIPTION)
            .register(registry);
  }

  @Override
  public void bindTo(@NotNull MeterRegistry registry) {
    // Metrics are already bound in the constructor via doBindTo()
    // This method is provided for MeterBinder interface compatibility
    // and can be called to re-bind to a different registry if needed
    doBindTo(registry);
  }

  /**
   * Starts timing connection acquisition.
   *
   * @return a timer sample
   */
  @NotNull
  public Timer.Sample startAcquisitionTimer() {
    pendingConnections.incrementAndGet();
    return Timer.start(registry);
  }

  /**
   * Records a successful connection acquisition.
   *
   * @param sample the timer sample from {@link #startAcquisitionTimer()}
   */
  public void connectionAcquired(@NotNull Timer.Sample sample) {
    pendingConnections.decrementAndGet();
    activeConnections.incrementAndGet();
    totalConnectionsCounter.increment();
    sample.stop(connectionAcquisitionTimer);
  }

  /** Records that a connection has been acquired (without timing). */
  public void connectionAcquired() {
    activeConnections.incrementAndGet();
    totalConnectionsCounter.increment();
  }

  /** Records that a connection has been released. */
  public void connectionReleased() {
    activeConnections.decrementAndGet();
  }

  /** Records that a pending connection request was cancelled. */
  public void connectionCancelled() {
    pendingConnections.decrementAndGet();
  }

  /**
   * Returns the current number of active connections.
   *
   * @return active connection count
   */
  public int getActiveConnections() {
    return activeConnections.get();
  }

  /**
   * Returns the current number of pending connection requests.
   *
   * @return pending connection count
   */
  public int getPendingConnections() {
    return pendingConnections.get();
  }

  /**
   * Returns the maximum number of allowed connections.
   *
   * @return max connections (0 = unlimited)
   */
  public int getMaxConnections() {
    return maxConnections;
  }

  /**
   * Returns the client name used for tagging metrics.
   *
   * @return the client name
   */
  @NotNull
  public String getClientName() {
    return clientName;
  }
}
