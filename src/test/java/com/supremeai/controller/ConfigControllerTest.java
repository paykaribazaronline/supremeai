package com.supremeai.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConfigControllerTest {

    private ConfigController controller;

    @BeforeEach
    void setUp() {
        controller = new ConfigController();
        ReflectionTestUtils.setField(controller, "apiKey", "AIzaSyCib1UPogwLoAshIWm9YQJB_RR0UxC07i8");
        ReflectionTestUtils.setField(controller, "authDomain", "supremeai-a.firebaseapp.com");
        ReflectionTestUtils.setField(controller, "databaseUrl", "https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app/");
        ReflectionTestUtils.setField(controller, "projectId", "supremeai-a");
        ReflectionTestUtils.setField(controller, "storageBucket", "supremeai-a.firebasestorage.app");
        ReflectionTestUtils.setField(controller, "messagingSenderId", "565236080752");
        ReflectionTestUtils.setField(controller, "appId", "1:565236080752:web:572bb9313db9afb355d4b5");
    }

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
