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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.matching.RequestPatternBuilder;
import io.github.graphite.GraphQLError;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A mock GraphQL server for testing Graphite client code.
 *
 * <p>This class provides a simple API for stubbing GraphQL queries and mutations, verifying
 * requests, and testing error scenarios. It wraps WireMock for HTTP stubbing with GraphQL-aware
 * matchers.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * try (GraphiteMockServer server = GraphiteMockServer.create()) {
 *     server.stubQuery("GetUser", Map.of("id", "1", "name", "John"));
 *
 *     // Execute query with client pointing to server.getUrl()
 *     // ...
 *
 *     server.verify("GetUser", 1);
 * }
 * }</pre>
 *
 * <p>The server automatically allocates a free port unless explicitly specified. Resources are
 * automatically cleaned up when the server is closed.
 *
 * @see GraphiteResponseBuilder
 * @see GraphiteRequestMatcher
 */
public class GraphiteMockServer implements AutoCloseable {

  private static final String GRAPHQL_PATH = "/graphql";
  private static final String CONTENT_TYPE_JSON = "application/json";

  private final WireMockServer wireMockServer;
  private final ObjectMapper objectMapper;

  /**
   * Creates a mock server with the given WireMock configuration.
   *
   * @param configuration the WireMock configuration
   */
  private GraphiteMockServer(@NotNull WireMockConfiguration configuration) {
    this.wireMockServer = new WireMockServer(configuration);
    this.objectMapper = new ObjectMapper();
    this.wireMockServer.start();
    WireMock.configureFor("localhost", wireMockServer.port());
  }

  /**
   * Creates a new mock server with an automatically allocated port.
   *
   * @return the mock server
   */
  @NotNull
  public static GraphiteMockServer create() {
    return new GraphiteMockServer(WireMockConfiguration.options().dynamicPort());
  }

  /**
   * Creates a new mock server on the specified port.
   *
   * @param port the port to listen on
   * @return the mock server
   */
  @NotNull
  public static GraphiteMockServer create(int port) {
    return new GraphiteMockServer(WireMockConfiguration.options().port(port));
  }

  /**
   * Returns the base URL of the mock server.
   *
   * @return the URL (e.g., "http://localhost:8080/graphql")
   */
  @NotNull
  public String getUrl() {
    return "http://localhost:" + wireMockServer.port() + GRAPHQL_PATH;
  }

  /**
   * Returns the port the mock server is listening on.
   *
   * @return the port number
   */
  public int getPort() {
    return wireMockServer.port();
  }

  /**
   * Stubs a GraphQL query to return the given response data.
   *
   * @param operationName the name of the query operation
   * @param data the response data object (will be serialized to JSON)
   */
  public void stubQuery(@NotNull String operationName, @Nullable Object data) {
    stubOperation(operationName, data, null);
  }

  /**
   * Stubs a GraphQL mutation to return the given response data.
   *
   * @param operationName the name of the mutation operation
   * @param data the response data object (will be serialized to JSON)
   */
  public void stubMutation(@NotNull String operationName, @Nullable Object data) {
    stubOperation(operationName, data, null);
  }

  /**
   * Stubs a GraphQL operation to return the given errors.
   *
   * @param operationName the name of the operation
   * @param errors the errors to return
   */
  public void stubError(@NotNull String operationName, @NotNull GraphQLError... errors) {
    stubOperation(operationName, null, List.of(errors));
  }

  /**
   * Stubs a GraphQL operation with both data and errors.
   *
   * @param operationName the name of the operation
   * @param data the response data (may be null)
   * @param errors the errors (may be null or empty)
   */
  public void stubOperation(
      @NotNull String operationName, @Nullable Object data, @Nullable List<GraphQLError> errors) {
    try {
      String responseBody = buildResponseBody(data, errors);

      wireMockServer.stubFor(
          post(urlEqualTo(GRAPHQL_PATH))
              .withRequestBody(matchingJsonPath("$.operationName", WireMock.equalTo(operationName)))
              .willReturn(
                  aResponse()
                      .withStatus(200)
                      .withHeader("Content-Type", CONTENT_TYPE_JSON)
                      .withBody(responseBody)));
    } catch (JsonProcessingException e) {
      throw new GraphiteTestException("Failed to serialize response", e);
    }
  }

  /**
   * Stubs any GraphQL operation to return the given response.
   *
   * @param data the response data
   */
  public void stubAny(@Nullable Object data) {
    try {
      String responseBody = buildResponseBody(data, null);

      wireMockServer.stubFor(
          post(urlEqualTo(GRAPHQL_PATH))
              .willReturn(
                  aResponse()
                      .withStatus(200)
                      .withHeader("Content-Type", CONTENT_TYPE_JSON)
                      .withBody(responseBody)));
    } catch (JsonProcessingException e) {
      throw new GraphiteTestException("Failed to serialize response", e);
    }
  }

  /**
   * Stubs a GraphQL operation to return an HTTP error.
   *
   * @param operationName the name of the operation
   * @param statusCode the HTTP status code
   */
  public void stubHttpError(@NotNull String operationName, int statusCode) {
    wireMockServer.stubFor(
        post(urlEqualTo(GRAPHQL_PATH))
            .withRequestBody(matchingJsonPath("$.operationName", WireMock.equalTo(operationName)))
            .willReturn(aResponse().withStatus(statusCode)));
  }

  /**
   * Stubs a GraphQL operation to cause a network delay.
   *
   * @param operationName the name of the operation
   * @param delayMillis the delay in milliseconds
   * @param data the response data
   */
  public void stubWithDelay(@NotNull String operationName, int delayMillis, @Nullable Object data) {
    try {
      String responseBody = buildResponseBody(data, null);

      wireMockServer.stubFor(
          post(urlEqualTo(GRAPHQL_PATH))
              .withRequestBody(matchingJsonPath("$.operationName", WireMock.equalTo(operationName)))
              .willReturn(
                  aResponse()
                      .withStatus(200)
                      .withHeader("Content-Type", CONTENT_TYPE_JSON)
                      .withBody(responseBody)
                      .withFixedDelay(delayMillis)));
    } catch (JsonProcessingException e) {
      throw new GraphiteTestException("Failed to serialize response", e);
    }
  }

  /**
   * Stubs a GraphQL operation using a custom request matcher.
   *
   * <p>This method provides more control over request matching than the simple operation name-based
   * methods. Use {@link GraphiteRequestMatcher} to build complex matching rules including variable
   * matching, query content matching, and header matching.
   *
   * <p>Example:
   *
   * <pre>{@code
   * GraphiteRequestMatcher matcher = GraphiteRequestMatcher.forOperation("GetUser")
   *     .withVariable("id", "123")
   *     .withBearerToken("secret-token");
   *
   * server.stub(matcher, Map.of("id", "123", "name", "John"));
   * }</pre>
   *
   * @param matcher the request matcher
   * @param data the response data
   */
  public void stub(@NotNull GraphiteRequestMatcher matcher, @Nullable Object data) {
    stub(matcher, data, null);
  }

  /**
   * Stubs a GraphQL operation using a custom request matcher with errors.
   *
   * @param matcher the request matcher
   * @param data the response data (may be null)
   * @param errors the errors (may be null or empty)
   */
  public void stub(
      @NotNull GraphiteRequestMatcher matcher,
      @Nullable Object data,
      @Nullable List<GraphQLError> errors) {
    try {
      String responseBody = buildResponseBody(data, errors);

      wireMockServer.stubFor(
          matcher
              .toMappingBuilder()
              .willReturn(
                  aResponse()
                      .withStatus(200)
                      .withHeader("Content-Type", CONTENT_TYPE_JSON)
                      .withBody(responseBody)));
    } catch (JsonProcessingException e) {
      throw new GraphiteTestException("Failed to serialize response", e);
    }
  }

  /**
   * Stubs a GraphQL operation using a custom request matcher to return errors.
   *
   * @param matcher the request matcher
   * @param errors the errors to return
   */
  public void stubError(@NotNull GraphiteRequestMatcher matcher, @NotNull GraphQLError... errors) {
    stub(matcher, null, List.of(errors));
  }

  /**
   * Stubs a GraphQL operation using a custom request matcher with delay.
   *
   * @param matcher the request matcher
   * @param delayMillis the delay in milliseconds
   * @param data the response data
   */
  public void stubWithDelay(
      @NotNull GraphiteRequestMatcher matcher, int delayMillis, @Nullable Object data) {
    try {
      String responseBody = buildResponseBody(data, null);

      wireMockServer.stubFor(
          matcher
              .toMappingBuilder()
              .willReturn(
                  aResponse()
                      .withStatus(200)
                      .withHeader("Content-Type", CONTENT_TYPE_JSON)
                      .withBody(responseBody)
                      .withFixedDelay(delayMillis)));
    } catch (JsonProcessingException e) {
      throw new GraphiteTestException("Failed to serialize response", e);
    }
  }

  /**
   * Stubs a GraphQL operation using a custom request matcher to return an HTTP error.
   *
   * @param matcher the request matcher
   * @param statusCode the HTTP status code
   */
  public void stubHttpError(@NotNull GraphiteRequestMatcher matcher, int statusCode) {
    wireMockServer.stubFor(
        matcher.toMappingBuilder().willReturn(aResponse().withStatus(statusCode)));
  }

  /**
   * Verifies that the specified operation was called the expected number of times.
   *
   * @param operationName the name of the operation
   * @param times the expected number of invocations
   */
  public void verify(@NotNull String operationName, int times) {
    RequestPatternBuilder pattern =
        postRequestedFor(urlEqualTo(GRAPHQL_PATH))
            .withRequestBody(matchingJsonPath("$.operationName", WireMock.equalTo(operationName)));

    WireMock.verify(times, pattern);
  }

  /**
   * Verifies that the specified operation was called at least once.
   *
   * @param operationName the name of the operation
   */
  public void verifyCalled(@NotNull String operationName) {
    RequestPatternBuilder pattern =
        postRequestedFor(urlEqualTo(GRAPHQL_PATH))
            .withRequestBody(matchingJsonPath("$.operationName", WireMock.equalTo(operationName)));

    WireMock.verify(pattern);
  }

  /**
   * Verifies that no unmatched requests were made.
   *
   * @throws AssertionError if there are unmatched requests
   */
  public void verifyNoMoreInteractions() {
    var unmatched = WireMock.findUnmatchedRequests();
    if (!unmatched.isEmpty()) {
      throw new AssertionError("Found " + unmatched.size() + " unmatched requests: " + unmatched);
    }
  }

  /** Resets all stubs and request history. */
  public void reset() {
    wireMockServer.resetAll();
  }

  /** Stops the mock server and releases resources. */
  @Override
  public void close() {
    if (wireMockServer.isRunning()) {
      wireMockServer.stop();
    }
  }

  /**
   * Returns the underlying WireMock server for advanced configuration.
   *
   * @return the WireMock server
   */
  @NotNull
  public WireMockServer getWireMockServer() {
    return wireMockServer;
  }

  /**
   * Builds a GraphQL response body from data and errors.
   *
   * @param data the response data
   * @param errors the errors
   * @return the JSON response body
   */
  private String buildResponseBody(@Nullable Object data, @Nullable List<GraphQLError> errors)
      throws JsonProcessingException {
    GraphQLResponse response = new GraphQLResponse(data, errors);
    return objectMapper.writeValueAsString(response);
  }

  /** Internal record for GraphQL response structure. */
  private record GraphQLResponse(@Nullable Object data, @Nullable List<GraphQLError> errors) {}
}
