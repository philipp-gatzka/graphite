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
 * Observability support for the Graphite GraphQL client.
 *
 * <p>This package provides observability components for monitoring Graphite client operations:
 *
 * <ul>
 *   <li>{@link io.github.graphite.spring.observability.GraphiteMetrics} - Facade for recording
 *       Micrometer metrics
 *   <li>{@link io.github.graphite.spring.observability.GraphiteMetricsInterceptor} - Interceptor
 *       for automatic metrics collection
 * </ul>
 *
 * <p>These components are automatically configured when Micrometer is on the classpath.
 *
 * @see io.github.graphite.spring.autoconfigure.GraphiteMetricsAutoConfiguration
 */
@org.jetbrains.annotations.ApiStatus.Experimental
package io.github.graphite.spring.observability;
