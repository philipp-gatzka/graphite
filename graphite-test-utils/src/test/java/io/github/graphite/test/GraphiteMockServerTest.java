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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.graphite.GraphQLError;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GraphiteMockServer")
class GraphiteMockServerTest {

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
  @DisplayName("create")
  class Create {

    @Test
    @DisplayName("should allocate dynamic port")
    void shouldAllocateDynamicPort() {
      assertThat(server.getPort()).isPositive();
    }

    @Test
    @DisplayName("should create server on specified port")
    void shouldCreateServerOnSpecifiedPort() {
      try (GraphiteMockServer specificServer = GraphiteMockServer.create(9999)) {
        assertThat(specificServer.getPort()).isEqualTo(9999);
      }
    }

    @Test
    @DisplayName("should return correct url")
    void shouldReturnCorrectUrl() {
      assertThat(server.getUrl()).isEqualTo("http://localhost:" + server.getPort() + "/graphql");
    }
  }

  @Nested
  @DisplayName("stubQuery")
  class StubQuery {

    @Test
    @DisplayName("should stub query with response data")
    void shouldStubQueryWithResponseData() throws Exception {
      server.stubQuery("GetUser", Map.of("id", "1", "name", "John"));

      String response =
          sendGraphQLRequest("{\"operationName\":\"GetUser\",\"query\":\"{ user }\"}");

      assertThat(response)
          .contains("\"data\"")
          .contains("\"id\":\"1\"")
          .contains("\"name\":\"John\"");
    }

    @Test
    @DisplayName("should stub query with null data")
    void shouldStubQueryWithNullData() throws Exception {
      server.stubQuery("GetUser", null);

      String response =
          sendGraphQLRequest("{\"operationName\":\"GetUser\",\"query\":\"{ user }\"}");

      assertThat(response).contains("\"data\":null");
    }
  }

  @Nested
  @DisplayName("stubMutation")
  class StubMutation {

    @Test
    @DisplayName("should stub mutation with response data")
    void shouldStubMutationWithResponseData() throws Exception {
      server.stubMutation("CreateUser", Map.of("id", "2", "name", "Jane"));

      String response =
          sendGraphQLRequest(
              "{\"operationName\":\"CreateUser\",\"query\":\"mutation { createUser }\"}");

      assertThat(response).contains("\"data\"");
      assertThat(response).contains("\"id\":\"2\"");
    }
  }

  @Nested
  @DisplayName("stubError")
  class StubError {

    @Test
    @DisplayName("should stub with GraphQL errors")
    void shouldStubWithGraphQLErrors() throws Exception {
      GraphQLError error =
          new GraphQLError("User not found", List.of(), List.of(), Map.of("code", "NOT_FOUND"));
      server.stubError("GetUser", error);

      String response =
          sendGraphQLRequest("{\"operationName\":\"GetUser\",\"query\":\"{ user }\"}");

      assertThat(response).contains("\"errors\"").contains("User not found");
    }
  }

  @Nested
  @DisplayName("stubHttpError")
  class StubHttpError {

    @Test
    @DisplayName("should return HTTP error status")
    void shouldReturnHttpErrorStatus() throws Exception {
      server.stubHttpError("GetUser", 500);

      HttpResponse<String> response =
          sendRawGraphQLRequest("{\"operationName\":\"GetUser\",\"query\":\"{ user }\"}");

      assertThat(response.statusCode()).isEqualTo(500);
    }
  }

  @Nested
  @DisplayName("stubWithDelay")
  class StubWithDelay {

    @Test
    @DisplayName("should delay response")
    void shouldDelayResponse() throws Exception {
      server.stubWithDelay("GetUser", 100, Map.of("id", "1"));

      long startTime = System.currentTimeMillis();
      sendGraphQLRequest("{\"operationName\":\"GetUser\",\"query\":\"{ user }\"}");
      long duration = System.currentTimeMillis() - startTime;

      assertThat(duration).isGreaterThanOrEqualTo(100);
    }
  }

  @Nested
  @DisplayName("stubAny")
  class StubAny {

    @Test
    @DisplayName("should match any operation")
    void shouldMatchAnyOperation() throws Exception {
      server.stubAny(Map.of("result", "ok"));

      String response = sendGraphQLRequest("{\"operationName\":\"AnyOp\",\"query\":\"{ any }\"}");

      assertThat(response).contains("\"result\":\"ok\"");
    }
  }

  @Nested
  @DisplayName("verify")
  class Verify {

    @Test
    @DisplayName("should verify exact call count")
    void shouldVerifyExactCallCount() throws Exception {
      server.stubQuery("GetUser", Map.of("id", "1"));

      sendGraphQLRequest("{\"operationName\":\"GetUser\",\"query\":\"{ user }\"}");
      sendGraphQLRequest("{\"operationName\":\"GetUser\",\"query\":\"{ user }\"}");

      server.verify("GetUser", 2);
    }

    @Test
    @DisplayName("should fail when call count doesn't match")
    void shouldFailWhenCallCountDoesntMatch() throws Exception {
      server.stubQuery("GetUser", Map.of("id", "1"));

      sendGraphQLRequest("{\"operationName\":\"GetUser\",\"query\":\"{ user }\"}");

      assertThatThrownBy(() -> server.verify("GetUser", 2))
          .isInstanceOf(com.github.tomakehurst.wiremock.client.VerificationException.class);
    }
  }

  @Nested
  @DisplayName("reset")
  class Reset {

    @Test
    @DisplayName("should clear all stubs")
    void shouldClearAllStubs() throws Exception {
      server.stubQuery("GetUser", Map.of("id", "1"));
      server.reset();

      // After reset, the stub should no longer work
      HttpResponse<String> response =
          sendRawGraphQLRequest("{\"operationName\":\"GetUser\",\"query\":\"{ user }\"}");

      // WireMock returns 404 for unmatched requests
      assertThat(response.statusCode()).isNotEqualTo(200);
    }
  }

  @Nested
  @DisplayName("close")
  class Close {

    @Test
    @DisplayName("should stop the server")
    void shouldStopTheServer() {
      server.close();

      assertThat(server.getWireMockServer().isRunning()).isFalse();
    }
  }

  @Nested
  @DisplayName("getWireMockServer")
  class GetWireMockServer {

    @Test
    @DisplayName("should return underlying WireMock server")
    void shouldReturnUnderlyingWireMockServer() {
      assertThat(server.getWireMockServer()).isNotNull();
      assertThat(server.getWireMockServer().isRunning()).isTrue();
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
}
