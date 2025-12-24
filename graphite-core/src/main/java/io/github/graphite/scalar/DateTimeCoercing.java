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

import java.time.Instant;
import java.time.format.DateTimeParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Coercing implementation for DateTime scalar type.
 *
 * <p>This coercing maps the GraphQL DateTime scalar to {@link Instant}. Values are serialized and
 * deserialized using ISO-8601 format (e.g., "2024-01-15T10:30:00Z").
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ScalarRegistry registry = ScalarRegistry.builder()
 *     .register("DateTime", new DateTimeCoercing())
 *     .build();
 * }</pre>
 *
 * @see Instant
 * @see ScalarCoercing
 */
public final class DateTimeCoercing implements ScalarCoercing<Instant> {

  /** Singleton instance for convenience. */
  public static final DateTimeCoercing INSTANCE = new DateTimeCoercing();

  @Override
  @NotNull
  public Class<Instant> javaType() {
    return Instant.class;
  }

  @Override
  @Nullable
  public Object serialize(@Nullable Instant value) {
    if (value == null) {
      return null;
    }
    return value.toString();
  }

  @Override
  @Nullable
  public Instant deserialize(@Nullable Object input) {
    if (input == null) {
      return null;
    }
    if (input instanceof Instant instant) {
      return instant;
    }
    if (input instanceof String str) {
      try {
        return Instant.parse(str);
      } catch (DateTimeParseException e) {
        throw new IllegalArgumentException("Cannot parse DateTime value: " + str, e);
      }
    }
    if (input instanceof Number number) {
      return Instant.ofEpochMilli(number.longValue());
    }
    throw new IllegalArgumentException(
        "Cannot coerce " + input.getClass().getName() + " to Instant");
  }
}
