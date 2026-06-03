package com.supremeai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for updating a user's tier.
 */
public class UserTierUpdateRequest {

    @NotBlank(message = "Tier is required")
    @Pattern(regexp = "^(?i)(GUEST|FREE|BASIC|PRO|ENTERPRISE|ADMIN)$", 
             message = "Invalid tier. Allowed values: GUEST, FREE, BASIC, PRO, ENTERPRISE, ADMIN")
    private String tier;

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
    }
}
