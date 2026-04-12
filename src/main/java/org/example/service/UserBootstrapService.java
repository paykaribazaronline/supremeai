package org.example.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;

/**
 * User Bootstrap Service - DEPRECATED (SECURITY HARDENING PHASE 11)
 * 
 * ⚠️ SECURITY: This service no longer creates ANY default users.
 * 
 * Previous behavior:
 * - ❌ Auto-created "supremeai" user with hardcoded password
 * - ❌ Set automatic SUPERADMIN tier for specific usernames
 * - ❌ Exposed system to credential compromise
 * 
 * New behavior:
 * - ✅ Zero auto-creation
 * - ✅ Explicit setup via /api/auth/setup (token-protected)
 * - ✅ All users start with FREE tier
 * - ✅ Admin must manually promote users
 * 
 * Kept for backward compatibility only. All initialization is disabled.
 * 
 * @see <a href="https://docs.supremeai.com/security/default-user-removal">Security Fix: Remove Default User</a>
 */
@Service
public class UserBootstrapService {
    private static final Logger logger = LoggerFactory.getLogger(UserBootstrapService.class);
    
    @PostConstruct
    public void initializeDefaultUser() {
        logger.warn("");
        logger.warn("═══════════════════════════════════════════════════════════════");
        logger.warn("🔐 SECURITY: Default User Bootstrap DISABLED (Phase 11 Hardening)");
        logger.warn("═══════════════════════════════════════════════════════════════");
        logger.warn("  No default users are created.");
        logger.warn("  To create first admin, use: /api/auth/setup (setup-token required)");
        logger.warn("═══════════════════════════════════════════════════════════════");
        logger.warn("");
    }
}
