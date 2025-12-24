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

/**
 * Result of a code generation execution.
 *
 * <p>This record encapsulates the outcome of running {@link GraphiteCodegen#generate()}:
 *
 * <ul>
 *   <li>{@code status} - Whether generation succeeded or was skipped
 *   <li>{@code filesGenerated} - The number of Java source files generated
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * CodegenResult result = codegen.generate();
 * if (result.wasSkipped()) {
 *     System.out.println("Code is up-to-date, skipped generation");
 * } else {
 *     System.out.println("Generated " + result.filesGenerated() + " files");
 * }
 * }</pre>
 *
 * @param status the status of code generation
 * @param filesGenerated the number of files generated (0 if skipped)
 * @see GraphiteCodegen
 */
public record CodegenResult(@NotNull Status status, int filesGenerated) {

  /** The status of code generation. */
  public enum Status {
    /** Code generation completed successfully. */
    SUCCESS,
    /** Code generation was skipped because the schema is up-to-date. */
    SKIPPED
  }

  /**
   * Creates a result indicating successful code generation.
   *
   * @param filesGenerated the number of files generated
   * @return a success result
   */
  @NotNull
  public static CodegenResult success(int filesGenerated) {
    return new CodegenResult(Status.SUCCESS, filesGenerated);
  }

  /**
   * Creates a result indicating code generation was skipped.
   *
   * @return a skipped result
   */
  @NotNull
  public static CodegenResult skipped() {
    return new CodegenResult(Status.SKIPPED, 0);
  }

  /**
   * Returns whether code generation was successful.
   *
   * @return true if generation completed successfully
   */
  public boolean wasSuccessful() {
    return status == Status.SUCCESS;
  }

  /**
   * Returns whether code generation was skipped.
   *
   * @return true if generation was skipped due to up-to-date schema
   */
  public boolean wasSkipped() {
    return status == Status.SKIPPED;
  }
}
