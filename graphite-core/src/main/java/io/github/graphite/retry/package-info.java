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
 * Retry and backoff strategies for handling transient failures.
 *
 * <p>This package provides components for implementing retry logic:
 *
 * <ul>
 *   <li>{@link io.github.graphite.retry.RetryPolicy} - Configuration for retry behavior
 *   <li>{@link io.github.graphite.retry.BackoffStrategy} - Strategy for calculating delays
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * RetryPolicy policy = RetryPolicy.builder()
 *     .maxAttempts(3)
 *     .backoffStrategy(ExponentialBackoff.builder()
 *         .initialDelay(Duration.ofMillis(100))
 *         .maxDelay(Duration.ofSeconds(30))
 *         .multiplier(2.0)
 *         .build())
 *     .build();
 * }</pre>
 *
 * @see io.github.graphite.retry.RetryPolicy
 * @see io.github.graphite.retry.BackoffStrategy
 */
package io.github.graphite.retry;
