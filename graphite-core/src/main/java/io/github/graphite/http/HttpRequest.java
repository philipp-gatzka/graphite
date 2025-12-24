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

import java.net.URI;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an HTTP request to be sent to a GraphQL endpoint.
 *
 * <p>This record encapsulates all the information needed to make an HTTP request:
 * <ul>
 *   <li>{@code method} - The HTTP method (typically POST for GraphQL)</li>
 *   <li>{@code uri} - The target URI</li>
 *   <li>{@code headers} - Request headers</li>
 *   <li>{@code body} - The request body (GraphQL query as JSON)</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * HttpRequest request = new HttpRequest(
 *     HttpMethod.POST,
 *     URI.create("https://api.example.com/graphql"),
 *     Map.of("Content-Type", "application/json"),
 *     "{\"query\": \"{ user { id } }\"}"
 * );
 * }</pre>
 *
 * @param method the HTTP method
 * @param uri the target URI
 * @param headers the request headers (immutable)
 * @param body the request body, may be {@code null} for GET requests
 * @see HttpResponse
 * @see HttpTransport
 */
public record HttpRequest(
        HttpMethod method, URI uri, Map<String, String> headers, @Nullable String body) {

    /**
     * Creates a POST request with the given URI, headers, and body.
     *
     * @param uri the target URI
     * @param headers the request headers
     * @param body the request body
     * @return a new POST request
     */
    public static HttpRequest post(URI uri, Map<String, String> headers, String body) {
        return new HttpRequest(HttpMethod.POST, uri, headers, body);
    }

    /**
     * Creates a GET request with the given URI and headers.
     *
     * @param uri the target URI (may include query parameters)
     * @param headers the request headers
     * @return a new GET request
     */
    public static HttpRequest get(URI uri, Map<String, String> headers) {
        return new HttpRequest(HttpMethod.GET, uri, headers, null);
    }
}
