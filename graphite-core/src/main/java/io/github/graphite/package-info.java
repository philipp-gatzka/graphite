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
 * Graphite Core module providing the runtime client library for type-safe GraphQL queries.
 *
 * <p>This module contains:
 *
 * <ul>
 *   <li>{@code GraphiteClient} - The main client interface for executing GraphQL operations
 *   <li>{@code GraphiteResponse} - Response wrapper with data, errors, and extensions
 *   <li>HTTP transport layer using Java 11+ HttpClient
 *   <li>Retry and rate limiting mechanisms
 *   <li>Custom scalar support
 *   <li>Request and response interceptors
 * </ul>
 *
 * @see io.github.graphite.GraphiteClient
 */
package io.github.graphite;
