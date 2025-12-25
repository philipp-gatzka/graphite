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

import java.math.BigDecimal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Coercing implementation for BigDecimal scalar type.
 *
 * <p>This coercing maps the GraphQL BigDecimal scalar to {@link BigDecimal}. Values are serialized
 * as strings to preserve precision, and can be deserialized from strings or numbers.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ScalarRegistry registry = ScalarRegistry.builder()
 *     .register("BigDecimal", new BigDecimalCoercing())
 *     .build();
 * }</pre>
 *
 * @see BigDecimal
 * @see ScalarCoercing
 */
@SuppressWarnings("java:S6548") // Singleton is appropriate for stateless, immutable coercing
public final class BigDecimalCoercing implements ScalarCoercing<BigDecimal> {

  /** Singleton instance for convenience. */
  public static final BigDecimalCoercing INSTANCE = new BigDecimalCoercing();

  @Override
  @NotNull
  public Class<BigDecimal> javaType() {
    return BigDecimal.class;
  }

  @Override
  @Nullable
  public Object serialize(@Nullable BigDecimal value) {
    if (value == null) {
      return null;
    }
    // Serialize as string to preserve precision
    return value.toPlainString();
  }

  @Override
  @Nullable
  public BigDecimal deserialize(@Nullable Object input) {
    if (input == null) {
      return null;
    }
    if (input instanceof BigDecimal bd) {
      return bd;
    }
    if (input instanceof String str) {
      try {
        return new BigDecimal(str.trim());
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Cannot parse BigDecimal value: " + str, e);
      }
    }
    if (input instanceof Number number) {
      return BigDecimal.valueOf(number.doubleValue());
    }
    throw new IllegalArgumentException(
        "Cannot coerce " + input.getClass().getName() + " to BigDecimal");
  }
}
