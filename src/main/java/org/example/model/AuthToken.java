package org.example.model;

/**
 * JWT Token Response Model
 */
public class AuthToken {
    private String token;
    private String refreshToken;
    private String type;
    private long expiresIn;
    private User user;
    
    public AuthToken() {}
    
    public AuthToken(String token, String refreshToken, User user, long expiresIn) {
        this.token = token;
        this.refreshToken = refreshToken;
        this.type = "Bearer";
        this.expiresIn = expiresIn;
        this.user = user;
    }
    
    // Clean user data for response (no password hash)
    public static class UserResponse {
        public String id;
        public String username;
        public String email;
        
        public UserResponse(User user) {
            this.id = user.getId();
            this.username = user.getUsername();
            this.email = user.getEmail();
        }
    }
    
    // Getters & Setters
    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public long getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(long expiresIn) {
        this.expiresIn = expiresIn;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
