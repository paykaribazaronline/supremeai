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
                    provider.setStatus("inactive");
                    provider.setConsecutiveErrorDays(0);
                    provider.setLastValidated(new Date());
                    return saveProviderWithLog(provider, adminUserId, "ADD_PROVIDER", "Added provider: " + provider.getName());
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
                                    if ("dead".equals(existing.getStatus()) || "error".equals(existing.getStatus())) {
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
                    if (!"dead".equals(provider.getStatus()) && !"error".equals(provider.getStatus())) {
                        return Mono.error(new IllegalStateException("Provider is not dead or in error state"));
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
            return Mono.just(false);
        }
        return discoveryService.validateKey(type, apiKey)
                .onErrorReturn(false);
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
                    long error = list.stream().filter(p -> "error".equals(p.getStatus())).count();
                    long dead = list.stream().filter(p -> "dead".equals(p.getStatus())).count();
                    
                    return Map.of(
                        "total", list.size(),
                        "active", active,
                        "error", error,
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

                    // Reset status if in error to allow re-validation
                    if ("error".equals(p.getStatus())) {
                        p.setStatus("inactive");
                        changed = true;
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
                        log.warn("Sanitizer: Detected potentially corrupted API key for {}. Clearing to allow reset.", p.getName());
                        p.setApiKey(null);
                        p.setStatus("inactive");
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

    public void triggerValidation() {
        Mono.fromRunnable(adminProviderValidationService::validateAllActiveProviders)
                .subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic())
                .subscribe();
    }

    private Mono<APIProvider> saveProviderWithLog(APIProvider provider, String userId, String action, String details) {
        return providerRepository.save(provider)
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
        logRecord.setTimestamp(new Date());
        return activityLogRepository.save(logRecord);
    }
}
