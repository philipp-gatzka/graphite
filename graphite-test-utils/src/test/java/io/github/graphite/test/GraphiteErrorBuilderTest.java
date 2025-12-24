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

import io.github.graphite.GraphQLError;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GraphiteErrorBuilder")
class GraphiteErrorBuilderTest {

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    @DisplayName("should create error with message only")
    void shouldCreateErrorWithMessageOnly() {
      GraphQLError error = GraphiteErrorBuilder.create("Something went wrong").build();

      assertThat(error.message()).isEqualTo("Something went wrong");
      assertThat(error.locations()).isEmpty();
      assertThat(error.path()).isEmpty();
      assertThat(error.extensions()).isEmpty();
    }

    @Test
    @DisplayName("should create error with code and message")
    void shouldCreateErrorWithCodeAndMessage() {
      GraphQLError error = GraphiteErrorBuilder.create("NOT_FOUND", "User not found").build();

      assertThat(error.message()).isEqualTo("User not found");
      assertThat(error.extensions()).containsEntry("code", "NOT_FOUND");
    }
  }

  @Nested
  @DisplayName("factory methods")
  class FactoryMethods {

    @Test
    @DisplayName("should create not found error")
    void shouldCreateNotFoundError() {
      GraphQLError error = GraphiteErrorBuilder.notFound("User", "123").build();

      assertThat(error.message()).isEqualTo("User with id '123' not found");
      assertThat(error.extensions()).containsEntry("code", "NOT_FOUND");
      assertThat(error.extensions()).containsEntry("entity", "User");
      assertThat(error.extensions()).containsEntry("id", "123");
    }

    @Test
    @DisplayName("should create validation error")
    void shouldCreateValidationError() {
      GraphQLError error = GraphiteErrorBuilder.validation("email", "Invalid email format").build();

      assertThat(error.message()).isEqualTo("Invalid email format");
      assertThat(error.extensions()).containsEntry("code", "VALIDATION_ERROR");
      assertThat(error.extensions()).containsEntry("field", "email");
    }

    @Test
    @DisplayName("should create unauthenticated error")
    void shouldCreateUnauthenticatedError() {
      GraphQLError error = GraphiteErrorBuilder.unauthenticated().build();

      assertThat(error.message()).isEqualTo("Authentication required");
      assertThat(error.extensions()).containsEntry("code", "UNAUTHENTICATED");
    }

    @Test
    @DisplayName("should create forbidden error")
    void shouldCreateForbiddenError() {
      GraphQLError error = GraphiteErrorBuilder.forbidden().build();

      assertThat(error.message()).isEqualTo("Access denied");
      assertThat(error.extensions()).containsEntry("code", "FORBIDDEN");
    }

    @Test
    @DisplayName("should create rate limited error")
    void shouldCreateRateLimitedError() {
      GraphQLError error = GraphiteErrorBuilder.rateLimited(60).build();

      assertThat(error.message()).isEqualTo("Rate limit exceeded");
      assertThat(error.extensions()).containsEntry("code", "RATE_LIMITED");
      assertThat(error.extensions()).containsEntry("retryAfter", 60);
    }

    @Test
    @DisplayName("should create internal error")
    void shouldCreateInternalError() {
      GraphQLError error = GraphiteErrorBuilder.internalError().build();

      assertThat(error.message()).isEqualTo("Internal server error");
      assertThat(error.extensions()).containsEntry("code", "INTERNAL_ERROR");
    }
  }

  @Nested
  @DisplayName("location")
  class Location {

    @Test
    @DisplayName("should add single location")
    void shouldAddSingleLocation() {
      GraphQLError error = GraphiteErrorBuilder.create("Error").location(1, 10).build();

      assertThat(error.locations()).hasSize(1);
      assertThat(error.locations().get(0).line()).isEqualTo(1);
      assertThat(error.locations().get(0).column()).isEqualTo(10);
    }

    @Test
    @DisplayName("should add multiple locations")
    void shouldAddMultipleLocations() {
      GraphQLError error =
          GraphiteErrorBuilder.create("Error").location(1, 10).location(5, 20).build();

      assertThat(error.locations()).hasSize(2);
    }
  }

  @Nested
  @DisplayName("path")
  class Path {

    @Test
    @DisplayName("should add path segments")
    void shouldAddPathSegments() {
      GraphQLError error =
          GraphiteErrorBuilder.create("Error").path("user", "profile", "name").build();

      assertThat(error.path()).containsExactly("user", "profile", "name");
    }

    @Test
    @DisplayName("should add path with array index")
    void shouldAddPathWithArrayIndex() {
      GraphQLError error = GraphiteErrorBuilder.create("Error").path("users", 0, "name").build();

      assertThat(error.path()).containsExactly("users", 0, "name");
    }

    @Test
    @DisplayName("should add path from list")
    void shouldAddPathFromList() {
      GraphQLError error =
          GraphiteErrorBuilder.create("Error").path(List.of("user", "email")).build();

      assertThat(error.path()).containsExactly("user", "email");
    }
  }

  @Nested
  @DisplayName("extension")
  class Extension {

    @Test
    @DisplayName("should add single extension")
    void shouldAddSingleExtension() {
      GraphQLError error = GraphiteErrorBuilder.create("Error").extension("key", "value").build();

      assertThat(error.extensions()).containsEntry("key", "value");
    }

    @Test
    @DisplayName("should add multiple extensions")
    void shouldAddMultipleExtensions() {
      GraphQLError error =
          GraphiteErrorBuilder.create("Error")
              .extensions(Map.of("key1", "value1", "key2", "value2"))
              .build();

      assertThat(error.extensions())
          .containsEntry("key1", "value1")
          .containsEntry("key2", "value2");
    }

    @Test
    @DisplayName("should add classification")
    void shouldAddClassification() {
      GraphQLError error =
          GraphiteErrorBuilder.create("Error").classification("ValidationError").build();

      assertThat(error.extensions()).containsEntry("classification", "ValidationError");
    }

    @Test
    @DisplayName("should handle null extension value")
    void shouldHandleNullExtensionValue() {
      GraphQLError error = GraphiteErrorBuilder.create("Error").extension("nullKey", null).build();

      assertThat(error.extensions()).containsKey("nullKey");
      assertThat(error.extensions().get("nullKey")).isNull();
    }
  }

  @Nested
  @DisplayName("getters")
  class Getters {

    @Test
    @DisplayName("should return message")
    void shouldReturnMessage() {
      GraphiteErrorBuilder builder = GraphiteErrorBuilder.create("Test message");

      assertThat(builder.getMessage()).isEqualTo("Test message");
    }

    @Test
    @DisplayName("should return locations")
    void shouldReturnLocations() {
      GraphiteErrorBuilder builder = GraphiteErrorBuilder.create("Error").location(1, 1);

      assertThat(builder.getLocations()).hasSize(1);
    }

    @Test
    @DisplayName("should return path")
    void shouldReturnPath() {
      GraphiteErrorBuilder builder = GraphiteErrorBuilder.create("Error").path("field");

      assertThat(builder.getPath()).containsExactly("field");
    }

    @Test
    @DisplayName("should return extensions")
    void shouldReturnExtensions() {
      GraphiteErrorBuilder builder = GraphiteErrorBuilder.create("Error").extension("key", "value");

      assertThat(builder.getExtensions()).containsEntry("key", "value");
    }
  }

  @Nested
  @DisplayName("chaining")
  class Chaining {

    @Test
    @DisplayName("should support method chaining")
    void shouldSupportMethodChaining() {
      GraphQLError error =
          GraphiteErrorBuilder.create("VALIDATION_ERROR", "Invalid input")
              .location(1, 15)
              .path("mutation", "createUser", "input", "email")
              .extension("constraint", "email")
              .extension("value", "invalid-email")
              .classification("ValidationError")
              .build();

      assertThat(error.message()).isEqualTo("Invalid input");
      assertThat(error.locations()).hasSize(1);
      assertThat(error.path()).hasSize(4);
      assertThat(error.extensions()).hasSize(4);
    }
  }
}
