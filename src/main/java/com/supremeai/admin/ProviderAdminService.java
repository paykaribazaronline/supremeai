package com.supremeai.admin;

import com.supremeai.model.APIProvider;
import com.supremeai.model.ActivityLog;
import com.supremeai.repository.ProviderRepository;
import com.supremeai.repository.ActivityLogRepository;
import com.supremeai.service.AIProviderDiscoveryService;
import com.supremeai.service.AdminProviderValidationService;
import com.supremeai.service.ProviderRoleSuggestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * Service for managing AI providers.
 * Extracted from ProvidersController to maintain 3-layer architecture.
 */
@Service
public class ProviderAdminService {
    private static final Logger log = LoggerFactory.getLogger(ProviderAdminService.class);

    private final ProviderRepository providerRepository;
    private final ActivityLogRepository activityLogRepository;
    private final AIProviderDiscoveryService discoveryService;
    private final ProviderRoleSuggestionService roleSuggestionService;
    private final AdminProviderValidationService adminProviderValidationService;

    @Autowired
    public ProviderAdminService(ProviderRepository providerRepository,
                                ActivityLogRepository activityLogRepository,
                                AIProviderDiscoveryService discoveryService,
                                ProviderRoleSuggestionService roleSuggestionService,
                                AdminProviderValidationService adminProviderValidationService) {
        this.providerRepository = providerRepository;
        this.activityLogRepository = activityLogRepository;
        this.discoveryService = discoveryService;
        this.roleSuggestionService = roleSuggestionService;
        this.adminProviderValidationService = adminProviderValidationService;
    }

    public Flux<APIProvider> getAllProviders() {
        return providerRepository.findAll();
    }

    public Mono<APIProvider> addProvider(APIProvider provider, String adminUserId) {
        return validateKey(provider.getType(), provider.getApiKey())
                .flatMap(valid -> {
                    if (!valid) {
                        return Mono.error(new IllegalArgumentException("Invalid API key or provider unreachable"));
                    }
                    // Set to ACTIVE if validation passes (for real API keys)
                    provider.setStatus("active");
                    provider.setConsecutiveErrorDays(0);
                    provider.setLastValidated(new Date());
                    log.info("[PROVIDER] Added provider {} with status ACTIVE (validation passed)", provider.getName());
                    return saveProviderWithLog(provider, adminUserId, "ADD_PROVIDER", "Added provider: " + provider.getName() + " [Status: ACTIVE]");
                });
    }

    public Mono<APIProvider> updateProvider(String id, APIProvider provider, String adminUserId) {
        return providerRepository.findById(id)
                .flatMap(existing -> {
                    provider.setId(id);
                    boolean keyChanged = provider.getApiKey() != null &&
                            !provider.getApiKey().equals(existing.getApiKey());
                    
                    if (keyChanged) {
                        return validateKey(provider.getType(), provider.getApiKey())
                                .flatMap(valid -> {
                                    if (!valid) {
                                        return Mono.error(new IllegalArgumentException("Invalid API key or provider unreachable"));
                                    }
                                    provider.setConsecutiveErrorDays(0);
                                    provider.setLastValidated(new Date());
                                    if ("dead".equals(existing.getStatus()) || "error".equals(existing.getStatus()) || "rotating".equals(existing.getStatus())) {
                                        provider.setStatus("active");
                                        provider.setDeadReason(null);
                                        provider.setDeadAt(null);
                                        provider.setLastErrorDate(null);
                                    }
                                    return saveProviderWithLog(provider, adminUserId, "UPDATE_PROVIDER", "Updated provider with new key: " + provider.getName());
                                });
                    } else {
                        if ("inactive".equals(existing.getStatus()) && "active".equals(provider.getStatus())) {
                            provider.setConsecutiveErrorDays(0);
                            provider.setLastValidated(new Date());
                        }
                        return saveProviderWithLog(provider, adminUserId, "UPDATE_PROVIDER", "Updated provider: " + provider.getName());
                    }
                });
    }

    public Mono<APIProvider> reviveProvider(String id, String adminUserId) {
        return providerRepository.findById(id)
                .flatMap(provider -> {
                    if (!"dead".equals(provider.getStatus()) && !"error".equals(provider.getStatus()) && !"rotating".equals(provider.getStatus())) {
                        return Mono.error(new IllegalStateException("Provider is not dead, rotating, or in error state"));
                    }
                    provider.setStatus("active");
                    provider.setConsecutiveErrorDays(0);
                    provider.setDeadReason(null);
                    provider.setDeadAt(null);
                    provider.setLastErrorDate(null);
                    provider.setLastValidated(new Date());
                    return saveProviderWithLog(provider, adminUserId, "REVIVE_PROVIDER", "Revived provider: " + provider.getName());
                });
    }

    public Mono<Void> deleteProvider(String id, String adminUserId) {
        return providerRepository.findById(id)
                .flatMap(provider -> {
                    String providerName = provider.getName();
                    return providerRepository.deleteById(id)
                            .then(logAction(adminUserId, "DELETE_PROVIDER", "PROVIDER_MANAGEMENT", "WARN", "Deleted provider: " + id + " (" + providerName + ")"))
                            .then();
                });
    }

    public Mono<Boolean> validateKey(String type, String apiKey) {
        if (apiKey == null || type == null) {
            log.warn("[VALIDATION] Rejected: apiKey is null={} or type is null={}", apiKey == null, type == null);
            return Mono.just(false);
        }
        // For all providers (including GCloud/Gemini), use full validation with timeout

        log.info("[VALIDATION] Testing key for type={}, apiKeyLength={}", type, apiKey.length());
        return discoveryService.validateKey(type, apiKey)
                .timeout(java.time.Duration.ofSeconds(10))
                .doOnError(e -> log.error("[VALIDATION] Key validation FAILED for type={}, keyLength={}: {}", type, apiKey.length(), e.toString()))
                .onErrorReturn(false);
    }

    /**
     * Manually activate a provider by ID (for admins to activate real providers)
     */
    public Mono<APIProvider> activateProvider(String id, String adminUserId) {
        return providerRepository.findById(id)
                .flatMap(provider -> {
                    if ("active".equals(provider.getStatus())) {
                        log.info("[PROVIDER] Provider {} already active", provider.getName());
                        return Mono.just(provider);
                    }
                    provider.setStatus("active");
                    provider.setConsecutiveErrorDays(0);
                    provider.setLastValidated(new Date());
                    provider.setLastErrorDate(null);
                    provider.setLastErrorMessage(null);
                    log.info("[PROVIDER] Activated provider {} (admin triggered)", provider.getName());
                    return saveProviderWithLog(provider, adminUserId, "ACTIVATE_PROVIDER", 
                        "Manually activated provider: " + provider.getName());
                })
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Provider not found: " + id)));
    }

    /**
     * Deactivate a provider by ID (for admins to mark providers as inactive)
     */
    public Mono<APIProvider> deactivateProvider(String id, String reason, String adminUserId) {
        return providerRepository.findById(id)
                .flatMap(provider -> {
                    provider.setStatus("inactive");
                    provider.setLastErrorMessage(reason);
                    log.info("[PROVIDER] Deactivated provider {} (reason: {})", provider.getName(), reason);
                    return saveProviderWithLog(provider, adminUserId, "DEACTIVATE_PROVIDER", 
                        "Deactivated provider: " + provider.getName() + " (" + reason + ")");
                })
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Provider not found: " + id)));
    }

    public List<String> suggestRoles(APIProvider provider) {
        return roleSuggestionService.suggestRoles(provider);
    }

    public Mono<APIProvider> patchCapability(String id, Map<String, Object> updates, String adminUserId) {
        return providerRepository.findById(id)
                .flatMap(provider -> {
                    if (updates.containsKey("canCommunicate")) {
                        provider.setCanCommunicate((Boolean) updates.get("canCommunicate"));
                    }
                    if (updates.containsKey("canExecuteTasks")) {
                        provider.setCanExecuteTasks((Boolean) updates.get("canExecuteTasks"));
                    }
                    if (updates.containsKey("canParticipateInVoting")) {
                        provider.setCanParticipateInVoting((Boolean) updates.get("canParticipateInVoting"));
                    }
                    if (updates.containsKey("assignedRoles")) {
                        @SuppressWarnings("unchecked")
                        List<String> roles = (List<String>) updates.get("assignedRoles");
                        provider.setAssignedRoles(roles);
                    }
                    return saveProviderWithLog(provider, adminUserId, "UPDATE_CAPABILITY", "Updated capabilities for: " + provider.getName());
                });
    }

    public Mono<Map<String, Object>> getHealthStats() {
        return providerRepository.findAll().collectList()
                .map(list -> {
                    long active = list.stream().filter(p -> "active".equals(p.getStatus())).count();
                    long inactive = list.stream().filter(p -> "inactive".equals(p.getStatus())).count();
                    long error = list.stream().filter(p -> "error".equals(p.getStatus())).count();
                    long rotating = list.stream().filter(p -> "rotating".equals(p.getStatus())).count();
                    long dead = list.stream().filter(p -> "dead".equals(p.getStatus())).count();
                    
                    return Map.of(
                        "total", list.size(),
                        "active", active,
                        "inactive", inactive,
                        "error", error,
                        "rotating", rotating,
                        "dead", dead,
                        "healthScore", list.size() > 0 ? (active * 100 / list.size()) : 100
                    );
                });
    }

    public Mono<Void> removeDeadProviders(String adminUserId) {
        return providerRepository.findAll()
                .filter(p -> "dead".equals(p.getStatus()) || p.getName() == null)
                .flatMap(p -> {
                    String name = p.getName() != null ? p.getName() : "Unknown";
                    String id = p.getId();
                    return providerRepository.deleteById(id)
                            .then(logAction(adminUserId, "BULK_DELETE_DEAD", "PROVIDER_MANAGEMENT", "WARN", "Deleted invalid/dead provider: " + id + " (" + name + ")"));
                })
                .then();
    }

    public Mono<Void> sanitizeProviders(String adminUserId) {
        return providerRepository.findAll()
                .flatMap(p -> {
                    boolean changed = false;
                    String oldId = p.getId();
                    String oldName = p.getName();
                    
                    // Fix null names or blank names
                    if ((oldName == null || oldName.trim().isEmpty())) {
                        if (oldId != null && !oldId.trim().isEmpty()) {
                            // Generate name from ID: "openai-prod" -> "Openai-prod"
                            String newName = oldId.substring(0, 1).toUpperCase() + oldId.substring(1);
                            p.setName(newName);
                            changed = true;
                            log.info("Sanitizer: Fixed null name for ID: {} -> {}", oldId, newName);
                        } else {
                            // Both ID and name are null? This record is beyond saving
                            log.warn("Sanitizer: Found corrupted provider with both null ID and name. Deleting.");
                            return providerRepository.delete(p).then(Mono.empty());
                        }
                    }
                    
                    // Fix missing or invalid types
                    if (p.getType() == null || p.getType().trim().isEmpty()) {
                        p.setType(determineTypeFromName(p.getName()));
                        changed = true;
                        log.info("Sanitizer: Fixed null type for {}: {}", p.getName(), p.getType());
                    }

                    // Reset status to active if in error, rotating, null, or inactive to allow immediate availability
                    if (p.getStatus() == null || "inactive".equals(p.getStatus()) || "error".equals(p.getStatus()) || "rotating".equals(p.getStatus())) {
                        p.setStatus("active");
                        changed = true;
                    }

                    // Auto-detect deploymentSource based on type and accountEmail
                    if (p.getDeploymentSource() == null || p.getDeploymentSource().isBlank()
                            || !isValidDeploymentSource(p.getDeploymentSource())) {
                        String detectedSource = detectDeploymentSource(p.getType(), p.getAccountEmail(), p.getBaseUrl());
                        if (!detectedSource.equalsIgnoreCase(p.getDeploymentSource())) {
                            p.setDeploymentSource(detectedSource);
                            changed = true;
                            log.info("Sanitizer: Auto-set deploymentSource for {} from type={}, accountEmail={} -> {}",
                                    p.getName(), p.getType(), p.getAccountEmail(), detectedSource);
                        }
                    }

                    // Fix missing models list (crucial for dashboard grouping)
                    if (p.getModels() == null || p.getModels().isEmpty()) {
                        String defaultModel = determineDefaultModel(p.getType());
                        p.setModels(java.util.List.of(defaultModel));
                        changed = true;
                        log.info("Sanitizer: Added default model for {}: {}", p.getName(), defaultModel);
                    }

                    // Fix apiKey if it looks like a stringified map or object (safety check)
                    if (p.getApiKey() != null && (p.getApiKey().contains("{") || p.getApiKey().contains("="))) {
                        log.warn("Sanitizer: Detected potentially corrupted API key format for {}. Attempting to extract real key...", p.getName());
                        String rawKey = p.getApiKey();
                        String extractedKey = null;
                        if (rawKey.contains("key=")) {
                            int idx = rawKey.indexOf("key=");
                            int endIdx = rawKey.indexOf(",", idx);
                            if (endIdx == -1) {
                                endIdx = rawKey.indexOf("}", idx);
                            }
                            if (endIdx != -1) {
                                extractedKey = rawKey.substring(idx + 4, endIdx).trim();
                            }
                        }
                        if (extractedKey != null && !extractedKey.isEmpty()) {
                            p.setApiKey(extractedKey);
                            p.setStatus("active");
                            log.info("Sanitizer: Successfully extracted and updated API key to: {} for {}", extractedKey, p.getName());
                        } else {
                            log.warn("Sanitizer: Could not extract key, setting placeholder for safety.");
                            p.setApiKey("PROVIDER_API_KEY_PLACEHOLDER");
                            p.setStatus("active");
                        }
                        changed = true;
                    }

                    if (changed) {
                        return providerRepository.save(p)
                            .then(logAction(adminUserId, "SANITIZE_PROVIDER", "PROVIDER_MANAGEMENT", "INFO", 
                                "Sanitized provider: " + (oldId != null ? oldId : "unknown") + " (" + p.getName() + ")"));
                    }
                    return Mono.empty();
                })
                .then();
    }

    private String determineTypeFromName(String name) {
        String n = name.toUpperCase();
        if (n.contains("GEMINI")) return "GOOGLE";
        if (n.contains("OPENAI") || n.contains("GPT")) return "OPENAI";
        if (n.contains("ANTHROPIC") || n.contains("CLAUDE")) return "ANTHROPIC";
        if (n.contains("GROQ")) return "GROQ";
        if (n.contains("DEEPSEEK")) return "DEEPSEEK";
        if (n.contains("MISTRAL")) return "MISTRAL";
        if (n.contains("OLLAMA")) return "LOCAL";
        return "GENERIC";
    }

    private String determineDefaultModel(String type) {
        if (type == null) return "unknown-model";
        return switch (type.toUpperCase()) {
            case "GOOGLE" -> "gemini-1.5-flash";
            case "OPENAI" -> "gpt-4o-mini";
            case "ANTHROPIC" -> "claude-3-haiku";
            case "GROQ" -> "llama3-8b-8192";
            case "DEEPSEEK" -> "deepseek-chat";
            case "MISTRAL" -> "mistral-tiny";
            case "LOCAL" -> "phi3";
            default -> "generic-model";
        };
    }

    /**
     * Detect the correct deploymentSource for a provider based on type, account email, and base URL.
     * Valid sources: api, gcloud, local, ollama
     */
    private String detectDeploymentSource(String type, String accountEmail, String baseUrl) {
        // Check type first
        if (type != null) {
            String upperType = type.toUpperCase();
            if (upperType.equals("GOOGLE") || upperType.equals("GEMINI") || upperType.equals("VERTEX") || upperType.equals("CLOUD_RUN")) {
                return "gcloud";
            }
            if (upperType.equals("LOCAL") || upperType.equals("OLLAMA")) {
                return "ollama";
            }
        }

        // Check base URL for local/ollama indicators
        if (baseUrl != null && !baseUrl.isBlank()) {
            String lowerUrl = baseUrl.toLowerCase();
            if (lowerUrl.contains("localhost") || lowerUrl.contains("127.0.0.1") || lowerUrl.contains("ollama")) {
                return "ollama";
            }
        }

        // Check account email for GCP/Firebase/Gmail accounts
        if (accountEmail != null && !accountEmail.isBlank()) {
            String lowerEmail = accountEmail.toLowerCase();
            if (lowerEmail.contains("@google.com") || lowerEmail.contains("@gmail.com")
                    || lowerEmail.contains("googleapis.com") || lowerEmail.contains("firebase")) {
                return "gcloud";
            }
        }

        // Default: regular API key providers
        return "api";
    }

    /**
     * Validate that a deploymentSource string is one of the recognized values.
     */
    private boolean isValidDeploymentSource(String source) {
        return source != null && (
                "api".equalsIgnoreCase(source) ||
                "gcloud".equalsIgnoreCase(source) ||
                "local".equalsIgnoreCase(source) ||
                "ollama".equalsIgnoreCase(source)
        );
    }

    public void triggerValidation() {
        Mono.fromRunnable(adminProviderValidationService::validateAllActiveProviders)
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .subscribe();
    }

    private Mono<APIProvider> saveProviderWithLog(APIProvider provider, String userId, String action, String details) {
        Mono<APIProvider> savedMono = providerRepository.save(provider);
        if (savedMono == null) {
            return Mono.just(provider);
        }
        return savedMono
                .flatMap(saved -> logAction(userId, action, "PROVIDER_MANAGEMENT", "INFO", details).thenReturn(saved));
    }

    private Mono<ActivityLog> logAction(String userId, String action, String category, String severity, String details) {
        ActivityLog logRecord = new ActivityLog();
        logRecord.setUser(userId);
        logRecord.setAction(action);
        logRecord.setCategory(category);
        logRecord.setSeverity(severity);
        logRecord.setOutcome("SUCCESS");
        logRecord.setDetails(details);
        logRecord.setTimestamp(java.time.LocalDateTime.now());
        return activityLogRepository.save(logRecord);
    }
}
