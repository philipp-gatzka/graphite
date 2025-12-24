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

import static org.assertj.core.api.Assertions.assertThat;

import io.github.graphite.spring.observability.GraphiteMetrics;
import io.github.graphite.spring.observability.GraphiteMetricsInterceptor;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@DisplayName("GraphiteMetricsAutoConfiguration")
class GraphiteMetricsAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(GraphiteMetricsAutoConfiguration.class));

  @Nested
  @DisplayName("when MeterRegistry is present")
  class WhenMeterRegistryPresent {

    @Test
    @DisplayName("should create GraphiteMetrics bean")
    void shouldCreateGraphiteMetricsBean() {
      contextRunner
          .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
          .run(
              context -> {
                assertThat(context).hasSingleBean(GraphiteMetrics.class);
              });
    }

    @Test
    @DisplayName("should create GraphiteMetricsInterceptor bean")
    void shouldCreateGraphiteMetricsInterceptorBean() {
      contextRunner
          .withBean(MeterRegistry.class, SimpleMeterRegistry::new)
          .run(
              context -> {
                assertThat(context).hasSingleBean(GraphiteMetricsInterceptor.class);
              });
    }
  }

  @Nested
  @DisplayName("when MeterRegistry is not present")
  class WhenMeterRegistryNotPresent {

    @Test
    @DisplayName("should not create GraphiteMetrics bean")
    void shouldNotCreateGraphiteMetricsBean() {
      contextRunner.run(
          context -> {
            assertThat(context).doesNotHaveBean(GraphiteMetrics.class);
          });
    }

    @Test
    @DisplayName("should not create GraphiteMetricsInterceptor bean")
    void shouldNotCreateGraphiteMetricsInterceptorBean() {
      contextRunner.run(
          context -> {
            assertThat(context).doesNotHaveBean(GraphiteMetricsInterceptor.class);
          });
    }
  }

  @Nested
  @DisplayName("when custom beans exist")
  class WhenCustomBeansExist {

    @Test
    @DisplayName("should not override existing GraphiteMetrics bean")
    void shouldNotOverrideExistingGraphiteMetrics() {
      MeterRegistry registry = new SimpleMeterRegistry();
      GraphiteMetrics customMetrics = new GraphiteMetrics(registry);

      contextRunner
          .withBean(MeterRegistry.class, () -> registry)
          .withBean(GraphiteMetrics.class, () -> customMetrics)
          .run(
              context -> {
                assertThat(context).hasSingleBean(GraphiteMetrics.class);
                assertThat(context.getBean(GraphiteMetrics.class)).isSameAs(customMetrics);
              });
    }

    @Test
    @DisplayName("should not override existing GraphiteMetricsInterceptor bean")
    void shouldNotOverrideExistingGraphiteMetricsInterceptor() {
      MeterRegistry registry = new SimpleMeterRegistry();
      GraphiteMetricsInterceptor customInterceptor = new GraphiteMetricsInterceptor(registry);

      contextRunner
          .withBean(MeterRegistry.class, () -> registry)
          .withBean(GraphiteMetricsInterceptor.class, () -> customInterceptor)
          .run(
              context -> {
                assertThat(context).hasSingleBean(GraphiteMetricsInterceptor.class);
                assertThat(context.getBean(GraphiteMetricsInterceptor.class))
                    .isSameAs(customInterceptor);
              });
    }
  }
}
