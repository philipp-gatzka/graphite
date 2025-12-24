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

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * A rate limiter using the token bucket algorithm.
 *
 * <p>The token bucket algorithm works as follows:
 *
 * <ul>
 *   <li>A bucket holds tokens up to a maximum capacity (burst capacity)
 *   <li>Tokens are added at a fixed rate (requests per second)
 *   <li>Each request consumes one token
 *   <li>If no tokens are available, the request waits or is rejected
 * </ul>
 *
 * <p>This implementation is thread-safe and uses a lazy token refill strategy for efficiency.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * RateLimiter limiter = RateLimiter.builder()
 *     .requestsPerSecond(100)
 *     .burstCapacity(150)
 *     .build();
 *
 * if (limiter.tryAcquire()) {
 *     // Proceed with request
 * } else {
 *     // Handle rate limit exceeded
 * }
 *
 * // Or wait for a permit
 * limiter.acquire(); // Blocks until permit is available
 * }</pre>
 *
 * @see RateLimitConfig
 */
public final class RateLimiter {

  private final double requestsPerSecond;
  private final int burstCapacity;
  private final Object lock = new Object();

  private double availableTokens;
  private long lastRefillTimestamp;

  private RateLimiter(double requestsPerSecond, int burstCapacity) {
    this.requestsPerSecond = requestsPerSecond;
    this.burstCapacity = burstCapacity;
    this.availableTokens = burstCapacity;
    this.lastRefillTimestamp = System.nanoTime();
  }

  /**
   * Creates a rate limiter with the specified rate.
   *
   * @param requestsPerSecond the maximum requests per second
   * @return a new rate limiter
   * @throws IllegalArgumentException if requestsPerSecond is not positive
   */
  public static RateLimiter create(double requestsPerSecond) {
    return builder().requestsPerSecond(requestsPerSecond).build();
  }

  /**
   * Creates a new builder for constructing rate limiters.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Acquires a permit, blocking if necessary until one is available.
   *
   * @throws InterruptedException if interrupted while waiting
   */
  public void acquire() throws InterruptedException {
    acquire(1);
  }

  /**
   * Acquires the specified number of permits, blocking if necessary.
   *
   * @param permits the number of permits to acquire
   * @throws InterruptedException if interrupted while waiting
   * @throws IllegalArgumentException if permits is not positive
   */
  public void acquire(int permits) throws InterruptedException {
    if (permits <= 0) {
      throw new IllegalArgumentException("permits must be positive");
    }

    long waitTimeNanos;
    synchronized (lock) {
      refillTokens();
      waitTimeNanos = reserveTokens(permits);
    }

    if (waitTimeNanos > 0) {
      TimeUnit.NANOSECONDS.sleep(waitTimeNanos);
    }
  }

  /**
   * Tries to acquire a permit without blocking.
   *
   * @return {@code true} if the permit was acquired, {@code false} otherwise
   */
  public boolean tryAcquire() {
    return tryAcquire(1);
  }

  /**
   * Tries to acquire the specified number of permits without blocking.
   *
   * @param permits the number of permits to acquire
   * @return {@code true} if the permits were acquired, {@code false} otherwise
   * @throws IllegalArgumentException if permits is not positive
   */
  public boolean tryAcquire(int permits) {
    if (permits <= 0) {
      throw new IllegalArgumentException("permits must be positive");
    }

    synchronized (lock) {
      refillTokens();
      if (availableTokens >= permits) {
        availableTokens -= permits;
        return true;
      }
      return false;
    }
  }

  /**
   * Tries to acquire a permit, waiting up to the specified timeout.
   *
   * @param timeout the maximum time to wait
   * @return {@code true} if the permit was acquired, {@code false} if timeout elapsed
   * @throws InterruptedException if interrupted while waiting
   * @throws NullPointerException if timeout is null
   */
  public boolean tryAcquire(Duration timeout) throws InterruptedException {
    return tryAcquire(1, timeout);
  }

  /**
   * Tries to acquire permits, waiting up to the specified timeout.
   *
   * @param permits the number of permits to acquire
   * @param timeout the maximum time to wait
   * @return {@code true} if the permits were acquired, {@code false} if timeout elapsed
   * @throws InterruptedException if interrupted while waiting
   * @throws IllegalArgumentException if permits is not positive
   * @throws NullPointerException if timeout is null
   */
  public boolean tryAcquire(int permits, Duration timeout) throws InterruptedException {
    if (permits <= 0) {
      throw new IllegalArgumentException("permits must be positive");
    }
    if (timeout == null) {
      throw new NullPointerException("timeout must not be null");
    }

    long waitTimeNanos;
    synchronized (lock) {
      refillTokens();
      if (availableTokens >= permits) {
        availableTokens -= permits;
        return true;
      }
      waitTimeNanos = calculateWaitTime(permits);
    }

    if (waitTimeNanos <= timeout.toNanos()) {
      TimeUnit.NANOSECONDS.sleep(waitTimeNanos);
      synchronized (lock) {
        availableTokens -= permits;
      }
      return true;
    }
    return false;
  }

  /**
   * Returns the current number of available permits.
   *
   * <p>This is an estimate and may change immediately after returning.
   *
   * @return the number of available permits
   */
  public int availablePermits() {
    synchronized (lock) {
      refillTokens();
      return (int) availableTokens;
    }
  }

  /**
   * Returns the configured requests per second rate.
   *
   * @return the rate in requests per second
   */
  public double getRequestsPerSecond() {
    return requestsPerSecond;
  }

  /**
   * Returns the configured burst capacity.
   *
   * @return the burst capacity
   */
  public int getBurstCapacity() {
    return burstCapacity;
  }

  private void refillTokens() {
    long now = System.nanoTime();
    long elapsedNanos = now - lastRefillTimestamp;

    if (elapsedNanos > 0) {
      double tokensToAdd = (elapsedNanos / 1_000_000_000.0) * requestsPerSecond;
      availableTokens = Math.min(burstCapacity, availableTokens + tokensToAdd);
      lastRefillTimestamp = now;
    }
  }

  private long reserveTokens(int permits) {
    if (availableTokens >= permits) {
      availableTokens -= permits;
      return 0;
    }

    double tokensNeeded = permits - availableTokens;
    long waitTimeNanos = (long) ((tokensNeeded / requestsPerSecond) * 1_000_000_000);
    availableTokens -= permits;
    return waitTimeNanos;
  }

  private long calculateWaitTime(int permits) {
    double tokensNeeded = permits - availableTokens;
    return (long) ((tokensNeeded / requestsPerSecond) * 1_000_000_000);
  }

  /** Builder for creating {@link RateLimiter} instances. */
  public static final class Builder {

    private double requestsPerSecond = 10.0;
    private int burstCapacity = -1;

    private Builder() {}

    /**
     * Sets the maximum requests per second.
     *
     * @param requestsPerSecond the rate limit
     * @return this builder
     * @throws IllegalArgumentException if requestsPerSecond is not positive
     */
    public Builder requestsPerSecond(double requestsPerSecond) {
      if (requestsPerSecond <= 0) {
        throw new IllegalArgumentException("requestsPerSecond must be positive");
      }
      this.requestsPerSecond = requestsPerSecond;
      return this;
    }

    /**
     * Sets the burst capacity (maximum tokens in bucket).
     *
     * <p>If not set, defaults to the requestsPerSecond value.
     *
     * @param burstCapacity the maximum burst size
     * @return this builder
     * @throws IllegalArgumentException if burstCapacity is not positive
     */
    public Builder burstCapacity(int burstCapacity) {
      if (burstCapacity <= 0) {
        throw new IllegalArgumentException("burstCapacity must be positive");
      }
      this.burstCapacity = burstCapacity;
      return this;
    }

    /**
     * Builds the rate limiter.
     *
     * @return the configured rate limiter
     */
    public RateLimiter build() {
      int capacity = burstCapacity > 0 ? burstCapacity : (int) Math.ceil(requestsPerSecond);
      return new RateLimiter(requestsPerSecond, capacity);
    }
  }
}
