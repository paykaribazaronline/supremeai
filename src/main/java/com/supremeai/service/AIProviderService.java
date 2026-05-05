package com.supremeai.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AIProviderService {

    private final Map<String, List<String>> providerKeys = new ConcurrentHashMap<>();
    private final Map<String, Integer> currentKeyIndex = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> exhaustedKeys = new ConcurrentHashMap<>();

    @Autowired
    public AIProviderService(Map<String, String> initialKeys) {
        // Initialize with keys from ProviderConfig
        initialKeys.forEach((provider, key) -> {
            providerKeys.computeIfAbsent(provider, k -> new ArrayList<>()).add(key);
            currentKeyIndex.putIfAbsent(provider, 0);
            exhaustedKeys.putIfAbsent(provider, new HashSet<>());
        });
    }

    public synchronized String getActiveKey(String provider) {
        List<String> keys = providerKeys.get(provider);
        if (keys == null || keys.isEmpty()) return null;

        int index = currentKeyIndex.get(provider);
        String key = keys.get(index);

        if (exhaustedKeys.get(provider).contains(key)) {
            return rotateKey(provider);
        }

        return key;
    }

    public synchronized String rotateKey(String provider) {
        List<String> keys = providerKeys.get(provider);
        if (keys == null || keys.isEmpty()) return null;

        int nextIndex = (currentKeyIndex.get(provider) + 1) % keys.size();
        currentKeyIndex.put(provider, nextIndex);
        
        String newKey = keys.get(nextIndex);
        
        // If we've circled back to an exhausted key, we have no valid keys
        if (exhaustedKeys.get(provider).contains(newKey)) {
            return null; 
        }

        return newKey;
    }

    public void markKeyAsExhausted(String provider, String key) {
        exhaustedKeys.computeIfAbsent(provider, k -> new HashSet<>()).add(key);
    }

    public void resetExhaustedKeys(String provider) {
        exhaustedKeys.put(provider, new HashSet<>());
    }

    public void addKey(String provider, String key) {
        providerKeys.computeIfAbsent(provider, k -> new ArrayList<>()).add(key);
        currentKeyIndex.putIfAbsent(provider, 0);
    }
}
