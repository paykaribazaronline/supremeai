package com.supremeai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
public class ConfigController {

    @GetMapping("/firebase")
    public Map<String, String> getFirebaseConfig() {
        Map<String, String> config = new HashMap<>();
        config.put("apiKey", "AIzaSyCib1UPogwLoAshIWm9YQJB_RR0UxC07i8");
        config.put("authDomain", "supremeai-a.firebaseapp.com");
        config.put("databaseURL", "https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app/");
        config.put("projectId", "supremeai-a");
        config.put("storageBucket", "supremeai-a.firebasestorage.app");
        config.put("messagingSenderId", "565236080752");
        config.put("appId", "1:565236080752:web:572bb9313db9afb355d4b5");
        return config;
    }
}
