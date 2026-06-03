package com.supremeai.command;
import java.util.Map;

/**
 * Base interface for all SupremeAI commands
 * 
 * Every command:
 * - Has a unique name (e.g., "health-check", "refresh-github")
 * - Can be sync or async
 * - Takes parameters and returns a result
 * - Gets validated before execution
 * - Gets logged for audit trail
 */
public interface Command {
    
    /**
     * Unique command identifier
     * Examples: "health-check", "refresh-github", "optimize-quotas"
     */
    String getName();
    
    /**
     * Human-readable description
     */
    String getDescription();
    
    /**
     * Command category for grouping
     */
    CommandCategory getCategory();
    
    /**
     * Whether command runs sync or async
     */
    CommandType getType();
    
    /**
     * Required permissions to execute
     */
    String[] getRequiredPermissions();
    
    /**
     * Parameter schema (validation rules)
     */
    CommandSchema getSchema();
    
    /**
     * Execute the command with given parameters
     * 
     * @param params Command parameters
     * @param context Execution context (user, auth, etc)
     * @return Command result
     */
    CommandResult execute(Map<String, Object> params, CommandContext context);
    
    /**
     * Validate parameters before execution
     * Throws exception if invalid
     */
    void validate(Map<String, Object> params);
}
