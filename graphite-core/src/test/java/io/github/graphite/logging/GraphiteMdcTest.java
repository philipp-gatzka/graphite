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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

@DisplayName("GraphiteMdc")
@SuppressWarnings("try")
class GraphiteMdcTest {

  @AfterEach
  void clearMdc() {
    MDC.clear();
  }

  @Nested
  @DisplayName("start")
  class Start {

    @Test
    @DisplayName("should set operation name in MDC")
    void shouldSetOperationName() {
      try (var ctx = GraphiteMdc.start("GetUser")) {
        assertThat(MDC.get(GraphiteMdc.KEY_OPERATION)).isEqualTo("GetUser");
      }
    }

    @Test
    @DisplayName("should generate request ID")
    void shouldGenerateRequestId() {
      try (var ctx = GraphiteMdc.start("GetUser")) {
        assertThat(MDC.get(GraphiteMdc.KEY_REQUEST_ID)).isNotNull().hasSize(8);
      }
    }

    @Test
    @DisplayName("should set correlation ID when provided")
    void shouldSetCorrelationId() {
      try (var ctx = GraphiteMdc.start("GetUser", "corr-123")) {
        assertThat(MDC.get(GraphiteMdc.KEY_CORRELATION_ID)).isEqualTo("corr-123");
      }
    }

    @Test
    @DisplayName("should not set correlation ID when null")
    void shouldNotSetCorrelationIdWhenNull() {
      try (var ctx = GraphiteMdc.start("GetUser", null)) {
        assertThat(MDC.get(GraphiteMdc.KEY_CORRELATION_ID)).isNull();
      }
    }

    @Test
    @DisplayName("should not set correlation ID when blank")
    void shouldNotSetCorrelationIdWhenBlank() {
      try (var ctx = GraphiteMdc.start("GetUser", "   ")) {
        assertThat(MDC.get(GraphiteMdc.KEY_CORRELATION_ID)).isNull();
      }
    }
  }

  @Nested
  @DisplayName("Context close")
  class ContextClose {

    @Test
    @DisplayName("should clear operation name on close")
    void shouldClearOperationName() {
      try (var ctx = GraphiteMdc.start("GetUser")) {
        assertThat(MDC.get(GraphiteMdc.KEY_OPERATION)).isNotNull();
      }
      assertThat(MDC.get(GraphiteMdc.KEY_OPERATION)).isNull();
    }

    @Test
    @DisplayName("should clear request ID on close")
    void shouldClearRequestId() {
      try (var ctx = GraphiteMdc.start("GetUser")) {
        assertThat(MDC.get(GraphiteMdc.KEY_REQUEST_ID)).isNotNull();
      }
      assertThat(MDC.get(GraphiteMdc.KEY_REQUEST_ID)).isNull();
    }

    @Test
    @DisplayName("should clear correlation ID on close when set")
    void shouldClearCorrelationId() {
      try (var ctx = GraphiteMdc.start("GetUser", "corr-123")) {
        assertThat(MDC.get(GraphiteMdc.KEY_CORRELATION_ID)).isNotNull();
      }
      assertThat(MDC.get(GraphiteMdc.KEY_CORRELATION_ID)).isNull();
    }
  }

  @Nested
  @DisplayName("getters")
  class Getters {

    @Test
    @DisplayName("getRequestId should return current request ID")
    void getRequestIdShouldReturnCurrentValue() {
      try (var ctx = GraphiteMdc.start("GetUser")) {
        assertThat(GraphiteMdc.getRequestId()).isNotNull().hasSize(8);
      }
    }

    @Test
    @DisplayName("getRequestId should return null when not set")
    void getRequestIdShouldReturnNullWhenNotSet() {
      assertThat(GraphiteMdc.getRequestId()).isNull();
    }

    @Test
    @DisplayName("getOperationName should return current operation name")
    void getOperationNameShouldReturnCurrentValue() {
      try (var ctx = GraphiteMdc.start("GetUser")) {
        assertThat(GraphiteMdc.getOperationName()).isEqualTo("GetUser");
      }
    }

    @Test
    @DisplayName("getOperationName should return null when not set")
    void getOperationNameShouldReturnNullWhenNotSet() {
      assertThat(GraphiteMdc.getOperationName()).isNull();
    }

    @Test
    @DisplayName("getCorrelationId should return current correlation ID")
    void getCorrelationIdShouldReturnCurrentValue() {
      try (var ctx = GraphiteMdc.start("GetUser", "corr-123")) {
        assertThat(GraphiteMdc.getCorrelationId()).isEqualTo("corr-123");
      }
    }

    @Test
    @DisplayName("getCorrelationId should return null when not set")
    void getCorrelationIdShouldReturnNullWhenNotSet() {
      assertThat(GraphiteMdc.getCorrelationId()).isNull();
    }
  }
}
