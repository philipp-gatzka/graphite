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

import java.util.UUID;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Coercing implementation for UUID scalar type.
 *
 * <p>This coercing maps the GraphQL UUID scalar to {@link UUID}. Values are serialized and
 * deserialized using the standard UUID string format (e.g.,
 * "550e8400-e29b-41d4-a716-446655440000").
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ScalarRegistry registry = ScalarRegistry.builder()
 *     .register("UUID", new UuidCoercing())
 *     .build();
 * }</pre>
 *
 * @see UUID
 * @see ScalarCoercing
 */
public final class UuidCoercing implements ScalarCoercing<UUID> {

  /** Singleton instance for convenience. */
  public static final UuidCoercing INSTANCE = new UuidCoercing();

  @Override
  @NotNull
  public Class<UUID> javaType() {
    return UUID.class;
  }

  @Override
  @Nullable
  public Object serialize(@Nullable UUID value) {
    if (value == null) {
      return null;
    }
    return value.toString();
  }

  @Override
  @Nullable
  public UUID deserialize(@Nullable Object input) {
    if (input == null) {
      return null;
    }
    if (input instanceof UUID uuid) {
      return uuid;
    }
    if (input instanceof String str) {
      try {
        return UUID.fromString(str);
      } catch (IllegalArgumentException e) {
        throw new IllegalArgumentException("Cannot parse UUID value: " + str, e);
      }
    }
    throw new IllegalArgumentException("Cannot coerce " + input.getClass().getName() + " to UUID");
  }
}
