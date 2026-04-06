package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores and retrieves the admin-configured fallback chain (ordered list of provider IDs).
 *
 * When the admin saves a fallback order via POST /api/providers/fallback-chain,
 * it is persisted here and consulted by AIAPIService instead of the hard-coded
 * DEFAULT_FALLBACK_CHAIN constant.
 *
 * If the admin has not configured a chain yet, an empty list is returned and
 * AIAPIService falls back to using all active providers from ProviderRegistryService.
 */
@Service
public class FallbackConfigService {

    private static final Logger logger = LoggerFactory.getLogger(FallbackConfigService.class);
    private static final String STORE_PATH = "fallback-chain.json";

    @Autowired
    private LocalJsonStoreService localJsonStoreService;

    /**
     * Return the admin-configured fallback chain.
     * Returns an empty list when not yet configured (means "use all active providers from DB").
     */
    public List<String> getFallbackChain() {
        try {
            List<String> chain = localJsonStoreService.read(
                STORE_PATH,
                new TypeReference<List<String>>() {},
                new ArrayList<>()
            );
            return chain == null ? new ArrayList<>() : chain;
        } catch (Exception e) {
            logger.warn("Failed to read fallback chain config: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Persist a new fallback chain set by the admin.
     * Pass an empty list to clear the chain (system will use all active providers).
     */
    public void setFallbackChain(List<String> chain) {
        try {
            List<String> toSave = chain == null ? new ArrayList<>() : new ArrayList<>(chain);
            localJsonStoreService.write(STORE_PATH, toSave);
            logger.info("✅ Fallback chain updated: {}", toSave);
        } catch (Exception e) {
            logger.error("Failed to persist fallback chain: {}", e.getMessage());
            throw new IllegalStateException("Failed to save fallback chain", e);
        }
    }
}
