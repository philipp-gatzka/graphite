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
 * Coercing implementation for Void scalar type.
 *
 * <p>This coercing is used for mutations or fields that return no meaningful value. It always
 * serializes to null and always deserializes to null, regardless of input.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ScalarRegistry registry = ScalarRegistry.builder()
 *     .register("Void", new VoidCoercing())
 *     .build();
 * }</pre>
 *
 * @see ScalarCoercing
 */
public final class VoidCoercing implements ScalarCoercing<Void> {

  /** Singleton instance for convenience. */
  public static final VoidCoercing INSTANCE = new VoidCoercing();

  @Override
  @NotNull
  public Class<Void> javaType() {
    return Void.class;
  }

  @Override
  @Nullable
  public Object serialize(@Nullable Void value) {
    // Void always serializes to null
    return null;
  }

  @Override
  @Nullable
  public Void deserialize(@Nullable Object input) {
    // Void always deserializes to null regardless of input
    return null;
  }
}
