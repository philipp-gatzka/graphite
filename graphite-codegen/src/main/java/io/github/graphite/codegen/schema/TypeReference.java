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

/**
 * Represents a type reference in a GraphQL schema.
 *
 * <p>GraphQL types can be:
 *
 * <ul>
 *   <li>{@link Named} - A reference to a named type (scalar, object, enum, etc.)
 *   <li>{@link NonNull} - A non-nullable wrapper around another type
 *   <li>{@link ListType} - A list wrapper around another type
 * </ul>
 *
 * <p>Type references can be nested, for example {@code [String!]!} would be represented as:
 *
 * <pre>{@code
 * new NonNull(new ListType(new NonNull(new Named("String"))))
 * }</pre>
 *
 * @see FieldDefinition
 * @see ArgumentDefinition
 */
public sealed interface TypeReference {

  /**
   * Returns the underlying named type, unwrapping any NonNull or List wrappers.
   *
   * @return the base named type
   */
  default @NotNull String getBaseName() {
    return switch (this) {
      case Named(var name) -> name;
      case NonNull(var inner) -> inner.getBaseName();
      case ListType(var inner) -> inner.getBaseName();
    };
  }

  /**
   * Returns whether this type reference is non-nullable.
   *
   * @return true if this is a NonNull type
   */
  default boolean isNonNull() {
    return this instanceof NonNull;
  }

  /**
   * Returns whether this type reference is a list.
   *
   * @return true if this is a List type (at the top level)
   */
  default boolean isList() {
    return switch (this) {
      case ListType ignored -> true;
      case NonNull(var inner) -> inner.isList();
      case Named ignored -> false;
    };
  }

  /**
   * Returns the GraphQL type notation for this reference.
   *
   * @return the type notation (e.g., "[String!]!")
   */
  @NotNull
  default String toGraphQL() {
    return switch (this) {
      case Named(var name) -> name;
      case NonNull(var inner) -> inner.toGraphQL() + "!";
      case ListType(var inner) -> "[" + inner.toGraphQL() + "]";
    };
  }

  /**
   * A reference to a named type (scalar, object, interface, union, enum, or input object).
   *
   * @param name the type name
   */
  record Named(@NotNull String name) implements TypeReference {}

  /**
   * A non-null wrapper around another type.
   *
   * @param inner the wrapped type
   */
  record NonNull(@NotNull TypeReference inner) implements TypeReference {}

  /**
   * A list wrapper around another type.
   *
   * @param inner the element type
   */
  record ListType(@NotNull TypeReference inner) implements TypeReference {}
}
