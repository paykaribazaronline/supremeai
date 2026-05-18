package com.supremeai.model;

/**
 * Centralized provider status constants to resolve casing conflicts.
 * Standardizes on lowercase to match Firestore storage.
 */
public final class ProviderStatus {
 public static final String ACTIVE = "active";
 public static final String INACTIVE = "inactive";
 public static final String PENDING = "pending_validation";
 public static final String ERROR = "error";

 private ProviderStatus() {
 } // Prevent instantiation
}
