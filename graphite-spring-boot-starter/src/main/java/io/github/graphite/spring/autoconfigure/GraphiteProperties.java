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
package io.github.graphite.spring.autoconfigure;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the Graphite GraphQL client.
 *
 * <p>These properties are bound from the {@code graphite} prefix in your application configuration.
 *
 * <p>Example configuration in {@code application.yml}:
 *
 * <pre>{@code
 * graphite:
 *   url: https://api.example.com/graphql
 *   headers:
 *     Authorization: Bearer ${TOKEN}
 *   timeout:
 *     connect: 10s
 *     read: 30s
 *     request: 60s
 *   retry:
 *     max-attempts: 3
 *     initial-delay: 100ms
 *     multiplier: 2.0
 *     max-delay: 5s
 *   rate-limit:
 *     requests-per-second: 100
 *     burst-capacity: 150
 *   connection-pool:
 *     max-connections: 50
 *     idle-timeout: 30s
 * }</pre>
 *
 * @see GraphiteAutoConfiguration
 */
@ConfigurationProperties(prefix = "graphite")
public class GraphiteProperties {

  /** The GraphQL endpoint URL. */
  private String url;

  /** HTTP headers to include in all requests. */
  private Map<String, String> headers = new HashMap<>();

  /** Timeout configuration. */
  private Timeout timeout = new Timeout();

  /** Retry configuration. */
  private Retry retry = new Retry();

  /** Rate limiting configuration. */
  private RateLimit rateLimit = new RateLimit();

  /** Connection pool configuration. */
  private ConnectionPool connectionPool = new ConnectionPool();

  /** Whether the Graphite client is enabled. */
  private boolean enabled = true;

  /** Client name for metrics tagging. */
  private String clientName = "default";

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public void setHeaders(Map<String, String> headers) {
    this.headers = headers;
  }

  public Timeout getTimeout() {
    return timeout;
  }

  public void setTimeout(Timeout timeout) {
    this.timeout = timeout;
  }

  public Retry getRetry() {
    return retry;
  }

  public void setRetry(Retry retry) {
    this.retry = retry;
  }

  public RateLimit getRateLimit() {
    return rateLimit;
  }

  public void setRateLimit(RateLimit rateLimit) {
    this.rateLimit = rateLimit;
  }

  public ConnectionPool getConnectionPool() {
    return connectionPool;
  }

  public void setConnectionPool(ConnectionPool connectionPool) {
    this.connectionPool = connectionPool;
  }

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public String getClientName() {
    return clientName;
  }

  public void setClientName(String clientName) {
    this.clientName = clientName;
  }

  /** Timeout configuration properties. */
  public static class Timeout {

    /** Connection timeout. */
    private Duration connect = Duration.ofSeconds(10);

    /** Read timeout. */
    private Duration read = Duration.ofSeconds(30);

    /** Overall request timeout. */
    private Duration request = Duration.ofSeconds(60);

    public Duration getConnect() {
      return connect;
    }

    public void setConnect(Duration connect) {
      this.connect = connect;
    }

    public Duration getRead() {
      return read;
    }

    public void setRead(Duration read) {
      this.read = read;
    }

    public Duration getRequest() {
      return request;
    }

    public void setRequest(Duration request) {
      this.request = request;
    }
  }

  /** Retry configuration properties. */
  public static class Retry {

    /** Maximum number of retry attempts. */
    private int maxAttempts = 3;

    /** Initial delay between retries. */
    private Duration initialDelay = Duration.ofMillis(100);

    /** Multiplier for exponential backoff. */
    private double multiplier = 2.0;

    /** Maximum delay between retries. */
    private Duration maxDelay = Duration.ofSeconds(5);

    /** Whether retry is enabled. */
    private boolean enabled = true;

    public int getMaxAttempts() {
      return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
      this.maxAttempts = maxAttempts;
    }

    public Duration getInitialDelay() {
      return initialDelay;
    }

    public void setInitialDelay(Duration initialDelay) {
      this.initialDelay = initialDelay;
    }

    public double getMultiplier() {
      return multiplier;
    }

    public void setMultiplier(double multiplier) {
      this.multiplier = multiplier;
    }

    public Duration getMaxDelay() {
      return maxDelay;
    }

    public void setMaxDelay(Duration maxDelay) {
      this.maxDelay = maxDelay;
    }

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }

  /** Rate limiting configuration properties. */
  public static class RateLimit {

    /** Maximum requests per second. */
    private double requestsPerSecond = 100.0;

    /** Burst capacity for rate limiter. */
    private int burstCapacity = 150;

    /** Whether rate limiting is enabled. */
    private boolean enabled = false;

    public double getRequestsPerSecond() {
      return requestsPerSecond;
    }

    public void setRequestsPerSecond(double requestsPerSecond) {
      this.requestsPerSecond = requestsPerSecond;
    }

    public int getBurstCapacity() {
      return burstCapacity;
    }

    public void setBurstCapacity(int burstCapacity) {
      this.burstCapacity = burstCapacity;
    }

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }
  }

  /** Connection pool configuration properties. */
  public static class ConnectionPool {

    /** Maximum number of connections in the pool. */
    private int maxConnections = 50;

    /** Idle timeout for pooled connections. */
    private Duration idleTimeout = Duration.ofSeconds(30);

    public int getMaxConnections() {
      return maxConnections;
    }

    public void setMaxConnections(int maxConnections) {
      this.maxConnections = maxConnections;
    }

    public Duration getIdleTimeout() {
      return idleTimeout;
    }

    public void setIdleTimeout(Duration idleTimeout) {
      this.idleTimeout = idleTimeout;
    }
  }
}
