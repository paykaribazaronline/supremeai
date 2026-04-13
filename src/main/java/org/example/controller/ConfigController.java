package org.example.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Value;
import java.util.HashMap;
import java.util.Map;

/**
 * Public config endpoint (safe for frontend)
 * Provides Firebase configuration and other public settings
 */
@RestController
@RequestMapping("/api/config")
@CrossOrigin(origins = "*")
public class ConfigController {

    @Value("${firebase.apiKey:${FIREBASE_API_KEY:}}")
    private String firebaseApiKey;

    @Value("${firebase.authDomain:supremeai-a.firebaseapp.com}")
    private String firebaseAuthDomain;

    @Value("${firebase.databaseURL:https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app/}")
    private String firebaseDatabaseURL;

    @Value("${firebase.projectId:supremeai-a}")
    private String firebaseProjectId;

    @Value("${firebase.storageBucket:supremeai-a.firebasestorage.app}")
    private String firebaseStorageBucket;

    @Value("${firebase.messagingSenderId:565236080752}")
    private String firebaseMessagingSenderId;

    @Value("${firebase.appId:1:565236080752:web:572bb9313db9afb355d4b5}")
    private String firebaseAppId;

    /**
     * Get Firebase configuration for client-side initialization
     * All values come from environment variables or application properties
     * NO hardcoded credentials
     */
    @GetMapping("/firebase")
    public Map<String, Object> getFirebaseConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("apiKey", firebaseApiKey);
        config.put("authDomain", firebaseAuthDomain);
        config.put("databaseURL", firebaseDatabaseURL);
        config.put("projectId", firebaseProjectId);
        config.put("storageBucket", firebaseStorageBucket);
        config.put("messagingSenderId", firebaseMessagingSenderId);
        config.put("appId", firebaseAppId);
        return config;
    }

    /**
     * Health check for config service
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        Map<String, String> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "ConfigController");
        return status;
    }
}
