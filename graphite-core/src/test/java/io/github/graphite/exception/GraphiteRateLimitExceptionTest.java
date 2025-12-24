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

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("GraphiteRateLimitException")
class GraphiteRateLimitExceptionTest {

    @Nested
    @DisplayName("constructor")
    class Constructor {

        @Test
        @DisplayName("should create exception with message only")
        void shouldCreateWithMessageOnly() {
            var exception = new GraphiteRateLimitException("Rate limit exceeded");

            assertThat(exception.getMessage()).isEqualTo("Rate limit exceeded");
            assertThat(exception.getRetryAfter()).isNull();
            assertThat(exception.getResetTime()).isNull();
            assertThat(exception.getLimit()).isNull();
            assertThat(exception.getRemaining()).isNull();
        }

        @Test
        @DisplayName("should create exception with retry-after")
        void shouldCreateWithRetryAfter() {
            var retryAfter = Duration.ofSeconds(30);
            var exception = new GraphiteRateLimitException("Rate limit exceeded", retryAfter);

            assertThat(exception.getMessage()).isEqualTo("Rate limit exceeded");
            assertThat(exception.getRetryAfter()).isEqualTo(retryAfter);
        }

        @Test
        @DisplayName("should create exception with all parameters")
        void shouldCreateWithAllParameters() {
            var retryAfter = Duration.ofSeconds(30);
            var resetTime = Instant.now().plusSeconds(30);
            var limit = 100;
            var remaining = 0;

            var exception =
                    new GraphiteRateLimitException(
                            "Rate limit exceeded", retryAfter, resetTime, limit, remaining);

            assertThat(exception.getMessage()).isEqualTo("Rate limit exceeded");
            assertThat(exception.getRetryAfter()).isEqualTo(retryAfter);
            assertThat(exception.getResetTime()).isEqualTo(resetTime);
            assertThat(exception.getLimit()).isEqualTo(limit);
            assertThat(exception.getRemaining()).isEqualTo(remaining);
        }
    }

    @Nested
    @DisplayName("errorCode")
    class ErrorCode {

        @Test
        @DisplayName("should have RATE_LIMIT_EXCEEDED error code")
        void shouldHaveRateLimitExceededErrorCode() {
            var exception = new GraphiteRateLimitException("Rate limit exceeded");

            assertThat(exception.getErrorCode()).isEqualTo("RATE_LIMIT_EXCEEDED");
        }
    }

    @Nested
    @DisplayName("inheritance")
    class Inheritance {

        @Test
        @DisplayName("should extend GraphiteClientException")
        void shouldExtendGraphiteClientException() {
            var exception = new GraphiteRateLimitException("Test");

            assertThat(exception).isInstanceOf(GraphiteClientException.class);
        }

        @Test
        @DisplayName("should be catchable as GraphiteException")
        void shouldBeCatchableAsGraphiteException() {
            GraphiteException exception = new GraphiteRateLimitException("Test");

            assertThat(exception).isNotNull();
        }
    }
}
