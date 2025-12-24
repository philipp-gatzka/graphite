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

/**
 * Rate limiting components using the token bucket algorithm.
 *
 * <p>This package provides rate limiting functionality to control the rate of requests to GraphQL
 * endpoints:
 *
 * <ul>
 *   <li>{@link io.github.graphite.ratelimit.RateLimiter} - Token bucket rate limiter
 *   <li>{@link io.github.graphite.ratelimit.RateLimitConfig} - Configuration for rate limiting
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * RateLimiter limiter = RateLimiter.builder()
 *     .requestsPerSecond(100)
 *     .burstCapacity(150)
 *     .build();
 *
 * // Non-blocking
 * if (limiter.tryAcquire()) {
 *     executeRequest();
 * }
 *
 * // Blocking
 * limiter.acquire();
 * executeRequest();
 * }</pre>
 *
 * @see io.github.graphite.ratelimit.RateLimiter
 * @see io.github.graphite.ratelimit.RateLimitConfig
 */
package io.github.graphite.ratelimit;
