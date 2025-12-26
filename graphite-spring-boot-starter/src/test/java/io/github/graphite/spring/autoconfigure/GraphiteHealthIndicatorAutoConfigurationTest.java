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

import io.github.graphite.spring.actuator.GraphiteHealthIndicator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@DisplayName("GraphiteHealthIndicatorAutoConfiguration")
class GraphiteHealthIndicatorAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  GraphiteAutoConfiguration.class, GraphiteHealthIndicatorAutoConfiguration.class));

  @Nested
  @DisplayName("when url is configured")
  class WhenUrlConfigured {

    @Test
    @DisplayName("should create GraphiteHealthIndicator bean")
    void shouldCreateGraphiteHealthIndicatorBean() {
      contextRunner
          .withPropertyValues("graphite.url=https://api.example.com/graphql")
          .run(
              context -> {
                assertThat(context).hasSingleBean(GraphiteHealthIndicator.class);
              });
    }

    @Test
    @DisplayName("should configure health indicator with correct url")
    void shouldConfigureHealthIndicatorWithCorrectUrl() {
      contextRunner
          .withPropertyValues("graphite.url=https://api.example.com/graphql")
          .run(
              context -> {
                GraphiteHealthIndicator indicator = context.getBean(GraphiteHealthIndicator.class);
                assertThat(indicator.getUrl()).isEqualTo("https://api.example.com/graphql");
              });
    }
  }

  @Nested
  @DisplayName("when url is not configured")
  class WhenUrlNotConfigured {

    @Test
    @DisplayName("should not create GraphiteHealthIndicator bean")
    void shouldNotCreateGraphiteHealthIndicatorBean() {
      // Use a separate runner without GraphiteAutoConfiguration to avoid context failure
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(GraphiteHealthIndicatorAutoConfiguration.class))
          .run(
              context -> {
                assertThat(context).doesNotHaveBean(GraphiteHealthIndicator.class);
              });
    }
  }

  @Nested
  @DisplayName("when custom bean exists")
  class WhenCustomBeanExists {

    @Test
    @DisplayName("should not override existing GraphiteHealthIndicator bean")
    void shouldNotOverrideExistingGraphiteHealthIndicator() {
      GraphiteHealthIndicator customIndicator =
          new GraphiteHealthIndicator("https://custom.example.com/graphql");

      contextRunner
          .withPropertyValues("graphite.url=https://api.example.com/graphql")
          .withBean("graphiteHealthIndicator", GraphiteHealthIndicator.class, () -> customIndicator)
          .run(
              context -> {
                assertThat(context).hasSingleBean(GraphiteHealthIndicator.class);
                assertThat(context.getBean(GraphiteHealthIndicator.class).getUrl())
                    .isEqualTo("https://custom.example.com/graphql");
              });
    }
  }
}
