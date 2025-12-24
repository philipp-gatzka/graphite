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
 * Custom GraphQL scalar type handling.
 *
 * <p>This package provides support for custom GraphQL scalar types:
 *
 * <ul>
 *   <li>{@link io.github.graphite.scalar.ScalarCoercing} - Interface for scalar
 *       serialization/deserialization
 *   <li>{@link io.github.graphite.scalar.ScalarRegistry} - Registry of scalar type handlers
 * </ul>
 *
 * <p>GraphQL scalars are leaf types that represent concrete values. While GraphQL provides built-in
 * scalars (Int, Float, String, Boolean, ID), many APIs define custom scalars for types like:
 *
 * <ul>
 *   <li>DateTime - Maps to {@link java.time.Instant}
 *   <li>Date - Maps to {@link java.time.LocalDate}
 *   <li>UUID - Maps to {@link java.util.UUID}
 *   <li>JSON - Maps to arbitrary JSON objects
 * </ul>
 *
 * <p>Example custom scalar:
 *
 * <pre>{@code
 * public class DateTimeCoercing implements ScalarCoercing<Instant> {
 *     @Override
 *     public Class<Instant> javaType() {
 *         return Instant.class;
 *     }
 *
 *     @Override
 *     public Object serialize(Instant value) {
 *         return value != null ? value.toString() : null;
 *     }
 *
 *     @Override
 *     public Instant deserialize(Object input) {
 *         if (input instanceof String str) {
 *             return Instant.parse(str);
 *         }
 *         throw new IllegalArgumentException("Cannot coerce to Instant: " + input);
 *     }
 * }
 * }</pre>
 *
 * @see io.github.graphite.GraphiteClientBuilder
 */
package io.github.graphite.scalar;
