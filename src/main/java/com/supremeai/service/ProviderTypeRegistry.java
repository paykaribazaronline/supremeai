package com.supremeai.service;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.ListenerRegistration;
import com.supremeai.model.ProviderTypeConfig;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Dynamic provider type registry backed by Firestore.
 * Replaces all hardcoded switch/case provider mappings.
 *
 * Firestore collection: provider_types
 * Each document contains: typeId, defaultBaseUrl, defaultModel, authType, etc.
 * Admin can add/edit/delete provider types via dashboard without code changes.
 */
@Service
public class ProviderTypeRegistry {
    public ProviderTypeRegistry(Firestore firestore) {
        this.firestore = firestore;
    }


    private static final Logger log = LoggerFactory.getLogger(ProviderTypeRegistry.class);
    private static final String COLLECTION = "provider_types";


    private final Map<String, ProviderTypeConfig> typeCache = new ConcurrentHashMap<>();
    private ListenerRegistration listenerRegistration;
    private final Executor listenerExecutor = Executors.newSingleThreadExecutor();

    @PostConstruct
    public void init() {
        log.info("[ProviderTypeRegistry] Initializing with Firestore real-time listener...");
        try {
            listenerRegistration = firestore.collection(COLLECTION)
                    .addSnapshotListener(listenerExecutor, (snapshot, error) -> {
                        if (error != null) {
                            log.error("[ProviderTypeRegistry] Firestore listener error", error);
                            return;
                        }
                        if (snapshot != null) {
                            snapshot.getDocumentChanges().forEach(change -> {
                                try {
                                    ProviderTypeConfig type = change.getDocument().toObject(ProviderTypeConfig.class);
                                    if (type != null && type.getTypeId() != null) {
                                        String key = type.getTypeId().toLowerCase();
                                        switch (change.getType()) {
                                            case ADDED, MODIFIED -> {
                                                typeCache.put(key, type);
                                                log.info("[ProviderTypeRegistry] Type updated: {} -> baseUrl={}", key, type.getDefaultBaseUrl());
                                            }
                                            case REMOVED -> {
                                                typeCache.remove(key);
                                                log.info("[ProviderTypeRegistry] Type removed: {}", key);
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    log.error("[ProviderTypeRegistry] Error deserializing ProviderTypeConfig", e);
        throw new RuntimeException("Swallowed exception: " + e.getMessage(), e);
    }
                            });
                        }
                    });
        } catch (Exception e) {
            log.error("[ProviderTypeRegistry] Failed to setup listener", e);
        throw new RuntimeException("Swallowed exception: " + e.getMessage(), e);
    }
    }

    @PreDestroy
    public void cleanup() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    public ProviderTypeConfig getTypeConfig(String typeId) {
        if (typeId == null) return null;
        return typeCache.get(typeId.toLowerCase());
    }

    public String getDefaultBaseUrl(String typeId) {
        ProviderTypeConfig config = getTypeConfig(typeId);
        return config != null ? config.getDefaultBaseUrl() : null;
    }

    public String getDefaultModel(String typeId) {
        ProviderTypeConfig config = getTypeConfig(typeId);
        return config != null ? config.getDefaultModel() : null;
    }

    public String getAuthType(String typeId) {
        ProviderTypeConfig config = getTypeConfig(typeId);
        return config != null ? config.getAuthType() : "bearer";
    }

    public Map<String, ProviderTypeConfig> getAllTypes() {
        return Map.copyOf(typeCache);
    }

    public boolean hasType(String typeId) {
        return typeId != null && typeCache.containsKey(typeId.toLowerCase());
    }
}
