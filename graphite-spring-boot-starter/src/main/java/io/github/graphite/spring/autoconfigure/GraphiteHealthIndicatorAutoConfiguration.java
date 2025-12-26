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
import io.github.graphite.spring.actuator.GraphiteHealthIndicator;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Graphite health indicator with Spring Boot Actuator.
 *
 * <p>This configuration is activated when:
 *
 * <ul>
 *   <li>Spring Boot Actuator's {@link HealthIndicator} is on the classpath
 *   <li>{@link GraphiteClient} is on the classpath
 *   <li>The {@code graphite.url} property is configured
 * </ul>
 *
 * <p>The health indicator sends a lightweight introspection query to verify GraphQL endpoint
 * availability.
 *
 * <p>Example health response:
 *
 * <pre>{@code
 * {
 *   "status": "UP",
 *   "details": {
 *     "url": "https://api.example.com/graphql",
 *     "responseTime": "45ms"
 *   }
 * }
 * }</pre>
 *
 * @see GraphiteHealthIndicator
 * @see GraphiteAutoConfiguration
 */
@AutoConfiguration(after = GraphiteAutoConfiguration.class)
@ConditionalOnClass({HealthIndicator.class, GraphiteClient.class})
@ConditionalOnProperty(prefix = "graphite", name = "url")
public class GraphiteHealthIndicatorAutoConfiguration {

  /**
   * Creates a {@link GraphiteHealthIndicator} bean for monitoring GraphQL endpoint health.
   *
   * @param properties the Graphite configuration properties
   * @return the health indicator
   */
  @Bean
  @ConditionalOnMissingBean(name = "graphiteHealthIndicator")
  public GraphiteHealthIndicator graphiteHealthIndicator(GraphiteProperties properties) {
    return new GraphiteHealthIndicator(properties.getUrl());
  }
}
