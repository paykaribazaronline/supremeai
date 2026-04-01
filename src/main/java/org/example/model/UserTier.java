package org.example.model;

/**
 * User Tier Enumeration
 * Defines tier levels with quotas for free/paid users
 * SUPERADMIN = you (unlimited access)
 */
public enum UserTier {
    FREE("free", 5, 1000, 1, 0),                    // 5 req/day, 1,000/month, 1 app/day, $0
    STARTER("starter", 500, 10000, 5, 9),          // 500 req/day, 10k/month, 5 apps/day, $9/mo
    PROFESSIONAL("professional", 5000, 100000, 50, 99),  // 5k req/day, 100k/month, 50 apps/day, $99/mo
    ENTERPRISE("enterprise", -1, -1, -1, 9999),    // Unlimited, custom pricing
    SUPERADMIN("superadmin", -1, -1, -1, 0);       // YOU - Unlimited everything, free
    
    public final String name;
    public final int dailyLimit;           // -1 = unlimited
    public final int monthlyLimit;         // -1 = unlimited
    public final int appCreationsPerDay;   // -1 = unlimited
    public final int monthlyPrice;         // In USD
    
    UserTier(String name, int dailyLimit, int monthlyLimit, int appCreationsPerDay, int monthlyPrice) {
        this.name = name;
        this.dailyLimit = dailyLimit;
        this.monthlyLimit = monthlyLimit;
        this.appCreationsPerDay = appCreationsPerDay;
        this.monthlyPrice = monthlyPrice;
    }
    
    public boolean isUnlimited() {
        return this == ENTERPRISE || this == SUPERADMIN;
    }
    
    public static UserTier fromString(String tier) {
        try {
            return UserTier.valueOf(tier.toUpperCase());
        } catch (IllegalArgumentException e) {
            return FREE; // Default to free tier
        }
    }
}
