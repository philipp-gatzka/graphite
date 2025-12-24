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
import io.github.graphite.spring.observability.GraphiteTracingInterceptor;
import io.github.graphite.spring.observability.HeaderPropagatingInterceptor;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Graphite client distributed tracing with Micrometer Tracing.
 *
 * <p>This configuration is activated when:
 *
 * <ul>
 *   <li>Micrometer Tracing's {@link Tracer} is on the classpath
 *   <li>A {@link Tracer} bean is available
 *   <li>{@link GraphiteClient} is on the classpath
 * </ul>
 *
 * <p>The following beans are created:
 *
 * <ul>
 *   <li>{@link GraphiteTracingInterceptor} - Creates spans for GraphQL operations
 *   <li>{@link HeaderPropagatingInterceptor} - Propagates trace context headers (when Propagator is
 *       available)
 * </ul>
 *
 * <p>To use the tracing interceptors with a client:
 *
 * <pre>{@code
 * @Autowired
 * private GraphiteTracingInterceptor tracingInterceptor;
 *
 * @Autowired(required = false)
 * private HeaderPropagatingInterceptor propagatingInterceptor;
 *
 * @Bean
 * public GraphiteClient graphiteClient(GraphiteProperties properties) {
 *     var builder = GraphiteClient.builder()
 *         .endpoint(URI.create(properties.getUrl()))
 *         .requestInterceptor(tracingInterceptor.requestInterceptor())
 *         .responseInterceptor(tracingInterceptor.responseInterceptor());
 *
 *     if (propagatingInterceptor != null) {
 *         builder.requestInterceptor(propagatingInterceptor.requestInterceptor());
 *     }
 *
 *     return builder.build();
 * }
 * }</pre>
 *
 * <p>Span attributes recorded:
 *
 * <ul>
 *   <li>{@code graphql.operation.name} - The GraphQL operation name
 *   <li>{@code graphql.operation.type} - The operation type (query/mutation/subscription)
 *   <li>{@code http.url} - The target URL
 *   <li>{@code http.status_code} - The HTTP response status code
 * </ul>
 *
 * @see GraphiteTracingInterceptor
 * @see HeaderPropagatingInterceptor
 * @see GraphiteAutoConfiguration
 */
@AutoConfiguration(after = GraphiteAutoConfiguration.class)
@ConditionalOnClass({Tracer.class, GraphiteClient.class})
@ConditionalOnBean(Tracer.class)
public class GraphiteTracingAutoConfiguration {

  /**
   * Creates a {@link GraphiteTracingInterceptor} bean for creating spans.
   *
   * @param tracer the Micrometer tracer
   * @return the tracing interceptor
   */
  @Bean
  @ConditionalOnMissingBean
  public GraphiteTracingInterceptor graphiteTracingInterceptor(Tracer tracer) {
    return new GraphiteTracingInterceptor(tracer);
  }

  /**
   * Creates a {@link HeaderPropagatingInterceptor} bean for propagating trace context.
   *
   * <p>This bean is only created when a {@link Propagator} bean is available.
   *
   * @param tracer the Micrometer tracer
   * @param propagator the trace context propagator
   * @return the header propagating interceptor
   */
  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnBean(Propagator.class)
  public HeaderPropagatingInterceptor headerPropagatingInterceptor(
      Tracer tracer, Propagator propagator) {
    return new HeaderPropagatingInterceptor(tracer, propagator);
  }
}
