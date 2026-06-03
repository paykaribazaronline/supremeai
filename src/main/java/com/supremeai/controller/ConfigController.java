package com.supremeai.controller;

import com.supremeai.response.ApiResponse;
import com.google.firebase.FirebaseApp;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {
    public ConfigController(String apiKey, String authDomain, String databaseUrl, String projectId, String storageBucket, String messagingSenderId, String appId) {
        this.apiKey = apiKey;
        this.authDomain = authDomain;
        this.databaseUrl = databaseUrl;
        this.projectId = projectId;
        this.storageBucket = storageBucket;
        this.messagingSenderId = messagingSenderId;
        this.appId = appId;
    }









    @GetMapping("/firebase")
    public Map<String, String> getFirebaseConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("apiKey", apiKey);
        config.put("authDomain", authDomain);
        config.put("databaseURL", databaseUrl);
        config.put("projectId", projectId);
        config.put("storageBucket", storageBucket);
        config.put("messagingSenderId", messagingSenderId);
        config.put("appId", appId);
        return config;
    }

    @GetMapping("/public")
    public Map<String, String> getPublicConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("version", "1.0.0");
        config.put("environment", "production");
        config.put("features", "admin,api,chat");
        return config;
    }

    @GetMapping("/diagnose")
    public Mono<ApiResponse<Map<String, Object>>> diagnose() {
        Map<String, Object> diagnosis = new HashMap<>();
        diagnosis.put("projectId", projectId);
        diagnosis.put("authDomain", authDomain);
        diagnosis.put("firebaseAppInitialized", !FirebaseApp.getApps().isEmpty());
        return Mono.just(ApiResponse.ok(diagnosis));
    }
}
