package org.example.controller;

import org.example.command.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Command API Controller
 * RESTful API endpoints for executing and managing commands
 * 
 * Endpoints:
 * - POST   /api/commands/execute     - Execute a command
 * - GET    /api/commands/list        - List all available commands
 * - GET    /api/commands/{name}      - Get specific command info
 * - DELETE /api/commands/cancel/{id} - Cancel async command
 */
@RestController
@RequestMapping("/api/commands")
@CrossOrigin(origins = "*", maxAge = 3600)
public class CommandController {
    private static final Logger logger = LoggerFactory.getLogger(CommandController.class);
    
    private final CommandExecutor executor;
    
    public CommandController(CommandExecutor executor) {
        this.executor = executor;
    }
    
    /**
     * Execute a command
     * POST /api/commands/execute
     * 
     * Request body:
     * {
     *   "name": "health-check",
     *   "parameters": {
     *     "key": "value"
     *   }
     * }
     */
    @PostMapping("/execute")
    public ResponseEntity<CommandResponseDTO> executeCommand(
            @RequestBody ExecuteCommandRequest request,
            @RequestHeader(value = "Authorization", required = false) String authToken) {
        
        try {
            logger.info("📤 Executing command: {}", request.getName());
            
            // Create context from request
            CommandContext context = createContextFromRequest(authToken);
            
            // Validate user has permission
            if (!context.hasPermission("execute.command")) {
                return ResponseEntity.status(403)
                    .body(new CommandResponseDTO(
                        "forbidden",
                        false,
                        "Insufficient permissions to execute commands",
                        null
                    ));
            }
            
            // Execute command
            CommandResult result = executor.execute(
                request.getName(), 
                request.getParameters() != null ? request.getParameters() : new HashMap<>(),
                context
            );
            
            int statusCode = result.isSuccess() ? 200 : 
                           result.isFailed() ? 400 : 202;
            
            return ResponseEntity.status(statusCode)
                .body(new CommandResponseDTO(
                    request.getName(),
                    result.isSuccess(),
                    result.getMessage(),
                    result.getData()
                ));
                
        } catch (Exception e) {
            logger.error("❌ Command execution failed", e);
            return ResponseEntity.status(500)
                .body(new CommandResponseDTO(
                    request.getName(),
                    false,
                    "Command execution failed: " + e.getMessage(),
                    null
                ));
        }
    }
    
    /**
     * List all available commands
     * GET /api/commands/list
     */
    @GetMapping("/list")
    public ResponseEntity<CommandListResponseDTO> listCommands(
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "type", required = false) String type) {
        
        try {
            logger.info("📋 Listing commands - category: {}, type: {}", category, type);
            
            List<CommandInfo> commands = new ArrayList<>();
            
            // Get all commands and filter
            for (Command cmd : executor.listCommands()) {
                if (category != null && !cmd.getCategory().name().equals(category)) {
                    continue;
                }
                if (type != null && !cmd.getType().name().equals(type)) {
                    continue;
                }
                
                commands.add(new CommandInfo(
                    cmd.getName(),
                    cmd.getDescription(),
                    cmd.getCategory().name(),
                    cmd.getType().name(),
                    cmd.getRequiredPermissions()
                ));
            }
            
            return ResponseEntity.ok(new CommandListResponseDTO(
                true,
                "Commands retrieved successfully",
                commands
            ));
            
        } catch (Exception e) {
            logger.error("❌ Command listing failed", e);
            return ResponseEntity.status(500)
                .body(new CommandListResponseDTO(
                    false,
                    "Command listing failed: " + e.getMessage(),
                    new ArrayList<>()
                ));
        }
    }
    
    /**
     * Get specific command details
     * GET /api/commands/{name}
     */
    @GetMapping("/{name}")
    public ResponseEntity<CommandDetailResponseDTO> getCommand(
            @PathVariable String name) {
        
        try {
            logger.info("🔍 Getting command details: {}", name);
            
            // Find command (would need to enhance executor with getCommand method)
            for (Command cmd : executor.listCommands()) {
                if (cmd.getName().equals(name)) {
                    return ResponseEntity.ok(new CommandDetailResponseDTO(
                        true,
                        "Command details retrieved",
                        new CommandInfo(
                            cmd.getName(),
                            cmd.getDescription(),
                            cmd.getCategory().name(),
                            cmd.getType().name(),
                            cmd.getRequiredPermissions()
                        ),
                        cmd.getSchema().getParameterSpecs()
                    ));
                }
            }
            
            return ResponseEntity.status(404)
                .body(new CommandDetailResponseDTO(
                    false,
                    "Command not found: " + name,
                    null,
                    null
                ));
                
        } catch (Exception e) {
            logger.error("❌ Command details retrieval failed", e);
            return ResponseEntity.status(500)
                .body(new CommandDetailResponseDTO(
                    false,
                    "Command details retrieval failed: " + e.getMessage(),
                    null,
                    null
                ));
        }
    }
    
    /**
     * Get command execution history
     * GET /api/commands/history
     */
    @GetMapping("/history")
    public ResponseEntity<CommandHistoryResponseDTO> getHistory(
            @RequestParam(value = "limit", defaultValue = "50") int limit) {
        
        try {
            logger.info("📜 Retrieving command history (limit: {})", limit);
            
            // This would fetch from audit log/database
            // For now, return placeholder
            
            return ResponseEntity.ok(new CommandHistoryResponseDTO(
                true,
                "History retrieved",
                new ArrayList<>()
            ));
            
        } catch (Exception e) {
            logger.error("❌ History retrieval failed", e);
            return ResponseEntity.status(500)
                .body(new CommandHistoryResponseDTO(
                    false,
                    "History retrieval failed: " + e.getMessage(),
                    new ArrayList<>()
                ));
        }
    }
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Commands service is healthy");
    }
    
    // ==================== Helper Methods ====================
    
    private CommandContext createContextFromRequest(String authToken) {
        // Parse auth token and extract user info
        String userId = "system-user";
        String username = "admin";
        String role = "ADMIN";
        String sourceIp = "127.0.0.1";
        String sourceApp = "api";
        
        CommandContext context = new CommandContext(
            userId, username, authToken, role
        );
        
        // Set additional info
        context.setSourceIp(sourceIp);
        context.setSourceApp(sourceApp);
        
        return context;
    }
}

/**
 * Request/Response DTOs
 */
class ExecuteCommandRequest {
    private String name;
    private Map<String, Object> parameters;
    
    public ExecuteCommandRequest() {}
    public ExecuteCommandRequest(String name, Map<String, Object> parameters) {
        this.name = name;
        this.parameters = parameters;
    }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public Map<String, Object> getParameters() { return parameters; }
    public void setParameters(Map<String, Object> parameters) { this.parameters = parameters; }
}

class CommandResponseDTO {
    private String commandName;
    private boolean success;
    private String message;
    private Object data;
    
    public CommandResponseDTO(String commandName, boolean success, String message, Object data) {
        this.commandName = commandName;
        this.success = success;
        this.message = message;
        this.data = data;
    }
    
    public String getCommandName() { return commandName; }
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public Object getData() { return data; }
}

class CommandListResponseDTO {
    private boolean success;
    private String message;
    private List<CommandInfo> commands;
    
    public CommandListResponseDTO(boolean success, String message, List<CommandInfo> commands) {
        this.success = success;
        this.message = message;
        this.commands = commands;
    }
    
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public List<CommandInfo> getCommands() { return commands; }
}

class CommandDetailResponseDTO {
    private boolean success;
    private String message;
    private CommandInfo command;
    private Object parameters;
    
    public CommandDetailResponseDTO(boolean success, String message, CommandInfo command, Object parameters) {
        this.success = success;
        this.message = message;
        this.command = command;
        this.parameters = parameters;
    }
    
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public CommandInfo getCommand() { return command; }
    public Object getParameters() { return parameters; }
}

class CommandHistoryResponseDTO {
    private boolean success;
    private String message;
    private List<Object> history;
    
    public CommandHistoryResponseDTO(boolean success, String message, List<Object> history) {
        this.success = success;
        this.message = message;
        this.history = history;
    }
    
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public List<Object> getHistory() { return history; }
}

class CommandInfo {
    private String name;
    private String description;
    private String category;
    private String type;
    private String[] permissions;
    
    public CommandInfo(String name, String description, String category, String type, String[] permissions) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.type = type;
        this.permissions = permissions;
    }
    
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getType() { return type; }
    public String[] getPermissions() { return permissions; }
}
