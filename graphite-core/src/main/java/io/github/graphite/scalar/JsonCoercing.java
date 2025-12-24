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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Coercing implementation for JSON scalar type.
 *
 * <p>This coercing maps the GraphQL JSON scalar to Jackson's {@link JsonNode}. This allows
 * arbitrary JSON structures to be passed through the GraphQL API without requiring a specific
 * schema definition.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ScalarRegistry registry = ScalarRegistry.builder()
 *     .register("JSON", new JsonCoercing())
 *     .build();
 * }</pre>
 *
 * @see JsonNode
 * @see ScalarCoercing
 */
public final class JsonCoercing implements ScalarCoercing<JsonNode> {

  /** Singleton instance for convenience. */
  public static final JsonCoercing INSTANCE = new JsonCoercing();

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Override
  @NotNull
  public Class<JsonNode> javaType() {
    return JsonNode.class;
  }

  @Override
  @Nullable
  public Object serialize(@Nullable JsonNode value) {
    if (value == null) {
      return null;
    }
    // Return the raw value that the JSON structure represents
    return OBJECT_MAPPER.convertValue(value, Object.class);
  }

  @Override
  @Nullable
  public JsonNode deserialize(@Nullable Object input) {
    if (input == null) {
      return null;
    }
    if (input instanceof JsonNode node) {
      return node;
    }
    if (input instanceof String str) {
      try {
        return OBJECT_MAPPER.readTree(str);
      } catch (JsonProcessingException e) {
        throw new IllegalArgumentException("Cannot parse JSON value: " + str, e);
      }
    }
    // Convert any other object to JsonNode
    return OBJECT_MAPPER.valueToTree(input);
  }
}
