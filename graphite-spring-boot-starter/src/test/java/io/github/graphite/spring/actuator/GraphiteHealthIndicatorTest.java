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
package io.github.graphite.spring.actuator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.Status;

@DisplayName("GraphiteHealthIndicator")
class GraphiteHealthIndicatorTest {

  private static final String TEST_URL = "https://api.example.com/graphql";

  @Nested
  @DisplayName("health check")
  class HealthCheck {

    @Test
    @DisplayName("should return UP when endpoint responds with 200")
    @SuppressWarnings("unchecked")
    void shouldReturnUpWhenEndpointResponds() throws Exception {
      HttpClient httpClient = mock(HttpClient.class);
      HttpResponse<String> response = mock(HttpResponse.class);
      doReturn(200).when(response).statusCode();
      doReturn("{\"data\":{\"__typename\":\"Query\"}}").when(response).body();
      doReturn(response).when(httpClient).send(any(HttpRequest.class), any(BodyHandler.class));

      GraphiteHealthIndicator indicator =
          new GraphiteHealthIndicator(TEST_URL, httpClient, Duration.ofSeconds(5));
      Health health = indicator.health();

      assertThat(health.getStatus()).isEqualTo(Status.UP);
      assertThat(health.getDetails()).containsEntry("url", TEST_URL);
      assertThat(health.getDetails()).containsEntry("statusCode", 200);
      assertThat(health.getDetails()).containsKey("responseTime");
    }

    @Test
    @DisplayName("should return DOWN when endpoint responds with 500")
    @SuppressWarnings("unchecked")
    void shouldReturnDownWhenEndpointReturns500() throws Exception {
      HttpClient httpClient = mock(HttpClient.class);
      HttpResponse<String> response = mock(HttpResponse.class);
      doReturn(500).when(response).statusCode();
      doReturn("{\"errors\":[]}").when(response).body();
      doReturn(response).when(httpClient).send(any(HttpRequest.class), any(BodyHandler.class));

      GraphiteHealthIndicator indicator =
          new GraphiteHealthIndicator(TEST_URL, httpClient, Duration.ofSeconds(5));
      Health health = indicator.health();

      assertThat(health.getStatus()).isEqualTo(Status.DOWN);
      assertThat(health.getDetails()).containsEntry("url", TEST_URL);
      assertThat(health.getDetails()).containsEntry("statusCode", 500);
      assertThat(health.getDetails()).containsEntry("reason", "Non-2xx response");
    }

    @Test
    @DisplayName("should return DOWN when connection fails")
    @SuppressWarnings("unchecked")
    void shouldReturnDownWhenConnectionFails() throws Exception {
      HttpClient httpClient = mock(HttpClient.class);
      doThrow(new RuntimeException("Connection refused"))
          .when(httpClient)
          .send(any(HttpRequest.class), any(BodyHandler.class));

      GraphiteHealthIndicator indicator =
          new GraphiteHealthIndicator(TEST_URL, httpClient, Duration.ofSeconds(5));
      Health health = indicator.health();

      assertThat(health.getStatus()).isEqualTo(Status.DOWN);
      assertThat(health.getDetails()).containsEntry("url", TEST_URL);
      assertThat(health.getDetails()).containsEntry("reason", "Connection refused");
    }

    @Test
    @DisplayName("should include response time in details")
    @SuppressWarnings("unchecked")
    void shouldIncludeResponseTime() throws Exception {
      HttpClient httpClient = mock(HttpClient.class);
      HttpResponse<String> response = mock(HttpResponse.class);
      doReturn(200).when(response).statusCode();
      doReturn(response).when(httpClient).send(any(HttpRequest.class), any(BodyHandler.class));

      GraphiteHealthIndicator indicator =
          new GraphiteHealthIndicator(TEST_URL, httpClient, Duration.ofSeconds(5));
      Health health = indicator.health();

      assertThat(health.getDetails().get("responseTime")).isNotNull();
      assertThat(health.getDetails().get("responseTime").toString()).endsWith("ms");
    }
  }

  @Nested
  @DisplayName("constructor")
  class Constructor {

    @Test
    @DisplayName("should use default timeout")
    void shouldUseDefaultTimeout() {
      GraphiteHealthIndicator indicator = new GraphiteHealthIndicator(TEST_URL);

      assertThat(indicator.getTimeout()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    @DisplayName("should use custom timeout")
    void shouldUseCustomTimeout() {
      GraphiteHealthIndicator indicator =
          new GraphiteHealthIndicator(TEST_URL, Duration.ofSeconds(10));

      assertThat(indicator.getTimeout()).isEqualTo(Duration.ofSeconds(10));
    }
  }

  @Nested
  @DisplayName("getters")
  class Getters {

    @Test
    @DisplayName("should return url")
    void shouldReturnUrl() {
      GraphiteHealthIndicator indicator = new GraphiteHealthIndicator(TEST_URL);

      assertThat(indicator.getUrl()).isEqualTo(TEST_URL);
    }

    @Test
    @DisplayName("should return timeout")
    void shouldReturnTimeout() {
      GraphiteHealthIndicator indicator =
          new GraphiteHealthIndicator(TEST_URL, Duration.ofSeconds(15));

      assertThat(indicator.getTimeout()).isEqualTo(Duration.ofSeconds(15));
    }
  }
}
