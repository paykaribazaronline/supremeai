package org.example.model;

/**
 * User Tier Enumeration
 *
 * MASTER RULE: No artificial hardcoded limits.
 * - SUPERADMIN/ENTERPRISE = truly unlimited, zero checks
 * - Other tiers: defaults below are just INITIAL values
 * - Admin can override any tier's limits from the dashboard (User Management & Tiers)
 * - The ONLY real limit is actual resource availability (API quota, Firebase, GCloud)
 */
public enum UserTier {
    FREE("free", -1, -1, -1, 0),                   // Default: unlimited (admin can restrict from dashboard)
    STARTER("starter", -1, -1, -1, 9),             // Default: unlimited (admin can restrict from dashboard)
    PROFESSIONAL("professional", -1, -1, -1, 99),  // Default: unlimited (admin can restrict from dashboard)
    ENTERPRISE("enterprise", -1, -1, -1, 9999),    // Unlimited, custom pricing
    SUPERADMIN("superadmin", -1, -1, -1, 0);       // YOU - Unlimited everything, free
    
    public final String name;
    public final int dailyLimit;           // -1 = unlimited (no artificial cap)
    public final int monthlyLimit;         // -1 = unlimited (no artificial cap)
    public final int appCreationsPerDay;   // -1 = unlimited (no artificial cap)
    public final int monthlyPrice;         // In USD
    
    UserTier(String name, int dailyLimit, int monthlyLimit, int appCreationsPerDay, int monthlyPrice) {
        this.name = name;
        this.dailyLimit = dailyLimit;
        this.monthlyLimit = monthlyLimit;
        this.appCreationsPerDay = appCreationsPerDay;
        this.monthlyPrice = monthlyPrice;
    }
    
    public boolean isUnlimited() {
        return dailyLimit == -1 && monthlyLimit == -1 && appCreationsPerDay == -1;
    }
    
    public static UserTier fromString(String tier) {
        try {
            return UserTier.valueOf(tier.toUpperCase());
        } catch (IllegalArgumentException e) {
            return FREE; // Default to free tier
        }
    }
}
