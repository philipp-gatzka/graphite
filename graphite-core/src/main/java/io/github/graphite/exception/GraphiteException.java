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

import org.jetbrains.annotations.Nullable;

/**
 * Base exception for all Graphite-related errors.
 *
 * <p>This is an unchecked exception that serves as the root of the Graphite exception
 * hierarchy. All exceptions thrown by Graphite extend this class, allowing callers
 * to catch all Graphite errors with a single catch block if desired.
 *
 * <p>The exception hierarchy is organized as follows:
 * <ul>
 *   <li>{@link GraphiteClientException} - Errors occurring on the client side</li>
 *   <li>{@link GraphiteServerException} - Errors returned by the GraphQL server</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * try {
 *     client.execute(query);
 * } catch (GraphiteException e) {
 *     // Handle any Graphite error
 *     log.error("GraphQL operation failed: {}", e.getMessage());
 * }
 * }</pre>
 *
 * @see GraphiteClientException
 * @see GraphiteServerException
 */
public class GraphiteException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    @Nullable
    private final String errorCode;

    /**
     * Constructs a new Graphite exception with the specified message.
     *
     * @param message the detail message describing the error
     */
    public GraphiteException(String message) {
        this(message, null, null);
    }

    /**
     * Constructs a new Graphite exception with the specified message and cause.
     *
     * @param message the detail message describing the error
     * @param cause the underlying cause of this exception, may be {@code null}
     */
    public GraphiteException(String message, @Nullable Throwable cause) {
        this(message, cause, null);
    }

    /**
     * Constructs a new Graphite exception with the specified message and error code.
     *
     * @param message the detail message describing the error
     * @param errorCode an optional error code for programmatic error handling,
     *                  may be {@code null}
     */
    public GraphiteException(String message, @Nullable String errorCode) {
        this(message, null, errorCode);
    }

    /**
     * Constructs a new Graphite exception with the specified message, cause, and error code.
     *
     * @param message the detail message describing the error
     * @param cause the underlying cause of this exception, may be {@code null}
     * @param errorCode an optional error code for programmatic error handling,
     *                  may be {@code null}
     */
    public GraphiteException(String message, @Nullable Throwable cause, @Nullable String errorCode) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    /**
     * Returns the error code associated with this exception, if any.
     *
     * <p>Error codes provide a programmatic way to identify specific error conditions
     * without parsing the error message. They can be used for:
     * <ul>
     *   <li>Mapping errors to user-friendly messages</li>
     *   <li>Implementing error-specific recovery logic</li>
     *   <li>Logging and monitoring categorization</li>
     * </ul>
     *
     * @return the error code, or {@code null} if no error code was specified
     */
    @Nullable
    public String getErrorCode() {
        return errorCode;
    }
}
