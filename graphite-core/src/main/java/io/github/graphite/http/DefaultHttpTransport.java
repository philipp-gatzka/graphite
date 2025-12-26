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

import io.github.graphite.exception.GraphiteConnectionException;
import io.github.graphite.exception.GraphiteException;
import io.github.graphite.exception.GraphiteTimeoutException;
import io.github.graphite.exception.TimeoutType;
import java.io.IOException;
import java.net.ConnectException;
import java.net.http.HttpClient;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link HttpTransport} using Java's built-in HttpClient.
 *
 * <p>This implementation provides:
 *
 * <ul>
 *   <li>Connection pooling via the underlying HttpClient
 *   <li>Configurable timeouts (connect, read, request)
 *   <li>HTTP/2 support with fallback to HTTP/1.1
 *   <li>Thread-safe operation
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * HttpTransportConfig config = HttpTransportConfig.builder()
 *     .connectTimeout(Duration.ofSeconds(10))
 *     .requestTimeout(Duration.ofSeconds(30))
 *     .build();
 *
 * try (HttpTransport transport = new DefaultHttpTransport(config)) {
 *     HttpRequest request = HttpRequest.post(
 *         URI.create("https://api.example.com/graphql"),
 *         Map.of("Content-Type", "application/json"),
 *         "{\"query\": \"{ user { id } }\"}"
 *     );
 *     HttpResponse response = transport.execute(request);
 * }
 * }</pre>
 *
 * @see HttpTransport
 * @see HttpTransportConfig
 */
public final class DefaultHttpTransport implements HttpTransport {

  private static final Logger LOG = LoggerFactory.getLogger(DefaultHttpTransport.class);

  private final HttpClient httpClient;
  private final HttpTransportConfig config;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  @Nullable private final Semaphore concurrencyLimiter;

  /** Creates a new transport with default configuration. */
  public DefaultHttpTransport() {
    this(HttpTransportConfig.defaults());
  }

  /**
   * Creates a new transport with the specified configuration.
   *
   * @param config the transport configuration
   * @throws NullPointerException if config is null
   */
  public DefaultHttpTransport(HttpTransportConfig config) {
    this.config = Objects.requireNonNull(config, "config must not be null");
    this.httpClient = createHttpClient(config);
    this.concurrencyLimiter = createConcurrencyLimiter(config);
  }

  /**
   * Creates a new transport with a custom HttpClient and configuration.
   *
   * <p>This constructor allows for advanced customization of the underlying HttpClient while still
   * using Graphite's request/response handling.
   *
   * @param httpClient the HTTP client to use
   * @param config the transport configuration
   * @throws NullPointerException if httpClient or config is null
   */
  public DefaultHttpTransport(HttpClient httpClient, HttpTransportConfig config) {
    this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
    this.config = Objects.requireNonNull(config, "config must not be null");
    this.concurrencyLimiter = createConcurrencyLimiter(config);
  }

  @Nullable
  private static Semaphore createConcurrencyLimiter(HttpTransportConfig config) {
    int maxConcurrent = config.maxConcurrentRequests();
    return maxConcurrent > 0 ? new Semaphore(maxConcurrent, true) : null;
  }

  private static HttpClient createHttpClient(HttpTransportConfig config) {
    HttpClient.Builder builder =
        HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .connectTimeout(config.connectTimeout())
            .followRedirects(HttpClient.Redirect.NORMAL);

    // Use custom executor if provided
    if (config.executor() != null) {
      builder.executor(config.executor());
    }

    return builder.build();
  }

  @Override
  public HttpResponse execute(HttpRequest request) throws GraphiteException {
    ensureNotClosed();
    Objects.requireNonNull(request, "request must not be null");

    LOG.debug("Executing {} request to {}", request.method(), request.uri());
    if (LOG.isTraceEnabled()) {
      LOG.trace("Request headers: {}", request.headers());
      LOG.trace("Request body: {}", request.body());
    }

    acquireConcurrencyPermit();
    try {
      java.net.http.HttpRequest javaRequest = buildJavaRequest(request);
      long startTime = System.nanoTime();

      try {
        java.net.http.HttpResponse<String> javaResponse =
            httpClient.send(javaRequest, java.net.http.HttpResponse.BodyHandlers.ofString());
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;
        HttpResponse response = convertResponse(javaResponse);

        LOG.debug(
            "Received response: status={}, duration={}ms", javaResponse.statusCode(), durationMs);
        if (LOG.isTraceEnabled()) {
          LOG.trace("Response headers: {}", javaResponse.headers().map());
          LOG.trace("Response body: {}", response.body());
        }

        return response;
      } catch (HttpTimeoutException e) {
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;
        LOG.debug("Request timed out after {}ms: {}", durationMs, e.getMessage());
        throw createTimeoutException(request, e);
      } catch (ConnectException e) {
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;
        LOG.debug("Connection failed after {}ms: {}", durationMs, e.getMessage());
        throw createConnectionException(request, e);
      } catch (IOException e) {
        long durationMs = (System.nanoTime() - startTime) / 1_000_000;
        LOG.debug("Request failed after {}ms: {}", durationMs, e.getMessage());
        throw new GraphiteConnectionException(
            "Failed to execute request: " + e.getMessage(),
            e,
            request.uri().getHost(),
            request.uri().getPort() != -1 ? request.uri().getPort() : getDefaultPort(request));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        LOG.debug("Request interrupted");
        throw new GraphiteException("Request interrupted", e);
      }
    } finally {
      releaseConcurrencyPermit();
    }
  }

  @Override
  public CompletableFuture<HttpResponse> executeAsync(HttpRequest request) {
    ensureNotClosed();
    Objects.requireNonNull(request, "request must not be null");

    acquireConcurrencyPermit();
    java.net.http.HttpRequest javaRequest = buildJavaRequest(request);

    return httpClient
        .sendAsync(javaRequest, java.net.http.HttpResponse.BodyHandlers.ofString())
        .thenApply(this::convertResponse)
        .exceptionally(
            throwable -> {
              Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
              if (cause instanceof HttpTimeoutException e) {
                throw createTimeoutException(request, e);
              } else if (cause instanceof ConnectException e) {
                throw createConnectionException(request, e);
              } else if (cause instanceof IOException e) {
                throw new GraphiteConnectionException(
                    "Failed to execute request: " + e.getMessage(),
                    e,
                    request.uri().getHost(),
                    request.uri().getPort() != -1
                        ? request.uri().getPort()
                        : getDefaultPort(request));
              }
              throw new GraphiteException("Request failed: " + cause.getMessage(), cause);
            })
        .whenComplete((result, error) -> releaseConcurrencyPermit());
  }

  @Override
  public void close() {
    closed.set(true);
    // HttpClient doesn't have an explicit close method in Java 11-17
    // In Java 21+, we could call httpClient.close() if available
  }

  /**
   * Returns the configuration used by this transport.
   *
   * @return the transport configuration
   */
  public HttpTransportConfig getConfig() {
    return config;
  }

  /**
   * Returns whether this transport has been closed.
   *
   * @return {@code true} if closed
   */
  public boolean isClosed() {
    return closed.get();
  }

  private void ensureNotClosed() {
    if (closed.get()) {
      throw new IllegalStateException("Transport has been closed");
    }
  }

  private void acquireConcurrencyPermit() {
    if (concurrencyLimiter != null) {
      try {
        concurrencyLimiter.acquire();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new GraphiteException("Interrupted while waiting for concurrency permit", e);
      }
    }
  }

  private void releaseConcurrencyPermit() {
    if (concurrencyLimiter != null) {
      concurrencyLimiter.release();
    }
  }

  private java.net.http.HttpRequest buildJavaRequest(HttpRequest request) {
    java.net.http.HttpRequest.Builder builder =
        java.net.http.HttpRequest.newBuilder().uri(request.uri());

    // Set request timeout
    builder.timeout(config.requestTimeout());

    // Set headers
    for (Map.Entry<String, String> header : request.headers().entrySet()) {
      builder.header(header.getKey(), header.getValue());
    }

    // Set method and body
    switch (request.method()) {
      case GET -> builder.GET();
      case POST -> {
        String body = request.body() != null ? request.body() : "";
        builder.POST(java.net.http.HttpRequest.BodyPublishers.ofString(body));
      }
      default -> throw new IllegalArgumentException("Unsupported HTTP method: " + request.method());
    }

    return builder.build();
  }

  private HttpResponse convertResponse(java.net.http.HttpResponse<String> javaResponse) {
    int statusCode = javaResponse.statusCode();
    Map<String, List<String>> headers = javaResponse.headers().map();
    String body = javaResponse.body();
    return new HttpResponse(statusCode, headers, body);
  }

  private GraphiteTimeoutException createTimeoutException(
      HttpRequest request, HttpTimeoutException cause) {
    return new GraphiteTimeoutException(
        "Request timed out to " + request.uri(),
        TimeoutType.REQUEST,
        config.requestTimeout(),
        null,
        cause);
  }

  private GraphiteConnectionException createConnectionException(
      HttpRequest request, ConnectException cause) {
    return new GraphiteConnectionException(
        "Failed to connect to " + request.uri().getHost() + ": " + cause.getMessage(),
        cause,
        request.uri().getHost(),
        request.uri().getPort() != -1 ? request.uri().getPort() : getDefaultPort(request));
  }

  private int getDefaultPort(HttpRequest request) {
    String scheme = request.uri().getScheme();
    if ("https".equalsIgnoreCase(scheme)) {
      return 443;
    } else if ("http".equalsIgnoreCase(scheme)) {
      return 80;
    }
    return -1;
  }
}
