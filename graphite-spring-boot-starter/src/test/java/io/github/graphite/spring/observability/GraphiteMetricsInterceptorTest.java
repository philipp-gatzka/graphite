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

import static org.assertj.core.api.Assertions.assertThat;

import io.github.graphite.http.HttpMethod;
import io.github.graphite.http.HttpRequest;
import io.github.graphite.http.HttpResponse;
import io.github.graphite.interceptor.RequestInterceptor;
import io.github.graphite.interceptor.ResponseInterceptor;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.URI;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("GraphiteMetricsInterceptor")
class GraphiteMetricsInterceptorTest {

  private MeterRegistry registry;
  private GraphiteMetricsInterceptor interceptor;

  @BeforeEach
  void setUp() {
    registry = new SimpleMeterRegistry();
    interceptor = new GraphiteMetricsInterceptor(registry);
  }

  @Nested
  @DisplayName("request and response flow")
  class RequestResponseFlow {

    @Test
    @DisplayName("should record metrics for successful request")
    void shouldRecordMetricsForSuccessfulRequest() {
      HttpRequest request = createRequest("{\"operationName\":\"GetUser\",\"query\":\"{ user }\"}");
      HttpResponse response = new HttpResponse(200, Map.of(), "{\"data\":{}}");

      RequestInterceptor reqInterceptor = interceptor.requestInterceptor();
      ResponseInterceptor resInterceptor = interceptor.responseInterceptor();

      // Simulate request-response flow
      reqInterceptor.intercept(request);
      resInterceptor.intercept(response);

      // Verify metrics
      Counter counter =
          registry.find(GraphiteMetrics.REQUESTS_METRIC).tag("operation", "GetUser").counter();
      assertThat(counter).isNotNull();
      assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should record duration timer")
    void shouldRecordDurationTimer() {
      HttpRequest request = createRequest("{\"operationName\":\"GetUser\",\"query\":\"{ user }\"}");
      HttpResponse response = new HttpResponse(200, Map.of(), "{\"data\":{}}");

      RequestInterceptor reqInterceptor = interceptor.requestInterceptor();
      ResponseInterceptor resInterceptor = interceptor.responseInterceptor();

      reqInterceptor.intercept(request);
      resInterceptor.intercept(response);

      Timer timer =
          registry.find(GraphiteMetrics.DURATION_METRIC).tag("operation", "GetUser").timer();
      assertThat(timer).isNotNull();
      assertThat(timer.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("should record success status for 2xx response")
    void shouldRecordSuccessStatusFor2xxResponse() {
      HttpRequest request = createRequest("{\"operationName\":\"GetUser\",\"query\":\"{ user }\"}");
      HttpResponse response = new HttpResponse(200, Map.of(), "{\"data\":{}}");

      RequestInterceptor reqInterceptor = interceptor.requestInterceptor();
      ResponseInterceptor resInterceptor = interceptor.responseInterceptor();

      reqInterceptor.intercept(request);
      resInterceptor.intercept(response);

      Counter counter =
          registry.find(GraphiteMetrics.REQUESTS_METRIC).tag("status", "success").counter();
      assertThat(counter).isNotNull();
      assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should record error status for non-2xx response")
    void shouldRecordErrorStatusForNon2xxResponse() {
      HttpRequest request = createRequest("{\"operationName\":\"GetUser\",\"query\":\"{ user }\"}");
      HttpResponse response = new HttpResponse(500, Map.of(), "{\"errors\":[]}");

      RequestInterceptor reqInterceptor = interceptor.requestInterceptor();
      ResponseInterceptor resInterceptor = interceptor.responseInterceptor();

      reqInterceptor.intercept(request);
      resInterceptor.intercept(response);

      Counter counter =
          registry.find(GraphiteMetrics.REQUESTS_METRIC).tag("status", "error").counter();
      assertThat(counter).isNotNull();
      assertThat(counter.count()).isEqualTo(1.0);
    }
  }

  @Nested
  @DisplayName("operation name extraction")
  class OperationNameExtraction {

    static Stream<Arguments> operationNameExtractionTestCases() {
      return Stream.of(
          Arguments.of(
              "operationName from JSON body",
              "{\"operationName\":\"GetUser\",\"query\":\"query GetUser { user }\"}",
              "GetUser"),
          Arguments.of(
              "operation name from query when operationName not present",
              "{\"query\":\"query GetUserById { user }\"}",
              "GetUserById"),
          Arguments.of(
              "mutation name from query",
              "{\"query\":\"mutation CreateUser { createUser }\"}",
              "CreateUser"),
          Arguments.of("unknown for anonymous query", "{\"query\":\"{ user }\"}", "unknown"));
    }

    @ParameterizedTest(name = "should extract {0}")
    @MethodSource("operationNameExtractionTestCases")
    void shouldExtractOperationName(
        String description, String requestBody, String expectedOperation) {
      HttpRequest request = createRequest(requestBody);
      HttpResponse response = new HttpResponse(200, Map.of(), "{}");

      interceptor.requestInterceptor().intercept(request);
      interceptor.responseInterceptor().intercept(response);

      Counter counter =
          registry
              .find(GraphiteMetrics.REQUESTS_METRIC)
              .tag("operation", expectedOperation)
              .counter();
      assertThat(counter).isNotNull();
    }

    @Test
    @DisplayName("should use unknown for null body")
    void shouldUseUnknownForNullBody() {
      HttpRequest request =
          new HttpRequest(HttpMethod.POST, URI.create("http://test"), Map.of(), null);
      HttpResponse response = new HttpResponse(200, Map.of(), "{}");

      interceptor.requestInterceptor().intercept(request);
      interceptor.responseInterceptor().intercept(response);

      Counter counter =
          registry.find(GraphiteMetrics.REQUESTS_METRIC).tag("operation", "unknown").counter();
      assertThat(counter).isNotNull();
    }
  }

  @Nested
  @DisplayName("recordError")
  class RecordErrorTest {

    @Test
    @DisplayName("should record error metrics")
    void shouldRecordErrorMetrics() {
      HttpRequest request = createRequest("{\"operationName\":\"GetUser\",\"query\":\"{ user }\"}");

      interceptor.requestInterceptor().intercept(request);
      interceptor.recordError(new IllegalStateException("test error"));

      Counter counter =
          registry
              .find(GraphiteMetrics.ERRORS_METRIC)
              .tag("error_type", "IllegalStateException")
              .counter();
      assertThat(counter).isNotNull();
      assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("should record operation in error")
    void shouldRecordOperationInError() {
      HttpRequest request =
          createRequest("{\"operationName\":\"CreateUser\",\"query\":\"{ user }\"}");

      interceptor.requestInterceptor().intercept(request);
      interceptor.recordError(new RuntimeException("test"));

      Counter counter =
          registry.find(GraphiteMetrics.ERRORS_METRIC).tag("operation", "CreateUser").counter();
      assertThat(counter).isNotNull();
    }
  }

  @Nested
  @DisplayName("getMetrics")
  class GetMetricsTest {

    @Test
    @DisplayName("should return metrics instance")
    void shouldReturnMetricsInstance() {
      assertThat(interceptor.getMetrics()).isNotNull();
      assertThat(interceptor.getMetrics().getRegistry()).isSameAs(registry);
    }
  }

  private HttpRequest createRequest(String body) {
    return new HttpRequest(
        HttpMethod.POST, URI.create("https://api.example.com/graphql"), Map.of(), body);
  }
}
