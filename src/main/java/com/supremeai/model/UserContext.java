package com.supremeai.model;

public class UserContext {
    private String userId;
    private String sessionId;
    private String tenantId;
    private String apiKeyId;
    private String codeContext;

    public UserContext() {}

    public UserContext(String userId, String sessionId) {
        this.userId = userId;
        this.sessionId = sessionId;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getTenantId() { return tenantId; }
    public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    public String getApiKeyId() { return apiKeyId; }
    public void setApiKeyId(String apiKeyId) { this.apiKeyId = apiKeyId; }
    public String getCodeContext() { return codeContext; }
    public void setCodeContext(String codeContext) { this.codeContext = codeContext; }
}
