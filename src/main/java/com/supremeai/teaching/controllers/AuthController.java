package com.supremeai.teaching.controllers;

import com.supremeai.teaching.security.JwtTokenProvider;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {
    
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, UserAccount> userStore = new ConcurrentHashMap<>();
    
    public AuthController(JwtTokenProvider jwtTokenProvider, PasswordEncoder passwordEncoder) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.passwordEncoder = passwordEncoder;
        // Create default user for testing
        UserAccount defaultUser = new UserAccount("admin", "supremeai", "admin@supremeai.ai");
        userStore.put("admin", defaultUser);
    }
    
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        UserAccount user = userStore.get(request.username);
        
        if (user != null && passwordEncoder.matches(request.password, user.passwordHash)) {
            String token = jwtTokenProvider.generateToken(user.userId, user.username);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", token);
            response.put("userId", user.userId);
            response.put("username", user.username);
            return ResponseEntity.ok(response);
        }
        
        Map<String, Object> error = new HashMap<>();
        error.put("success", false);
        error.put("error", "Invalid username or password");
        return ResponseEntity.status(401).body(error);
    }
    
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest request) {
        if (userStore.containsKey(request.username)) {
            Map<String, Object> error = new HashMap<>();
            error.put("success", false);
            error.put("error", "Username already exists");
            return ResponseEntity.status(400).body(error);
        }
        
        String userId = UUID.randomUUID().toString();
        String passwordHash = passwordEncoder.encode(request.password);
        UserAccount newUser = new UserAccount(request.username, passwordHash, request.email);
        newUser.userId = userId;
        userStore.put(request.username, newUser);
        
        String token = jwtTokenProvider.generateToken(userId, request.username);
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("token", token);
        response.put("userId", userId);
        response.put("username", request.username);
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            Map<String, Object> error = new HashMap<>();
            error.put("valid", false);
            return ResponseEntity.ok(error);
        }
        
        String token = authHeader.substring(7);
        boolean valid = jwtTokenProvider.validateToken(token);
        
        Map<String, Object> response = new HashMap<>();
        response.put("valid", valid);
        if (valid) {
            response.put("username", jwtTokenProvider.getUsernameFromToken(token));
            response.put("userId", jwtTokenProvider.getUserIdFromToken(token));
        }
        return ResponseEntity.ok(response);
    }
    
    // DTOs
    public static class LoginRequest {
        public String username;
        public String password;
    }
    
    public static class RegisterRequest {
        public String username;
        public String password;
        public String email;
    }
    
    public static class UserAccount {
        public String userId;
        public String username;
        public String passwordHash;
        public String email;
        
        public UserAccount(String username, String passwordHash, String email) {
            this.userId = UUID.randomUUID().toString();
            this.username = username;
            this.passwordHash = passwordHash;
            this.email = email;
        }
    }
}
