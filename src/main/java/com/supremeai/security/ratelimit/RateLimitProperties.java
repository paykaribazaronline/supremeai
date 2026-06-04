package com.supremeai.security.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component("securityRateLimitProperties")
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitProperties {

  private boolean distributed = false;
  private int defaultLimit = 100;
  private int defaultWindowSeconds = 60;

  public boolean isDistributed() {
    return distributed;
  }

  public void setDistributed(boolean distributed) {
    this.distributed = distributed;
  }

  public int getDefaultLimit() {
    return defaultLimit;
  }

  public void setDefaultLimit(int defaultLimit) {
    this.defaultLimit = defaultLimit;
  }

  public int getDefaultWindowSeconds() {
    return defaultWindowSeconds;
  }

  public void setDefaultWindowSeconds(int defaultWindowSeconds) {
    this.defaultWindowSeconds = defaultWindowSeconds;
  }
}
