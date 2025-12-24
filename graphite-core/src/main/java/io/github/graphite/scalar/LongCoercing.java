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
 * Coercing implementation for Long scalar type.
 *
 * <p>This coercing maps the GraphQL Long scalar to {@link Long}. Values are serialized as strings
 * to avoid precision loss with large numbers in JavaScript, and can be deserialized from strings or
 * numbers.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ScalarRegistry registry = ScalarRegistry.builder()
 *     .register("Long", new LongCoercing())
 *     .build();
 * }</pre>
 *
 * @see ScalarCoercing
 */
public final class LongCoercing implements ScalarCoercing<Long> {

  /** Singleton instance for convenience. */
  public static final LongCoercing INSTANCE = new LongCoercing();

  @Override
  @NotNull
  public Class<Long> javaType() {
    return Long.class;
  }

  @Override
  @Nullable
  public Object serialize(@Nullable Long value) {
    if (value == null) {
      return null;
    }
    // Serialize as string to avoid precision loss in JavaScript
    return value.toString();
  }

  @Override
  @Nullable
  public Long deserialize(@Nullable Object input) {
    if (input == null) {
      return null;
    }
    if (input instanceof Long l) {
      return l;
    }
    if (input instanceof Number number) {
      return number.longValue();
    }
    if (input instanceof String str) {
      try {
        return Long.parseLong(str.trim());
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Cannot parse Long value: " + str, e);
      }
    }
    throw new IllegalArgumentException("Cannot coerce " + input.getClass().getName() + " to Long");
  }
}
