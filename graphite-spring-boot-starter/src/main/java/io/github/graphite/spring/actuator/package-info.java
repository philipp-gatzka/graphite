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
 * Spring Boot Actuator integration for the Graphite GraphQL client.
 *
 * <p>This package provides actuator components for monitoring and managing Graphite client
 * operations:
 *
 * <ul>
 *   <li>{@link io.github.graphite.spring.actuator.GraphiteHealthIndicator} - Health indicator for
 *       GraphQL endpoint availability
 * </ul>
 *
 * <p>These components are automatically configured when Spring Boot Actuator is on the classpath.
 *
 * @see io.github.graphite.spring.autoconfigure.GraphiteHealthIndicatorAutoConfiguration
 */
@org.jetbrains.annotations.ApiStatus.Experimental
package io.github.graphite.spring.actuator;
