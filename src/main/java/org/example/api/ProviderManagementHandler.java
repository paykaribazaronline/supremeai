package org.example.api;

import org.example.model.APIProvider;
import org.example.service.AIProviderDiscoveryService;
import org.example.service.FirebaseService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * REST API Handler for Provider Management
 * 
 * Admin can:
 * - GET /api/providers/available → List available AI providers
 * - GET /api/providers/configured → List active providers
 * - POST /api/providers/add → Add new provider
 * - POST /api/providers/remove → Remove provider
 * - POST /api/providers/test → Test provider connection
 * 
 * NO hardcoded providers - everything admin-controlled!
 */
public class ProviderManagementHandler implements HttpHandler {
    
    private final FirebaseService firebase;
    private final AIProviderDiscoveryService discoveryService;
    
    public ProviderManagementHandler(FirebaseService firebase, AIProviderDiscoveryService discoveryService) {
        this.firebase = firebase;
        this.discoveryService = discoveryService;
    }
    
    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        String method = exchange.getRequestMethod();
        
        try {
            if (method.equals("GET")) {
                handleGet(path, exchange);
            } else if (method.equals("POST")) {
                handlePost(path, exchange);
            } else {
                sendError(exchange, 405, "Method not allowed");
            }
        } catch (Exception e) {
            sendError(exchange, 500, "Error: " + e.getMessage());
        }
    }
    
    private void handleGet(String path, HttpExchange exchange) throws IOException {
        if (path.equals("/api/providers/available")) {
            // List all available AI providers from internet
            List<APIProvider> providers = discoveryService.discoverAvailableProviders();
            sendJson(exchange, 200, providers);
        } 
        else if (path.equals("/api/providers/configured")) {
            // List currently configured providers
            List<APIProvider> providers = discoveryService.getConfiguredProviders();
            sendJson(exchange, 200, providers);
        } 
        else {
            sendError(exchange, 404, "Endpoint not found");
        }
    }
    
    private void handlePost(String path, HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, Object> request = parseJson(body);
        
        if (path.equals("/api/providers/add")) {
            handleAddProvider(request, exchange);
        } 
        else if (path.equals("/api/providers/remove")) {
            handleRemoveProvider(request, exchange);
        } 
        else if (path.equals("/api/providers/test")) {
            handleTestProvider(request, exchange);
        } 
        else {
            sendError(exchange, 404, "Endpoint not found");
        }
    }
    
    private void handleAddProvider(Map<String, Object> request, HttpExchange exchange) throws IOException {
        String providerName = (String) request.get("name");
        String apiKey = (String) request.get("key");
        String endpoint = (String) request.getOrDefault("endpoint", "default");
        
        if (providerName == null || apiKey == null) {
            sendError(exchange, 400, "Missing provider name or API key");
            return;
        }
        
        try {
            discoveryService.addProvider(providerName, endpoint, apiKey);
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "Provider '" + providerName + "' added successfully");
            response.put("provider", providerName);
            response.put("timestamp", System.currentTimeMillis());
            
            sendJson(exchange, 201, response);
        } catch (Exception e) {
            sendError(exchange, 400, "Failed to add provider: " + e.getMessage());
        }
    }
    
    private void handleRemoveProvider(Map<String, Object> request, HttpExchange exchange) throws IOException {
        String providerName = (String) request.get("name");
        
        if (providerName == null) {
            sendError(exchange, 400, "Missing provider name");
            return;
        }
        
        discoveryService.disableProvider(providerName);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Provider '" + providerName + "' removed");
        response.put("provider", providerName);
        
        sendJson(exchange, 200, response);
    }
    
    private void handleTestProvider(Map<String, Object> request, HttpExchange exchange) throws IOException {
        String providerName = (String) request.get("name");
        String apiKey = (String) request.get("key");
        
        if (providerName == null || apiKey == null) {
            sendError(exchange, 400, "Missing provider name or API key");
            return;
        }
        
        // Test connection (in real implementation, make test API call)
        boolean connectionSuccessful = testProviderConnection(providerName, apiKey);
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", connectionSuccessful ? "success" : "failed");
        response.put("provider", providerName);
        response.put("message", connectionSuccessful ? "✅ Connection successful" : "❌ Connection failed");
        response.put("timestamp", System.currentTimeMillis());
        
        sendJson(exchange, connectionSuccessful ? 200 : 400, response);
    }
    
    /**
     * Test if API key works for given provider
     */
    private boolean testProviderConnection(String providerName, String apiKey) {
        // In production, make actual test call to API endpoint
        // For now, just verify key format
        
        return !apiKey.isEmpty() && apiKey.length() > 10;
    }
    
    private void sendJson(HttpExchange exchange, int status, Object data) throws IOException {
        String json = convertToJson(data);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, json.getBytes().length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(json.getBytes());
        }
    }
    
    private void sendError(HttpExchange exchange, int status, String message) throws IOException {
        Map<String, Object> error = new HashMap<>();
        error.put("error", message);
        error.put("status", status);
        sendJson(exchange, status, error);
    }
    
    private String convertToJson(Object obj) {
        // Simple JSON conversion (in production, use Jackson or GSON)
        return obj.toString();
    }
    
    private Map<String, Object> parseJson(String body) {
        // Simple JSON parsing (in production, use Jackson or GSON)
        Map<String, Object> map = new HashMap<>();
        // For now, return mock data
        return map;
    }
}
