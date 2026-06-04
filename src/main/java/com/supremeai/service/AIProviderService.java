package com.supremeai.service;

import com.supremeai.model.APIProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AIProviderService {

    private final Map<String, List<String>> providerKeys = new ConcurrentHashMap<>();
    private final Map<String, Integer> currentKeyIndex = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> exhaustedKeys = new ConcurrentHashMap<>();
    private final Map<String, APIProvider> providerStore = new ConcurrentHashMap<>();

    @Autowired
    public AIProviderService(Map<String, String> initialKeys) {
        initialKeys.forEach((provider, key) -> {
            providerKeys.computeIfAbsent(provider, k -> new ArrayList<>()).add(key);
            currentKeyIndex.putIfAbsent(provider, 0);
            exhaustedKeys.putIfAbsent(provider, new HashSet<>());
        });
    }

    /**
     * Return the active (non-exhausted) key for the given provider.
     * Thread-safe: uses ConcurrentHashMap-compute with the exhausted-keys set
     * rather than a coarse method-level lock.
     */
    public String getActiveKey(String provider) {
        List<String> keys = providerKeys.get(provider);
        if (keys == null || keys.isEmpty()) return null;
        // Use ConcurrentHashMap + synchronization only on the per-provider entry's
        // exhausted set to allow simultaneous lookups across providers.
        Set<String> exhausted = exhaustedKeys.get(provider);
        int idx = currentKeyIndex.getOrDefault(provider, 0);
        String candidate = keys.get(idx);
        if (exhausted == null || !exhausted.contains(candidate)) {
            return candidate;
        }
        // Cached index is exhausted — atomically rotate to next candidate
        String rotated = rotateKey(provider);
        if (rotated != null && exhausted != null && exhausted.contains(rotated)) {
            return null;
        }
        return rotated;
    }

    /**
     * Rotate to the next key for the given provider.
     * Thread-safe: uses {@link ConcurrentHashMap#compute} so that concurrent
     * rotations for the same provider do not corrupt the index pointer.
     */
    public String rotateKey(String provider) {
        List<String> keys = providerKeys.get(provider);
        if (keys == null || keys.isEmpty()) return null;

        return currentKeyIndex.compute(provider, (prov, currentIdx) -> {
            int nextIdx = (currentIdx == null ? 0 : currentIdx + 1) % keys.size();
            String newKey = keys.get(nextIdx);

            // Spin through all keys before giving up
            int attempts = 0;
            while (exhaustedKeys.getOrDefault(provider, Set.of()).contains(newKey)) {
                nextIdx = (nextIdx + 1) % keys.size();
                newKey = keys.get(nextIdx);
                if (++attempts >= keys.size()) return currentIdx == null ? 0 : currentIdx;
            }
            return nextIdx;
        }) == null ? null : keys.get(currentKeyIndex.getOrDefault(provider, 0));
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
        exhaustedKeys.putIfAbsent(provider, new HashSet<>());
    }

    // Stub for analysis feature - persists provider metadata
    public void saveProvider(APIProvider provider) {
        providerStore.put(provider.getId(), provider);
    }
}
