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

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Coercing implementation for Date scalar type.
 *
 * <p>This coercing maps the GraphQL Date scalar to {@link LocalDate}. Values are serialized and
 * deserialized using ISO-8601 format (e.g., "2024-01-15").
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ScalarRegistry registry = ScalarRegistry.builder()
 *     .register("Date", new DateCoercing())
 *     .build();
 * }</pre>
 *
 * @see LocalDate
 * @see ScalarCoercing
 */
@SuppressWarnings("java:S6548") // Singleton is appropriate for stateless, immutable coercing
public final class DateCoercing implements ScalarCoercing<LocalDate> {

  /** Singleton instance for convenience. */
  public static final DateCoercing INSTANCE = new DateCoercing();

  @Override
  @NotNull
  public Class<LocalDate> javaType() {
    return LocalDate.class;
  }

  @Override
  @Nullable
  public Object serialize(@Nullable LocalDate value) {
    if (value == null) {
      return null;
    }
    return value.toString();
  }

  @Override
  @Nullable
  public LocalDate deserialize(@Nullable Object input) {
    if (input == null) {
      return null;
    }
    if (input instanceof LocalDate date) {
      return date;
    }
    if (input instanceof String str) {
      try {
        return LocalDate.parse(str);
      } catch (DateTimeParseException e) {
        throw new IllegalArgumentException("Cannot parse Date value: " + str, e);
      }
    }
    throw new IllegalArgumentException(
        "Cannot coerce " + input.getClass().getName() + " to LocalDate");
  }
}
