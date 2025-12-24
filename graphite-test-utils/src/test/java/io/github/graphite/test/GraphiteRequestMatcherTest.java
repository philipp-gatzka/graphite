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
package io.github.graphite.test;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GraphiteRequestMatcher")
class GraphiteRequestMatcherTest {

  private GraphiteMockServer server;
  private HttpClient httpClient;

  @BeforeEach
  void setUp() {
    server = GraphiteMockServer.create();
    httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
  }

  @AfterEach
  void tearDown() {
    if (server != null) {
      server.close();
    }
  }

  @Nested
  @DisplayName("forOperation")
  class ForOperation {

    @Test
    @DisplayName("should create matcher with operation name")
    void shouldCreateMatcherWithOperationName() {
      GraphiteRequestMatcher matcher = GraphiteRequestMatcher.forOperation("GetUser");

      assertThat(matcher.getOperationName()).isEqualTo("GetUser");
    }
  }

  @Nested
  @DisplayName("anyOperation")
  class AnyOperation {

    @Test
    @DisplayName("should create matcher without operation name")
    void shouldCreateMatcherWithoutOperationName() {
      GraphiteRequestMatcher matcher = GraphiteRequestMatcher.anyOperation();

      assertThat(matcher.getOperationName()).isNull();
    }
  }

  @Nested
  @DisplayName("forQuery")
  class ForQuery {

    @Test
    @DisplayName("should create matcher for query operation")
    void shouldCreateMatcherForQueryOperation() {
      GraphiteRequestMatcher matcher = GraphiteRequestMatcher.forQuery("GetUser");

      assertThat(matcher.getOperationName()).isEqualTo("GetUser");
    }
  }

  @Nested
  @DisplayName("forMutation")
  class ForMutation {

    @Test
    @DisplayName("should create matcher for mutation operation")
    void shouldCreateMatcherForMutationOperation() {
      GraphiteRequestMatcher matcher = GraphiteRequestMatcher.forMutation("CreateUser");

      assertThat(matcher.getOperationName()).isEqualTo("CreateUser");
    }
  }

  @Nested
  @DisplayName("withVariable")
  class WithVariable {

    @Test
    @DisplayName("should match request with string variable")
    void shouldMatchRequestWithStringVariable() throws Exception {
      GraphiteRequestMatcher matcher =
          GraphiteRequestMatcher.forOperation("GetUser").withVariable("id", "123");

      server.stub(matcher, Map.of("id", "123", "name", "John"));

      String response =
          sendGraphQLRequest(
              "{\"operationName\":\"GetUser\",\"query\":\"{ user }\",\"variables\":{\"id\":\"123\"}}");

      assertThat(response).contains("\"name\":\"John\"");
    }

    @Test
    @DisplayName("should not match request with different variable value")
    void shouldNotMatchRequestWithDifferentVariableValue() throws Exception {
      GraphiteRequestMatcher matcher =
          GraphiteRequestMatcher.forOperation("GetUser").withVariable("id", "123");

      server.stub(matcher, Map.of("id", "123", "name", "John"));

      // Different variable value should not match
      HttpResponse<String> response =
          sendRawGraphQLRequest(
              "{\"operationName\":\"GetUser\",\"query\":\"{ user }\",\"variables\":{\"id\":\"456\"}}");

      // WireMock returns 404 for unmatched requests
      assertThat(response.statusCode()).isNotEqualTo(200);
    }

    @Test
    @DisplayName("should match request with numeric variable")
    void shouldMatchRequestWithNumericVariable() throws Exception {
      GraphiteRequestMatcher matcher =
          GraphiteRequestMatcher.forOperation("GetUser").withVariable("limit", 10);

      server.stub(matcher, Map.of("users", "list"));

      String response =
          sendGraphQLRequest(
              "{\"operationName\":\"GetUser\",\"query\":\"{ users }\",\"variables\":{\"limit\":10}}");

      assertThat(response).contains("\"users\":\"list\"");
    }

    @Test
    @DisplayName("should match request with boolean variable")
    void shouldMatchRequestWithBooleanVariable() throws Exception {
      GraphiteRequestMatcher matcher =
          GraphiteRequestMatcher.forOperation("GetUser").withVariable("active", true);

      server.stub(matcher, Map.of("result", "ok"));

      String response =
          sendGraphQLRequest(
              "{\"operationName\":\"GetUser\",\"query\":\"{ users }\",\"variables\":{\"active\":true}}");

      assertThat(response).contains("\"result\":\"ok\"");
    }

    @Test
    @DisplayName("should match request with multiple variables")
    void shouldMatchRequestWithMultipleVariables() throws Exception {
      GraphiteRequestMatcher matcher =
          GraphiteRequestMatcher.forOperation("GetUsers")
              .withVariable("limit", 10)
              .withVariable("offset", 0)
              .withVariable("active", true);

      server.stub(matcher, Map.of("users", "list"));

      String response =
          sendGraphQLRequest(
              "{\"operationName\":\"GetUsers\",\"query\":\"{ users }\","
                  + "\"variables\":{\"limit\":10,\"offset\":0,\"active\":true}}");

      assertThat(response).contains("\"users\":\"list\"");
    }
  }

  @Nested
  @DisplayName("withQueryContaining")
  class WithQueryContaining {

    @Test
    @DisplayName("should match request with query containing text")
    void shouldMatchRequestWithQueryContainingText() throws Exception {
      GraphiteRequestMatcher matcher =
          GraphiteRequestMatcher.forOperation("GetUser").withQueryContaining("email");

      server.stub(matcher, Map.of("email", "john@example.com"));

      String response =
          sendGraphQLRequest(
              "{\"operationName\":\"GetUser\",\"query\":\"{ user { id name email } }\"}");

      assertThat(response).contains("\"email\":\"john@example.com\"");
    }
  }

  @Nested
  @DisplayName("withQueryMatching")
  class WithQueryMatching {

    @Test
    @DisplayName("should match request with query matching regex")
    void shouldMatchRequestWithQueryMatchingRegex() throws Exception {
      GraphiteRequestMatcher matcher =
          GraphiteRequestMatcher.forOperation("GetUser").withQueryMatching(".*user.*id.*");

      server.stub(matcher, Map.of("id", "123"));

      String response =
          sendGraphQLRequest("{\"operationName\":\"GetUser\",\"query\":\"{ user { id name } }\"}");

      assertThat(response).contains("\"id\":\"123\"");
    }
  }

  @Nested
  @DisplayName("withHeader")
  class WithHeader {

    @Test
    @DisplayName("should match request with specific header")
    void shouldMatchRequestWithSpecificHeader() throws Exception {
      GraphiteRequestMatcher matcher =
          GraphiteRequestMatcher.forOperation("GetUser")
              .withHeader("X-Custom-Header", "custom-value");

      server.stub(matcher, Map.of("custom", "response"));

      String response =
          sendGraphQLRequestWithHeader(
              "{\"operationName\":\"GetUser\",\"query\":\"{ user }\"}",
              "X-Custom-Header",
              "custom-value");

      assertThat(response).contains("\"custom\":\"response\"");
    }
  }

  @Nested
  @DisplayName("withAuthorization")
  class WithAuthorization {

    @Test
    @DisplayName("should match request with authorization header")
    void shouldMatchRequestWithAuthorizationHeader() throws Exception {
      GraphiteRequestMatcher matcher =
          GraphiteRequestMatcher.forOperation("GetUser").withAuthorization("Bearer secret-token");

      server.stub(matcher, Map.of("authorized", "true"));

      String response =
          sendGraphQLRequestWithHeader(
              "{\"operationName\":\"GetUser\",\"query\":\"{ user }\"}",
              "Authorization",
              "Bearer secret-token");

      assertThat(response).contains("\"authorized\":\"true\"");
    }
  }

  @Nested
  @DisplayName("withBearerToken")
  class WithBearerToken {

    @Test
    @DisplayName("should match request with bearer token")
    void shouldMatchRequestWithBearerToken() throws Exception {
      GraphiteRequestMatcher matcher =
          GraphiteRequestMatcher.forOperation("GetUser").withBearerToken("my-token");

      server.stub(matcher, Map.of("token", "valid"));

      String response =
          sendGraphQLRequestWithHeader(
              "{\"operationName\":\"GetUser\",\"query\":\"{ user }\"}",
              "Authorization",
              "Bearer my-token");

      assertThat(response).contains("\"token\":\"valid\"");
    }
  }

  @Nested
  @DisplayName("stub with matcher")
  class StubWithMatcher {

    @Test
    @DisplayName("should stub with delay using matcher")
    void shouldStubWithDelayUsingMatcher() throws Exception {
      GraphiteRequestMatcher matcher = GraphiteRequestMatcher.forOperation("GetUser");

      server.stubWithDelay(matcher, 100, Map.of("id", "1"));

      long startTime = System.currentTimeMillis();
      sendGraphQLRequest("{\"operationName\":\"GetUser\",\"query\":\"{ user }\"}");
      long duration = System.currentTimeMillis() - startTime;

      assertThat(duration).isGreaterThanOrEqualTo(100);
    }

    @Test
    @DisplayName("should stub HTTP error using matcher")
    void shouldStubHttpErrorUsingMatcher() throws Exception {
      GraphiteRequestMatcher matcher = GraphiteRequestMatcher.forOperation("GetUser");

      server.stubHttpError(matcher, 503);

      HttpResponse<String> response =
          sendRawGraphQLRequest("{\"operationName\":\"GetUser\",\"query\":\"{ user }\"}");

      assertThat(response.statusCode()).isEqualTo(503);
    }
  }

  private String sendGraphQLRequest(String body) throws IOException, InterruptedException {
    return sendRawGraphQLRequest(body).body();
  }

  private HttpResponse<String> sendRawGraphQLRequest(String body)
      throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(server.getUrl()))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

    return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }

  private String sendGraphQLRequestWithHeader(String body, String headerName, String headerValue)
      throws IOException, InterruptedException {
    HttpRequest request =
        HttpRequest.newBuilder()
            .uri(URI.create(server.getUrl()))
            .header("Content-Type", "application/json")
            .header(headerName, headerValue)
            .POST(HttpRequest.BodyPublishers.ofString(body))
            .build();

    return httpClient.send(request, HttpResponse.BodyHandlers.ofString()).body();
  }
}
