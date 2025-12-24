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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.jetbrains.annotations.NotNull;

/**
 * Registry for custom GraphQL scalar type coercings.
 *
 * <p>The ScalarRegistry maps GraphQL scalar type names to their corresponding {@link
 * ScalarCoercing} implementations. This allows the client to properly serialize and deserialize
 * custom scalar types.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ScalarRegistry registry = ScalarRegistry.builder()
 *     .register("DateTime", new DateTimeCoercing())
 *     .register("UUID", new UuidCoercing())
 *     .register("JSON", new JsonCoercing())
 *     .build();
 *
 * GraphiteClient client = GraphiteClient.builder()
 *     .endpoint("https://api.example.com/graphql")
 *     .scalarRegistry(registry)
 *     .build();
 * }</pre>
 *
 * @see ScalarCoercing
 */
public final class ScalarRegistry {

  private static final ScalarRegistry EMPTY = new ScalarRegistry(Map.of());

  private final Map<String, ScalarCoercing<?>> coercings;

  private ScalarRegistry(Map<String, ScalarCoercing<?>> coercings) {
    this.coercings = Collections.unmodifiableMap(new HashMap<>(coercings));
  }

  /**
   * Returns an empty scalar registry with no custom coercings.
   *
   * @return an empty registry
   */
  @NotNull
  public static ScalarRegistry empty() {
    return EMPTY;
  }

  /**
   * Returns a registry with default scalar coercings.
   *
   * <p>The default registry includes coercings for common scalar types that are not handled by
   * Jackson's default serialization.
   *
   * @return a registry with defaults
   */
  @NotNull
  public static ScalarRegistry defaults() {
    // For now, return empty. Built-in coercings will be added in a future PR.
    return EMPTY;
  }

  /**
   * Creates a new builder for constructing a ScalarRegistry.
   *
   * @return a new builder
   */
  @NotNull
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Looks up a coercing by scalar type name.
   *
   * @param scalarName the GraphQL scalar type name
   * @return the coercing if registered, empty otherwise
   */
  @NotNull
  public Optional<ScalarCoercing<?>> get(@NotNull String scalarName) {
    return Optional.ofNullable(coercings.get(scalarName));
  }

  /**
   * Returns whether a coercing is registered for the given scalar type.
   *
   * @param scalarName the GraphQL scalar type name
   * @return {@code true} if a coercing is registered
   */
  public boolean contains(@NotNull String scalarName) {
    return coercings.containsKey(scalarName);
  }

  /**
   * Returns all registered scalar names.
   *
   * @return an unmodifiable set of scalar names
   */
  @NotNull
  public java.util.Set<String> scalarNames() {
    return coercings.keySet();
  }

  /**
   * Returns the number of registered coercings.
   *
   * @return the count of registered coercings
   */
  public int size() {
    return coercings.size();
  }

  /**
   * Returns whether this registry is empty.
   *
   * @return {@code true} if no coercings are registered
   */
  public boolean isEmpty() {
    return coercings.isEmpty();
  }

  /** Builder for constructing {@link ScalarRegistry} instances. */
  public static final class Builder {

    private final Map<String, ScalarCoercing<?>> coercings = new HashMap<>();

    private Builder() {}

    /**
     * Registers a coercing for a scalar type.
     *
     * @param scalarName the GraphQL scalar type name
     * @param coercing the coercing implementation
     * @return this builder
     * @throws NullPointerException if scalarName or coercing is null
     */
    @NotNull
    public Builder register(@NotNull String scalarName, @NotNull ScalarCoercing<?> coercing) {
      if (scalarName == null) {
        throw new NullPointerException("scalarName must not be null");
      }
      if (coercing == null) {
        throw new NullPointerException("coercing must not be null");
      }
      coercings.put(scalarName, coercing);
      return this;
    }

    /**
     * Registers all coercings from another registry.
     *
     * @param registry the registry to copy from
     * @return this builder
     */
    @NotNull
    public Builder registerAll(@NotNull ScalarRegistry registry) {
      coercings.putAll(registry.coercings);
      return this;
    }

    /**
     * Builds the scalar registry.
     *
     * @return the constructed registry
     */
    @NotNull
    public ScalarRegistry build() {
      if (coercings.isEmpty()) {
        return EMPTY;
      }
      return new ScalarRegistry(coercings);
    }
  }
}
