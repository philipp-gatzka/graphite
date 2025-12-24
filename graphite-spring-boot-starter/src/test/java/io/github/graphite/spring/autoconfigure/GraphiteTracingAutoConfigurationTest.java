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
import static org.mockito.Mockito.mock;

import io.github.graphite.spring.observability.GraphiteTracingInterceptor;
import io.github.graphite.spring.observability.HeaderPropagatingInterceptor;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@DisplayName("GraphiteTracingAutoConfiguration")
class GraphiteTracingAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(GraphiteTracingAutoConfiguration.class));

  @Nested
  @DisplayName("when Tracer is present")
  class WhenTracerPresent {

    @Test
    @DisplayName("should create GraphiteTracingInterceptor bean")
    void shouldCreateGraphiteTracingInterceptorBean() {
      contextRunner
          .withBean(Tracer.class, () -> mock(Tracer.class))
          .run(
              context -> {
                assertThat(context).hasSingleBean(GraphiteTracingInterceptor.class);
              });
    }
  }

  @Nested
  @DisplayName("when Tracer is not present")
  class WhenTracerNotPresent {

    @Test
    @DisplayName("should not create GraphiteTracingInterceptor bean")
    void shouldNotCreateGraphiteTracingInterceptorBean() {
      contextRunner.run(
          context -> {
            assertThat(context).doesNotHaveBean(GraphiteTracingInterceptor.class);
          });
    }
  }

  @Nested
  @DisplayName("when Tracer and Propagator are present")
  class WhenTracerAndPropagatorPresent {

    @Test
    @DisplayName("should create HeaderPropagatingInterceptor bean")
    void shouldCreateHeaderPropagatingInterceptorBean() {
      contextRunner
          .withBean(Tracer.class, () -> mock(Tracer.class))
          .withBean(Propagator.class, () -> mock(Propagator.class))
          .run(
              context -> {
                assertThat(context).hasSingleBean(HeaderPropagatingInterceptor.class);
              });
    }
  }

  @Nested
  @DisplayName("when only Tracer is present")
  class WhenOnlyTracerPresent {

    @Test
    @DisplayName("should not create HeaderPropagatingInterceptor bean")
    void shouldNotCreateHeaderPropagatingInterceptorBean() {
      contextRunner
          .withBean(Tracer.class, () -> mock(Tracer.class))
          .run(
              context -> {
                assertThat(context).doesNotHaveBean(HeaderPropagatingInterceptor.class);
              });
    }
  }

  @Nested
  @DisplayName("when custom beans exist")
  class WhenCustomBeansExist {

    @Test
    @DisplayName("should not override existing GraphiteTracingInterceptor bean")
    void shouldNotOverrideExistingGraphiteTracingInterceptor() {
      Tracer tracer = mock(Tracer.class);
      GraphiteTracingInterceptor customInterceptor = new GraphiteTracingInterceptor(tracer);

      contextRunner
          .withBean(Tracer.class, () -> tracer)
          .withBean(GraphiteTracingInterceptor.class, () -> customInterceptor)
          .run(
              context -> {
                assertThat(context).hasSingleBean(GraphiteTracingInterceptor.class);
                assertThat(context.getBean(GraphiteTracingInterceptor.class))
                    .isSameAs(customInterceptor);
              });
    }

    @Test
    @DisplayName("should not override existing HeaderPropagatingInterceptor bean")
    void shouldNotOverrideExistingHeaderPropagatingInterceptor() {
      Tracer tracer = mock(Tracer.class);
      Propagator propagator = mock(Propagator.class);
      HeaderPropagatingInterceptor customInterceptor =
          new HeaderPropagatingInterceptor(tracer, propagator);

      contextRunner
          .withBean(Tracer.class, () -> tracer)
          .withBean(Propagator.class, () -> propagator)
          .withBean(HeaderPropagatingInterceptor.class, () -> customInterceptor)
          .run(
              context -> {
                assertThat(context).hasSingleBean(HeaderPropagatingInterceptor.class);
                assertThat(context.getBean(HeaderPropagatingInterceptor.class))
                    .isSameAs(customInterceptor);
              });
    }
  }
}
