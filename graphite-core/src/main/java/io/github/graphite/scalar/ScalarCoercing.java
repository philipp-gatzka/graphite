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
package io.github.graphite.scalar;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Defines how to serialize and deserialize a custom GraphQL scalar type.
 *
 * <p>GraphQL scalars are leaf types that represent concrete values. While GraphQL provides built-in
 * scalars (Int, Float, String, Boolean, ID), many APIs define custom scalars for types like
 * DateTime, UUID, JSON, etc.
 *
 * <p>This interface defines the coercion (conversion) logic between the scalar's wire format
 * (typically a String or JSON value) and its Java representation.
 *
 * <p>Example implementation for a DateTime scalar:
 *
 * <pre>{@code
 * public class DateTimeCoercing implements ScalarCoercing<Instant> {
 *
 *     @Override
 *     public Class<Instant> javaType() {
 *         return Instant.class;
 *     }
 *
 *     @Override
 *     public Object serialize(Instant value) {
 *         return value.toString(); // ISO-8601 format
 *     }
 *
 *     @Override
 *     public Instant deserialize(Object input) {
 *         if (input instanceof String str) {
 *             return Instant.parse(str);
 *         }
 *         throw new IllegalArgumentException("Cannot coerce " + input + " to Instant");
 *     }
 * }
 * }</pre>
 *
 * @param <T> the Java type that this scalar maps to
 * @see ScalarRegistry
 */
public interface ScalarCoercing<T> {

  /**
   * Returns the Java type that this coercing handles.
   *
   * @return the Java type class
   */
  @NotNull
  Class<T> javaType();

  /**
   * Serializes a Java value to its wire format.
   *
   * <p>This method is called when sending a value to the server, typically when the value is used
   * as a variable.
   *
   * @param value the Java value to serialize (may be {@code null})
   * @return the serialized value, typically a String or primitive
   */
  @Nullable
  Object serialize(@Nullable T value);

  /**
   * Deserializes a wire format value to its Java representation.
   *
   * <p>This method is called when parsing a response from the server. The input is typically a
   * String, Number, or other JSON-compatible type.
   *
   * @param input the wire format value (may be {@code null})
   * @return the deserialized Java value
   * @throws IllegalArgumentException if the input cannot be coerced
   */
  @Nullable
  T deserialize(@Nullable Object input);
}
