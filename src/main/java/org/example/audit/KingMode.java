package org.example.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that require King Mode (Admin Override) audit logging.
 * 
 * When applied to a method, the KingModeAuditAspect will:
 * 1. Log the action before execution
 * 2. Log the result after execution
 * 3. Log any exceptions
 * 4. Apply 4-eyes principle for critical actions
 * 
 * Usage:
 * @KingMode
 * public void criticalAdminAction(Request req) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface KingMode {
    
    /**
     * Description of what this action does
     */
    String value() default "";
    
    /**
     * Whether this action requires secondary admin approval (4-eyes principle)
     */
    boolean requireSecondaryApproval() default false;
    
    /**
     * Severity level of this action for audit purposes
     */
    Severity severity() default Severity.HIGH;
    
    enum Severity {
        LOW,      // Routine admin action
        MEDIUM,   // Standard admin action
        HIGH,     // Important admin action
        CRITICAL  // Critical system action
    }
}
