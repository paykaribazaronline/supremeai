package com.supremeai.model;

public enum UserTier {
    FREE(1000L, "Free tier with basic API access"),
    BASIC(10000L, "Basic tier with increased API limits"),
    PRO(50000L, "Professional tier with high API limits"),
    ENTERPRISE(200000L, "Enterprise tier with maximum API limits"),
    ADMIN(-1L, "Admin tier with unlimited access");

    private final Long defaultMonthlyQuota;
    private final String description;

    UserTier(Long defaultMonthlyQuota, String description) {
        this.defaultMonthlyQuota = defaultMonthlyQuota;
        this.description = description;
    }

    public Long getDefaultMonthlyQuota() {
        return defaultMonthlyQuota;
    }

    public String getDescription() {
        return description;
    }

    public boolean hasUnlimitedQuota() {
        return this == ADMIN;
    }
}