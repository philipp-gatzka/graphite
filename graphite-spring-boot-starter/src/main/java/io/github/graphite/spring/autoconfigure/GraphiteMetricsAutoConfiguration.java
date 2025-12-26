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
package io.github.graphite.spring.autoconfigure;

import io.github.graphite.GraphiteClient;
import io.github.graphite.spring.observability.GraphiteHttpMetrics;
import io.github.graphite.spring.observability.GraphiteMetrics;
import io.github.graphite.spring.observability.GraphiteMetricsInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Graphite client metrics with Micrometer.
 *
 * <p>This configuration is activated when:
 *
 * <ul>
 *   <li>Micrometer's {@link MeterRegistry} is on the classpath
 *   <li>A {@link MeterRegistry} bean is available
 *   <li>{@link GraphiteClient} is on the classpath
 * </ul>
 *
 * <p>The following beans are created:
 *
 * <ul>
 *   <li>{@link GraphiteMetrics} - Facade for recording Graphite metrics
 *   <li>{@link GraphiteMetricsInterceptor} - Interceptor for automatic metrics collection
 * </ul>
 *
 * <p>To use the metrics interceptor with a client:
 *
 * <pre>{@code
 * @Autowired
 * private GraphiteMetricsInterceptor metricsInterceptor;
 *
 * @Bean
 * public GraphiteClient graphiteClient(GraphiteProperties properties) {
 *     return GraphiteClient.builder()
 *         .endpoint(URI.create(properties.getUrl()))
 *         .requestInterceptor(metricsInterceptor.requestInterceptor())
 *         .responseInterceptor(metricsInterceptor.responseInterceptor())
 *         .build();
 * }
 * }</pre>
 *
 * <p>Recorded metrics:
 *
 * <ul>
 *   <li>{@code graphite.client.requests} - Counter with tags: operation, status
 *   <li>{@code graphite.client.request.duration} - Timer with tags: operation
 *   <li>{@code graphite.client.errors} - Counter with tags: operation, error_type
 * </ul>
 *
 * @see GraphiteMetrics
 * @see GraphiteMetricsInterceptor
 * @see GraphiteAutoConfiguration
 */
@AutoConfiguration(after = GraphiteAutoConfiguration.class)
@ConditionalOnClass({MeterRegistry.class, GraphiteClient.class})
@ConditionalOnBean(MeterRegistry.class)
public class GraphiteMetricsAutoConfiguration {

  /**
   * Creates a {@link GraphiteMetrics} bean for recording Graphite client metrics.
   *
   * @param registry the meter registry
   * @return the metrics facade
   */
  @Bean
  @ConditionalOnMissingBean
  public GraphiteMetrics graphiteMetrics(MeterRegistry registry) {
    return new GraphiteMetrics(registry);
  }

  /**
   * Creates a {@link GraphiteMetricsInterceptor} bean for automatic metrics collection.
   *
   * <p>The interceptor can be used with a GraphiteClient to automatically record metrics for all
   * requests.
   *
   * @param registry the meter registry
   * @return the metrics interceptor
   */
  @Bean
  @ConditionalOnMissingBean
  public GraphiteMetricsInterceptor graphiteMetricsInterceptor(MeterRegistry registry) {
    return new GraphiteMetricsInterceptor(registry);
  }

  /**
   * Creates a {@link GraphiteHttpMetrics} bean for HTTP connection pool metrics.
   *
   * <p>This bean provides metrics for monitoring HTTP connection behavior:
   *
   * <ul>
   *   <li>{@code graphite.http.connections.active} - Currently active connections
   *   <li>{@code graphite.http.connections.pending} - Pending connection requests
   *   <li>{@code graphite.http.connections.max} - Maximum allowed connections
   *   <li>{@code graphite.http.connections.total} - Total connection attempts
   *   <li>{@code graphite.http.connections.acquired} - Connection acquisition timing
   * </ul>
   *
   * @param registry the meter registry
   * @param properties the graphite properties for configuration
   * @return the HTTP metrics
   */
  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnProperty(
      prefix = "graphite.metrics.http",
      name = "enabled",
      havingValue = "true",
      matchIfMissing = true)
  public GraphiteHttpMetrics graphiteHttpMetrics(
      MeterRegistry registry, GraphiteProperties properties) {
    String clientName = properties.getClientName() != null ? properties.getClientName() : "default";
    return new GraphiteHttpMetrics(registry, clientName);
  }
}
