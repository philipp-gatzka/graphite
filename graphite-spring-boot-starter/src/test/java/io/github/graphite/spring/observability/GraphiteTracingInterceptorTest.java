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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.github.graphite.http.HttpMethod;
import io.github.graphite.http.HttpRequest;
import io.github.graphite.http.HttpResponse;
import io.github.graphite.interceptor.RequestInterceptor;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GraphiteTracingInterceptor")
class GraphiteTracingInterceptorTest {

  private Tracer tracer;
  private Span span;
  private GraphiteTracingInterceptor interceptor;

  @BeforeEach
  void setUp() {
    tracer = mock(Tracer.class);
    span = mock(Span.class);
    when(tracer.nextSpan()).thenReturn(span);
    when(span.name(anyString())).thenReturn(span);
    when(span.tag(anyString(), anyString())).thenReturn(span);
    when(span.start()).thenReturn(span);
    interceptor = new GraphiteTracingInterceptor(tracer);
  }

  @Nested
  @DisplayName("request and response flow")
  class RequestResponseFlow {

    @Test
    @DisplayName("should create span on request")
    void shouldCreateSpanOnRequest() {
      HttpRequest request = createRequest("{\"operationName\":\"GetUser\",\"query\":\"{ user }\"}");

      RequestInterceptor reqInterceptor = interceptor.requestInterceptor();
      reqInterceptor.intercept(request);

      verify(tracer).nextSpan();
      verify(span).start();
    }

    @Test
    @DisplayName("should set span name with operation")
    void shouldSetSpanNameWithOperation() {
      HttpRequest request = createRequest("{\"operationName\":\"GetUser\",\"query\":\"{ user }\"}");

      interceptor.requestInterceptor().intercept(request);

      verify(span).name("graphite GetUser");
    }

    @Test
    @DisplayName("should tag span with http url")
    void shouldTagSpanWithHttpUrl() {
      HttpRequest request = createRequest("{\"query\":\"{ user }\"}");

      interceptor.requestInterceptor().intercept(request);

      verify(span).tag(GraphiteTracingInterceptor.ATTR_HTTP_URL, "https://api.example.com/graphql");
    }

    @Test
    @DisplayName("should end span on response")
    void shouldEndSpanOnResponse() {
      HttpRequest request = createRequest("{\"operationName\":\"GetUser\",\"query\":\"{ user }\"}");
      HttpResponse response = new HttpResponse(200, Map.of(), "{}");

      interceptor.requestInterceptor().intercept(request);
      interceptor.responseInterceptor().intercept(response);

      verify(span).end();
    }

    @Test
    @DisplayName("should tag span with status code")
    void shouldTagSpanWithStatusCode() {
      HttpRequest request = createRequest("{\"query\":\"{ user }\"}");
      HttpResponse response = new HttpResponse(200, Map.of(), "{}");

      interceptor.requestInterceptor().intercept(request);
      interceptor.responseInterceptor().intercept(response);

      verify(span).tag(GraphiteTracingInterceptor.ATTR_HTTP_STATUS_CODE, "200");
    }

    @Test
    @DisplayName("should mark span as error for non-2xx status")
    void shouldMarkSpanAsErrorForNon2xxStatus() {
      HttpRequest request = createRequest("{\"query\":\"{ user }\"}");
      HttpResponse response = new HttpResponse(500, Map.of(), "{}");

      interceptor.requestInterceptor().intercept(request);
      interceptor.responseInterceptor().intercept(response);

      verify(span).error(any(RuntimeException.class));
    }

    @Test
    @DisplayName("should not mark span as error for 2xx status")
    void shouldNotMarkSpanAsErrorFor2xxStatus() {
      HttpRequest request = createRequest("{\"query\":\"{ user }\"}");
      HttpResponse response = new HttpResponse(200, Map.of(), "{}");

      interceptor.requestInterceptor().intercept(request);
      interceptor.responseInterceptor().intercept(response);

      verify(span, never()).error(any());
    }
  }

  @Nested
  @DisplayName("operation extraction")
  class OperationExtraction {

    @Test
    @DisplayName("should tag with operation name from JSON")
    void shouldTagWithOperationNameFromJson() {
      HttpRequest request =
          createRequest("{\"operationName\":\"CreateUser\",\"query\":\"mutation { }\"}");

      interceptor.requestInterceptor().intercept(request);

      verify(span).tag(GraphiteTracingInterceptor.ATTR_OPERATION_NAME, "CreateUser");
    }

    @Test
    @DisplayName("should tag with query operation type")
    void shouldTagWithQueryOperationType() {
      HttpRequest request = createRequest("{\"query\":\"query GetUser { user }\"}");

      interceptor.requestInterceptor().intercept(request);

      verify(span).tag(GraphiteTracingInterceptor.ATTR_OPERATION_TYPE, "query");
    }

    @Test
    @DisplayName("should tag with mutation operation type")
    void shouldTagWithMutationOperationType() {
      HttpRequest request = createRequest("{\"query\":\"mutation CreateUser { createUser }\"}");

      interceptor.requestInterceptor().intercept(request);

      verify(span).tag(GraphiteTracingInterceptor.ATTR_OPERATION_TYPE, "mutation");
    }

    @Test
    @DisplayName("should tag with subscription operation type")
    void shouldTagWithSubscriptionOperationType() {
      HttpRequest request = createRequest("{\"query\":\"subscription OnMessage { onMessage }\"}");

      interceptor.requestInterceptor().intercept(request);

      verify(span).tag(GraphiteTracingInterceptor.ATTR_OPERATION_TYPE, "subscription");
    }

    @Test
    @DisplayName("should extract name from query when operationName not present")
    void shouldExtractNameFromQuery() {
      HttpRequest request = createRequest("{\"query\":\"query GetUserById { user }\"}");

      interceptor.requestInterceptor().intercept(request);

      verify(span).tag(GraphiteTracingInterceptor.ATTR_OPERATION_NAME, "GetUserById");
    }

    @Test
    @DisplayName("should use unknown span name for anonymous query")
    void shouldUseUnknownForAnonymousQuery() {
      HttpRequest request = createRequest("{\"query\":\"{ user }\"}");

      interceptor.requestInterceptor().intercept(request);

      verify(span).name("graphite unknown");
    }
  }

  @Nested
  @DisplayName("recordError")
  class RecordError {

    @Test
    @DisplayName("should record error on span")
    void shouldRecordErrorOnSpan() {
      HttpRequest request = createRequest("{\"query\":\"{ user }\"}");
      RuntimeException error = new RuntimeException("test error");

      interceptor.requestInterceptor().intercept(request);
      interceptor.recordError(error);

      verify(span).error(error);
      verify(span).end();
    }
  }

  @Nested
  @DisplayName("getTracer")
  class GetTracer {

    @Test
    @DisplayName("should return tracer")
    void shouldReturnTracer() {
      assertThat(interceptor.getTracer()).isSameAs(tracer);
    }
  }

  private HttpRequest createRequest(String body) {
    return new HttpRequest(
        HttpMethod.POST, URI.create("https://api.example.com/graphql"), Map.of(), body);
  }
}
