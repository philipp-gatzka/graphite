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

import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an enum type definition in a GraphQL schema.
 *
 * <p>Enum types represent a finite set of possible values.
 *
 * <p>Example GraphQL:
 *
 * <pre>{@code
 * enum UserStatus {
 *   ACTIVE
 *   INACTIVE
 *   SUSPENDED
 * }
 * }</pre>
 *
 * @param name the enum type name
 * @param description the enum description, may be null
 * @param values the enum values
 * @see EnumValueDefinition
 */
public record EnumDefinition(
    @NotNull String name,
    @Nullable String description,
    @NotNull List<EnumValueDefinition> values) {}
