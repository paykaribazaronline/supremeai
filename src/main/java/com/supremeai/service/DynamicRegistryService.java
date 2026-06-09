package com.supremeai.service;

import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Comparator;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.ArrayList;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * এই সার্ভিসটি সিস্টেমের বিভিন্ন হার্ডকোডেড লজিক সরিয়ে সেগুলোকে ডাইনামিক করে তোলে।
 * এটি Firestore থেকে রিয়েল-টাইম কনফিগারেশন এবং প্যাটার্ন লোড করে।
 */
@Service
public class DynamicRegistryService {

    private static final Logger log = LoggerFactory.getLogger(DynamicRegistryService.class);

    private final DynamicSignatureRegistry signatureRegistry;
    private final Firestore firestore;

    // বাংলা মন্তব্য: দীর্ঘমেয়াদী রক্ষণাবেক্ষণের জন্য এখানে ফিল্ড ইনজেকশনের বদলে কনস্ট্রাক্টর ইনজেকশন ব্যবহার করা হয়েছে।
    public DynamicRegistryService(
            DynamicSignatureRegistry signatureRegistry,
            @Autowired(required = false) Firestore firestore) {
        this.signatureRegistry = signatureRegistry;
        this.firestore = firestore;
    }

    // সার্চ রেজিস্ট্রি ক্যাশে
    private final Map<String, Map<String, Object>> searchRegistriesCache = new ConcurrentHashMap<>();
    // সিস্টেম মেসেজ ক্যাশে (error messages, templates)
    private final Map<String, String> messageTemplatesCache = new ConcurrentHashMap<>();
    // প্রম্পট ক্যাশে (AI system prompts with placeholders)
    private final Map<String, String> promptsCache = new ConcurrentHashMap<>();
    // CSRF ইগনোরড পাথ ক্যাশে
    private volatile List<String> csrfIgnoredPathsCache = new ArrayList<>(); 

    @PostConstruct
    public void init() {
        log.info("[DynamicRegistryService] Initializing dynamic registries...");
        if (firestore == null) {
            // বাংলা মন্তব্য: যদি ফায়ারস্টোর না থাকে, তবে সিস্টেমকে সচল রাখতে ন্যূনতম ডিফল্ট পাথ সেট করা হচ্ছে।
            this.csrfIgnoredPathsCache = List.of("/api/auth/**", "/api/chat/**");
            log.warn("[DynamicRegistryService] Firestore bean not available. Running with default/fallback configurations.");
            return;
        }
        loadSearchRegistries();
        loadCsrfIgnoredPaths();
        loadMessageTemplates(); // বাংলা মন্তব্য: গ্রিটিং এবং এরর মেসেজগুলো ইনিশিয়ালাইজ করা হচ্ছে।
        loadPrompts(); // বাংলা মন্তব্য: এআই প্রম্পটগুলো ডাটাবেস থেকে লোড করা হচ্ছে।
    }

    /**
     * বাংলা মন্তব্য: প্রতি ৩০ মিনিট অন্তর ডাটাবেস থেকে তথ্য রিফ্রেশ করা হয়।
     * এতে সিস্টেম রিস্টার্ট ছাড়াই কনফিগারেশন আপডেট করা সম্ভব।
     */
    @Scheduled(fixedRateString = "${supremeai.registry.refresh-rate:1800000}")
    public void refreshRegistries() {
        if (firestore != null) {
            log.info("[DynamicRegistryService] Periodic refresh: Syncing with Firestore...");
            loadSearchRegistries();
            loadCsrfIgnoredPaths();
            loadMessageTemplates();
            loadPrompts();
        }
    }

    private void loadSearchRegistries() {
        try {
            firestore.collection("search_registries").get().get().getDocuments().forEach(doc -> {
                Map<String, Object> data = doc.getData();
                if (data.containsKey("name") && data.containsKey("url_template")) {
                    searchRegistriesCache.put(doc.getId(), data);
                }
            });
            log.info("[DynamicRegistryService] Loaded {} search registries from Firestore.", searchRegistriesCache.size());
        } catch (InterruptedException | ExecutionException e) {
            log.error("[DynamicRegistryService] Error loading search registries: {}", e.getMessage());
        }
    }

    private void loadCsrfIgnoredPaths() {
        try {
            DocumentSnapshot securityConfig = firestore.collection("system_configs").document("security").get().get();
            if (securityConfig.exists() && securityConfig.contains("csrf_ignored_paths")) {
                Map<String, Object> csrfConfig = (Map<String, Object>) securityConfig.get("csrf_ignored_paths");
                if (csrfConfig != null && csrfConfig.containsKey("paths")) {
                    this.csrfIgnoredPathsCache = (List<String>) csrfConfig.get("paths");
                    log.info("[DynamicRegistryService] Loaded {} CSRF ignored paths from Firestore.", csrfIgnoredPathsCache.size());
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            log.error("[DynamicRegistryService] Error loading CSRF ignored paths: {}", e.getMessage());
        }
    }

    /**
     * বাংলা মন্তব্য: ডাটাবেস থেকে ডাইনামিক মেসেজ টেম্পলেট বা এরর মেসেজ লোড করে।
     * এটি হার্ডকোডেড স্ট্রিং রিমুভ করতে সাহায্য করে।
     */
    private void loadMessageTemplates() {
        try {
            DocumentSnapshot doc = firestore.collection("system_configs").document("messages").get().get();
            if (doc.exists() && doc.getData() != null) {
                doc.getData().forEach((k, v) -> {
                    messageTemplatesCache.put(k, String.valueOf(v));
                });
            } else {
                log.warn("[DynamicRegistryService] Message templates document not found or empty in Firestore.");
            }
            log.info("[DynamicRegistryService] Loaded {} message templates.", messageTemplatesCache.size());
        } catch (Exception e) {
            log.error("[DynamicRegistryService] Error loading messages: {}", e.getMessage());
            // ফলব্যাক মেসেজ সেট করা হচ্ছে
            messageTemplatesCache.putIfAbsent("error.no_info", "Sorry, no information found.");
            messageTemplatesCache.putIfAbsent("error.ai_down", "AI processing issue occurred.");
            messageTemplatesCache.putIfAbsent("error.search_failed", "Search operation failed.");
        }
    }

    /**
     * বাংলা মন্তব্য: ডাটাবেস থেকে সিস্টেম প্রম্পটগুলো লোড করে।
     */
    private void loadPrompts() {
        try {
            DocumentSnapshot doc = firestore.collection("system_configs").document("prompts").get().get();
            if (doc.exists() && doc.getData() != null) {
                doc.getData().forEach((k, v) -> {
                    promptsCache.put(k, String.valueOf(v));
                });
            }
            log.info("[DynamicRegistryService] Loaded {} system prompts from Firestore.", promptsCache.size());
        } catch (Exception e) {
            log.error("[DynamicRegistryService] Error loading prompts: {}", e.getMessage());
        }
    }

    /**
     * ইউজার ইনপুট অনুযায়ী সঠিক সার্চ ইউআরএল নির্ধারণ করে।
     * হার্ডকোডেড ম্যাপিংয়ের পরিবর্তে এটি ডেটাবেস থেকে সঠিক URL টেম্পলেট খুঁজে নেয়।
     */
    public String determineSearchUrl(String query) {
        // বাংলা মন্তব্য: কোয়েরির উপর ভিত্তি করে সবচেয়ে উপযুক্ত সার্চ URL নির্ধারণ করা হচ্ছে।
        // এটি Firestore থেকে লোড করা ডাইনামিক সার্চ রেজিস্ট্রি এবং তাদের অগ্রাধিকার ব্যবহার করে।
        String lowerQuery = query.toLowerCase();
        
        // রেজিস্ট্রি এন্ট্রিগুলোকে অগ্রাধিকার (priority) অনুযায়ী সাজানো হচ্ছে।
        // কম সংখ্যা মানে উচ্চ অগ্রাধিকার।
        List<Map.Entry<String, Map<String, Object>>> sortedRegistries = new ArrayList<>(searchRegistriesCache.entrySet());
        sortedRegistries.sort(Comparator.comparingInt(e -> (Integer) e.getValue().getOrDefault("priority", 999)));

        for (Map.Entry<String, Map<String, Object>> entry : sortedRegistries) {
            Map<String, Object> registryConfig = entry.getValue();
            String name = (String) registryConfig.get("name");
            String urlTemplate = (String) registryConfig.get("url_template");
            List<String> keywords = (List<String>) registryConfig.get("keywords");

            if (urlTemplate != null) {
                // যদি কোয়েরিতে রেজিস্ট্রিটির নাম থাকে
                if (name != null && lowerQuery.contains(name.toLowerCase())) {
                    log.debug("[DynamicRegistryService] Matched search registry by name: {}", name);
                    return urlTemplate;
                }
                // যদি কোয়েরিতে রেজিস্ট্রিটির সাথে সম্পর্কিত কোনো কি-ওয়ার্ড থাকে
                if (keywords != null) {
                    for (String keyword : keywords) {
                        if (lowerQuery.contains(keyword.toLowerCase())) {
                            log.debug("[DynamicRegistryService] Matched search registry by keyword: {}", keyword);
                            return urlTemplate;
                        }
                    }
                }
            }
        }
        
        // বাংলা মন্তব্য: যদি কোনো নির্দিষ্ট ম্যাচ না পাওয়া যায়, তাহলে Google-কে ডিফল্ট হিসেবে ব্যবহার করুন।
        // ভবিষ্যতে, এটি একটি AI-ভিত্তিক মডেল দ্বারা প্রতিস্থাপিত হতে পারে যা কোয়েরির ইনটেন্ট বুঝে সঠিক সার্চ ইঞ্জিন নির্বাচন করবে।
        return "https://www.google.com/search?q=" + query;
    }

    /**
     * বাংলা মন্তব্য: হার্ডকোড করা এরর মেসেজের পরিবর্তে ডাটাবেস থেকে মেসেজ নিয়ে আসে।
     */
    public String getMessage(String key, String defaultMsg) {
        return messageTemplatesCache.getOrDefault(key, defaultMsg);
    }

    /**
     * বাংলা মন্তব্য: হার্ডকোড করা প্রম্পটের পরিবর্তে ডাটাবেস থেকে প্রম্পট এবং প্লেসহোল্ডার ব্যবহার করে নিয়ে আসে।
     */
    public String getPrompt(String key, String defaultPrompt, Map<String, String> placeholders) {
        String prompt = promptsCache.getOrDefault(key, defaultPrompt);
        if (placeholders != null && !placeholders.isEmpty()) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                String value = entry.getValue() != null ? entry.getValue() : "";
                prompt = prompt.replace("{" + entry.getKey() + "}", value);
            }
        }
        return prompt;
    }

    /**
     * সিকিউরিটি কনফিগারেশনের জন্য বাদ দেওয়া পাথগুলো লোড করে।
     * এর ফলে SecurityConfig ফাইলে আর বারবার হার্ডকোড করতে হয় না।
     */
    public List<String> getCsrfIgnoredPaths() {
        // বাংলা মন্তব্য: এটি সিস্টেমকে আরও নমনীয় করে তোলে
        return csrfIgnoredPathsCache;
    }

    /**
     * ডাইনামিক প্যাটার্ন ব্যবহার করে ইনটেন্ট বা এরর শনাক্ত করে।
     */
    public boolean matchesDynamicPattern(String input, String category) {
        Map<String, String> patterns = signatureRegistry.getPatternsByCategory(category);
        for (String patternStr : patterns.values()) {
            if (Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE).matcher(input).find()) {
                return true;
            }
        }
        return false;
    }
    
    // এই সার্ভিসটি ব্যবহার করে সিস্টেম এখন আগের চেয়ে অনেক বেশি গতিশীল এবং সহজ
}