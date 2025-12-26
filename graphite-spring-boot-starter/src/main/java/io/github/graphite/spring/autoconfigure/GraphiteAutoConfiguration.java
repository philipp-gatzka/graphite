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
import io.github.graphite.GraphiteClientBuilder;
import io.github.graphite.ratelimit.RateLimiter;
import io.github.graphite.retry.ExponentialBackoff;
import io.github.graphite.retry.RetryPolicy;
import java.net.URI;
import java.util.Map;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for the Graphite GraphQL client.
 *
 * <p>This class automatically configures a {@link GraphiteClient} bean when:
 *
 * <ul>
 *   <li>The {@code GraphiteClient} class is on the classpath
 *   <li>No existing {@code GraphiteClient} bean is defined
 *   <li>The {@code graphite.enabled} property is {@code true} (default)
 * </ul>
 *
 * <p>The client is configured based on the properties defined in {@link GraphiteProperties}.
 *
 * @see GraphiteProperties
 * @see GraphiteClient
 */
@AutoConfiguration
@ConditionalOnClass(GraphiteClient.class)
@ConditionalOnProperty(
    prefix = "graphite",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = true)
@EnableConfigurationProperties(GraphiteProperties.class)
public class GraphiteAutoConfiguration {

  /**
   * Creates a {@link GraphiteClient} bean configured from application properties.
   *
   * <p>The client is configured with:
   *
   * <ul>
   *   <li>Endpoint URL from {@code graphite.url}
   *   <li>Headers from {@code graphite.headers}
   *   <li>Timeouts from {@code graphite.timeout.*}
   *   <li>Retry policy from {@code graphite.retry.*}
   *   <li>Rate limiting from {@code graphite.rate-limit.*}
   * </ul>
   *
   * @param properties the configuration properties
   * @return the configured GraphQL client
   */
  @Bean
  @ConditionalOnMissingBean
  public GraphiteClient graphiteClient(GraphiteProperties properties) {
    GraphiteClientBuilder builder = GraphiteClient.builder();
    configureEndpoint(builder, properties);
    configureHeaders(builder, properties.getHeaders());
    configureTimeouts(builder, properties.getTimeout());
    configureRetryPolicy(builder, properties.getRetry());
    configureRateLimit(builder, properties.getRateLimit());
    return builder.build();
  }

  private void configureEndpoint(GraphiteClientBuilder builder, GraphiteProperties properties) {
    if (properties.getUrl() != null && !properties.getUrl().isBlank()) {
      builder.endpoint(URI.create(properties.getUrl()));
    }
  }

  private void configureHeaders(GraphiteClientBuilder builder, Map<String, String> headers) {
    if (headers != null && !headers.isEmpty()) {
      headers.forEach(builder::header);
    }
  }

  private void configureTimeouts(
      GraphiteClientBuilder builder, GraphiteProperties.Timeout timeout) {
    if (timeout == null) {
      return;
    }
    if (timeout.getConnect() != null) {
      builder.connectTimeout(timeout.getConnect());
    }
    if (timeout.getRead() != null) {
      builder.readTimeout(timeout.getRead());
    }
    if (timeout.getRequest() != null) {
      builder.requestTimeout(timeout.getRequest());
    }
  }

  private void configureRetryPolicy(GraphiteClientBuilder builder, GraphiteProperties.Retry retry) {
    if (retry == null || !retry.isEnabled()) {
      builder.retryPolicy(RetryPolicy.disabled());
      return;
    }
    ExponentialBackoff backoff =
        ExponentialBackoff.builder()
            .initialDelay(retry.getInitialDelay())
            .maxDelay(retry.getMaxDelay())
            .multiplier(retry.getMultiplier())
            .build();
    RetryPolicy retryPolicy =
        RetryPolicy.builder().maxAttempts(retry.getMaxAttempts()).backoffStrategy(backoff).build();
    builder.retryPolicy(retryPolicy);
  }

  private void configureRateLimit(
      GraphiteClientBuilder builder, GraphiteProperties.RateLimit rateLimit) {
    if (rateLimit != null && rateLimit.isEnabled()) {
      builder.rateLimiter(RateLimiter.create(rateLimit.getRequestsPerSecond()));
    }
  }
}
