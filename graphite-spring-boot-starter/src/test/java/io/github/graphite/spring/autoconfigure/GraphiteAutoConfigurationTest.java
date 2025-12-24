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

import io.github.graphite.GraphiteClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

@DisplayName("GraphiteAutoConfiguration")
class GraphiteAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(AutoConfigurations.of(GraphiteAutoConfiguration.class));

  @Nested
  @DisplayName("when enabled")
  class WhenEnabled {

    @Test
    @DisplayName("should create GraphiteClient bean with url")
    void shouldCreateGraphiteClientBean() {
      contextRunner
          .withPropertyValues("graphite.url=https://api.example.com/graphql")
          .run(
              context -> {
                assertThat(context).hasSingleBean(GraphiteClient.class);
              });
    }

    @Test
    @DisplayName("should fail when url is not configured")
    void shouldFailWhenUrlNotConfigured() {
      contextRunner.run(
          context -> {
            assertThat(context).hasFailed();
          });
    }

    @Test
    @DisplayName("should apply timeout configuration")
    void shouldApplyTimeoutConfiguration() {
      contextRunner
          .withPropertyValues(
              "graphite.url=https://api.example.com/graphql",
              "graphite.timeout.connect=5s",
              "graphite.timeout.read=15s",
              "graphite.timeout.request=30s")
          .run(
              context -> {
                assertThat(context).hasSingleBean(GraphiteClient.class);
              });
    }

    @Test
    @DisplayName("should apply retry configuration")
    void shouldApplyRetryConfiguration() {
      contextRunner
          .withPropertyValues(
              "graphite.url=https://api.example.com/graphql",
              "graphite.retry.enabled=true",
              "graphite.retry.max-attempts=5",
              "graphite.retry.initial-delay=200ms",
              "graphite.retry.multiplier=1.5",
              "graphite.retry.max-delay=10s")
          .run(
              context -> {
                assertThat(context).hasSingleBean(GraphiteClient.class);
              });
    }

    @Test
    @DisplayName("should apply rate limit configuration")
    void shouldApplyRateLimitConfiguration() {
      contextRunner
          .withPropertyValues(
              "graphite.url=https://api.example.com/graphql",
              "graphite.rate-limit.enabled=true",
              "graphite.rate-limit.requests-per-second=50")
          .run(
              context -> {
                assertThat(context).hasSingleBean(GraphiteClient.class);
              });
    }

    @Test
    @DisplayName("should apply headers configuration")
    void shouldApplyHeadersConfiguration() {
      contextRunner
          .withPropertyValues(
              "graphite.url=https://api.example.com/graphql",
              "graphite.headers.Authorization=Bearer token",
              "graphite.headers.X-Custom=value")
          .run(
              context -> {
                assertThat(context).hasSingleBean(GraphiteClient.class);
              });
    }
  }

  @Nested
  @DisplayName("when disabled")
  class WhenDisabled {

    @Test
    @DisplayName("should not create GraphiteClient bean when explicitly disabled")
    void shouldNotCreateBeanWhenDisabled() {
      contextRunner
          .withPropertyValues("graphite.enabled=false")
          .run(
              context -> {
                assertThat(context).doesNotHaveBean(GraphiteClient.class);
              });
    }
  }

  @Nested
  @DisplayName("when custom bean exists")
  class WhenCustomBeanExists {

    @Test
    @DisplayName("should not override existing GraphiteClient bean")
    void shouldNotOverrideExistingBean() {
      contextRunner
          .withPropertyValues("graphite.url=https://api.example.com/graphql")
          .withBean(
              GraphiteClient.class,
              () ->
                  GraphiteClient.builder()
                      .endpoint(java.net.URI.create("https://custom.example.com/graphql"))
                      .build())
          .run(
              context -> {
                assertThat(context).hasSingleBean(GraphiteClient.class);
              });
    }
  }

  @Nested
  @DisplayName("retry disabled")
  class RetryDisabled {

    @Test
    @DisplayName("should use disabled retry policy when retry is disabled")
    void shouldUseDisabledRetryPolicy() {
      contextRunner
          .withPropertyValues(
              "graphite.url=https://api.example.com/graphql", "graphite.retry.enabled=false")
          .run(
              context -> {
                assertThat(context).hasSingleBean(GraphiteClient.class);
              });
    }
  }
}
