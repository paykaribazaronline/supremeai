package com.supremeai.service;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.ListenerRegistration;
import com.supremeai.model.APIProvider;
import com.supremeai.repository.ProviderRepository;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Service to manage AI Provider metadata with real-time Firestore synchronization.
 */
@Service
public class ProviderMetadataService {
    private static final Logger logger = LoggerFactory.getLogger(ProviderMetadataService.class);

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private Firestore firestore;

    private final Map<String, APIProvider> metadataCache = new ConcurrentHashMap<>();
    private ListenerRegistration listenerRegistration;
    private final Executor listenerExecutor = Executors.newSingleThreadExecutor();

    @PostConstruct
    public void init() {
        logger.info("Initializing ProviderMetadataService with real-time listener...");
        try {
            this.listenerRegistration = firestore.collection("api_providers")
                    .addSnapshotListener(listenerExecutor, (snapshot, error) -> {
                        if (error != null) {
                            logger.error("Firestore listener error for api_providers", error);
                            return;
                        }

                        if (snapshot != null) {
                            snapshot.getDocumentChanges().forEach(change -> {
                                try {
                                    APIProvider provider = change.getDocument().toObject(APIProvider.class);
                                    if (provider != null) {
                                        String docId = change.getDocument().getId();
                                        provider.setDocumentId(docId);
                                        if (provider.getId() == null) {
                                            provider.setId(docId);
                                        }

                                        if (change.getType() == com.google.cloud.firestore.DocumentChange.Type.REMOVED) {
                                            if (provider.getName() != null) {
                                                metadataCache.remove(provider.getName().toLowerCase().trim());
                                            }
                                            metadataCache.remove(docId.toLowerCase().trim());
                                            if (provider.getId() != null) {
                                                metadataCache.remove(provider.getId().toLowerCase().trim());
                                            }
                                            logger.info("Provider metadata removed: {}", docId);
                                        } else {
                                            if (provider.getName() != null) {
                                                metadataCache.put(provider.getName().toLowerCase().trim(), provider);
                                            }
                                            metadataCache.put(docId.toLowerCase().trim(), provider);
                                            if (provider.getId() != null) {
                                                metadataCache.put(provider.getId().toLowerCase().trim(), provider);
                                            }
                                            logger.info("Provider metadata updated: docId={}, name={}, id={}", docId, provider.getName(), provider.getId());
                                        }
                                    }
                                } catch (Exception e) {
                                    logger.error("Error deserializing APIProvider from Firestore", e);
                                }
                            });
                        }
                    });
        } catch (Exception e) {
            logger.error("Failed to setup ProviderMetadataService listener", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    public APIProvider getMetadata(String providerName) {
        return metadataCache.get(providerName.toLowerCase());
    }
    
    public String getBaseUrl(String providerName, String defaultUrl) {
        APIProvider meta = getMetadata(providerName);
        return (meta != null && meta.getBaseUrl() != null && !meta.getBaseUrl().isBlank()) ? meta.getBaseUrl() : defaultUrl;
    }

    public String getDefaultModel(String providerName, String defaultModel) {
        APIProvider meta = getMetadata(providerName);
        if (meta != null && meta.getModels() != null && !meta.getModels().isEmpty()) {
            return meta.getModels().get(0);
        }
        return defaultModel;
    }

    public Map<String, APIProvider> getAllMetadata() {
        return new HashMap<>(metadataCache);
    }
}
