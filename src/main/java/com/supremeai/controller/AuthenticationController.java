package com.supremeai.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthenticationController {

    @PostMapping("/firebase-login")
    public Map<String, Object> firebaseLogin(@RequestBody Map<String, String> request) {
        String idToken = request.get("idToken");
        
        // In a real scenario, you'd verify the token with FirebaseAuth.getInstance().verifyIdToken(idToken)
        // For now, we simulate a successful token exchange to unblock the UI.
        
        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("token", "simulated_jwt_" + UUID.randomUUID().toString());
        response.put("refreshToken", "simulated_refresh_" + UUID.randomUUID().toString());
        response.put("type", "Bearer");
        response.put("expiresIn", 86400);
        
        Map<String, Object> user = new HashMap<>();
        user.put("id", "firebase_user_" + System.currentTimeMillis());
        user.put("username", "admin");
        user.put("email", "admin@supremeai.com");
        user.put("role", "admin");
        response.put("user", user);
        
        return response;
    }
}
