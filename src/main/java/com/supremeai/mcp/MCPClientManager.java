package com.supremeai.mcp;

import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.*;

/**
 * MCP Client Manager - Plan 24 Phase 2
 * Manages connections to external MCP servers (Ruflo, Pinokio, etc.)
 */
@Component
public class MCPClientManager {
    private final Map<String, MCPServer> servers = new HashMap<>();
    private final RestTemplate restTemplate = new RestTemplate();
    
    /**
     * Connect to external MCP server
     */
    public void connectServer(String name, String url) {
        MCPServer server = new MCPServer(name, url);
        servers.put(name, server);
    }
    
    /**
     * List all tools from all connected servers
     */
    public List<Map<String, Object>> listAllTools() {
        List<Map<String, Object>> allTools = new ArrayList<>();
        for (MCPServer server : servers.values()) {
            allTools.addAll(server.listTools());
        }
        return allTools;
    }
    
    /**
     * Execute tool from any connected MCP server
     */
    public Object executeTool(String serverName, String toolName, Map<String, Object> args) {
        MCPServer server = servers.get(serverName);
        if (server == null) {
            throw new IllegalArgumentException("Server not found: " + serverName);
        }
        return server.callTool(toolName, args);
    }
    
    /**
     * MCP Server wrapper
     */
    private static class MCPServer {
        private final String name;
        private final String url;
        
        MCPServer(String name, String url) {
            this.name = name;
            this.url = url;
        }
        
        List<Map<String, Object>> listTools() {
            try {
                org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
                Map<String, Object> response = restTemplate.postForObject(url + "/tools/list", Collections.emptyMap(), Map.class);
                if (response != null && response.containsKey("tools")) {
                    return (List<Map<String, Object>>) response.get("tools");
                }
            } catch (Exception e) {
                // ignore
            }
            return Collections.emptyList();
        }
        
        Object callTool(String toolName, Map<String, Object> args) {
            try {
                org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
                Map<String, Object> request = new HashMap<>();
                request.put("name", toolName);
                request.put("arguments", args);
                return restTemplate.postForObject(url + "/tools/call", request, Map.class);
            } catch (Exception e) {
                Map<String, Object> result = new HashMap<>();
                result.put("error", e.getMessage());
                return result;
            }
        }
    }
}
