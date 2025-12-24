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

import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Coercing implementation for Time scalar type.
 *
 * <p>This coercing maps the GraphQL Time scalar to {@link LocalTime}. Values are serialized and
 * deserialized using ISO-8601 format (e.g., "10:30:00").
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ScalarRegistry registry = ScalarRegistry.builder()
 *     .register("Time", new TimeCoercing())
 *     .build();
 * }</pre>
 *
 * @see LocalTime
 * @see ScalarCoercing
 */
public final class TimeCoercing implements ScalarCoercing<LocalTime> {

  /** Singleton instance for convenience. */
  public static final TimeCoercing INSTANCE = new TimeCoercing();

  @Override
  @NotNull
  public Class<LocalTime> javaType() {
    return LocalTime.class;
  }

  @Override
  @Nullable
  public Object serialize(@Nullable LocalTime value) {
    if (value == null) {
      return null;
    }
    return value.toString();
  }

  @Override
  @Nullable
  public LocalTime deserialize(@Nullable Object input) {
    if (input == null) {
      return null;
    }
    if (input instanceof LocalTime time) {
      return time;
    }
    if (input instanceof String str) {
      try {
        return LocalTime.parse(str);
      } catch (DateTimeParseException e) {
        throw new IllegalArgumentException("Cannot parse Time value: " + str, e);
      }
    }
    throw new IllegalArgumentException(
        "Cannot coerce " + input.getClass().getName() + " to LocalTime");
  }
}
