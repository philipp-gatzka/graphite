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

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GraphQLError")
class GraphQLErrorTest {

    @Nested
    @DisplayName("construction")
    class Construction {

        @Test
        @DisplayName("should create error with message only")
        void shouldCreateWithMessageOnly() {
            var error = new GraphQLError("Something went wrong", null, null, null);

            assertThat(error.message()).isEqualTo("Something went wrong");
            assertThat(error.locations()).isNull();
            assertThat(error.path()).isNull();
            assertThat(error.extensions()).isNull();
        }

        @Test
        @DisplayName("should create error with all fields")
        void shouldCreateWithAllFields() {
            var locations = List.of(new GraphQLError.Location(2, 3));
            var path = List.<Object>of("user", "posts", 0, "title");
            var extensions = Map.<String, Object>of("code", "NOT_FOUND");

            var error = new GraphQLError("User not found", locations, path, extensions);

            assertThat(error.message()).isEqualTo("User not found");
            assertThat(error.locations()).hasSize(1);
            assertThat(error.locations().get(0).line()).isEqualTo(2);
            assertThat(error.locations().get(0).column()).isEqualTo(3);
            assertThat(error.path()).containsExactly("user", "posts", 0, "title");
            assertThat(error.extensions()).containsEntry("code", "NOT_FOUND");
        }
    }

    @Nested
    @DisplayName("getCode")
    class GetCode {

        @Test
        @DisplayName("should return code from extensions")
        void shouldReturnCodeFromExtensions() {
            var error =
                    new GraphQLError(
                            "Error", null, null, Map.of("code", "VALIDATION_ERROR"));

            assertThat(error.getCode()).isEqualTo("VALIDATION_ERROR");
        }

        @Test
        @DisplayName("should return null when extensions is null")
        void shouldReturnNullWhenExtensionsIsNull() {
            var error = new GraphQLError("Error", null, null, null);

            assertThat(error.getCode()).isNull();
        }

        @Test
        @DisplayName("should return null when code is not in extensions")
        void shouldReturnNullWhenCodeIsNotInExtensions() {
            var error = new GraphQLError("Error", null, null, Map.of("other", "value"));

            assertThat(error.getCode()).isNull();
        }

        @Test
        @DisplayName("should convert non-string code to string")
        void shouldConvertNonStringCodeToString() {
            var error = new GraphQLError("Error", null, null, Map.of("code", 42));

            assertThat(error.getCode()).isEqualTo("42");
        }
    }

    @Nested
    @DisplayName("getFormattedPath")
    class GetFormattedPath {

        @Test
        @DisplayName("should return empty string when path is null")
        void shouldReturnEmptyStringWhenPathIsNull() {
            var error = new GraphQLError("Error", null, null, null);

            assertThat(error.getFormattedPath()).isEmpty();
        }

        @Test
        @DisplayName("should return empty string when path is empty")
        void shouldReturnEmptyStringWhenPathIsEmpty() {
            var error = new GraphQLError("Error", null, List.of(), null);

            assertThat(error.getFormattedPath()).isEmpty();
        }

        @Test
        @DisplayName("should format simple path")
        void shouldFormatSimplePath() {
            var error = new GraphQLError("Error", null, List.of("user"), null);

            assertThat(error.getFormattedPath()).isEqualTo("user");
        }

        @Test
        @DisplayName("should format nested path with array index")
        void shouldFormatNestedPathWithArrayIndex() {
            var error = new GraphQLError("Error", null, List.of("user", "posts", 0, "title"), null);

            assertThat(error.getFormattedPath()).isEqualTo("user.posts.0.title");
        }
    }

    @Nested
    @DisplayName("Location")
    class LocationTest {

        @Test
        @DisplayName("should create location with line and column")
        void shouldCreateLocationWithLineAndColumn() {
            var location = new GraphQLError.Location(10, 5);

            assertThat(location.line()).isEqualTo(10);
            assertThat(location.column()).isEqualTo(5);
        }
    }
}
