package com.supremeai.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import java.util.HashMap;
import java.util.Map;

/**
 * Firestore-backed configuration for each UserTier. Stored in collection "user_tiers" — one
 * document per tier. Maps to the UserTier enum for business logic.
 */
@Document(collectionName = "user_tiers")
public class UserTierConfig {

  @DocumentId
  /** Tier name, matching the {@link UserTier} enum name (e.g. "GUEST", "FREE"). */
  private String id;

  private String description;
  private long defaultMonthlyQuota;
  private boolean premium;
  private boolean unlimited;
  private Map<String, Object> limits;

  public UserTierConfig() {
    this.limits = new HashMap<>();
  }

  public UserTierConfig(
      String id, String description, long defaultMonthlyQuota, boolean premium, boolean unlimited) {
    this.id = id;
    this.description = description;
    this.defaultMonthlyQuota = defaultMonthlyQuota;
    this.premium = premium;
    this.unlimited = unlimited;
    this.limits = new HashMap<>();
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public long getDefaultMonthlyQuota() {
    return defaultMonthlyQuota;
  }

  public void setDefaultMonthlyQuota(long defaultMonthlyQuota) {
    this.defaultMonthlyQuota = defaultMonthlyQuota;
  }

  public boolean isPremium() {
    return premium;
  }

  public void setPremium(boolean premium) {
    this.premium = premium;
  }

  public boolean isUnlimited() {
    return unlimited;
  }

  public void setUnlimited(boolean unlimited) {
    this.unlimited = unlimited;
  }

  public Map<String, Object> getLimits() {
    return limits;
  }

  public void setLimits(Map<String, Object> limits) {
    this.limits = limits;
  }
}
