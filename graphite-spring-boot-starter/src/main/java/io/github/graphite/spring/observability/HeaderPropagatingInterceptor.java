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
package io.github.graphite.spring.observability;

import io.github.graphite.interceptor.RequestInterceptor;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import java.util.HashMap;
import java.util.Map;
import org.jetbrains.annotations.NotNull;

/**
 * Interceptor that propagates trace context headers to downstream GraphQL servers.
 *
 * <p>This interceptor injects trace context headers (such as B3 or W3C TraceContext) into outgoing
 * requests, enabling distributed tracing across service boundaries.
 *
 * <p>The headers injected depend on the configured {@link Propagator}. Common formats include:
 *
 * <ul>
 *   <li>B3 propagation (X-B3-TraceId, X-B3-SpanId, etc.)
 *   <li>W3C Trace Context (traceparent, tracestate)
 * </ul>
 *
 * <p>Example configuration:
 *
 * <pre>{@code
 * HeaderPropagatingInterceptor propagatingInterceptor =
 *     new HeaderPropagatingInterceptor(tracer, propagator);
 *
 * GraphiteClient client = GraphiteClient.builder()
 *     .endpoint("https://api.example.com/graphql")
 *     .requestInterceptor(propagatingInterceptor.requestInterceptor())
 *     .build();
 * }</pre>
 *
 * @see GraphiteTracingAutoConfiguration
 */
public class HeaderPropagatingInterceptor {

  private final Tracer tracer;
  private final Propagator propagator;

  /**
   * Creates a new header propagating interceptor.
   *
   * @param tracer the Micrometer tracer
   * @param propagator the propagator for injecting trace context
   */
  public HeaderPropagatingInterceptor(@NotNull Tracer tracer, @NotNull Propagator propagator) {
    this.tracer = tracer;
    this.propagator = propagator;
  }

  /**
   * Returns a request interceptor that injects trace context headers.
   *
   * @return the request interceptor
   */
  @NotNull
  public RequestInterceptor requestInterceptor() {
    return request -> {
      // Get current span context
      if (tracer.currentSpan() == null) {
        return request;
      }

      // Create a mutable map to collect headers
      Map<String, String> traceHeaders = new HashMap<>();

      // Inject trace context using the propagator
      propagator.inject(
          tracer.currentTraceContext().context(),
          traceHeaders,
          (carrier, key, value) -> carrier.put(key, value));

      if (traceHeaders.isEmpty()) {
        return request;
      }

      // Add trace headers to the request
      return request.withHeaders(traceHeaders);
    };
  }

  /**
   * Returns the tracer.
   *
   * @return the tracer
   */
  @NotNull
  public Tracer getTracer() {
    return tracer;
  }

  /**
   * Returns the propagator.
   *
   * @return the propagator
   */
  @NotNull
  public Propagator getPropagator() {
    return propagator;
  }
}
