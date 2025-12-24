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

import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("HttpRequest")
class HttpRequestTest {

    private static final URI TEST_URI = URI.create("https://api.example.com/graphql");
    private static final Map<String, String> TEST_HEADERS = Map.of("Content-Type", "application/json");
    private static final String TEST_BODY = "{\"query\": \"{ user { id } }\"}";

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("should create request with all fields")
        void shouldCreateWithAllFields() {
            var request = new HttpRequest(HttpMethod.POST, TEST_URI, TEST_HEADERS, TEST_BODY);

            assertThat(request.method()).isEqualTo(HttpMethod.POST);
            assertThat(request.uri()).isEqualTo(TEST_URI);
            assertThat(request.headers()).isEqualTo(TEST_HEADERS);
            assertThat(request.body()).isEqualTo(TEST_BODY);
        }

        @Test
        @DisplayName("should allow null body")
        void shouldAllowNullBody() {
            var request = new HttpRequest(HttpMethod.GET, TEST_URI, TEST_HEADERS, null);

            assertThat(request.body()).isNull();
        }
    }

    @Nested
    @DisplayName("factory methods")
    class FactoryMethods {

        @Test
        @DisplayName("post() should create POST request")
        void postShouldCreatePostRequest() {
            var request = HttpRequest.post(TEST_URI, TEST_HEADERS, TEST_BODY);

            assertThat(request.method()).isEqualTo(HttpMethod.POST);
            assertThat(request.uri()).isEqualTo(TEST_URI);
            assertThat(request.headers()).isEqualTo(TEST_HEADERS);
            assertThat(request.body()).isEqualTo(TEST_BODY);
        }

        @Test
        @DisplayName("get() should create GET request with null body")
        void getShouldCreateGetRequest() {
            var request = HttpRequest.get(TEST_URI, TEST_HEADERS);

            assertThat(request.method()).isEqualTo(HttpMethod.GET);
            assertThat(request.uri()).isEqualTo(TEST_URI);
            assertThat(request.headers()).isEqualTo(TEST_HEADERS);
            assertThat(request.body()).isNull();
        }
    }
}
