package org.example.command;

/**
 * Command execution context
 * Contains user info, auth tokens, and request metadata
 */
public class CommandContext {
    private String userId;
    private String username;
    private String[] roles;              // ADMIN, USER, VIEWER
    private String[] permissions;        // Specific permissions
    private String authToken;
    private String sourceIp;
    private String sourceApp;            // CLI, Dashboard, API
    private long requestId;              // Unique request ID for tracking
    
    public CommandContext(String userId, String username, String[] roles) {
        this.userId = userId;
        this.username = username;
        this.roles = roles;
        this.requestId = System.currentTimeMillis();
    }
    
    // Check if user has permission
    public boolean hasPermission(String permission) {
        if (hasRole("ADMIN")) return true;
        if (permissions == null) return false;
        
        for (String p : permissions) {
            if (p.equals(permission)) return true;
        }
        return false;
    }
    
    // Check if user has role
    public boolean hasRole(String role) {
        if (roles == null) return false;
        for (String r : roles) {
            if (r.equals(role)) return true;
        }
        return false;
    }
    
    // Getters
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public String[] getRoles() { return roles; }
    public String[] getPermissions() { return permissions; }
    public String getAuthToken() { return authToken; }
    public String getSourceIp() { return sourceIp; }
    public String getSourceApp() { return sourceApp; }
    public long getRequestId() { return requestId; }
    
    // Setters
    public void setPermissions(String[] permissions) { this.permissions = permissions; }
    public void setAuthToken(String token) { this.authToken = token; }
    public void setSourceIp(String ip) { this.sourceIp = ip; }
    public void setSourceApp(String app) { this.sourceApp = app; }
}
