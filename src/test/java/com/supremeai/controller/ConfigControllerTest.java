package com.supremeai.controller;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigControllerTest {

    private final ConfigController controller = new ConfigController();

    @Test
    void getFirebaseConfig_shouldReturnAllRequiredFields() {
        Map<String, String> config = controller.getFirebaseConfig();

        assertNotNull(config);
        assertEquals("AIzaSyCib1UPogwLoAshIWm9YQJB_RR0UxC07i8", config.get("apiKey"));
        assertEquals("supremeai-a.firebaseapp.com", config.get("authDomain"));
        assertEquals("https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app/", config.get("databaseURL"));
        assertEquals("supremeai-a", config.get("projectId"));
        assertEquals("supremeai-a.firebasestorage.app", config.get("storageBucket"));
        assertEquals("565236080752", config.get("messagingSenderId"));
        assertEquals("1:565236080752:web:572bb9313db9afb355d4b5", config.get("appId"));
    }

    @Test
    void getFirebaseConfig_shouldReturnSevenFields() {
        Map<String, String> config = controller.getFirebaseConfig();
        assertEquals(7, config.size());
    }

    @Test
    void getPublicConfig_shouldReturnVersionAndEnvironment() {
        Map<String, String> config = controller.getPublicConfig();

        assertNotNull(config);
        assertEquals("1.0.0", config.get("version"));
        assertEquals("production", config.get("environment"));
        assertEquals("admin,api,chat", config.get("features"));
    }

    @Test
    void getPublicConfig_shouldReturnThreeFields() {
        Map<String, String> config = controller.getPublicConfig();
        assertEquals(3, config.size());
    }
}
