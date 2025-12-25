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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.github.graphite.http.HttpMethod;
import io.github.graphite.http.HttpRequest;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("HeaderPropagatingInterceptor")
class HeaderPropagatingInterceptorTest {

  private Tracer tracer;
  private Propagator propagator;
  private Span currentSpan;
  private TraceContext traceContext;
  private io.micrometer.tracing.CurrentTraceContext currentTraceContext;
  private HeaderPropagatingInterceptor interceptor;

  @BeforeEach
  void setUp() {
    tracer = mock(Tracer.class);
    propagator = mock(Propagator.class);
    currentSpan = mock(Span.class);
    traceContext = mock(TraceContext.class);
    currentTraceContext = mock(io.micrometer.tracing.CurrentTraceContext.class);

    when(tracer.currentSpan()).thenReturn(currentSpan);
    when(tracer.currentTraceContext()).thenReturn(currentTraceContext);
    when(currentTraceContext.context()).thenReturn(traceContext);

    interceptor = new HeaderPropagatingInterceptor(tracer, propagator);
  }

  @Nested
  @DisplayName("requestInterceptor")
  class RequestInterceptorTest {

    @Test
    @DisplayName("should inject trace headers")
    void shouldInjectTraceHeaders() {
      HttpRequest request = createRequest("{\"query\":\"{ user }\"}");

      // Simulate propagator injecting headers
      doAnswer(
              invocation -> {
                Map<String, String> carrier = invocation.getArgument(1);
                Propagator.Setter<Map<String, String>> setter = invocation.getArgument(2);
                setter.set(carrier, "X-B3-TraceId", "abc123");
                setter.set(carrier, "X-B3-SpanId", "def456");
                return null;
              })
          .when(propagator)
          .inject(any(), any(), any());

      HttpRequest result = interceptor.requestInterceptor().intercept(request);

      assertThat(result.headers()).containsEntry("X-B3-TraceId", "abc123");
      assertThat(result.headers()).containsEntry("X-B3-SpanId", "def456");
    }

    @Test
    @DisplayName("should preserve original request when no span")
    void shouldPreserveOriginalRequestWhenNoSpan() {
      when(tracer.currentSpan()).thenReturn(null);
      HttpRequest request = createRequest("{\"query\":\"{ user }\"}");

      HttpRequest result = interceptor.requestInterceptor().intercept(request);

      assertThat(result).isSameAs(request);
    }

    @Test
    @DisplayName("should preserve original headers")
    void shouldPreserveOriginalHeaders() {
      HttpRequest request =
          new HttpRequest(
              HttpMethod.POST,
              URI.create("https://api.example.com/graphql"),
              Map.of("Authorization", "Bearer token"),
              "{\"query\":\"{ user }\"}");

      doAnswer(
              invocation -> {
                Map<String, String> carrier = invocation.getArgument(1);
                Propagator.Setter<Map<String, String>> setter = invocation.getArgument(2);
                setter.set(carrier, "X-B3-TraceId", "abc123");
                return null;
              })
          .when(propagator)
          .inject(any(), any(), any());

      HttpRequest result = interceptor.requestInterceptor().intercept(request);

      assertThat(result.headers()).containsEntry("Authorization", "Bearer token");
      assertThat(result.headers()).containsEntry("X-B3-TraceId", "abc123");
    }
  }

  @Nested
  @DisplayName("getters")
  class Getters {

    @Test
    @DisplayName("should return tracer")
    void shouldReturnTracer() {
      assertThat(interceptor.getTracer()).isSameAs(tracer);
    }

    @Test
    @DisplayName("should return propagator")
    void shouldReturnPropagator() {
      assertThat(interceptor.getPropagator()).isSameAs(propagator);
    }
  }

  private HttpRequest createRequest(String body) {
    return new HttpRequest(
        HttpMethod.POST, URI.create("https://api.example.com/graphql"), Map.of(), body);
  }
}
