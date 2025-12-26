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
package io.github.graphite.codegen.util;

/**
 * Utility methods for code generation.
 *
 * <p>This class provides common string manipulation methods used across multiple generators.
 */
public final class GeneratorUtils {

  private GeneratorUtils() {
    // Utility class
  }

  /**
   * Capitalizes the first character of a string.
   *
   * @param s the string to capitalize
   * @return the capitalized string, or the original string if null or empty
   */
  public static String capitalize(String s) {
    return s == null || s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
  }

  /**
   * Escapes special characters in Javadoc text.
   *
   * <p>This method escapes:
   *
   * <ul>
   *   <li>{@code $} to {@code $$} (for JavaPoet templates)
   *   <li>{@code @} to {@code {@literal @}} (for Javadoc annotations)
   *   <li>{@code <} to {@code &lt;} (for HTML entities)
   *   <li>{@code >} to {@code &gt;} (for HTML entities)
   * </ul>
   *
   * @param text the text to escape
   * @return the escaped text
   */
  public static String escapeJavadoc(String text) {
    return text.replace("$", "$$")
        .replace("@", "{@literal @}")
        .replace("<", "&lt;")
        .replace(">", "&gt;");
  }
}
