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

import org.jetbrains.annotations.NotNull;

/**
 * Builder for constructing {@link GraphiteClient} instances.
 *
 * <p>This builder provides a fluent API for configuring GraphiteClient with various options
 * including endpoint URL, headers, timeouts, retry policies, and rate limiting.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * GraphiteClient client = GraphiteClient.builder()
 *     .url("https://api.example.com/graphql")
 *     .header("Authorization", "Bearer " + token)
 *     .build();
 * }</pre>
 *
 * @see GraphiteClient
 */
public final class GraphiteClientBuilder {

  /**
   * Creates a new builder instance.
   *
   * <p>Use {@link GraphiteClient#builder()} instead of this constructor.
   */
  GraphiteClientBuilder() {
    // Package-private constructor
  }

  /**
   * Builds the GraphiteClient with the configured options.
   *
   * @return a new GraphiteClient instance
   * @throws IllegalStateException if required configuration is missing
   */
  @NotNull
  public GraphiteClient build() {
    throw new UnsupportedOperationException("GraphiteClient implementation not yet available");
  }
}
