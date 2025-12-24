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

/**
 * Code generators for producing Java source files from GraphQL schema.
 *
 * <p>This package contains generators for:
 *
 * <ul>
 *   <li>{@link io.github.graphite.codegen.generator.TypeGenerator} - DTO records for GraphQL types
 *   <li>EnumGenerator - Java enums for GraphQL enums
 *   <li>InputTypeGenerator - Input type classes with builders
 *   <li>QueryGenerator - Type-safe query classes
 *   <li>MutationGenerator - Type-safe mutation classes
 *   <li>ProjectionGenerator - Field selection builders
 * </ul>
 *
 * @see io.github.graphite.codegen.GraphiteCodegen
 */
package io.github.graphite.codegen.generator;
