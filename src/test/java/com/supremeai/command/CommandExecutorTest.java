package com.supremeai.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.mockito.Mock;
import org.mockito.Spy;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CommandExecutorTest {

    @Spy
    private CommandExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new CommandExecutor();
    }

    @Test
    @DisplayName("Should register a command successfully")
    void testRegisterCommand() {
        Command mockCommand = createMockCommand("test-command", CommandCategory.DATA_REFRESH, CommandType.SYNC);
        
        executor.register(mockCommand);
        
        assertEquals(1, executor.getCommandCount());
        assertNotNull(executor.getCommand("test-command"));
    }

    @Test
    @DisplayName("Should execute command successfully")
    void testExecuteCommandSuccessfully() {
        Command mockCommand = createMockCommand("test-command", CommandCategory.DATA_REFRESH, CommandType.SYNC);
        CommandContext context = createContext("admin", new String[]{"ADMIN"});
        
        when(mockCommand.execute(anyMap(), any(CommandContext.class)))
            .thenReturn(CommandResult.success("test-command", Map.of("key", "value")));
        
        executor.register(mockCommand);
        CommandResult result = executor.execute("test-command", Collections.emptyMap(), context);
        
        assertTrue(result.isSuccess());
        assertEquals("test-command", result.getCommandName());
    }

    @Test
    @DisplayName("Should return error for non-existent command")
    void testExecuteNonExistentCommand() {
        CommandContext context = createContext("user", new String[]{"USER"});
        
        CommandResult result = executor.execute("non-existent", Collections.emptyMap(), context);
        
        assertTrue(result.isFailed());
        assertEquals("NOT_FOUND", result.getErrorCode());
    }

    @Test
    @DisplayName("Should return error for permission denied")
    void testExecutePermissionDenied() {
        Command mockCommand = createMockCommand("protected-command", CommandCategory.DATA_REFRESH, CommandType.SYNC);
        CommandContext context = createContext("user", new String[]{"USER"});
        
        executor.register(mockCommand);
        CommandResult result = executor.execute("protected-command", Collections.emptyMap(), context);
        
        assertTrue(result.isFailed());
        assertEquals("PERMISSION_DENIED", result.getErrorCode());
    }

    @Test
    @DisplayName("Should handle validation error")
    void testHandleValidationError() {
        Command mockCommand = createMockCommand("test-command", CommandCategory.DATA_REFRESH, CommandType.SYNC);
        CommandContext context = createContext("admin", new String[]{"ADMIN"});
        
        doThrow(new CommandValidationException("Invalid parameter")).when(mockCommand).validate(anyMap());
        
        executor.register(mockCommand);
        CommandResult result = executor.execute("test-command", Collections.emptyMap(), context);
        
        assertTrue(result.isFailed());
        assertEquals("VALIDATION_ERROR", result.getErrorCode());
    }

    @Test
    @DisplayName("Should handle execution error")
    void testHandleExecutionError() {
        Command mockCommand = createMockCommand("test-command", CommandCategory.DATA_REFRESH, CommandType.SYNC);
        CommandContext context = createContext("admin", new String[]{"ADMIN"});
        
        when(mockCommand.execute(anyMap(), any(CommandContext.class)))
            .thenThrow(new RuntimeException("Execution failed"));
        
        executor.register(mockCommand);
        CommandResult result = executor.execute("test-command", Collections.emptyMap(), context);
        
        assertTrue(result.isFailed());
        assertEquals("EXECUTION_ERROR", result.getErrorCode());
    }

    @Test
    @DisplayName("Should list all commands")
    void testListCommands() {
        executor.register(createMockCommand("cmd1", CommandCategory.DATA_REFRESH, CommandType.SYNC));
        executor.register(createMockCommand("cmd2", CommandCategory.DEPLOYMENT, CommandType.ASYNC));
        
        List<Command> commands = executor.listCommands();
        
        assertEquals(2, commands.size());
    }

    @Test
    @DisplayName("Should get commands by category")
    void testGetCommandsByCategory() {
        executor.register(createMockCommand("cmd1", CommandCategory.DATA_REFRESH, CommandType.SYNC));
        executor.register(createMockCommand("cmd2", CommandCategory.DEPLOYMENT, CommandType.SYNC));
        
        List<Command> refreshCommands = executor.getCommandsByCategory(CommandCategory.DATA_REFRESH);
        
        assertEquals(1, refreshCommands.size());
        assertEquals("cmd1", refreshCommands.get(0).getName());
    }

    @Test
    @DisplayName("Should get commands by type")
    void testGetCommandsByType() {
        executor.register(createMockCommand("cmd1", CommandCategory.DATA_REFRESH, CommandType.SYNC));
        executor.register(createMockCommand("cmd2", CommandCategory.DEPLOYMENT, CommandType.ASYNC));
        
        List<Command> asyncCommands = executor.getCommandsByType(CommandType.ASYNC);
        
        assertEquals(1, asyncCommands.size());
        assertEquals("cmd2", asyncCommands.get(0).getName());
    }

    @Test
    @DisplayName("Should return correct command count")
    void testGetCommandCount() {
        assertEquals(0, executor.getCommandCount());
        
        executor.register(createMockCommand("cmd1", CommandCategory.DATA_REFRESH, CommandType.SYNC));
        assertEquals(1, executor.getCommandCount());
        
        executor.register(createMockCommand("cmd2", CommandCategory.DEPLOYMENT, CommandType.SYNC));
        assertEquals(2, executor.getCommandCount());
    }

    @Test
    @DisplayName("Admin user should have all permissions")
    void testAdminHasAllPermissions() {
        Command mockCommand = createMockCommand("test-command", CommandCategory.DATA_REFRESH, CommandType.SYNC);
        CommandContext adminContext = createContext("admin", new String[]{"ADMIN"});
        
        when(mockCommand.getRequiredPermissions()).thenReturn(new String[]{"ANY_PERMISSION"});
        
        executor.register(mockCommand);
        CommandResult result = executor.execute("test-command", Collections.emptyMap(), adminContext);
        
        assertTrue(result.isSuccess());
    }

    // Helper methods
    private Command createMockCommand(String name, CommandCategory category, CommandType type) {
        return new Command() {
            @Override
            public String getName() { return name; }
            @Override
            public String getDescription() { return "Test command"; }
            @Override
            public CommandCategory getCategory() { return category; }
            @Override
            public CommandType getType() { return type; }
            @Override
            public String[] getRequiredPermissions() { return new String[]{"ADMIN"}; }
            @Override
            public CommandSchema getSchema() { return new CommandSchema("test-command"); }
            @Override
            public CommandResult execute(Map<String, Object> params, CommandContext context) {
                return null;
            }
            @Override
            public void validate(Map<String, Object> params) {}
        };
    }

    private CommandContext createContext(String username, String[] roles) {
        CommandContext context = new CommandContext("user-id", username, roles);
        context.setPermissions(new String[]{"ADMIN"});
        return context;
    }
}