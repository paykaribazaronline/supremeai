package org.example.security;

import java.lang.annotation.*;

/**
 * MANDATORY FIREBASE AUTH ENFORCEMENT ANNOTATION
 * 
 * Apply to ALL controller endpoints that require authentication.
 * 
 * Effect: Endpoint MUST validate Firebase ID Token in Authorization header
 * 
 * ⚠️ RULES:
 * - Authorization header MUST contain: Bearer {firebaseIdToken}
 * - Token MUST be a valid Firebase ID Token
 * - Token MUST be verified by Firebase Admin SDK
 * - Requests without valid token MUST return 401 Unauthorized
 * - NO alternative auth methods accepted
 * 
 * Usage:
 * @RestController
 * public class MyController {
 *     @GetMapping("/api/data")
 *     @RequireFirebaseAuth  // ← Apply this
 *     public ResponseEntity<?> getData() { ... }
 * }
 * 
 * @author SupremeAI Security Team
 * @since 2026-04-09
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireFirebaseAuth {
    /**
     * Error message if auth fails (default enforced message)
     */
    String errorMessage() default "Unauthorized: Valid Firebase ID Token required. Only Firebase authentication is allowed.";
    
    /**
     * Allow bypass for testing (default: false - NEVER bypass in production)
     */
    boolean allowTestBypass() default false;
}
