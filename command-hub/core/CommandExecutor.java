package org.example.command;

import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command registry and executor
 * 
 * Maintains list of all commands and executes them with proper:
 * - Permission checking
 * - Validation
 * - Logging
 * - Error handling
 */
public class CommandExecutor {
    private static final Logger logger = LoggerFactory.getLogger(CommandExecutor.class);
    
    private final Map<String, Command> commands = new HashMap<>();
    
    /**
     * Register a command
     */
    public void register(Command command) {
        commands.put(command.getName(), command);
        logger.info("✅ Command registered: {}", command.getName());
    }
    
    /**
     * Execute a command
     */
    public CommandResult execute(String commandName, Map<String, Object> params, 
                                CommandContext context) {
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. Check if command exists
            if (!commands.containsKey(commandName)) {
                return CommandResult.error(commandName, "NOT_FOUND", 
                    "Command not found: " + commandName);
            }
            
            Command command = commands.get(commandName);
            
            // 2. Check permissions
            for (String required : command.getRequiredPermissions()) {
                if (!context.hasPermission(required)) {
                    logger.warn("❌ Permission denied for user {} to run {}", 
                        context.getUsername(), commandName);
                    return CommandResult.error(commandName, "PERMISSION_DENIED",
                        "User does not have permission to execute this command");
                }
            }
            
            // 3. Validate parameters
            command.validate(params);
            
            // 4. Execute command
            logger.info("▶️  Executing command: {} by {}", commandName, context.getUsername());
            CommandResult result = command.execute(params, context);
            
            // 5. Record execution time
            long executionTime = System.currentTimeMillis() - startTime;
            result.setExecutionTime(executionTime);
            result.setExecutedBy(context.getUsername());
            
            // 6. Log result
            logger.info("✅ Command completed: {} ({}ms)", commandName, executionTime);
            
            return result;
            
        } catch (CommandValidationException e) {
            logger.warn("⚠️  Validation failed for command {}: {}", commandName, e.getMessage());
            return CommandResult.error(commandName, "VALIDATION_ERROR", e.getMessage());
            
        } catch (Exception e) {
            logger.error("❌ Command failed: {} - {}", commandName, e.getMessage(), e);
            return CommandResult.error(commandName, "EXECUTION_ERROR", 
                "Command execution failed: " + e.getMessage());
        }
    }
    
    /**
     * Get all registered commands
     */
    public List<Command> listCommands() {
        return new ArrayList<>(commands.values());
    }
    
    /**
     * Get command by name
     */
    public Command getCommand(String name) {
        return commands.get(name);
    }
    
    /**
     * Get commands by category
     */
    public List<Command> getCommandsByCategory(CommandCategory category) {
        List<Command> result = new ArrayList<>();
        for (Command cmd : commands.values()) {
            if (cmd.getCategory() == category) {
                result.add(cmd);
            }
        }
        return result;
    }
    
    /**
     * Get commands by type
     */
    public List<Command> getCommandsByType(CommandType type) {
        List<Command> result = new ArrayList<>();
        for (Command cmd : commands.values()) {
            if (cmd.getType() == type) {
                result.add(cmd);
            }
        }
        return result;
    }
    
    public int getCommandCount() {
        return commands.size();
    }
}
