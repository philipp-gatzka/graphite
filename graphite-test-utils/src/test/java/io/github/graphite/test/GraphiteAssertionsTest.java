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

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.graphite.GraphQLError;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GraphiteAssertions")
class GraphiteAssertionsTest {

  @Nested
  @DisplayName("GraphiteResponseAssert")
  class ResponseAssertTests {

    @Nested
    @DisplayName("hasData")
    class HasData {

      @Test
      @DisplayName("should pass when data is present")
      void shouldPassWhenDataIsPresent() {
        Map<String, Object> response = Map.of("data", Map.of("user", "John"));

        GraphiteAssertions.assertThat(response).hasData();
      }

      @Test
      @DisplayName("should fail when data is null")
      void shouldFailWhenDataIsNull() {
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("data", null);

        assertThatThrownBy(() -> GraphiteAssertions.assertThat(response).hasData())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("data was null");
      }
    }

    @Nested
    @DisplayName("hasNullData")
    class HasNullData {

      @Test
      @DisplayName("should pass when data is null")
      void shouldPassWhenDataIsNull() {
        Map<String, Object> response = new java.util.HashMap<>();
        response.put("data", null);

        GraphiteAssertions.assertThat(response).hasNullData();
      }

      @Test
      @DisplayName("should fail when data is present")
      void shouldFailWhenDataIsPresent() {
        Map<String, Object> response = Map.of("data", Map.of("user", "John"));

        assertThatThrownBy(() -> GraphiteAssertions.assertThat(response).hasNullData())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("null data");
      }
    }

    @Nested
    @DisplayName("hasData with key")
    class HasDataWithKey {

      @Test
      @DisplayName("should pass when key is present")
      void shouldPassWhenKeyIsPresent() {
        Map<String, Object> response = Map.of("data", Map.of("user", Map.of("id", "123")));

        GraphiteAssertions.assertThat(response).hasData("user");
      }

      @Test
      @DisplayName("should fail when key is missing")
      void shouldFailWhenKeyIsMissing() {
        Map<String, Object> response = Map.of("data", Map.of("user", "John"));

        assertThatThrownBy(() -> GraphiteAssertions.assertThat(response).hasData("posts"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("posts");
      }
    }

    @Nested
    @DisplayName("dataAt")
    class DataAt {

      @Test
      @DisplayName("should navigate simple path")
      void shouldNavigateSimplePath() {
        Map<String, Object> response = Map.of("data", Map.of("user", Map.of("name", "John")));

        GraphiteAssertions.assertThat(response).dataAt("user.name").isEqualTo("John");
      }

      @Test
      @DisplayName("should navigate array path")
      void shouldNavigateArrayPath() {
        Map<String, Object> response =
            Map.of("data", Map.of("users", List.of(Map.of("id", "1"), Map.of("id", "2"))));

        GraphiteAssertions.assertThat(response).dataAt("users[0].id").isEqualTo("1");
        GraphiteAssertions.assertThat(response).dataAt("users[1].id").isEqualTo("2");
      }

      @Test
      @DisplayName("should fail when path value doesn't match")
      void shouldFailWhenPathValueDoesntMatch() {
        Map<String, Object> response = Map.of("data", Map.of("user", Map.of("name", "John")));

        assertThatThrownBy(
                () -> GraphiteAssertions.assertThat(response).dataAt("user.name").isEqualTo("Jane"))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Jane")
            .hasMessageContaining("John");
      }
    }

    @Nested
    @DisplayName("hasNoErrors")
    class HasNoErrors {

      @Test
      @DisplayName("should pass when no errors")
      void shouldPassWhenNoErrors() {
        Map<String, Object> response = Map.of("data", Map.of("user", "John"));

        GraphiteAssertions.assertThat(response).hasNoErrors();
      }

      @Test
      @DisplayName("should pass when errors is empty list")
      void shouldPassWhenErrorsIsEmptyList() {
        Map<String, Object> response = Map.of("data", Map.of(), "errors", List.of());

        GraphiteAssertions.assertThat(response).hasNoErrors();
      }

      @Test
      @DisplayName("should fail when errors present")
      void shouldFailWhenErrorsPresent() {
        Map<String, Object> response = Map.of("errors", List.of(Map.of("message", "Error")));

        assertThatThrownBy(() -> GraphiteAssertions.assertThat(response).hasNoErrors())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("no errors");
      }
    }

    @Nested
    @DisplayName("hasErrors")
    class HasErrors {

      @Test
      @DisplayName("should pass when errors present")
      void shouldPassWhenErrorsPresent() {
        GraphQLError error = GraphiteErrorBuilder.create("Error").build();
        Map<String, Object> response = Map.of("errors", List.of(error));

        GraphiteAssertions.assertThat(response).hasErrors();
      }

      @Test
      @DisplayName("should fail when no errors")
      void shouldFailWhenNoErrors() {
        Map<String, Object> response = Map.of("data", Map.of());

        assertThatThrownBy(() -> GraphiteAssertions.assertThat(response).hasErrors())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("have errors");
      }
    }

    @Nested
    @DisplayName("hasErrorCount")
    class HasErrorCount {

      @Test
      @DisplayName("should pass when error count matches")
      void shouldPassWhenErrorCountMatches() {
        List<GraphQLError> errors =
            List.of(
                GraphiteErrorBuilder.create("Error 1").build(),
                GraphiteErrorBuilder.create("Error 2").build());
        Map<String, Object> response = Map.of("errors", errors);

        GraphiteAssertions.assertThat(response).hasErrorCount(2);
      }

      @Test
      @DisplayName("should fail when error count doesn't match")
      void shouldFailWhenErrorCountDoesntMatch() {
        List<GraphQLError> errors = List.of(GraphiteErrorBuilder.create("Error").build());
        Map<String, Object> response = Map.of("errors", errors);

        assertThatThrownBy(() -> GraphiteAssertions.assertThat(response).hasErrorCount(2))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("2 errors")
            .hasMessageContaining("found 1");
      }
    }

    @Nested
    @DisplayName("errorAt")
    class ErrorAt {

      @Test
      @DisplayName("should return error at index")
      void shouldReturnErrorAtIndex() {
        List<GraphQLError> errors =
            List.of(GraphiteErrorBuilder.create("NOT_FOUND", "User not found").build());
        Map<String, Object> response = Map.of("errors", errors);

        GraphiteAssertions.assertThat(response)
            .errorAt(0)
            .hasMessage("User not found")
            .hasCode("NOT_FOUND");
      }

      @Test
      @DisplayName("should fail when index out of bounds")
      void shouldFailWhenIndexOutOfBounds() {
        List<GraphQLError> errors = List.of(GraphiteErrorBuilder.create("Error").build());
        Map<String, Object> response = Map.of("errors", errors);

        assertThatThrownBy(() -> GraphiteAssertions.assertThat(response).errorAt(5))
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("out of bounds");
      }
    }

    @Nested
    @DisplayName("hasExtensions")
    class HasExtensions {

      @Test
      @DisplayName("should pass when extensions present")
      void shouldPassWhenExtensionsPresent() {
        Map<String, Object> response =
            Map.of("data", Map.of(), "extensions", Map.of("traceId", "abc"));

        GraphiteAssertions.assertThat(response).hasExtensions();
      }

      @Test
      @DisplayName("should fail when no extensions")
      void shouldFailWhenNoExtensions() {
        Map<String, Object> response = Map.of("data", Map.of());

        assertThatThrownBy(() -> GraphiteAssertions.assertThat(response).hasExtensions())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("extensions");
      }
    }

    @Nested
    @DisplayName("hasExtension")
    class HasExtension {

      @Test
      @DisplayName("should pass when extension key present")
      void shouldPassWhenExtensionKeyPresent() {
        Map<String, Object> response =
            Map.of("data", Map.of(), "extensions", Map.of("traceId", "abc"));

        GraphiteAssertions.assertThat(response).hasExtension("traceId");
      }
    }

    @Nested
    @DisplayName("satisfies")
    class Satisfies {

      @Test
      @DisplayName("should apply custom assertions")
      void shouldApplyCustomAssertions() {
        Map<String, Object> response = Map.of("data", Map.of("count", 5));

        GraphiteAssertions.assertThat(response)
            .satisfies(
                r -> {
                  @SuppressWarnings("unchecked")
                  Map<String, Object> data = (Map<String, Object>) r.get("data");
                  org.assertj.core.api.Assertions.assertThat(data).containsEntry("count", 5);
                });
      }
    }

    @Nested
    @DisplayName("with GraphiteResponseBuilder")
    class WithResponseBuilder {

      @Test
      @DisplayName("should accept response builder")
      void shouldAcceptResponseBuilder() {
        GraphiteResponseBuilder builder =
            GraphiteResponseBuilder.success().data("user", Map.of("id", "123"));

        GraphiteAssertions.assertThat(builder).hasData("user").hasNoErrors();
      }
    }
  }

  @Nested
  @DisplayName("DataAssert")
  class DataAssertTests {

    @Nested
    @DisplayName("isNull")
    class IsNull {

      @Test
      @DisplayName("should pass when value is null")
      void shouldPassWhenValueIsNull() {
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("field", null);
        Map<String, Object> response = Map.of("data", data);

        GraphiteAssertions.assertThat(response).dataAt("field").isNull();
      }
    }

    @Nested
    @DisplayName("isNotNull")
    class IsNotNull {

      @Test
      @DisplayName("should pass when value is not null")
      void shouldPassWhenValueIsNotNull() {
        Map<String, Object> response = Map.of("data", Map.of("field", "value"));

        GraphiteAssertions.assertThat(response).dataAt("field").isNotNull();
      }
    }

    @Nested
    @DisplayName("hasSize")
    class HasSize {

      @Test
      @DisplayName("should pass when list has expected size")
      void shouldPassWhenListHasExpectedSize() {
        Map<String, Object> response = Map.of("data", Map.of("items", List.of("a", "b", "c")));

        GraphiteAssertions.assertThat(response).dataAt("items").hasSize(3);
      }
    }

    @Nested
    @DisplayName("isEmpty")
    class IsEmpty {

      @Test
      @DisplayName("should pass when list is empty")
      void shouldPassWhenListIsEmpty() {
        Map<String, Object> response = Map.of("data", Map.of("items", List.of()));

        GraphiteAssertions.assertThat(response).dataAt("items").isEmpty();
      }
    }

    @Nested
    @DisplayName("contains")
    class Contains {

      @Test
      @DisplayName("should pass when list contains item")
      void shouldPassWhenListContainsItem() {
        Map<String, Object> response = Map.of("data", Map.of("tags", List.of("java", "graphql")));

        GraphiteAssertions.assertThat(response).dataAt("tags").contains("java");
      }
    }

    @Nested
    @DisplayName("getValue")
    class GetValue {

      @Test
      @DisplayName("should return actual value")
      void shouldReturnActualValue() {
        Map<String, Object> response = Map.of("data", Map.of("count", 42));

        Object value = GraphiteAssertions.assertThat(response).dataAt("count").getValue();
        org.assertj.core.api.Assertions.assertThat(value).isEqualTo(42);
      }
    }
  }

  @Nested
  @DisplayName("GraphiteErrorAssert")
  class ErrorAssertTests {

    @Nested
    @DisplayName("hasMessage")
    class HasMessage {

      @Test
      @DisplayName("should pass when message matches")
      void shouldPassWhenMessageMatches() {
        GraphQLError error = GraphiteErrorBuilder.create("User not found").build();

        GraphiteAssertions.assertThat(error).hasMessage("User not found");
      }

      @Test
      @DisplayName("should fail when message doesn't match")
      void shouldFailWhenMessageDoesntMatch() {
        GraphQLError error = GraphiteErrorBuilder.create("User not found").build();

        assertThatThrownBy(() -> GraphiteAssertions.assertThat(error).hasMessage("Post not found"))
            .isInstanceOf(AssertionError.class);
      }
    }

    @Nested
    @DisplayName("messageContains")
    class MessageContains {

      @Test
      @DisplayName("should pass when message contains text")
      void shouldPassWhenMessageContainsText() {
        GraphQLError error = GraphiteErrorBuilder.create("User not found").build();

        GraphiteAssertions.assertThat(error).messageContains("not found");
      }
    }

    @Nested
    @DisplayName("hasCode")
    class HasCode {

      @Test
      @DisplayName("should pass when code matches")
      void shouldPassWhenCodeMatches() {
        GraphQLError error = GraphiteErrorBuilder.create("NOT_FOUND", "Not found").build();

        GraphiteAssertions.assertThat(error).hasCode("NOT_FOUND");
      }
    }

    @Nested
    @DisplayName("hasPath")
    class HasPath {

      @Test
      @DisplayName("should pass when path is present")
      void shouldPassWhenPathIsPresent() {
        GraphQLError error = GraphiteErrorBuilder.create("Error").path("user", "name").build();

        GraphiteAssertions.assertThat(error).hasPath();
      }

      @Test
      @DisplayName("should pass when path matches")
      void shouldPassWhenPathMatches() {
        GraphQLError error = GraphiteErrorBuilder.create("Error").path("user", "name").build();

        GraphiteAssertions.assertThat(error).hasPath("user", "name");
      }
    }

    @Nested
    @DisplayName("hasLocations")
    class HasLocations {

      @Test
      @DisplayName("should pass when locations present")
      void shouldPassWhenLocationsPresent() {
        GraphQLError error = GraphiteErrorBuilder.create("Error").location(1, 10).build();

        GraphiteAssertions.assertThat(error).hasLocations();
      }
    }

    @Nested
    @DisplayName("hasExtension")
    class HasExtension {

      @Test
      @DisplayName("should pass when extension matches")
      void shouldPassWhenExtensionMatches() {
        GraphQLError error =
            GraphiteErrorBuilder.create("Error").extension("field", "email").build();

        GraphiteAssertions.assertThat(error).hasExtension("field", "email");
      }
    }

    @Nested
    @DisplayName("satisfies")
    class Satisfies {

      @Test
      @DisplayName("should apply custom assertions")
      void shouldApplyCustomAssertions() {
        GraphQLError error = GraphiteErrorBuilder.create("Error").path("field").build();

        GraphiteAssertions.assertThat(error)
            .satisfies(e -> org.assertj.core.api.Assertions.assertThat(e.path()).hasSize(1));
      }
    }

    @Nested
    @DisplayName("getError")
    class GetError {

      @Test
      @DisplayName("should return underlying error")
      void shouldReturnUnderlyingError() {
        GraphQLError error = GraphiteErrorBuilder.create("Test").build();

        GraphQLError returned = GraphiteAssertions.assertThat(error).getError();
        org.assertj.core.api.Assertions.assertThat(returned.message()).isEqualTo("Test");
      }
    }
  }

  @Nested
  @DisplayName("chaining")
  class Chaining {

    @Test
    @DisplayName("should support method chaining")
    void shouldSupportMethodChaining() {
      GraphiteResponseBuilder builder =
          GraphiteResponseBuilder.success()
              .data("user", Map.of("id", "123", "name", "John"))
              .extension("traceId", "abc");

      GraphiteAssertions.assertThat(builder)
          .hasData()
          .hasData("user")
          .hasNoErrors()
          .hasExtensions()
          .hasExtension("traceId")
          .dataAt("user.id")
          .isEqualTo("123");
    }
  }
}
