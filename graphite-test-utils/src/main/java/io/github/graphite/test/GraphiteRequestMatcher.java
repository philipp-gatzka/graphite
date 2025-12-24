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

import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.matchingJsonPath;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.client.MappingBuilder;
import com.github.tomakehurst.wiremock.matching.StringValuePattern;
import java.util.ArrayList;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Type-safe request matcher for GraphQL operations.
 *
 * <p>This class provides a fluent API for building request matchers that can be used with {@link
 * GraphiteMockServer} for advanced stubbing scenarios.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * GraphiteRequestMatcher matcher = GraphiteRequestMatcher.forOperation("GetUser")
 *     .withVariable("id", "123")
 *     .withQueryContaining("email");
 *
 * server.stub(matcher, Map.of("id", "123", "name", "John"));
 * }</pre>
 *
 * @see GraphiteMockServer
 */
public class GraphiteRequestMatcher {

  private static final String GRAPHQL_PATH = "/graphql";

  private final @Nullable String operationName;
  private final List<VariableMatcher> variableMatchers;
  private final List<StringValuePattern> queryPatterns;
  private final List<HeaderMatcher> headerMatchers;

  private GraphiteRequestMatcher(@Nullable String operationName) {
    this.operationName = operationName;
    this.variableMatchers = new ArrayList<>();
    this.queryPatterns = new ArrayList<>();
    this.headerMatchers = new ArrayList<>();
  }

  /**
   * Creates a matcher for the specified operation name.
   *
   * @param operationName the GraphQL operation name
   * @return a new request matcher
   */
  @NotNull
  public static GraphiteRequestMatcher forOperation(@NotNull String operationName) {
    return new GraphiteRequestMatcher(operationName);
  }

  /**
   * Creates a matcher that matches any operation.
   *
   * @return a new request matcher for any operation
   */
  @NotNull
  public static GraphiteRequestMatcher anyOperation() {
    return new GraphiteRequestMatcher(null);
  }

  /**
   * Creates a matcher for a query operation.
   *
   * @param operationName the query operation name
   * @return a new request matcher
   */
  @NotNull
  public static GraphiteRequestMatcher forQuery(@NotNull String operationName) {
    return forOperation(operationName);
  }

  /**
   * Creates a matcher for a mutation operation.
   *
   * @param operationName the mutation operation name
   * @return a new request matcher
   */
  @NotNull
  public static GraphiteRequestMatcher forMutation(@NotNull String operationName) {
    return forOperation(operationName);
  }

  /**
   * Adds a variable matcher that requires an exact match.
   *
   * @param name the variable name
   * @param value the expected value
   * @return this matcher for chaining
   */
  @NotNull
  public GraphiteRequestMatcher withVariable(@NotNull String name, @NotNull String value) {
    variableMatchers.add(new VariableMatcher(name, equalTo(value)));
    return this;
  }

  /**
   * Adds a variable matcher that requires a numeric value.
   *
   * @param name the variable name
   * @param value the expected numeric value
   * @return this matcher for chaining
   */
  @NotNull
  public GraphiteRequestMatcher withVariable(@NotNull String name, @NotNull Number value) {
    variableMatchers.add(new VariableMatcher(name, equalTo(value.toString())));
    return this;
  }

  /**
   * Adds a variable matcher that requires a boolean value.
   *
   * @param name the variable name
   * @param value the expected boolean value
   * @return this matcher for chaining
   */
  @NotNull
  public GraphiteRequestMatcher withVariable(@NotNull String name, boolean value) {
    variableMatchers.add(new VariableMatcher(name, equalTo(String.valueOf(value))));
    return this;
  }

  /**
   * Adds a variable matcher with a custom pattern.
   *
   * @param name the variable name
   * @param pattern the value pattern to match
   * @return this matcher for chaining
   */
  @NotNull
  public GraphiteRequestMatcher withVariable(
      @NotNull String name, @NotNull StringValuePattern pattern) {
    variableMatchers.add(new VariableMatcher(name, pattern));
    return this;
  }

  /**
   * Adds a matcher that requires the query to contain the specified text.
   *
   * @param text the text that must be present in the query
   * @return this matcher for chaining
   */
  @NotNull
  public GraphiteRequestMatcher withQueryContaining(@NotNull String text) {
    queryPatterns.add(com.github.tomakehurst.wiremock.client.WireMock.containing(text));
    return this;
  }

  /**
   * Adds a matcher that requires the query to match the specified regex.
   *
   * @param regex the regex pattern the query must match
   * @return this matcher for chaining
   */
  @NotNull
  public GraphiteRequestMatcher withQueryMatching(@NotNull String regex) {
    queryPatterns.add(com.github.tomakehurst.wiremock.client.WireMock.matching(regex));
    return this;
  }

  /**
   * Adds a header matcher that requires an exact match.
   *
   * @param name the header name
   * @param value the expected header value
   * @return this matcher for chaining
   */
  @NotNull
  public GraphiteRequestMatcher withHeader(@NotNull String name, @NotNull String value) {
    headerMatchers.add(new HeaderMatcher(name, equalTo(value)));
    return this;
  }

  /**
   * Adds a header matcher with a custom pattern.
   *
   * @param name the header name
   * @param pattern the value pattern to match
   * @return this matcher for chaining
   */
  @NotNull
  public GraphiteRequestMatcher withHeader(
      @NotNull String name, @NotNull StringValuePattern pattern) {
    headerMatchers.add(new HeaderMatcher(name, pattern));
    return this;
  }

  /**
   * Adds a matcher for the Authorization header.
   *
   * @param value the expected Authorization header value
   * @return this matcher for chaining
   */
  @NotNull
  public GraphiteRequestMatcher withAuthorization(@NotNull String value) {
    return withHeader("Authorization", value);
  }

  /**
   * Adds a matcher for Bearer token authorization.
   *
   * @param token the expected bearer token
   * @return this matcher for chaining
   */
  @NotNull
  public GraphiteRequestMatcher withBearerToken(@NotNull String token) {
    return withHeader("Authorization", "Bearer " + token);
  }

  /**
   * Returns the operation name, if set.
   *
   * @return the operation name or null if matching any operation
   */
  @Nullable
  public String getOperationName() {
    return operationName;
  }

  /**
   * Builds a WireMock MappingBuilder for this matcher.
   *
   * @return the WireMock mapping builder
   */
  @NotNull
  public MappingBuilder toMappingBuilder() {
    MappingBuilder builder = post(urlEqualTo(GRAPHQL_PATH));

    // Match operation name if specified
    if (operationName != null) {
      builder.withRequestBody(matchingJsonPath("$.operationName", equalTo(operationName)));
    }

    // Add variable matchers
    for (VariableMatcher vm : variableMatchers) {
      builder.withRequestBody(matchingJsonPath("$.variables." + vm.name(), vm.pattern()));
    }

    // Add query pattern matchers
    for (StringValuePattern pattern : queryPatterns) {
      builder.withRequestBody(matchingJsonPath("$.query", pattern));
    }

    // Add header matchers
    for (HeaderMatcher hm : headerMatchers) {
      builder.withHeader(hm.name(), hm.pattern());
    }

    return builder;
  }

  /** Internal record for variable matching. */
  private record VariableMatcher(@NotNull String name, @NotNull StringValuePattern pattern) {}

  /** Internal record for header matching. */
  private record HeaderMatcher(@NotNull String name, @NotNull StringValuePattern pattern) {}
}
