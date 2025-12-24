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

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GraphQLOperation")
class GraphQLOperationTest {

  @Nested
  @DisplayName("default variables")
  class DefaultVariables {

    @Test
    @DisplayName("should return empty map by default")
    void shouldReturnEmptyMapByDefault() {
      GraphQLOperation<String> operation = new TestOperation();

      assertThat(operation.variables()).isEmpty();
    }
  }

  @Nested
  @DisplayName("custom implementation")
  class CustomImplementation {

    @Test
    @DisplayName("should return operation name")
    void shouldReturnOperationName() {
      GraphQLOperation<String> operation = new TestOperation();

      assertThat(operation.operationName()).isEqualTo("TestQuery");
    }

    @Test
    @DisplayName("should return GraphQL query")
    void shouldReturnGraphQLQuery() {
      GraphQLOperation<String> operation = new TestOperation();

      assertThat(operation.toGraphQL()).isEqualTo("query TestQuery { test }");
    }

    @Test
    @DisplayName("should return response type")
    void shouldReturnResponseType() {
      GraphQLOperation<String> operation = new TestOperation();

      assertThat(operation.responseType()).isEqualTo(String.class);
    }
  }

  @Nested
  @DisplayName("operation with variables")
  class OperationWithVariables {

    @Test
    @DisplayName("should return custom variables")
    void shouldReturnCustomVariables() {
      GraphQLOperation<String> operation = new TestOperationWithVariables("123");

      assertThat(operation.variables()).containsEntry("id", "123");
    }
  }

  /** Simple test implementation of GraphQLOperation. */
  private static class TestOperation implements GraphQLOperation<String> {

    @Override
    public String operationName() {
      return "TestQuery";
    }

    @Override
    public String toGraphQL() {
      return "query TestQuery { test }";
    }

    @Override
    public Class<String> responseType() {
      return String.class;
    }
  }

  /** Test implementation with variables. */
  private static class TestOperationWithVariables implements GraphQLOperation<String> {

    private final String id;

    TestOperationWithVariables(String id) {
      this.id = id;
    }

    @Override
    public String operationName() {
      return "GetById";
    }

    @Override
    public String toGraphQL() {
      return "query GetById($id: ID!) { getById(id: $id) }";
    }

    @Override
    public Map<String, Object> variables() {
      return Map.of("id", id);
    }

    @Override
    public Class<String> responseType() {
      return String.class;
    }
  }
}
