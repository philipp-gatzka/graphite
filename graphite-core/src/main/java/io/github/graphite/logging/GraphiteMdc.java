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
package io.github.graphite.logging;

import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.MDC;

/**
 * Provides MDC (Mapped Diagnostic Context) support for Graphite operations.
 *
 * <p>This class manages contextual information that is added to all log entries during the
 * execution of a GraphQL operation. The MDC values can be used by logging frameworks to include
 * structured context in log output.
 *
 * <p>MDC keys used:
 *
 * <ul>
 *   <li>{@code graphite.operation} - The name of the GraphQL operation being executed
 *   <li>{@code graphite.requestId} - A unique identifier for each request
 *   <li>{@code graphite.correlationId} - An optional correlation ID for distributed tracing
 * </ul>
 *
 * <p>Example log output with MDC (using a pattern like {@code %X{graphite.operation}}):
 *
 * <pre>
 * [GetUser] [req-abc123] Executing GraphQL operation
 * [GetUser] [req-abc123] Received response: status=200
 * </pre>
 *
 * <p>Usage:
 *
 * <pre>{@code
 * try (GraphiteMdc.Context ctx = GraphiteMdc.start("GetUser", correlationId)) {
 *     // All logs within this block will have MDC values set
 *     client.execute(query);
 * }
 * }</pre>
 *
 * @see MDC
 */
public final class GraphiteMdc {

  /** MDC key for the GraphQL operation name. */
  public static final String KEY_OPERATION = "graphite.operation";

  /** MDC key for the unique request identifier. */
  public static final String KEY_REQUEST_ID = "graphite.requestId";

  /** MDC key for the correlation ID (for distributed tracing). */
  public static final String KEY_CORRELATION_ID = "graphite.correlationId";

  private GraphiteMdc() {
    // Utility class
  }

  /**
   * Starts an MDC context for a GraphQL operation.
   *
   * <p>This method sets the operation name and generates a unique request ID. If a correlation ID
   * is provided, it will also be added to the MDC.
   *
   * @param operationName the name of the GraphQL operation
   * @param correlationId an optional correlation ID for distributed tracing
   * @return a context that should be closed when the operation completes
   */
  @NotNull
  public static Context start(@NotNull String operationName, @Nullable String correlationId) {
    String requestId = generateRequestId();
    MDC.put(KEY_OPERATION, operationName);
    MDC.put(KEY_REQUEST_ID, requestId);
    if (correlationId != null && !correlationId.isBlank()) {
      MDC.put(KEY_CORRELATION_ID, correlationId);
    }
    return new Context(correlationId != null && !correlationId.isBlank());
  }

  /**
   * Starts an MDC context for a GraphQL operation without a correlation ID.
   *
   * @param operationName the name of the GraphQL operation
   * @return a context that should be closed when the operation completes
   */
  @NotNull
  public static Context start(@NotNull String operationName) {
    return start(operationName, null);
  }

  /**
   * Gets the current request ID from MDC.
   *
   * @return the current request ID, or null if not set
   */
  @Nullable
  public static String getRequestId() {
    return MDC.get(KEY_REQUEST_ID);
  }

  /**
   * Gets the current operation name from MDC.
   *
   * @return the current operation name, or null if not set
   */
  @Nullable
  public static String getOperationName() {
    return MDC.get(KEY_OPERATION);
  }

  /**
   * Gets the current correlation ID from MDC.
   *
   * @return the current correlation ID, or null if not set
   */
  @Nullable
  public static String getCorrelationId() {
    return MDC.get(KEY_CORRELATION_ID);
  }

  private static String generateRequestId() {
    return UUID.randomUUID().toString().substring(0, 8);
  }

  /**
   * An auto-closeable context that clears MDC values when closed.
   *
   * <p>Use this with try-with-resources to ensure MDC values are properly cleaned up.
   */
  public static final class Context implements AutoCloseable {
    private final boolean hasCorrelationId;

    Context(boolean hasCorrelationId) {
      this.hasCorrelationId = hasCorrelationId;
    }

    @Override
    public void close() {
      MDC.remove(KEY_OPERATION);
      MDC.remove(KEY_REQUEST_ID);
      if (hasCorrelationId) {
        MDC.remove(KEY_CORRELATION_ID);
      }
    }
  }
}
