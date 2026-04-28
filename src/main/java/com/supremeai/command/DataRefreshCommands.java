package com.supremeai.command;

import com.supremeai.service.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Data Refresh Commands
 * - Refresh GitHub data
 * - Refresh Vercel status
 * - Refresh Firebase metrics
 * - Refresh all
 */
public class DataRefreshCommands {
    private static final Logger logger = LoggerFactory.getLogger(DataRefreshCommands.class);
    
    private final UnifiedDataService dataService;
    
    public DataRefreshCommands(UnifiedDataService dataService) {
        this.dataService = dataService;
    }
    
    /**
     * Refresh GitHub Command
     */
    public Command getRefreshGitHubCommand() {
        return new Command() {
            @Override
            public String getName() { return "refresh-github"; }
            
            @Override
            public String getDescription() { 
                return "Refresh GitHub repository data"; 
            }
            
            @Override
            public CommandCategory getCategory() { 
                return CommandCategory.DATA_REFRESH; 
            }
            
            @Override
            public CommandType getType() { 
                return CommandType.ASYNC; 
            }
            
            @Override
            public String[] getRequiredPermissions() { 
                return new String[] { "execute.refresh" }; 
            }
            
            @Override
            public CommandSchema getSchema() { 
                CommandSchema schema = new CommandSchema("refresh-github");
                // Add parameters if needed
                return schema;
            }
            
            @Override
            public CommandResult execute(Map<String, Object> params, CommandContext context) {
                try {
                    String owner = (String) params.getOrDefault("owner", "supremeai");
                    String repo = (String) params.getOrDefault("repo", "core");
                    
                    logger.info("🔄 Refreshing GitHub data: {}/{}", owner, repo);
                    
                    // Call the service to fetch fresh data
                    Map<String, Object> data = dataService.getCollectedData("github:" + owner + "/" + repo);
                    
                    logger.info("✅ GitHub data refreshed");
                    
                    // Return pending for async
                    String jobId = UUID.randomUUID().toString();
                    return CommandResult.pending("refresh-github", jobId);
                    
                } catch (Exception e) {
                    logger.error("❌ GitHub refresh failed", e);
                    return CommandResult.error("refresh-github", "REFRESH_FAILED", e.getMessage());
                }
            }
            
            @Override
            public void validate(Map<String, Object> params) {
                // owner and repo are optional with defaults
            }
        };
    }
    
    /**
     * Refresh Vercel Command
     */
    public Command getRefreshVercelCommand() {
        return new Command() {
            @Override
            public String getName() { return "refresh-vercel"; }
            
            @Override
            public String getDescription() { 
                return "Refresh Vercel deployment status"; 
            }
            
            @Override
            public CommandCategory getCategory() { 
                return CommandCategory.DATA_REFRESH; 
            }
            
            @Override
            public CommandType getType() { 
                return CommandType.ASYNC; 
            }
            
            @Override
            public String[] getRequiredPermissions() { 
                return new String[] { "execute.refresh" }; 
            }
            
            @Override
            public CommandSchema getSchema() { 
                return new CommandSchema("refresh-vercel");
            }
            
            @Override
            public CommandResult execute(Map<String, Object> params, CommandContext context) {
                try {
                    String projectId = (String) params.getOrDefault("projectId", "");
                    
                    logger.info("🔄 Refreshing Vercel status: {}", projectId);
                    
                    // Call the service
                    Map<String, Object> data = dataService.getCollectedData("vercel:" + projectId);
                    
                    logger.info("✅ Vercel data refreshed");
                    
                    String jobId = UUID.randomUUID().toString();
                    return CommandResult.pending("refresh-vercel", jobId);
                    
                } catch (Exception e) {
                    logger.error("❌ Vercel refresh failed", e);
                    return CommandResult.error("refresh-vercel", "REFRESH_FAILED", e.getMessage());
                }
            }
            
            @Override
            public void validate(Map<String, Object> params) {}
        };
    }
    
    /**
     * Refresh Firebase Command
     */
    public Command getRefreshFirebaseCommand() {
        return new Command() {
            @Override
            public String getName() { return "refresh-firebase"; }
            
            @Override
            public String getDescription() { 
                return "Refresh Firebase project metrics"; 
            }
            
            @Override
            public CommandCategory getCategory() { 
                return CommandCategory.DATA_REFRESH; 
            }
            
            @Override
            public CommandType getType() { 
                return CommandType.ASYNC; 
            }
            
            @Override
            public String[] getRequiredPermissions() { 
                return new String[] { "execute.refresh" }; 
            }
            
            @Override
            public CommandSchema getSchema() { 
                return new CommandSchema("refresh-firebase");
            }
            
            @Override
            public CommandResult execute(Map<String, Object> params, CommandContext context) {
                try {
                    logger.info("🔄 Refreshing Firebase metrics");
                    
                    // Call the service
                    Map<String, Object> data = dataService.getCollectedData("firebase");
                    
                    logger.info("✅ Firebase data refreshed");
                    
                    String jobId = UUID.randomUUID().toString();
                    return CommandResult.pending("refresh-firebase", jobId);
                    
                } catch (Exception e) {
                    logger.error("❌ Firebase refresh failed", e);
                    return CommandResult.error("refresh-firebase", "REFRESH_FAILED", e.getMessage());
                }
            }
            
            @Override
            public void validate(Map<String, Object> params) {}
        };
    }
    
    /**
     * Refresh All Command
     */
    public Command getRefreshAllCommand() {
        return new Command() {
            @Override
            public String getName() { return "refresh-all"; }
            
            @Override
            public String getDescription() { 
                return "Refresh all data sources"; 
            }
            
            @Override
            public CommandCategory getCategory() { 
                return CommandCategory.DATA_REFRESH; 
            }
            
            @Override
            public CommandType getType() { 
                return CommandType.ASYNC; 
            }
            
            @Override
            public String[] getRequiredPermissions() { 
                return new String[] { "execute.refresh" }; 
            }
            
            @Override
            public CommandSchema getSchema() { 
                return new CommandSchema("refresh-all");
            }
            
            @Override
            public CommandResult execute(Map<String, Object> params, CommandContext context) {
                try {
                    logger.info("🔄 Refreshing all data sources");
                    
                    // Refresh all in parallel
                    List<Map<String, Object>> results = new ArrayList<>();
                    results.add(dataService.getCollectedData("github:supremeai/core"));
                    results.add(dataService.getCollectedData("firebase"));
                    
                    logger.info("✅ All data sources refreshed");
                    
                    String jobId = UUID.randomUUID().toString();
                    return CommandResult.pending("refresh-all", jobId);
                    
                } catch (Exception e) {
                    logger.error("❌ Full refresh failed", e);
                    return CommandResult.error("refresh-all", "REFRESH_FAILED", e.getMessage());
                }
            }
            
            @Override
            public void validate(Map<String, Object> params) {}
        };
    }
}
