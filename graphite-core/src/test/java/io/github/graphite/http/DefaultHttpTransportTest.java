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
package io.github.graphite.http;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.graphite.exception.GraphiteConnectionException;
import io.github.graphite.exception.GraphiteTimeoutException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("DefaultHttpTransport")
@WireMockTest
class DefaultHttpTransportTest {

  private DefaultHttpTransport transport;

  @BeforeEach
  void setUp() {
    transport = new DefaultHttpTransport();
  }

  @Nested
  @DisplayName("constructor")
  class Constructor {

    @Test
    @DisplayName("should create transport with default config")
    void shouldCreateTransportWithDefaultConfig() {
      var newTransport = new DefaultHttpTransport();

      assertThat(newTransport.getConfig()).isEqualTo(HttpTransportConfig.defaults());
      assertThat(newTransport.isClosed()).isFalse();
    }

    @Test
    @DisplayName("should create transport with custom config")
    void shouldCreateTransportWithCustomConfig() {
      var config =
          HttpTransportConfig.builder()
              .connectTimeout(Duration.ofSeconds(5))
              .requestTimeout(Duration.ofSeconds(15))
              .build();

      var customTransport = new DefaultHttpTransport(config);

      assertThat(customTransport.getConfig()).isEqualTo(config);
    }

    @Test
    @DisplayName("should reject null config")
    void shouldRejectNullConfig() {
      assertThatThrownBy(() -> new DefaultHttpTransport((HttpTransportConfig) null))
          .isInstanceOf(NullPointerException.class)
          .hasMessage("config must not be null");
    }

    @Test
    @DisplayName("should reject null httpClient")
    void shouldRejectNullHttpClient() {
      var config = HttpTransportConfig.defaults();

      assertThatThrownBy(() -> new DefaultHttpTransport(null, config))
          .isInstanceOf(NullPointerException.class)
          .hasMessage("httpClient must not be null");
    }
  }

  @Nested
  @DisplayName("execute")
  class Execute {

    @Test
    @DisplayName("should execute POST request and return response")
    void shouldExecutePostRequestAndReturnResponse(WireMockRuntimeInfo wmRuntimeInfo) {
      stubFor(
          post(urlEqualTo("/graphql"))
              .willReturn(
                  aResponse()
                      .withStatus(200)
                      .withHeader("Content-Type", "application/json")
                      .withBody("{\"data\": {\"user\": {\"id\": \"1\"}}}")));

      var request =
          HttpRequest.post(
              URI.create(wmRuntimeInfo.getHttpBaseUrl() + "/graphql"),
              Map.of("Content-Type", "application/json"),
              "{\"query\": \"{ user { id } }\"}");

      var response = transport.execute(request);

      assertThat(response.statusCode()).isEqualTo(200);
      assertThat(response.body()).isEqualTo("{\"data\": {\"user\": {\"id\": \"1\"}}}");
      assertThat(response.getContentType()).isEqualTo("application/json");
      assertThat(response.isSuccessful()).isTrue();
    }

    @Test
    @DisplayName("should execute GET request")
    void shouldExecuteGetRequest(WireMockRuntimeInfo wmRuntimeInfo) {
      stubFor(
          get(urlEqualTo("/graphql?query=%7Buser%7D"))
              .willReturn(aResponse().withStatus(200).withBody("{\"data\": {}}")));

      var request =
          HttpRequest.get(
              URI.create(wmRuntimeInfo.getHttpBaseUrl() + "/graphql?query=%7Buser%7D"), Map.of());

      var response = transport.execute(request);

      assertThat(response.statusCode()).isEqualTo(200);
      assertThat(response.body()).isEqualTo("{\"data\": {}}");
    }

    @Test
    @DisplayName("should include request headers")
    void shouldIncludeRequestHeaders(WireMockRuntimeInfo wmRuntimeInfo) {
      stubFor(
          post(urlEqualTo("/graphql"))
              .withHeader("Authorization", equalTo("Bearer token123"))
              .withHeader("X-Custom-Header", equalTo("custom-value"))
              .willReturn(aResponse().withStatus(200)));

      var request =
          HttpRequest.post(
              URI.create(wmRuntimeInfo.getHttpBaseUrl() + "/graphql"),
              Map.of(
                  "Authorization", "Bearer token123",
                  "X-Custom-Header", "custom-value"),
              "{}");

      var response = transport.execute(request);

      assertThat(response.statusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("should handle 4xx error response")
    void shouldHandle4xxErrorResponse(WireMockRuntimeInfo wmRuntimeInfo) {
      stubFor(
          post(urlEqualTo("/graphql"))
              .willReturn(aResponse().withStatus(400).withBody("{\"errors\": []}")));

      var request =
          HttpRequest.post(URI.create(wmRuntimeInfo.getHttpBaseUrl() + "/graphql"), Map.of(), "{}");

      var response = transport.execute(request);

      assertThat(response.statusCode()).isEqualTo(400);
      assertThat(response.isClientError()).isTrue();
      assertThat(response.isSuccessful()).isFalse();
    }

    @Test
    @DisplayName("should handle 5xx error response")
    void shouldHandle5xxErrorResponse(WireMockRuntimeInfo wmRuntimeInfo) {
      stubFor(post(urlEqualTo("/graphql")).willReturn(aResponse().withStatus(500)));

      var request =
          HttpRequest.post(URI.create(wmRuntimeInfo.getHttpBaseUrl() + "/graphql"), Map.of(), "{}");

      var response = transport.execute(request);

      assertThat(response.statusCode()).isEqualTo(500);
      assertThat(response.isServerError()).isTrue();
    }

    @Test
    @DisplayName("should throw timeout exception on request timeout")
    void shouldThrowTimeoutExceptionOnRequestTimeout(WireMockRuntimeInfo wmRuntimeInfo) {
      stubFor(post(urlEqualTo("/graphql")).willReturn(aResponse().withFixedDelay(5000)));

      var config = HttpTransportConfig.builder().requestTimeout(Duration.ofMillis(100)).build();
      var shortTimeoutTransport = new DefaultHttpTransport(config);

      var request =
          HttpRequest.post(URI.create(wmRuntimeInfo.getHttpBaseUrl() + "/graphql"), Map.of(), "{}");

      assertThatThrownBy(() -> shortTimeoutTransport.execute(request))
          .isInstanceOf(GraphiteTimeoutException.class);
    }

    @Test
    @DisplayName("should throw connection exception on connection failure")
    void shouldThrowConnectionExceptionOnConnectionFailure() {
      var request = HttpRequest.post(URI.create("http://localhost:1"), Map.of(), "{}");

      assertThatThrownBy(() -> transport.execute(request))
          .isInstanceOf(GraphiteConnectionException.class)
          .satisfies(
              e -> {
                var connEx = (GraphiteConnectionException) e;
                assertThat(connEx.getHost()).isEqualTo("localhost");
                assertThat(connEx.getPort()).isEqualTo(1);
              });
    }

    @Test
    @DisplayName("should reject null request")
    void shouldRejectNullRequest() {
      assertThatThrownBy(() -> transport.execute(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessage("request must not be null");
    }

    @Test
    @DisplayName("should throw exception when closed")
    void shouldThrowExceptionWhenClosed(WireMockRuntimeInfo wmRuntimeInfo) {
      transport.close();

      var request =
          HttpRequest.post(URI.create(wmRuntimeInfo.getHttpBaseUrl() + "/graphql"), Map.of(), "{}");

      assertThatThrownBy(() -> transport.execute(request))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("Transport has been closed");
    }
  }

  @Nested
  @DisplayName("executeAsync")
  class ExecuteAsync {

    @Test
    @DisplayName("should execute async POST request")
    void shouldExecuteAsyncPostRequest(WireMockRuntimeInfo wmRuntimeInfo) throws Exception {
      stubFor(
          post(urlEqualTo("/graphql"))
              .willReturn(aResponse().withStatus(200).withBody("{\"data\": {}}")));

      var request =
          HttpRequest.post(URI.create(wmRuntimeInfo.getHttpBaseUrl() + "/graphql"), Map.of(), "{}");

      var response = transport.executeAsync(request).get();

      assertThat(response.statusCode()).isEqualTo(200);
      assertThat(response.body()).isEqualTo("{\"data\": {}}");
    }

    @Test
    @DisplayName("should handle async timeout exception")
    void shouldHandleAsyncTimeoutException(WireMockRuntimeInfo wmRuntimeInfo) {
      stubFor(post(urlEqualTo("/graphql")).willReturn(aResponse().withFixedDelay(5000)));

      var config = HttpTransportConfig.builder().requestTimeout(Duration.ofMillis(100)).build();
      var shortTimeoutTransport = new DefaultHttpTransport(config);

      var request =
          HttpRequest.post(URI.create(wmRuntimeInfo.getHttpBaseUrl() + "/graphql"), Map.of(), "{}");

      var future = shortTimeoutTransport.executeAsync(request);

      assertThatThrownBy(future::get)
          .isInstanceOf(ExecutionException.class)
          .hasCauseInstanceOf(GraphiteTimeoutException.class);
    }

    @Test
    @DisplayName("should reject null request in async")
    void shouldRejectNullRequestInAsync() {
      assertThatThrownBy(() -> transport.executeAsync(null))
          .isInstanceOf(NullPointerException.class)
          .hasMessage("request must not be null");
    }

    @Test
    @DisplayName("should throw exception when closed in async")
    void shouldThrowExceptionWhenClosedInAsync(WireMockRuntimeInfo wmRuntimeInfo) {
      transport.close();

      var request =
          HttpRequest.post(URI.create(wmRuntimeInfo.getHttpBaseUrl() + "/graphql"), Map.of(), "{}");

      assertThatThrownBy(() -> transport.executeAsync(request))
          .isInstanceOf(IllegalStateException.class)
          .hasMessage("Transport has been closed");
    }
  }

  @Nested
  @DisplayName("close")
  class Close {

    @Test
    @DisplayName("should mark transport as closed")
    void shouldMarkTransportAsClosed() {
      assertThat(transport.isClosed()).isFalse();

      transport.close();

      assertThat(transport.isClosed()).isTrue();
    }

    @Test
    @DisplayName("should be idempotent")
    void shouldBeIdempotent() {
      transport.close();
      transport.close();

      assertThat(transport.isClosed()).isTrue();
    }
  }

  @Nested
  @DisplayName("POST request with null body")
  class PostWithNullBody {

    @Test
    @DisplayName("should handle POST request with null body")
    void shouldHandlePostRequestWithNullBody(WireMockRuntimeInfo wmRuntimeInfo) {
      stubFor(post(urlEqualTo("/graphql")).willReturn(aResponse().withStatus(200)));

      var request =
          new HttpRequest(
              HttpMethod.POST,
              URI.create(wmRuntimeInfo.getHttpBaseUrl() + "/graphql"),
              Map.of(),
              null);

      var response = transport.execute(request);

      assertThat(response.statusCode()).isEqualTo(200);
    }
  }

  @Nested
  @DisplayName("response headers")
  class ResponseHeaders {

    @Test
    @DisplayName("should capture multiple response headers")
    void shouldCaptureMultipleResponseHeaders(WireMockRuntimeInfo wmRuntimeInfo) {
      stubFor(
          post(urlEqualTo("/graphql"))
              .willReturn(
                  aResponse()
                      .withStatus(200)
                      .withHeader("Content-Type", "application/json")
                      .withHeader("X-Request-Id", "abc123")
                      .withHeader("Cache-Control", "no-cache")));

      var request =
          HttpRequest.post(URI.create(wmRuntimeInfo.getHttpBaseUrl() + "/graphql"), Map.of(), "{}");

      var response = transport.execute(request);

      assertThat(response.getHeader("Content-Type")).isEqualTo("application/json");
      assertThat(response.getHeader("X-Request-Id")).isEqualTo("abc123");
      assertThat(response.getHeader("Cache-Control")).isEqualTo("no-cache");
    }
  }
}
