package com.supremeai.controller;

import com.google.firebase.FirebaseApp;
import com.supremeai.response.ApiResponse;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

  @Value("${firebase.api.key:}")
  private String apiKey;

  @Value("${firebase.auth.domain:supremeai-a.firebaseapp.com}")
  private String authDomain;

  @Value(
      "${firebase.database.url:https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app/}")
  private String databaseUrl;

  @Value("${firebase.project.id:supremeai-a}")
  private String projectId;

  @Value("${firebase.storage.bucket:supremeai-a.firebasestorage.app}")
  private String storageBucket;

  @Value("${firebase.messaging.sender.id:565236080752}")
  private String messagingSenderId;

  @Value("${firebase.app.id:1:565236080752:web:572bb9313db9afb355d4b5}")
  private String appId;

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
