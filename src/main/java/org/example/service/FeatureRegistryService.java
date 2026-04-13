package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Feature Registry Service — Single Source of Truth for all SupremeAI features.
 * 
 * Reads feature-registry.json and provides:
 * - Feature list (for all platforms to render dynamically)
 * - Sync status (which platform is missing which feature)
 * - Validation (build-time check that all platforms are in sync)
 */
@Service
public class FeatureRegistryService {

    private static final Logger logger = LoggerFactory.getLogger(FeatureRegistryService.class);
    private static final String REGISTRY_PATH = "feature-registry.json";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private List<Map<String, Object>> features = new ArrayList<>();
    private String lastLoadError = null;
    private long lastLoadTimestamp = 0;

    @PostConstruct
    public void init() {
        loadRegistry();
    }

    private void loadRegistry() {
        try {
            // Try classpath first (inside JAR)
            InputStream is = null;
            try {
                ClassPathResource resource = new ClassPathResource(REGISTRY_PATH);
                if (resource.exists()) {
                    is = resource.getInputStream();
                }
            } catch (Exception e) {
                // fallback below
            }

            // Fallback: try project root
            if (is == null) {
                java.io.File rootFile = new java.io.File(REGISTRY_PATH);
                if (rootFile.exists()) {
                    is = new java.io.FileInputStream(rootFile);
                }
            }

            if (is == null) {
                lastLoadError = "feature-registry.json not found";
                logger.warn("⚠️ Feature registry not found — sync checks disabled");
                return;
            }

            Map<String, Object> registry = objectMapper.readValue(is, new TypeReference<>() {});
            Object featuresObj = registry.get("features");
            if (featuresObj instanceof List<?>) {
                this.features = (List<Map<String, Object>>) featuresObj;
            }
            this.lastLoadTimestamp = System.currentTimeMillis();
            this.lastLoadError = null;
            logger.info("✅ Feature registry loaded: {} features", features.size());
        } catch (Exception e) {
            lastLoadError = e.getMessage();
            logger.error("❌ Failed to load feature registry: {}", e.getMessage());
        }
    }

    /** Reload the registry (admin action) */
    public void reload() {
        loadRegistry();
    }

    /** Get all features */
    public List<Map<String, Object>> getAllFeatures() {
        return Collections.unmodifiableList(features);
    }

    /** Get feature by ID */
    public Optional<Map<String, Object>> getFeature(String id) {
        return features.stream()
                .filter(f -> id.equals(f.get("id")))
                .findFirst();
    }

    /** Get features by priority */
    public List<Map<String, Object>> getFeaturesByPriority(String priority) {
        return features.stream()
                .filter(f -> priority.equals(f.get("priority")))
                .collect(Collectors.toList());
    }

    /**
     * SYNC REPORT — The key method.
     * Returns which features are missing from which platforms.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSyncReport() {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("totalFeatures", features.size());
        report.put("timestamp", System.currentTimeMillis());

        int adminHtmlCount = 0, reactCount = 0, flutterCount = 0;
        List<Map<String, Object>> missingInAdminHtml = new ArrayList<>();
        List<Map<String, Object>> missingInReact = new ArrayList<>();
        List<Map<String, Object>> missingInFlutter = new ArrayList<>();
        List<Map<String, Object>> fullySynced = new ArrayList<>();

        for (Map<String, Object> feature : features) {
            String id = (String) feature.get("id");
            String name = (String) feature.get("name");

            boolean hasAdminHtml = hasImplementation(feature, "adminHtml");
            boolean hasReact = hasImplementation(feature, "react");
            boolean hasFlutter = hasImplementation(feature, "flutter");

            if (hasAdminHtml) adminHtmlCount++;
            if (hasReact) reactCount++;
            if (hasFlutter) flutterCount++;

            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("id", id);
            entry.put("name", name);
            entry.put("priority", feature.get("priority"));

            if (hasAdminHtml && hasReact && hasFlutter) {
                fullySynced.add(entry);
            } else {
                if (!hasAdminHtml) missingInAdminHtml.add(entry);
                if (!hasReact) missingInReact.add(entry);
                if (!hasFlutter) missingInFlutter.add(entry);
            }
        }

        Map<String, Object> coverage = new LinkedHashMap<>();
        coverage.put("adminHtml", adminHtmlCount + "/" + features.size());
        coverage.put("react", reactCount + "/" + features.size());
        coverage.put("flutter", flutterCount + "/" + features.size());
        report.put("coverage", coverage);

        Map<String, Object> missing = new LinkedHashMap<>();
        missing.put("adminHtml", missingInAdminHtml);
        missing.put("react", missingInReact);
        missing.put("flutter", missingInFlutter);
        report.put("missing", missing);

        report.put("fullySynced", fullySynced);
        report.put("syncPercentage", features.isEmpty() ? 100 :
                Math.round(fullySynced.size() * 100.0 / features.size()));

        if (lastLoadError != null) {
            report.put("warning", lastLoadError);
        }

        return report;
    }

    @SuppressWarnings("unchecked")
    private boolean hasImplementation(Map<String, Object> feature, String platform) {
        Object platformObj = feature.get(platform);
        if (platformObj == null) return false;
        if (platformObj instanceof Map) {
            Map<String, Object> platformMap = (Map<String, Object>) platformObj;
            // Check if component/screen/sectionId is non-null
            for (Object val : platformMap.values()) {
                if (val != null && !val.toString().isEmpty()) return true;
            }
            return false;
        }
        return false;
    }

    /** Summary for quick dashboard display */
    public Map<String, Object> getQuickStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("totalFeatures", features.size());
        status.put("lastLoaded", lastLoadTimestamp);

        long adminCount = features.stream().filter(f -> hasImplementation(f, "adminHtml")).count();
        long reactCount = features.stream().filter(f -> hasImplementation(f, "react")).count();
        long flutterCount = features.stream().filter(f -> hasImplementation(f, "flutter")).count();
        long fullySynced = features.stream()
                .filter(f -> hasImplementation(f, "adminHtml")
                        && hasImplementation(f, "react")
                        && hasImplementation(f, "flutter"))
                .count();

        status.put("adminHtml", adminCount);
        status.put("react", reactCount);
        status.put("flutter", flutterCount);
        status.put("fullySynced", fullySynced);
        status.put("syncPercentage", features.isEmpty() ? 100 :
                Math.round(fullySynced * 100.0 / features.size()));

        if (lastLoadError != null) status.put("error", lastLoadError);
        return status;
    }
}
