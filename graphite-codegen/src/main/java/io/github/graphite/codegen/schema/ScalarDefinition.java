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
package io.github.graphite.codegen.schema;

import java.util.Set;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a scalar type definition in a GraphQL schema.
 *
 * <p>Scalar types represent primitive values. GraphQL includes built-in scalars (String, Int,
 * Float, Boolean, ID) and schemas may define custom scalars.
 *
 * <p>Example GraphQL:
 *
 * <pre>{@code
 * scalar DateTime
 * scalar JSON
 * scalar UUID
 * }</pre>
 *
 * @param name the scalar type name
 * @param description the scalar description, may be null
 * @see SchemaModel
 */
public record ScalarDefinition(@NotNull String name, @Nullable String description) {

  /** The built-in GraphQL scalar types. */
  public static final Set<String> BUILT_IN_SCALARS =
      Set.of("String", "Int", "Float", "Boolean", "ID");

  /**
   * Returns whether this is a built-in GraphQL scalar.
   *
   * @return true if this is a built-in scalar
   */
  public boolean isBuiltIn() {
    return BUILT_IN_SCALARS.contains(name);
  }
}
