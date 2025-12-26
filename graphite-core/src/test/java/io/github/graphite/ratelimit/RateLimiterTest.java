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
package io.github.graphite.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("RateLimiter")
class RateLimiterTest {

  @Nested
  @DisplayName("create")
  class Create {

    @Test
    @DisplayName("should create limiter with specified rate")
    void shouldCreateLimiterWithSpecifiedRate() {
      var limiter = RateLimiter.create(50.0);

      assertThat(limiter.getRequestsPerSecond()).isEqualTo(50.0);
      assertThat(limiter.getBurstCapacity()).isEqualTo(50);
    }

    @Test
    @DisplayName("should reject zero rate")
    void shouldRejectZeroRate() {
      assertThatThrownBy(() -> RateLimiter.create(0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("requestsPerSecond must be positive");
    }

    @Test
    @DisplayName("should reject negative rate")
    void shouldRejectNegativeRate() {
      assertThatThrownBy(() -> RateLimiter.create(-1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("requestsPerSecond must be positive");
    }
  }

  @Nested
  @DisplayName("builder")
  class Builder {

    @Test
    @DisplayName("should create limiter with custom settings")
    void shouldCreateLimiterWithCustomSettings() {
      var limiter = RateLimiter.builder().requestsPerSecond(100).burstCapacity(200).build();

      assertThat(limiter.getRequestsPerSecond()).isEqualTo(100.0);
      assertThat(limiter.getBurstCapacity()).isEqualTo(200);
    }

    @Test
    @DisplayName("should use rate as default burst capacity")
    void shouldUseRateAsDefaultBurstCapacity() {
      var limiter = RateLimiter.builder().requestsPerSecond(75).build();

      assertThat(limiter.getBurstCapacity()).isEqualTo(75);
    }

    @Test
    @DisplayName("should reject zero burst capacity")
    void shouldRejectZeroBurstCapacity() {
      var builder = RateLimiter.builder();
      assertThatThrownBy(() -> builder.burstCapacity(0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("burstCapacity must be positive");
    }

    @Test
    @DisplayName("should reject negative burst capacity")
    void shouldRejectNegativeBurstCapacity() {
      var builder = RateLimiter.builder();
      assertThatThrownBy(() -> builder.burstCapacity(-1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("burstCapacity must be positive");
    }
  }

  @Nested
  @DisplayName("tryAcquire")
  class TryAcquire {

    @Test
    @DisplayName("should succeed when tokens available")
    void shouldSucceedWhenTokensAvailable() {
      var limiter = RateLimiter.builder().requestsPerSecond(100).burstCapacity(10).build();

      assertThat(limiter.tryAcquire()).isTrue();
      assertThat(limiter.tryAcquire()).isTrue();
    }

    @Test
    @DisplayName("should fail when no tokens available")
    void shouldFailWhenNoTokensAvailable() {
      var limiter = RateLimiter.builder().requestsPerSecond(100).burstCapacity(2).build();

      assertThat(limiter.tryAcquire()).isTrue();
      assertThat(limiter.tryAcquire()).isTrue();
      assertThat(limiter.tryAcquire()).isFalse();
    }

    @Test
    @DisplayName("should acquire multiple permits")
    void shouldAcquireMultiplePermits() {
      var limiter = RateLimiter.builder().requestsPerSecond(100).burstCapacity(10).build();

      assertThat(limiter.tryAcquire(5)).isTrue();
      assertThat(limiter.tryAcquire(5)).isTrue();
      assertThat(limiter.tryAcquire(1)).isFalse();
    }

    @Test
    @DisplayName("should reject zero permits")
    void shouldRejectZeroPermits() {
      var limiter = RateLimiter.create(100);

      assertThatThrownBy(() -> limiter.tryAcquire(0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("permits must be positive");
    }

    @Test
    @DisplayName("should reject negative permits")
    void shouldRejectNegativePermits() {
      var limiter = RateLimiter.create(100);

      assertThatThrownBy(() -> limiter.tryAcquire(-1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("permits must be positive");
    }
  }

  @Nested
  @DisplayName("tryAcquire with timeout")
  class TryAcquireWithTimeout {

    @Test
    @DisplayName("should succeed immediately when tokens available")
    void shouldSucceedImmediatelyWhenTokensAvailable() throws InterruptedException {
      var limiter = RateLimiter.builder().requestsPerSecond(100).burstCapacity(10).build();

      assertThat(limiter.tryAcquire(Duration.ofMillis(100))).isTrue();
    }

    @Test
    @DisplayName("should fail when timeout too short")
    void shouldFailWhenTimeoutTooShort() throws InterruptedException {
      var limiter = RateLimiter.builder().requestsPerSecond(1).burstCapacity(1).build();

      limiter.tryAcquire(); // Exhaust token
      assertThat(limiter.tryAcquire(Duration.ofMillis(10))).isFalse();
    }

    @Test
    @DisplayName("should reject null timeout")
    void shouldRejectNullTimeout() {
      var limiter = RateLimiter.create(100);

      assertThatThrownBy(() -> limiter.tryAcquire(1, null))
          .isInstanceOf(NullPointerException.class)
          .hasMessageContaining("timeout must not be null");
    }
  }

  @Nested
  @DisplayName("acquire")
  class Acquire {

    @Test
    @DisplayName("should succeed immediately when tokens available")
    void shouldSucceedImmediatelyWhenTokensAvailable() throws InterruptedException {
      var limiter = RateLimiter.builder().requestsPerSecond(100).burstCapacity(10).build();

      long start = System.nanoTime();
      limiter.acquire();
      long elapsed = System.nanoTime() - start;

      // Should complete almost instantly
      assertThat(elapsed).isLessThan(50_000_000); // 50ms
    }

    @Test
    @DisplayName("should reject zero permits")
    void shouldRejectZeroPermits() {
      var limiter = RateLimiter.create(100);

      assertThatThrownBy(() -> limiter.acquire(0))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("permits must be positive");
    }

    @Test
    @DisplayName("should reject negative permits")
    void shouldRejectNegativePermits() {
      var limiter = RateLimiter.create(100);

      assertThatThrownBy(() -> limiter.acquire(-1))
          .isInstanceOf(IllegalArgumentException.class)
          .hasMessageContaining("permits must be positive");
    }
  }

  @Nested
  @DisplayName("availablePermits")
  class AvailablePermits {

    @Test
    @DisplayName("should return burst capacity initially")
    void shouldReturnBurstCapacityInitially() {
      var limiter = RateLimiter.builder().requestsPerSecond(100).burstCapacity(50).build();

      assertThat(limiter.availablePermits()).isEqualTo(50);
    }

    @Test
    @DisplayName("should decrease after acquiring")
    void shouldDecreaseAfterAcquiring() {
      var limiter = RateLimiter.builder().requestsPerSecond(100).burstCapacity(50).build();

      limiter.tryAcquire(10);

      assertThat(limiter.availablePermits()).isEqualTo(40);
    }
  }

  @Nested
  @DisplayName("token refill")
  class TokenRefill {

    @Test
    @DisplayName("should refill tokens over time")
    void shouldRefillTokensOverTime() {
      var limiter = RateLimiter.builder().requestsPerSecond(1000).burstCapacity(10).build();

      // Exhaust all tokens
      for (int i = 0; i < 10; i++) {
        limiter.tryAcquire();
      }
      assertThat(limiter.availablePermits()).isZero();

      // Wait for refill using Awaitility
      await()
          .atMost(Duration.ofMillis(200))
          .untilAsserted(() -> assertThat(limiter.availablePermits()).isGreaterThan(0));
    }

    @Test
    @DisplayName("should not exceed burst capacity")
    void shouldNotExceedBurstCapacity() {
      var limiter = RateLimiter.builder().requestsPerSecond(1000).burstCapacity(10).build();

      // Verify burst capacity is not exceeded after waiting for potential overfill
      await()
          .pollDelay(Duration.ofMillis(100))
          .atMost(Duration.ofMillis(150))
          .untilAsserted(() -> assertThat(limiter.availablePermits()).isLessThanOrEqualTo(10));
    }
  }
}
