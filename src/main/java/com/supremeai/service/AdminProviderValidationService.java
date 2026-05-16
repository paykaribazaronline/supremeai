package com.supremeai.service;

import com.supremeai.model.APIProvider;
import com.supremeai.repository.ProviderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.stream.Collectors;
import com.supremeai.model.*;
import com.supremeai.repository.*;
import com.supremeai.service.*;
import com.supremeai.fallback.AIFallbackOrchestrator;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Scheduled validation service for admin-managed APIProvider keys.
 * Runs daily at 3 AM (server time) and increments error streak for failed keys.
 * Quarantines providers with 3 consecutive failures by setting status="dead" and deadReason.
 *
 * (এডমিন ম্যানেজ করা APIProvider কীগুলোর স্বয়ংকřiব preferably np胆碱તলদা সমronĐ ই永মতর কার্ক।)
 */
@Service
public class AdminProviderValidationService {

    private static final Logger log = LoggerFactory.getLogger(AdminProviderValidationService.class);

    private static final int ERROR_THRESHOLD = 3; // Quarantine after 3 consecutive failures

    @Autowired
    private ProviderRepository providerRepository;

    @Autowired
    private AIProviderDiscoveryService discoveryService;

    @Autowired
    private APIHealthReportRepository healthReportRepository;

    /**
     * Daily cron (3 AM) to validate all active admin provider keys.
     * Uses reactive parallel processing (max 10 concurrent) to avoid timeouts.
     * Updates consecutiveErrorDays, lastValidated, and quarantines dead providers.
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void validateAllActiveProviders() {
        log.info("Running scheduled admin provider auto-validation...");

        try {
            List<APIProvider> allProviders = providerRepository.findAll().collectList().block();
            if (allProviders == null || allProviders.isEmpty()) {
                log.info("No API providers found to validate.");
                return;
            }

            // Filter to active providers only (skip inactive/error/dead)
            List<APIProvider> activeProviders = allProviders.stream()
                    .filter(p -> "active".equalsIgnoreCase(p.getStatus()))
                    .collect(java.util.stream.Collectors.toList());

            if (activeProviders.isEmpty()) {
                log.info("No active providers to validate.");
                return;
            }

            int concurrency = Math.min(10, activeProviders.size());

            Flux.fromIterable(activeProviders)
                    .flatMap(provider ->
                            Mono.fromCallable(() -> {
                                        // Call discovery service validation (same as manual test)
                                        boolean valid = discoveryService.validateKey(provider.getType(), provider.getApiKey())
                                                .onErrorReturn(false)
                                                .block();

                                        provider.setLastValidated(new Date());

                                        if (valid) {
                                            // Reset error streak on success
                                            provider.setConsecutiveErrorDays(0);
                                            provider.setLastErrorDate(null);
                                            provider.setStatus("active"); // ensure active
                                            return Map.of(
                                                    "provider", provider,
                                                    "valid", true,
                                                    "action", "none"
                                            );
                                        } else {
                                            // Increment error streak
                                            int currentStreak = provider.getConsecutiveErrorDays() != null ? provider.getConsecutiveErrorDays() : 0;
                                            int newStreak = currentStreak + 1;
                                            provider.setConsecutiveErrorDays(newStreak);
                                            provider.setLastErrorDate(new Date());

                                            String deadReason = null;
                                            if (newStreak >= ERROR_THRESHOLD) {
                                                // Quarantine as dead
                                                provider.setStatus("dead");
                                                provider.setDeadAt(new Date());
                                                deadReason = "Quarantined after " + newStreak + " consecutive validation failures";
                                                provider.setDeadReason(deadReason);
                                                log.error("Provider '{}' ({}) quarantined as DEAD. Reason: {}",
                                                        provider.getName(), provider.getId(), deadReason);
                                            } else {
                                                // Mark as error but not yet dead
                                                provider.setStatus("error");
                                                log.warn("Provider '{}' ({}) validation failed (streak: {}/{})",
                                                        provider.getName(), provider.getId(), newStreak, ERROR_THRESHOLD);
                                            }

                                            return Map.of(
                                                    "provider", provider,
                                                    "valid", false,
                                                    "action", newStreak >= ERROR_THRESHOLD ? "quarantined" : "marked_error"
                                            );
                                         }
                                     })
                                    .subscribeOn(Schedulers.boundedElastic())
                                    .doOnNext(res -> {
                                        APIProvider p = (APIProvider) res.get("provider");
                                        providerRepository.save(p).block();
                                        boolean valid = (Boolean) res.get("valid");
                                        String action = (String) res.get("action");
                                        log.info("Provider '{}' validation complete. valid={}, action={}",
                                                p.getName(), valid, action);
                                    })
                                    .onErrorResume(e -> {
                                        log.error("Error validating provider: {}", e.getMessage(), e);
                                        return Mono.empty();
                                    }),
                            concurrency)
                    .then()
                    .block();

            log.info("Admin provider validation complete. Processed {} active providers with concurrency {}.", activeProviders.size(), concurrency);

            // Generate daily report
            generateAndSaveReport(activeProviders);
        } catch (Exception e) {
            log.error("Failed to run admin provider validation: {}", e.getMessage(), e);
        }
    }

    private void generateAndSaveReport(List<APIProvider> providers) {
        long active = providers.stream().filter(p -> "active".equalsIgnoreCase(p.getStatus())).count();
        long dead = providers.stream().filter(p -> "dead".equalsIgnoreCase(p.getStatus())).count();
        
        List<Map<String, Object>> deadDetails = providers.stream()
                .filter(p -> "dead".equalsIgnoreCase(p.getStatus()))
                .map(p -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", p.getId() != null ? p.getId() : "unknown");
                    map.put("name", p.getName());
                    map.put("type", p.getType());
                    map.put("reason", p.getDeadReason() != null ? p.getDeadReason() : "Validation failed");
                    return map;
                })
                .collect(Collectors.toList());

        com.supremeai.model.APIHealthReport report = new com.supremeai.model.APIHealthReport(
                "REPORT_" + System.currentTimeMillis(),
                providers.size(),
                (int) active,
                (int) dead,
                0
        );
        report.setDeadKeyDetails(deadDetails);
        
        healthReportRepository.save(report).subscribe(
            saved -> log.info("Daily API health report saved: {}", saved.getId()),
            err -> log.error("Failed to save daily health report: {}", err.getMessage())
        );
    }

    /**
     * Stub method for ChatProcessingService command.
     * Triggers validation of all providers.
     */
    public void testAllProviders() {
        log.info("testAllProviders invoked (stub implementation)");
        // In a full implementation, this would iterate providers and test them
    }
}