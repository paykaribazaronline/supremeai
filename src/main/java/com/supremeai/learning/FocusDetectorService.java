package com.supremeai.learning;

import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.ListenerRegistration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

@Service
public class FocusDetectorService {

    private static final Logger log = LoggerFactory.getLogger(FocusDetectorService.class);

    @Autowired
    private Firestore firestore;

    private final Map<String, List<String>> focusKeywordsCache = new ConcurrentHashMap<>();
    private ListenerRegistration listenerRegistration;
    private final Executor listenerExecutor = Executors.newSingleThreadExecutor();

    @PostConstruct
    public void init() {
        log.info("[FocusDetector] Initializing with Firestore real-time listener...");
        try {
            listenerRegistration = firestore.collection("focus_keywords")
                    .addSnapshotListener(listenerExecutor, (snapshot, error) -> {
                        if (error != null) {
                            log.error("[FocusDetector] Firestore listener error", error);
                            return;
                        }
                        if (snapshot != null) {
                            focusKeywordsCache.clear();
                            snapshot.getDocuments().forEach(doc -> {
                                try {
                                    Object keywords = doc.get("keywords");
                                    if (keywords instanceof List) {
                                        List<String> kwList = new ArrayList<>();
                                        for (Object k : (List<?>) keywords) {
                                            if (k instanceof String) kwList.add(((String) k).toLowerCase());
                                        }
                                        focusKeywordsCache.put(doc.getId().toLowerCase(), kwList);
                                    }
                                } catch (Exception e) {
                                    log.error("[FocusDetector] Error deserializing focus keywords", e);
                                }
                            });
                            log.info("[FocusDetector] Loaded {} focus areas from Firestore", focusKeywordsCache.size());
                        }
                    });
        } catch (Exception e) {
            log.error("[FocusDetector] Failed to setup listener", e);
        }

        focusKeywordsCache.put("marketing", List.of("marketing", "growth", "seo", "ads", "advert", "promo", "brand", "social-media", "influencer", "copywriting", "content-writ", "hubspot", "mailchimp", "moz", "semrush", "ahrefs"));
        focusKeywordsCache.put("security_hacking", List.of("hack", "security", "exploit", "vulnerab", "pentest", "cve", "malware", "ransomware", "payload", "reverse-engine", "cybersec", "offensive", "defensive", "threat", "ctf", "kali", "metasploit", "burp"));
        focusKeywordsCache.put("ai_research", List.of("ai", "ml", "llm", "gpt", "deep-learning", "neural", "nlp", "openai", "anthropic", "huggingface", "model", "transformer", "machine-learning", "artificial-intelligence", "generative-ai"));
        focusKeywordsCache.put("programming", List.of("dev", "code", "program", "software", "engineer", "stackoverflow", "github", "gitlab", "docker", "kubernetes", "linux", "server", "api", "database", "sql", "nosql", "backend", "frontend", "java", "python", "javascript", "typescript", "go", "rust"));
        focusKeywordsCache.put("business_startup", List.of("startup", "business", "entrepreneur", "vc", "funding", "saas", "product-hunt", "pitch", "investor", "revenue", "biz"));
        focusKeywordsCache.put("data_science", List.of("data", "analytics", "statistics", "visuali", "pandas", "tableau", "powerbi", "bigdata", "etl", "warehouse", "lake"));
        focusKeywordsCache.put("design_ux", List.of("design", "figma", "ui", "ux", "user-experience", "photoshop", "illustrator", "adobe", "sketch", "prototyp"));
        focusKeywordsCache.put("cloud_devops", List.of("cloud", "aws", "gcp", "azure", "terraform", "ansible", "cicd", "jenkins", "github-actions", "devops", "sre", "infra"));
        focusKeywordsCache.put("blockchain_crypto", List.of("blockchain", "crypto", "bitcoin", "ethereum", "smart-contract", "web3", "defi", "nft", "solidity"));
        focusKeywordsCache.put("mobile_dev", List.of("android", "ios", "flutter", "react-native", "swift", "kotlin", "mobile", "app-dev", "xcode", "play-store"));
    }

    @PreDestroy
    public void cleanup() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }

    public String detectFocus(String url) {
        if (url == null || url.isBlank()) return "general";

        String lowerUrl = url.toLowerCase();
        try {
            URL parsed = new URL(url);
            String host = parsed.getHost().toLowerCase();
            String path = parsed.getPath().toLowerCase();
            String combined = host + " " + path;

            for (Map.Entry<String, List<String>> entry : focusKeywordsCache.entrySet()) {
                for (String keyword : entry.getValue()) {
                    if (combined.contains(keyword)) {
                        return entry.getKey();
                    }
                }
            }
        } catch (Exception e) {
            for (Map.Entry<String, List<String>> entry : focusKeywordsCache.entrySet()) {
                for (String keyword : entry.getValue()) {
                    if (lowerUrl.contains(keyword)) {
                        return entry.getKey();
                    }
                }
            }
        }

        return "general";
    }

    /**
     * Extract domain from a URL string.
     */
    public String extractDomain(String url) {
        if (url == null || url.isBlank()) return "unknown";
        try {
            return new URL(url).getHost().toLowerCase();
        } catch (Exception e) {
            return "unknown";
        }
    }

    /**
     * Return all available focus area names.
     * @deprecated Use getAvailableFocusAreas() instead
     */
    @Deprecated
    public List<String> getAllFocusAreas() {
        return getAvailableFocusAreas();
    }

    /**
     * Return all available focus area names.
     */
    public List<String> getAvailableFocusAreas() {
        return new ArrayList<>(focusKeywordsCache.keySet());
    }

    public Map<String, List<String>> getAllFocusKeywords() {
        return new HashMap<>(focusKeywordsCache);
    }
}
