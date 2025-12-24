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
package io.github.graphite.http;

/**
 * HTTP methods used for GraphQL requests.
 *
 * <p>GraphQL typically uses POST for queries and mutations, but GET can be used for queries when
 * the query is passed as a URL parameter.
 *
 * @see HttpRequest
 */
public enum HttpMethod {

  /**
   * HTTP GET method.
   *
   * <p>Can be used for GraphQL queries when the query is URL-encoded in the query string. Not
   * suitable for mutations or large queries.
   */
  GET,

  /**
   * HTTP POST method.
   *
   * <p>The standard method for GraphQL requests. The query is sent in the request body as JSON.
   */
  POST
}
