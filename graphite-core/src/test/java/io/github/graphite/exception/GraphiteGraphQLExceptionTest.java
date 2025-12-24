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

package io.github.graphite.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.graphite.GraphQLError;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GraphiteGraphQLException")
class GraphiteGraphQLExceptionTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("should create exception with single error")
        void shouldCreateWithSingleError() {
            var error = new GraphQLError("User not found", null, null, null);
            var exception = new GraphiteGraphQLException(List.of(error));

            assertThat(exception.getMessage()).isEqualTo("User not found");
            assertThat(exception.getErrors()).hasSize(1);
            assertThat(exception.getStatusCode()).isEqualTo(200);
        }

        @Test
        @DisplayName("should create exception with multiple errors")
        void shouldCreateWithMultipleErrors() {
            var error1 = new GraphQLError("First error", null, null, null);
            var error2 = new GraphQLError("Second error", null, null, null);
            var error3 = new GraphQLError("Third error", null, null, null);

            var exception = new GraphiteGraphQLException(List.of(error1, error2, error3));

            assertThat(exception.getMessage()).isEqualTo("First error (and 2 more errors)");
            assertThat(exception.getErrors()).hasSize(3);
        }

        @Test
        @DisplayName("should create exception with custom status code")
        void shouldCreateWithCustomStatusCode() {
            var error = new GraphQLError("Error", null, null, null);
            var exception = new GraphiteGraphQLException(List.of(error), 400);

            assertThat(exception.getStatusCode()).isEqualTo(400);
        }

        @Test
        @DisplayName("should throw when errors is null")
        void shouldThrowWhenErrorsIsNull() {
            assertThatThrownBy(() -> new GraphiteGraphQLException(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("errors must not be null or empty");
        }

        @Test
        @DisplayName("should throw when errors is empty")
        void shouldThrowWhenErrorsIsEmpty() {
            assertThatThrownBy(() -> new GraphiteGraphQLException(List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("errors must not be null or empty");
        }
    }

    @Nested
    @DisplayName("getFirstError")
    class GetFirstError {

        @Test
        @DisplayName("should return first error")
        void shouldReturnFirstError() {
            var error1 = new GraphQLError("First", null, null, null);
            var error2 = new GraphQLError("Second", null, null, null);

            var exception = new GraphiteGraphQLException(List.of(error1, error2));

            assertThat(exception.getFirstError()).isSameAs(error1);
        }
    }

    @Nested
    @DisplayName("getErrorCount")
    class GetErrorCount {

        @Test
        @DisplayName("should return error count")
        void shouldReturnErrorCount() {
            var errors =
                    List.of(
                            new GraphQLError("1", null, null, null),
                            new GraphQLError("2", null, null, null),
                            new GraphQLError("3", null, null, null));

            var exception = new GraphiteGraphQLException(errors);

            assertThat(exception.getErrorCount()).isEqualTo(3);
        }
    }

    @Nested
    @DisplayName("hasErrorWithCode")
    class HasErrorWithCode {

        @Test
        @DisplayName("should return true when error has code")
        void shouldReturnTrueWhenErrorHasCode() {
            var error =
                    new GraphQLError("Error", null, null, Map.of("code", "VALIDATION_ERROR"));
            var exception = new GraphiteGraphQLException(List.of(error));

            assertThat(exception.hasErrorWithCode("VALIDATION_ERROR")).isTrue();
        }

        @Test
        @DisplayName("should return false when no error has code")
        void shouldReturnFalseWhenNoErrorHasCode() {
            var error = new GraphQLError("Error", null, null, null);
            var exception = new GraphiteGraphQLException(List.of(error));

            assertThat(exception.hasErrorWithCode("NOT_FOUND")).isFalse();
        }
    }

    @Nested
    @DisplayName("getErrorsWithCode")
    class GetErrorsWithCode {

        @Test
        @DisplayName("should return errors with matching code")
        void shouldReturnErrorsWithMatchingCode() {
            var error1 =
                    new GraphQLError("Error 1", null, null, Map.of("code", "VALIDATION_ERROR"));
            var error2 = new GraphQLError("Error 2", null, null, Map.of("code", "NOT_FOUND"));
            var error3 =
                    new GraphQLError("Error 3", null, null, Map.of("code", "VALIDATION_ERROR"));

            var exception = new GraphiteGraphQLException(List.of(error1, error2, error3));

            assertThat(exception.getErrorsWithCode("VALIDATION_ERROR"))
                    .containsExactly(error1, error3);
        }

        @Test
        @DisplayName("should return empty list when no errors match")
        void shouldReturnEmptyListWhenNoErrorsMatch() {
            var error = new GraphQLError("Error", null, null, null);
            var exception = new GraphiteGraphQLException(List.of(error));

            assertThat(exception.getErrorsWithCode("NOT_FOUND")).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAllMessages")
    class GetAllMessages {

        @Test
        @DisplayName("should concatenate all messages")
        void shouldConcatenateAllMessages() {
            var errors =
                    List.of(
                            new GraphQLError("First error", null, null, null),
                            new GraphQLError("Second error", null, null, null));

            var exception = new GraphiteGraphQLException(errors);

            assertThat(exception.getAllMessages()).isEqualTo("First error\nSecond error");
        }
    }

    @Nested
    @DisplayName("errorCode")
    class ErrorCodeTest {

        @Test
        @DisplayName("should have GRAPHQL_ERROR error code")
        void shouldHaveGraphqlErrorCode() {
            var error = new GraphQLError("Error", null, null, null);
            var exception = new GraphiteGraphQLException(List.of(error));

            assertThat(exception.getErrorCode()).isEqualTo("GRAPHQL_ERROR");
        }
    }

    @Nested
    @DisplayName("inheritance")
    class Inheritance {

        @Test
        @DisplayName("should extend GraphiteServerException")
        void shouldExtendGraphiteServerException() {
            var error = new GraphQLError("Error", null, null, null);
            var exception = new GraphiteGraphQLException(List.of(error));

            assertThat(exception).isInstanceOf(GraphiteServerException.class);
        }

        @Test
        @DisplayName("should be catchable as GraphiteException")
        void shouldBeCatchableAsGraphiteException() {
            var error = new GraphQLError("Error", null, null, null);
            GraphiteException exception = new GraphiteGraphQLException(List.of(error));

            assertThat(exception).isNotNull();
        }
    }

    @Nested
    @DisplayName("immutability")
    class Immutability {

        @Test
        @DisplayName("errors list should be unmodifiable")
        void errorsListShouldBeUnmodifiable() {
            var error = new GraphQLError("Error", null, null, null);
            var exception = new GraphiteGraphQLException(List.of(error));

            assertThatThrownBy(() -> exception.getErrors().add(error))
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}
