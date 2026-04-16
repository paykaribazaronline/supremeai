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
        
        try {
            // Verify the token with Firebase Admin SDK
            com.google.firebase.auth.FirebaseToken decodedToken = 
                com.google.firebase.auth.FirebaseAuth.getInstance().verifyIdToken(idToken);
            
            String uid = decodedToken.getUid();
            String email = decodedToken.getEmail();
            String name = (String) decodedToken.getClaims().get("name");
            
            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            // We still provide a token for the app's internal session management if needed, 
            // but it's now tied to a verified Firebase identity.
            response.put("token", "fb_verified_" + UUID.randomUUID().toString());
            response.put("refreshToken", "fb_refresh_" + UUID.randomUUID().toString());
            response.put("type", "Bearer");
            response.put("expiresIn", 86400);
            
            Map<String, Object> user = new HashMap<>();
            user.put("id", uid);
            user.put("username", name != null ? name : email.split("@")[0]);
            user.put("email", email);
            user.put("role", "admin"); // Default to admin for now as requested for the dashboard
            response.put("user", user);
            
            return response;
        } catch (com.google.firebase.auth.FirebaseAuthException e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Invalid Firebase token: " + e.getMessage());
            return errorResponse;
        }
    }
}
