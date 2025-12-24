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
package io.github.graphite;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.graphite.interceptor.RequestInterceptor;
import io.github.graphite.interceptor.ResponseInterceptor;
import io.github.graphite.ratelimit.RateLimiter;
import io.github.graphite.retry.RetryPolicy;
import io.github.graphite.scalar.ScalarRegistry;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GraphiteClientBuilder")
class GraphiteClientBuilderTest {

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    @DisplayName("should return a new builder instance")
    void shouldReturnNewBuilderInstance() {
      var builder = GraphiteClientBuilder.create();

      assertThat(builder).isNotNull();
      assertThat(builder).isInstanceOf(GraphiteClientBuilder.class);
    }
  }

  @Nested
  @DisplayName("endpoint")
  class Endpoint {

    @Test
    @DisplayName("should set endpoint from string")
    void shouldSetEndpointFromString() {
      var client =
          (DefaultGraphiteClient)
              GraphiteClient.builder().endpoint("https://api.example.com/graphql").build();

      assertThat(client.getEndpoint()).isEqualTo(URI.create("https://api.example.com/graphql"));
    }

    @Test
    @DisplayName("should set endpoint from URI")
    void shouldSetEndpointFromUri() {
      var uri = URI.create("https://api.example.com/graphql");
      var client = (DefaultGraphiteClient) GraphiteClient.builder().endpoint(uri).build();

      assertThat(client.getEndpoint()).isEqualTo(uri);
    }

    @Test
    @DisplayName("should reject null string endpoint")
    void shouldRejectNullStringEndpoint() {
      assertThatNullPointerException()
          .isThrownBy(() -> GraphiteClient.builder().endpoint((String) null))
          .withMessageContaining("endpoint must not be null");
    }

    @Test
    @DisplayName("should reject null URI endpoint")
    void shouldRejectNullUriEndpoint() {
      assertThatNullPointerException()
          .isThrownBy(() -> GraphiteClient.builder().endpoint((URI) null))
          .withMessageContaining("endpoint must not be null");
    }
  }

  @Nested
  @DisplayName("build")
  class Build {

    @Test
    @DisplayName("should throw when endpoint not set")
    void shouldThrowWhenEndpointNotSet() {
      assertThatThrownBy(() -> GraphiteClient.builder().build())
          .isInstanceOf(IllegalStateException.class)
          .hasMessageContaining("endpoint is required");
    }

    @Test
    @DisplayName("should create client with endpoint")
    void shouldCreateClientWithEndpoint() {
      var client = GraphiteClient.builder().endpoint("https://api.example.com/graphql").build();

      assertThat(client).isNotNull();
      assertThat(client).isInstanceOf(DefaultGraphiteClient.class);
    }
  }

  @Nested
  @DisplayName("header")
  class Header {

    @Test
    @DisplayName("should add single header")
    void shouldAddSingleHeader() {
      var client =
          (DefaultGraphiteClient)
              GraphiteClient.builder()
                  .endpoint("https://api.example.com/graphql")
                  .header("Authorization", "Bearer token")
                  .build();

      assertThat(client.getHeaders()).containsEntry("Authorization", "Bearer token");
    }

    @Test
    @DisplayName("should add multiple headers")
    void shouldAddMultipleHeaders() {
      var client =
          (DefaultGraphiteClient)
              GraphiteClient.builder()
                  .endpoint("https://api.example.com/graphql")
                  .headers(Map.of("X-Custom", "value", "X-Another", "value2"))
                  .build();

      assertThat(client.getHeaders()).containsEntry("X-Custom", "value");
      assertThat(client.getHeaders()).containsEntry("X-Another", "value2");
    }

    @Test
    @DisplayName("should reject null header name")
    void shouldRejectNullHeaderName() {
      assertThatNullPointerException()
          .isThrownBy(() -> GraphiteClient.builder().header(null, "value"))
          .withMessageContaining("header name must not be null");
    }

    @Test
    @DisplayName("should reject null header value")
    void shouldRejectNullHeaderValue() {
      assertThatNullPointerException()
          .isThrownBy(() -> GraphiteClient.builder().header("name", null))
          .withMessageContaining("header value must not be null");
    }

    @Test
    @DisplayName("should reject null headers map")
    void shouldRejectNullHeadersMap() {
      assertThatNullPointerException()
          .isThrownBy(() -> GraphiteClient.builder().headers(null))
          .withMessageContaining("headers must not be null");
    }
  }

  @Nested
  @DisplayName("timeouts")
  class Timeouts {

    @Test
    @DisplayName("should set connect timeout")
    void shouldSetConnectTimeout() {
      var client =
          (DefaultGraphiteClient)
              GraphiteClient.builder()
                  .endpoint("https://api.example.com/graphql")
                  .connectTimeout(Duration.ofSeconds(5))
                  .build();

      assertThat(client.getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("should set read timeout")
    void shouldSetReadTimeout() {
      var client =
          (DefaultGraphiteClient)
              GraphiteClient.builder()
                  .endpoint("https://api.example.com/graphql")
                  .readTimeout(Duration.ofSeconds(15))
                  .build();

      assertThat(client.getReadTimeout()).isEqualTo(Duration.ofSeconds(15));
    }

    @Test
    @DisplayName("should set request timeout")
    void shouldSetRequestTimeout() {
      var client =
          (DefaultGraphiteClient)
              GraphiteClient.builder()
                  .endpoint("https://api.example.com/graphql")
                  .requestTimeout(Duration.ofSeconds(45))
                  .build();

      assertThat(client.getRequestTimeout()).isEqualTo(Duration.ofSeconds(45));
    }

    @Test
    @DisplayName("should reject null connect timeout")
    void shouldRejectNullConnectTimeout() {
      assertThatNullPointerException()
          .isThrownBy(() -> GraphiteClient.builder().connectTimeout(null))
          .withMessageContaining("connectTimeout must not be null");
    }

    @Test
    @DisplayName("should reject zero connect timeout")
    void shouldRejectZeroConnectTimeout() {
      assertThatThrownBy(() -> GraphiteClient.builder().connectTimeout(Duration.ZERO))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("connectTimeout must be positive");
    }

    @Test
    @DisplayName("should reject negative connect timeout")
    void shouldRejectNegativeConnectTimeout() {
      assertThatThrownBy(() -> GraphiteClient.builder().connectTimeout(Duration.ofSeconds(-1)))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("connectTimeout must be positive");
    }

    @Test
    @DisplayName("should use default timeouts")
    void shouldUseDefaultTimeouts() {
      var client =
          (DefaultGraphiteClient)
              GraphiteClient.builder().endpoint("https://api.example.com/graphql").build();

      assertThat(client.getConnectTimeout())
          .isEqualTo(GraphiteClientBuilder.DEFAULT_CONNECT_TIMEOUT);
      assertThat(client.getReadTimeout()).isEqualTo(GraphiteClientBuilder.DEFAULT_READ_TIMEOUT);
      assertThat(client.getRequestTimeout())
          .isEqualTo(GraphiteClientBuilder.DEFAULT_REQUEST_TIMEOUT);
    }
  }

  @Nested
  @DisplayName("retryPolicy")
  class RetryPolicyConfig {

    @Test
    @DisplayName("should set retry policy")
    void shouldSetRetryPolicy() {
      var policy = RetryPolicy.disabled();
      var client =
          (DefaultGraphiteClient)
              GraphiteClient.builder()
                  .endpoint("https://api.example.com/graphql")
                  .retryPolicy(policy)
                  .build();

      assertThat(client.getRetryPolicy()).isSameAs(policy);
    }

    @Test
    @DisplayName("should reject null retry policy")
    void shouldRejectNullRetryPolicy() {
      assertThatNullPointerException()
          .isThrownBy(() -> GraphiteClient.builder().retryPolicy(null))
          .withMessageContaining("retryPolicy must not be null");
    }

    @Test
    @DisplayName("should use default retry policy")
    void shouldUseDefaultRetryPolicy() {
      var client =
          (DefaultGraphiteClient)
              GraphiteClient.builder().endpoint("https://api.example.com/graphql").build();

      assertThat(client.getRetryPolicy()).isNotNull();
      assertThat(client.getRetryPolicy().maxAttempts()).isEqualTo(3);
    }
  }

  @Nested
  @DisplayName("rateLimiter")
  class RateLimiterConfig {

    @Test
    @DisplayName("should set rate limiter")
    void shouldSetRateLimiter() {
      var limiter = RateLimiter.create(100);
      var client =
          (DefaultGraphiteClient)
              GraphiteClient.builder()
                  .endpoint("https://api.example.com/graphql")
                  .rateLimiter(limiter)
                  .build();

      assertThat(client.getRateLimiter()).isSameAs(limiter);
    }

    @Test
    @DisplayName("should allow null rate limiter")
    void shouldAllowNullRateLimiter() {
      var client =
          (DefaultGraphiteClient)
              GraphiteClient.builder()
                  .endpoint("https://api.example.com/graphql")
                  .rateLimiter(null)
                  .build();

      assertThat(client.getRateLimiter()).isNull();
    }
  }

  @Nested
  @DisplayName("scalarRegistry")
  class ScalarRegistryConfig {

    @Test
    @DisplayName("should set scalar registry")
    void shouldSetScalarRegistry() {
      var registry = ScalarRegistry.empty();
      var client =
          (DefaultGraphiteClient)
              GraphiteClient.builder()
                  .endpoint("https://api.example.com/graphql")
                  .scalarRegistry(registry)
                  .build();

      assertThat(client.getScalarRegistry()).isSameAs(registry);
    }

    @Test
    @DisplayName("should reject null scalar registry")
    void shouldRejectNullScalarRegistry() {
      assertThatNullPointerException()
          .isThrownBy(() -> GraphiteClient.builder().scalarRegistry(null))
          .withMessageContaining("scalarRegistry must not be null");
    }
  }

  @Nested
  @DisplayName("interceptors")
  class Interceptors {

    @Test
    @DisplayName("should add request interceptor")
    void shouldAddRequestInterceptor() {
      RequestInterceptor interceptor = request -> request;
      var client =
          (DefaultGraphiteClient)
              GraphiteClient.builder()
                  .endpoint("https://api.example.com/graphql")
                  .requestInterceptor(interceptor)
                  .build();

      assertThat(client.getRequestInterceptors()).containsExactly(interceptor);
    }

    @Test
    @DisplayName("should add response interceptor")
    void shouldAddResponseInterceptor() {
      ResponseInterceptor interceptor = response -> response;
      var client =
          (DefaultGraphiteClient)
              GraphiteClient.builder()
                  .endpoint("https://api.example.com/graphql")
                  .responseInterceptor(interceptor)
                  .build();

      assertThat(client.getResponseInterceptors()).containsExactly(interceptor);
    }

    @Test
    @DisplayName("should reject null request interceptor")
    void shouldRejectNullRequestInterceptor() {
      assertThatNullPointerException()
          .isThrownBy(() -> GraphiteClient.builder().requestInterceptor(null))
          .withMessageContaining("requestInterceptor must not be null");
    }

    @Test
    @DisplayName("should reject null response interceptor")
    void shouldRejectNullResponseInterceptor() {
      assertThatNullPointerException()
          .isThrownBy(() -> GraphiteClient.builder().responseInterceptor(null))
          .withMessageContaining("responseInterceptor must not be null");
    }

    @Test
    @DisplayName("should add multiple interceptors in order")
    void shouldAddMultipleInterceptorsInOrder() {
      RequestInterceptor i1 = request -> request;
      RequestInterceptor i2 = request -> request;
      RequestInterceptor i3 = request -> request;

      var client =
          (DefaultGraphiteClient)
              GraphiteClient.builder()
                  .endpoint("https://api.example.com/graphql")
                  .requestInterceptor(i1)
                  .requestInterceptor(i2)
                  .requestInterceptor(i3)
                  .build();

      assertThat(client.getRequestInterceptors()).containsExactly(i1, i2, i3);
    }
  }

  @Nested
  @DisplayName("objectMapper")
  class ObjectMapperConfig {

    @Test
    @DisplayName("should set custom object mapper")
    void shouldSetCustomObjectMapper() {
      var mapper = new ObjectMapper();
      var client =
          (DefaultGraphiteClient)
              GraphiteClient.builder()
                  .endpoint("https://api.example.com/graphql")
                  .objectMapper(mapper)
                  .build();

      assertThat(client.getObjectMapper()).isSameAs(mapper);
    }

    @Test
    @DisplayName("should reject null object mapper")
    void shouldRejectNullObjectMapper() {
      assertThatNullPointerException()
          .isThrownBy(() -> GraphiteClient.builder().objectMapper(null))
          .withMessageContaining("objectMapper must not be null");
    }

    @Test
    @DisplayName("should use default object mapper when not set")
    void shouldUseDefaultObjectMapperWhenNotSet() {
      var client =
          (DefaultGraphiteClient)
              GraphiteClient.builder().endpoint("https://api.example.com/graphql").build();

      assertThat(client.getObjectMapper()).isNotNull();
    }
  }

  @Nested
  @DisplayName("fluent API")
  class FluentApi {

    @Test
    @DisplayName("should support full configuration chain")
    void shouldSupportFullConfigurationChain() {
      var client =
          (DefaultGraphiteClient)
              GraphiteClient.builder()
                  .endpoint("https://api.example.com/graphql")
                  .header("Authorization", "Bearer token")
                  .headers(Map.of("X-Custom", "value"))
                  .connectTimeout(Duration.ofSeconds(5))
                  .readTimeout(Duration.ofSeconds(15))
                  .requestTimeout(Duration.ofSeconds(45))
                  .retryPolicy(RetryPolicy.disabled())
                  .rateLimiter(RateLimiter.create(100))
                  .scalarRegistry(ScalarRegistry.empty())
                  .requestInterceptor(r -> r)
                  .responseInterceptor(r -> r)
                  .objectMapper(new ObjectMapper())
                  .build();

      assertThat(client.getEndpoint()).isEqualTo(URI.create("https://api.example.com/graphql"));
      assertThat(client.getHeaders()).containsKey("Authorization");
      assertThat(client.getHeaders()).containsKey("X-Custom");
      assertThat(client.getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
      assertThat(client.getReadTimeout()).isEqualTo(Duration.ofSeconds(15));
      assertThat(client.getRequestTimeout()).isEqualTo(Duration.ofSeconds(45));
      assertThat(client.getRetryPolicy().maxAttempts()).isZero();
      assertThat(client.getRateLimiter()).isNotNull();
      assertThat(client.getScalarRegistry()).isNotNull();
      assertThat(client.getRequestInterceptors()).hasSize(1);
      assertThat(client.getResponseInterceptors()).hasSize(1);
    }
  }

  @Nested
  @DisplayName("default constants")
  class DefaultConstants {

    @Test
    @DisplayName("should have expected default connect timeout")
    void shouldHaveExpectedDefaultConnectTimeout() {
      assertThat(GraphiteClientBuilder.DEFAULT_CONNECT_TIMEOUT).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    @DisplayName("should have expected default read timeout")
    void shouldHaveExpectedDefaultReadTimeout() {
      assertThat(GraphiteClientBuilder.DEFAULT_READ_TIMEOUT).isEqualTo(Duration.ofSeconds(30));
    }

    @Test
    @DisplayName("should have expected default request timeout")
    void shouldHaveExpectedDefaultRequestTimeout() {
      assertThat(GraphiteClientBuilder.DEFAULT_REQUEST_TIMEOUT).isEqualTo(Duration.ofSeconds(60));
    }
  }
}
