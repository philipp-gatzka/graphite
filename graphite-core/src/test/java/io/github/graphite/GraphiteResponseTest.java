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

import io.github.graphite.exception.GraphiteGraphQLException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GraphiteResponse")
class GraphiteResponseTest {

  @Nested
  @DisplayName("constructor")
  class Constructor {

    @Test
    @DisplayName("should create response with all parameters")
    void shouldCreateResponseWithAllParameters() {
      var response = new GraphiteResponse<>("data", List.of(), Map.of("key", "value"));

      assertThat(response.data()).isEqualTo("data");
      assertThat(response.errors()).isEmpty();
      assertThat(response.extensions()).containsEntry("key", "value");
    }

    @Test
    @DisplayName("should allow null data")
    void shouldAllowNullData() {
      var response = new GraphiteResponse<String>(null, List.of(), Map.of());

      assertThat(response.data()).isNull();
    }

    @Test
    @DisplayName("should reject null errors")
    void shouldRejectNullErrors() {
      assertThatNullPointerException()
          .isThrownBy(() -> new GraphiteResponse<>("data", null, Map.of()))
          .withMessageContaining("errors must not be null");
    }

    @Test
    @DisplayName("should reject null extensions")
    void shouldRejectNullExtensions() {
      assertThatNullPointerException()
          .isThrownBy(() -> new GraphiteResponse<>("data", List.of(), null))
          .withMessageContaining("extensions must not be null");
    }
  }

  @Nested
  @DisplayName("success factory")
  class SuccessFactory {

    @Test
    @DisplayName("should create successful response with data")
    void shouldCreateSuccessfulResponseWithData() {
      var response = GraphiteResponse.success("data");

      assertThat(response.data()).isEqualTo("data");
      assertThat(response.errors()).isEmpty();
      assertThat(response.extensions()).isEmpty();
    }

    @Test
    @DisplayName("should allow null data")
    void shouldAllowNullData() {
      var response = GraphiteResponse.success(null);

      assertThat(response.data()).isNull();
      assertThat(response.errors()).isEmpty();
    }
  }

  @Nested
  @DisplayName("error factory")
  class ErrorFactory {

    @Test
    @DisplayName("should create error response")
    void shouldCreateErrorResponse() {
      var error = new GraphQLError("error message", null, null, null);
      var response = GraphiteResponse.<String>error(List.of(error));

      assertThat(response.data()).isNull();
      assertThat(response.errors()).containsExactly(error);
      assertThat(response.extensions()).isEmpty();
    }
  }

  @Nested
  @DisplayName("hasData")
  class HasData {

    @Test
    @DisplayName("should return true when data is present")
    void shouldReturnTrueWhenDataPresent() {
      var response = GraphiteResponse.success("data");

      assertThat(response.hasData()).isTrue();
    }

    @Test
    @DisplayName("should return false when data is null")
    void shouldReturnFalseWhenDataNull() {
      var response = GraphiteResponse.success(null);

      assertThat(response.hasData()).isFalse();
    }
  }

  @Nested
  @DisplayName("hasErrors")
  class HasErrors {

    @Test
    @DisplayName("should return true when errors present")
    void shouldReturnTrueWhenErrorsPresent() {
      var error = new GraphQLError("error", null, null, null);
      var response = GraphiteResponse.<String>error(List.of(error));

      assertThat(response.hasErrors()).isTrue();
    }

    @Test
    @DisplayName("should return false when no errors")
    void shouldReturnFalseWhenNoErrors() {
      var response = GraphiteResponse.success("data");

      assertThat(response.hasErrors()).isFalse();
    }
  }

  @Nested
  @DisplayName("isSuccess")
  class IsSuccess {

    @Test
    @DisplayName("should return true when data present and no errors")
    void shouldReturnTrueWhenSuccessful() {
      var response = GraphiteResponse.success("data");

      assertThat(response.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("should return false when no data")
    void shouldReturnFalseWhenNoData() {
      var response = GraphiteResponse.success(null);

      assertThat(response.isSuccess()).isFalse();
    }

    @Test
    @DisplayName("should return false when has errors")
    void shouldReturnFalseWhenHasErrors() {
      var error = new GraphQLError("error", null, null, null);
      var response = new GraphiteResponse<>("data", List.of(error), Map.of());

      assertThat(response.isSuccess()).isFalse();
    }
  }

  @Nested
  @DisplayName("dataOrThrow")
  class DataOrThrow {

    @Test
    @DisplayName("should return data when no errors")
    void shouldReturnDataWhenNoErrors() {
      var response = GraphiteResponse.success("data");

      assertThat(response.dataOrThrow()).isEqualTo("data");
    }

    @Test
    @DisplayName("should return null data when no errors and null data")
    void shouldReturnNullDataWhenNoErrorsAndNullData() {
      var response = GraphiteResponse.success(null);

      assertThat(response.dataOrThrow()).isNull();
    }

    @Test
    @DisplayName("should throw when has errors")
    void shouldThrowWhenHasErrors() {
      var error = new GraphQLError("error message", null, null, null);
      var response = GraphiteResponse.<String>error(List.of(error));

      assertThatThrownBy(response::dataOrThrow)
          .isInstanceOf(GraphiteGraphQLException.class)
          .hasMessageContaining("error message");
    }

    @Test
    @DisplayName("should include all errors in exception")
    void shouldIncludeAllErrorsInException() {
      var error1 = new GraphQLError("error 1", null, null, null);
      var error2 = new GraphQLError("error 2", null, null, null);
      var response = GraphiteResponse.<String>error(List.of(error1, error2));

      assertThatThrownBy(response::dataOrThrow)
          .isInstanceOf(GraphiteGraphQLException.class)
          .hasMessageContaining("error 1")
          .hasMessageContaining("1 more errors")
          .satisfies(
              ex -> {
                var graphqlEx = (GraphiteGraphQLException) ex;
                assertThat(graphqlEx.getErrors()).containsExactly(error1, error2);
              });
    }
  }

  @Nested
  @DisplayName("record behavior")
  class RecordBehavior {

    @Test
    @DisplayName("should be equal for same values")
    void shouldBeEqualForSameValues() {
      var response1 = GraphiteResponse.success("data");
      var response2 = GraphiteResponse.success("data");

      assertThat(response1).isEqualTo(response2).hasSameHashCodeAs(response2);
    }

    @Test
    @DisplayName("should not be equal for different data")
    void shouldNotBeEqualForDifferentData() {
      var response1 = GraphiteResponse.success("data1");
      var response2 = GraphiteResponse.success("data2");

      assertThat(response1).isNotEqualTo(response2);
    }
  }
}
