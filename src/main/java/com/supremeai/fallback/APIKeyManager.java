package com.supremeai.fallback;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class APIKeyManager {

    private final Map<AIProvider, List<String>> providerKeys = new HashMap<>();

    public APIKeyManager() {
        // Mocking some initial keys (In production, load these from Firebase Secret Manager)
        
        // 2 Keys for Groq
        addKey(AIProvider.GROQ_LLAMA3, "gsk_groq_primary_11111");
        addKey(AIProvider.GROQ_LLAMA3, "gsk_groq_backup_22222");
        
        // 1 Key for Gemini
        addKey(AIProvider.GEMINI_PRO, "ai39_gemini_key_777");
        
        // 3 Keys for HuggingFace Free Tier
        addKey(AIProvider.HUGGINGFACE_FREE, "hf_free_key_alpha");
        addKey(AIProvider.HUGGINGFACE_FREE, "hf_free_key_beta");
        addKey(AIProvider.HUGGINGFACE_FREE, "hf_free_key_gamma");
    }

    public void addKey(AIProvider provider, String key) {
        providerKeys.computeIfAbsent(provider, k -> new ArrayList<>()).add(key);
    }

    public List<String> getKeys(AIProvider provider) {
        return providerKeys.getOrDefault(provider, Collections.singletonList("default_untracked_key"));
    }
}