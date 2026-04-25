package com.supremeai.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class APIProviderTest {

    @Test
    public void testDefaultConstructor() {
        APIProvider provider = new APIProvider();

        assertNull(provider.getId());
        assertNull(provider.getName());
        assertNull(provider.getType());
        assertNull(provider.getStatus());
        assertNull(provider.getBaseUrl());
        assertNull(provider.getApiKey());
        assertNull(provider.getUsageLimit());
        assertNull(provider.getCurrentUsage());
        assertNull(provider.getLastCheck());
    }

    @Test
    public void testConstructorWithIdNameTypeStatus() {
        APIProvider provider = new APIProvider("provider1", "OpenAI", "openai", "active");

        assertEquals("provider1", provider.getId());
        assertEquals("OpenAI", provider.getName());
        assertEquals("openai", provider.getType());
        assertEquals("active", provider.getStatus());
        assertNotNull(provider.getLastCheck());
    }

    @Test
    public void testSettersAndGetters() {
        APIProvider provider = new APIProvider();

        provider.setId("test-id");
        assertEquals("test-id", provider.getId());

        provider.setName("Test Provider");
        assertEquals("Test Provider", provider.getName());

        provider.setType("test-type");
        assertEquals("test-type", provider.getType());

        provider.setStatus("active");
        assertEquals("active", provider.getStatus());

        provider.setBaseUrl("https://api.test.com");
        assertEquals("https://api.test.com", provider.getBaseUrl());

        provider.setApiKey("test-api-key");
        assertEquals("test-api-key", provider.getApiKey());

        provider.setUsageLimit(1000.0);
        assertEquals(1000.0, provider.getUsageLimit(), 0.001);

        provider.setCurrentUsage(500.0);
        assertEquals(500.0, provider.getCurrentUsage(), 0.001);

        LocalDateTime now = LocalDateTime.now();
        provider.setLastCheck(now);
        assertEquals(now, provider.getLastCheck());
    }
}
