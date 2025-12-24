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

@DisplayName("GraphiteResponseBuilder")
class GraphiteResponseBuilderTest {

  @Nested
  @DisplayName("success")
  class Success {

    @Test
    @DisplayName("should create empty success response")
    void shouldCreateEmptySuccessResponse() {
      GraphiteResponseBuilder builder = GraphiteResponseBuilder.success();

      assertThat(builder.hasData()).isFalse();
      assertThat(builder.hasErrors()).isFalse();
      assertThat(builder.hasExtensions()).isFalse();
    }
  }

  @Nested
  @DisplayName("data")
  class Data {

    @Test
    @DisplayName("should add data with key")
    void shouldAddDataWithKey() {
      GraphiteResponseBuilder builder =
          GraphiteResponseBuilder.success().data("user", Map.of("id", "123", "name", "John"));

      assertThat(builder.hasData()).isTrue();
      assertThat(builder.getData()).containsKey("user");
    }

    @Test
    @DisplayName("should add multiple data keys")
    void shouldAddMultipleDataKeys() {
      GraphiteResponseBuilder builder =
          GraphiteResponseBuilder.success()
              .data("user", Map.of("id", "123"))
              .data("posts", List.of("post1", "post2"));

      assertThat(builder.getData()).containsKeys("user", "posts");
    }

    @Test
    @DisplayName("should handle map data")
    void shouldHandleMapData() {
      Map<String, Object> data = Map.of("id", "123", "name", "John");
      GraphiteResponseBuilder builder = GraphiteResponseBuilder.success().data(data);

      assertThat(builder.getData()).containsEntry("id", "123").containsEntry("name", "John");
    }

    @Test
    @DisplayName("should return data object")
    void shouldReturnDataObject() {
      GraphiteResponseBuilder builder =
          GraphiteResponseBuilder.success().data("user", Map.of("id", "123"));

      Object dataObject = builder.getDataObject();
      assertThat(dataObject).isInstanceOf(Map.class);
    }

    @Test
    @DisplayName("should return null for empty data")
    void shouldReturnNullForEmptyData() {
      GraphiteResponseBuilder builder = GraphiteResponseBuilder.success();

      assertThat(builder.getDataObject()).isNull();
    }
  }

  @Nested
  @DisplayName("withError")
  class WithError {

    @Test
    @DisplayName("should create response with error message")
    void shouldCreateResponseWithErrorMessage() {
      GraphiteResponseBuilder builder = GraphiteResponseBuilder.withError("Something went wrong");

      assertThat(builder.hasErrors()).isTrue();
      assertThat(builder.getErrors()).hasSize(1);
      assertThat(builder.getErrors().get(0).message()).isEqualTo("Something went wrong");
    }

    @Test
    @DisplayName("should create response with error code and message")
    void shouldCreateResponseWithErrorCodeAndMessage() {
      GraphiteResponseBuilder builder =
          GraphiteResponseBuilder.withError("NOT_FOUND", "User not found");

      assertThat(builder.getErrors()).hasSize(1);
      GraphQLError error = builder.getErrors().get(0);
      assertThat(error.message()).isEqualTo("User not found");
      assertThat(error.extensions()).containsEntry("code", "NOT_FOUND");
    }
  }

  @Nested
  @DisplayName("error")
  class Error {

    @Test
    @DisplayName("should add GraphQLError")
    void shouldAddGraphQLError() {
      GraphQLError error = new GraphQLError("Error message", List.of(), List.of(), Map.of());
      GraphiteResponseBuilder builder = GraphiteResponseBuilder.success().error(error);

      assertThat(builder.getErrors()).hasSize(1);
      assertThat(builder.getErrors().get(0).message()).isEqualTo("Error message");
    }

    @Test
    @DisplayName("should add error from builder")
    void shouldAddErrorFromBuilder() {
      GraphiteResponseBuilder builder =
          GraphiteResponseBuilder.success()
              .error(GraphiteErrorBuilder.create("NOT_FOUND", "Not found").path("user"));

      assertThat(builder.getErrors()).hasSize(1);
      assertThat(builder.getErrors().get(0).path()).containsExactly("user");
    }

    @Test
    @DisplayName("should add multiple errors")
    void shouldAddMultipleErrors() {
      List<GraphQLError> errors =
          List.of(
              new GraphQLError("Error 1", List.of(), List.of(), Map.of()),
              new GraphQLError("Error 2", List.of(), List.of(), Map.of()));

      GraphiteResponseBuilder builder = GraphiteResponseBuilder.success().errors(errors);

      assertThat(builder.getErrors()).hasSize(2);
    }
  }

  @Nested
  @DisplayName("extension")
  class Extension {

    @Test
    @DisplayName("should add single extension")
    void shouldAddSingleExtension() {
      GraphiteResponseBuilder builder =
          GraphiteResponseBuilder.success().extension("traceId", "abc123");

      assertThat(builder.hasExtensions()).isTrue();
      assertThat(builder.getExtensions()).containsEntry("traceId", "abc123");
    }

    @Test
    @DisplayName("should add multiple extensions")
    void shouldAddMultipleExtensions() {
      GraphiteResponseBuilder builder =
          GraphiteResponseBuilder.success()
              .extensions(Map.of("traceId", "abc123", "duration", 100));

      assertThat(builder.getExtensions())
          .containsEntry("traceId", "abc123")
          .containsEntry("duration", 100);
    }
  }

  @Nested
  @DisplayName("nullData")
  class NullData {

    @Test
    @DisplayName("should create response with null data")
    void shouldCreateResponseWithNullData() {
      GraphiteResponseBuilder builder = GraphiteResponseBuilder.nullData();

      assertThat(builder.hasData()).isFalse();
      assertThat(builder.getDataObject()).isNull();
    }
  }

  @Nested
  @DisplayName("toJson")
  class ToJson {

    @Test
    @DisplayName("should serialize to JSON with data")
    void shouldSerializeToJsonWithData() {
      GraphiteResponseBuilder builder =
          GraphiteResponseBuilder.success().data("user", Map.of("id", "123"));

      String json = builder.toJson();

      assertThat(json).contains("\"data\"");
      assertThat(json).contains("\"user\"");
      assertThat(json).contains("\"id\":\"123\"");
    }

    @Test
    @DisplayName("should serialize to JSON with null data")
    void shouldSerializeToJsonWithNullData() {
      GraphiteResponseBuilder builder = GraphiteResponseBuilder.nullData();

      String json = builder.toJson();

      assertThat(json).contains("\"data\":null");
    }

    @Test
    @DisplayName("should serialize to JSON with errors")
    void shouldSerializeToJsonWithErrors() {
      GraphiteResponseBuilder builder = GraphiteResponseBuilder.withError("Error message");

      String json = builder.toJson();

      assertThat(json).contains("\"errors\"");
      assertThat(json).contains("\"message\":\"Error message\"");
    }

    @Test
    @DisplayName("should serialize to JSON with extensions")
    void shouldSerializeToJsonWithExtensions() {
      GraphiteResponseBuilder builder =
          GraphiteResponseBuilder.success().extension("traceId", "abc");

      String json = builder.toJson();

      assertThat(json).contains("\"extensions\"");
      assertThat(json).contains("\"traceId\":\"abc\"");
    }
  }

  @Nested
  @DisplayName("toMap")
  class ToMap {

    @Test
    @DisplayName("should convert to map with all fields")
    void shouldConvertToMapWithAllFields() {
      GraphiteResponseBuilder builder =
          GraphiteResponseBuilder.success()
              .data("user", Map.of("id", "1"))
              .error(GraphiteErrorBuilder.create("Error").build())
              .extension("trace", "123");

      Map<String, Object> map = builder.toMap();

      assertThat(map).containsKeys("data", "errors", "extensions");
    }
  }

  @Nested
  @DisplayName("partial response")
  class PartialResponse {

    @Test
    @DisplayName("should create partial response with data and errors")
    void shouldCreatePartialResponseWithDataAndErrors() {
      Map<String, Object> userData = new java.util.HashMap<>();
      userData.put("id", "123");
      userData.put("name", null);

      GraphiteResponseBuilder builder =
          GraphiteResponseBuilder.success()
              .data("user", userData)
              .error(GraphiteErrorBuilder.validation("name", "Name cannot be null").build());

      assertThat(builder.hasData()).isTrue();
      assertThat(builder.hasErrors()).isTrue();

      String json = builder.toJson();
      assertThat(json).contains("\"data\"");
      assertThat(json).contains("\"errors\"");
    }
  }
}
