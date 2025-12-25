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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an enum value definition in a GraphQL enum type.
 *
 * <p>Example GraphQL:
 *
 * <pre>{@code
 * enum UserStatus {
 *   ACTIVE
 *   INACTIVE @Deprecated(reason: "Use SUSPENDED instead")
 *   SUSPENDED
 * }
 * }</pre>
 *
 * @param name the enum value name
 * @param description the value description, may be null
 * @param isDeprecated whether the value is marked as deprecated in the GraphQL schema
 * @param deprecationReason the deprecation reason, may be null
 * @see EnumDefinition
 */
@SuppressWarnings({"java:S1133", "java:S1123"})
public record EnumValueDefinition(
    @NotNull String name,
    @Nullable String description,
    boolean isDeprecated,
    @Nullable String deprecationReason) {

  /**
   * Creates an enum value definition without deprecation.
   *
   * @param name the enum value name
   * @param description the value description
   */
  public EnumValueDefinition(@NotNull String name, @Nullable String description) {
    this(name, description, false, null);
  }
}
