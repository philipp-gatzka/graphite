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

import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an HTTP response from a GraphQL endpoint.
 *
 * <p>This record encapsulates all the information from an HTTP response:
 * <ul>
 *   <li>{@code statusCode} - The HTTP status code</li>
 *   <li>{@code headers} - Response headers</li>
 *   <li>{@code body} - The response body (GraphQL response as JSON)</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * HttpResponse response = transport.execute(request);
 * if (response.isSuccessful()) {
 *     String json = response.body();
 *     // Parse GraphQL response
 * }
 * }</pre>
 *
 * @param statusCode the HTTP status code
 * @param headers the response headers (may contain multiple values per header)
 * @param body the response body, may be {@code null}
 * @see HttpRequest
 * @see HttpTransport
 */
public record HttpResponse(
        int statusCode, Map<String, List<String>> headers, @Nullable String body) {

    /**
     * Returns whether the response indicates success (2xx status code).
     *
     * @return {@code true} if the status code is in the 2xx range
     */
    public boolean isSuccessful() {
        return statusCode >= 200 && statusCode < 300;
    }

    /**
     * Returns whether the response indicates a client error (4xx status code).
     *
     * @return {@code true} if the status code is in the 4xx range
     */
    public boolean isClientError() {
        return statusCode >= 400 && statusCode < 500;
    }

    /**
     * Returns whether the response indicates a server error (5xx status code).
     *
     * @return {@code true} if the status code is in the 5xx range
     */
    public boolean isServerError() {
        return statusCode >= 500 && statusCode < 600;
    }

    /**
     * Returns the first value of the specified header, or {@code null} if not present.
     *
     * @param name the header name (case-insensitive)
     * @return the first header value, or {@code null}
     */
    @Nullable
    public String getHeader(String name) {
        String lowerName = name.toLowerCase();
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(lowerName)) {
                List<String> values = entry.getValue();
                return values.isEmpty() ? null : values.get(0);
            }
        }
        return null;
    }

    /**
     * Returns the Content-Type header value, or {@code null} if not present.
     *
     * @return the content type, or {@code null}
     */
    @Nullable
    public String getContentType() {
        return getHeader("Content-Type");
    }
}
