package com.supremeai.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class CommandExecutorTest {CommandExecutorpublic CommandExecutorTest(CommandExecutor executor, final String name, final CommandCategory category, final CommandType type) {
CommandExecutor    this.executor = executor;
CommandExecutor    this.name = name;
CommandExecutor    this.category = category;
CommandExecutor    this.type = type;
CommandExecutor}




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
        TestCommand mockCommand = new TestCommand("test-command", CommandCategory.DATA_REFRESH, CommandType.SYNC);
        CommandContext context = createContext("admin", new String[]{"ADMIN"});
        
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
        TestCommand mockCommand = new TestCommand("protected-command", CommandCategory.DATA_REFRESH, CommandType.SYNC);
        CommandContext context = createContext("user", new String[]{"USER"});
        context.setPermissions(new String[]{"USER"});
        
        executor.register(mockCommand);
        CommandResult result = executor.execute("protected-command", Collections.emptyMap(), context);
        
        assertTrue(result.isFailed());
        assertEquals("PERMISSION_DENIED", result.getErrorCode());
    }

    @Test
    @DisplayName("Should handle validation error")
    void testHandleValidationError() {
        TestCommand mockCommand = new TestCommand("test-command", CommandCategory.DATA_REFRESH, CommandType.SYNC) {
            @Override
            public void validate(Map<String, Object> params) {
                throw new CommandValidationException("Invalid parameter");
            }
        };
        CommandContext context = createContext("admin", new String[]{"ADMIN"});
        
        executor.register(mockCommand);
        CommandResult result = executor.execute("test-command", Collections.emptyMap(), context);
        
        assertTrue(result.isFailed());
        assertEquals("VALIDATION_ERROR", result.getErrorCode());
    }

    @Test
    @DisplayName("Should handle execution error")
    void testHandleExecutionError() {
        TestCommand mockCommand = new TestCommand("test-command", CommandCategory.DATA_REFRESH, CommandType.SYNC) {
            @Override
            public CommandResult execute(Map<String, Object> params, CommandContext context) {
                throw new RuntimeException("Execution failed");
            }
        };
        CommandContext context = createContext("admin", new String[]{"ADMIN"});
        
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
        TestCommand mockCommand = new TestCommand("test-command", CommandCategory.DATA_REFRESH, CommandType.SYNC);
        CommandContext adminContext = createContext("admin", new String[]{"ADMIN"});
        
        executor.register(mockCommand);
        CommandResult result = executor.execute("test-command", Collections.emptyMap(), adminContext);
        
        assertTrue(result.isSuccess());
    }

    // Helper methods
    private Command createMockCommand(String name, CommandCategory category, CommandType type) {
        return new TestCommand(name, category, type);
    }

    private CommandContext createContext(String username, String[] roles) {
        CommandContext context = new CommandContext("user-id", username, roles);
        context.setPermissions(new String[]{"ADMIN"});
        return context;
    }

    // Test command implementation
    private static class TestCommand implements Command {




        TestCommand(String name, CommandCategory category, CommandType type) {
            this.name = name;
            this.category = category;
            this.type = type;
        }

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
        public CommandSchema getSchema() { return new CommandSchema(name); }
        @Override
        public CommandResult execute(Map<String, Object> params, CommandContext context) {
            return CommandResult.success(name, Map.of("result", "success"));
        }
        @Override
        public void validate(Map<String, Object> params) {}
    }
}