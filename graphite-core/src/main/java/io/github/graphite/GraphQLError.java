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
package io.github.graphite;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a GraphQL error as defined in the GraphQL specification.
 *
 * <p>A GraphQL response may contain a list of errors, each with:
 *
 * <ul>
 *   <li>{@code message} - A description of the error (required)
 *   <li>{@code locations} - The locations in the GraphQL document where the error occurred
 *   <li>{@code path} - The path to the field that caused the error
 *   <li>{@code extensions} - Additional error metadata
 * </ul>
 *
 * <p>Example GraphQL error JSON:
 *
 * <pre>{@code
 * {
 *   "message": "User not found",
 *   "locations": [{"line": 2, "column": 3}],
 *   "path": ["user"],
 *   "extensions": {"code": "NOT_FOUND"}
 * }
 * }</pre>
 *
 * @param message the error message describing what went wrong
 * @param locations the locations in the GraphQL document where the error occurred, may be {@code
 *     null} or empty
 * @param path the path to the response field that caused the error, may be {@code null} or empty
 * @param extensions additional error metadata, may be {@code null} or empty
 * @see io.github.graphite.exception.GraphiteGraphQLException
 */
public record GraphQLError(
    String message,
    @Nullable List<Location> locations,
    @Nullable List<Object> path,
    @Nullable Map<String, Object> extensions)
    implements Serializable {

  private static final long serialVersionUID = 1L;

  /**
   * Represents a location in a GraphQL document.
   *
   * @param line the line number (1-indexed)
   * @param column the column number (1-indexed)
   */
  public record Location(int line, int column) implements Serializable {
    private static final long serialVersionUID = 1L;
  }

  /**
   * Returns the error code from the extensions, if present.
   *
   * <p>Many GraphQL servers include an error code in the extensions map under the key "code".
   *
   * @return the error code, or {@code null} if not present
   */
  @Nullable
  public String getCode() {
    if (extensions == null) {
      return null;
    }
    Object code = extensions.get("code");
    return code != null ? code.toString() : null;
  }

  /**
   * Returns the path as a dot-separated string.
   *
   * <p>For example, the path {@code ["user", "posts", 0, "title"]} would be formatted as {@code
   * "user.posts.0.title"}.
   *
   * @return the formatted path, or an empty string if the path is null or empty
   */
  public String getFormattedPath() {
    if (path == null || path.isEmpty()) {
      return "";
    }
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < path.size(); i++) {
      if (i > 0) {
        sb.append(".");
      }
      sb.append(path.get(i));
    }
    return sb.toString();
  }
}
