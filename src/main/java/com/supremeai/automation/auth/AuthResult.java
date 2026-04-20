package com.supremeai.automation.auth;

public class AuthResult {
    private boolean success;
    private String message;
    private String tokenOrUid; // UID if creating account, JWT Token if logging in

    public AuthResult(boolean success, String message, String tokenOrUid) {
        this.success = success;
        this.message = message;
        this.tokenOrUid = tokenOrUid;
    }

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getTokenOrUid() { return tokenOrUid; }
}