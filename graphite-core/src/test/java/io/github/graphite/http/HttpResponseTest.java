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

package io.github.graphite.http;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("HttpResponse")
class HttpResponseTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("should create response with all fields")
        void shouldCreateWithAllFields() {
            var headers = Map.of("Content-Type", List.of("application/json"));
            var body = "{\"data\": {}}";

            var response = new HttpResponse(200, headers, body);

            assertThat(response.statusCode()).isEqualTo(200);
            assertThat(response.headers()).isEqualTo(headers);
            assertThat(response.body()).isEqualTo(body);
        }

        @Test
        @DisplayName("should allow null body")
        void shouldAllowNullBody() {
            var response = new HttpResponse(204, Map.of(), null);

            assertThat(response.body()).isNull();
        }
    }

    @Nested
    @DisplayName("isSuccessful")
    class IsSuccessful {

        @ParameterizedTest
        @ValueSource(ints = {200, 201, 202, 204, 299})
        @DisplayName("should return true for 2xx status codes")
        void shouldReturnTrueFor2xxStatusCodes(int statusCode) {
            var response = new HttpResponse(statusCode, Map.of(), null);

            assertThat(response.isSuccessful()).isTrue();
        }

        @ParameterizedTest
        @ValueSource(ints = {100, 301, 400, 500})
        @DisplayName("should return false for non-2xx status codes")
        void shouldReturnFalseForNon2xxStatusCodes(int statusCode) {
            var response = new HttpResponse(statusCode, Map.of(), null);

            assertThat(response.isSuccessful()).isFalse();
        }
    }

    @Nested
    @DisplayName("isClientError")
    class IsClientError {

        @ParameterizedTest
        @ValueSource(ints = {400, 401, 403, 404, 422, 429, 499})
        @DisplayName("should return true for 4xx status codes")
        void shouldReturnTrueFor4xxStatusCodes(int statusCode) {
            var response = new HttpResponse(statusCode, Map.of(), null);

            assertThat(response.isClientError()).isTrue();
        }

        @ParameterizedTest
        @ValueSource(ints = {200, 301, 500})
        @DisplayName("should return false for non-4xx status codes")
        void shouldReturnFalseForNon4xxStatusCodes(int statusCode) {
            var response = new HttpResponse(statusCode, Map.of(), null);

            assertThat(response.isClientError()).isFalse();
        }
    }

    @Nested
    @DisplayName("isServerError")
    class IsServerError {

        @ParameterizedTest
        @ValueSource(ints = {500, 501, 502, 503, 504, 599})
        @DisplayName("should return true for 5xx status codes")
        void shouldReturnTrueFor5xxStatusCodes(int statusCode) {
            var response = new HttpResponse(statusCode, Map.of(), null);

            assertThat(response.isServerError()).isTrue();
        }

        @ParameterizedTest
        @ValueSource(ints = {200, 301, 400})
        @DisplayName("should return false for non-5xx status codes")
        void shouldReturnFalseForNon5xxStatusCodes(int statusCode) {
            var response = new HttpResponse(statusCode, Map.of(), null);

            assertThat(response.isServerError()).isFalse();
        }
    }

    @Nested
    @DisplayName("getHeader")
    class GetHeader {

        @Test
        @DisplayName("should return first header value")
        void shouldReturnFirstHeaderValue() {
            var headers = Map.of("Content-Type", List.of("application/json", "charset=utf-8"));
            var response = new HttpResponse(200, headers, null);

            assertThat(response.getHeader("Content-Type")).isEqualTo("application/json");
        }

        @Test
        @DisplayName("should return null for missing header")
        void shouldReturnNullForMissingHeader() {
            var response = new HttpResponse(200, Map.of(), null);

            assertThat(response.getHeader("X-Missing")).isNull();
        }

        @Test
        @DisplayName("should be case-insensitive")
        void shouldBeCaseInsensitive() {
            var headers = Map.of("Content-Type", List.of("application/json"));
            var response = new HttpResponse(200, headers, null);

            assertThat(response.getHeader("content-type")).isEqualTo("application/json");
            assertThat(response.getHeader("CONTENT-TYPE")).isEqualTo("application/json");
        }

        @Test
        @DisplayName("should return null for empty value list")
        void shouldReturnNullForEmptyValueList() {
            var headers = Map.of("Empty", List.<String>of());
            var response = new HttpResponse(200, headers, null);

            assertThat(response.getHeader("Empty")).isNull();
        }
    }

    @Nested
    @DisplayName("getContentType")
    class GetContentType {

        @Test
        @DisplayName("should return content type header")
        void shouldReturnContentTypeHeader() {
            var headers = Map.of("Content-Type", List.of("application/json"));
            var response = new HttpResponse(200, headers, null);

            assertThat(response.getContentType()).isEqualTo("application/json");
        }

        @Test
        @DisplayName("should return null when not present")
        void shouldReturnNullWhenNotPresent() {
            var response = new HttpResponse(200, Map.of(), null);

            assertThat(response.getContentType()).isNull();
        }
    }
}
