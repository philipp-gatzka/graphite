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
package io.github.graphite.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Exception thrown when code generation fails.
 *
 * <p>This is the base exception for all code generation errors. It may wrap underlying causes such
 * as:
 *
 * <ul>
 *   <li>Schema parsing errors
 *   <li>Invalid schema structure
 *   <li>File I/O errors
 *   <li>Template processing errors
 * </ul>
 *
 * <p>Example handling:
 *
 * <pre>{@code
 * try {
 *     codegen.generate();
 * } catch (CodegenException e) {
 *     System.err.println("Code generation failed: " + e.getMessage());
 *     if (e.getCause() != null) {
 *         e.getCause().printStackTrace();
 *     }
 * }
 * }</pre>
 *
 * @see GraphiteCodegen
 */
public class CodegenException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Creates a new exception with the specified message.
   *
   * @param message the detail message
   */
  public CodegenException(@NotNull String message) {
    super(message);
  }

  /**
   * Creates a new exception with the specified message and cause.
   *
   * @param message the detail message
   * @param cause the underlying cause
   */
  public CodegenException(@NotNull String message, @Nullable Throwable cause) {
    super(message, cause);
  }
}
