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

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import com.supremeai.repository.APIHealthReportRepository;
import com.supremeai.fallback.ThirdOpinionOrchestrator;
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

    private static final int ERROR_THRESHOLD = 3; // Quarantine after 3 consecutive failures (prevents false positives)

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

        // Reactive pipeline: fetch all providers, filter active, validate concurrently, and generate report.
        providerRepository.findAll()
                .filter(p -> "active".equalsIgnoreCase(p.getStatus()) || "rotating".equalsIgnoreCase(p.getStatus()))
                .collectList()
                .flatMapMany(activeProviders -> {
                    if (activeProviders.isEmpty()) {
                        log.info("No active providers to validate.");
                        return Flux.empty();
                    }
                    int concurrency = Math.min(10, activeProviders.size());
                    return Flux.fromIterable(activeProviders)
                            .flatMap(provider ->
                                    discoveryService.validateKey(provider.getType(), provider.getApiKey())
                                            .onErrorReturn(false)
                                            .map(valid -> {
                                                provider.setLastValidated(java.time.LocalDateTime.now());
                                                if (valid) {
                                                    provider.setConsecutiveErrorDays(0);
                                                    provider.setLastErrorDate(null);
                                                    provider.setStatus("active");
                                                    provider.setDeadReason(null);
                                                    provider.setDeadAt(null);
                                                } else {
                                                    int streak = Optional.ofNullable(provider.getConsecutiveErrorDays()).orElse(0) + 1;
                                                    provider.setConsecutiveErrorDays(streak);
                                                    provider.setLastErrorDate(java.time.LocalDateTime.now());
                                                    if (streak >= ERROR_THRESHOLD) {
                                                        provider.setStatus("dead");
                                                        provider.setDeadAt(java.time.LocalDateTime.now());
                                                        String deadReason = "Quarantined after " + streak + " consecutive validation failures";
                                                        provider.setDeadReason(deadReason);
                                                        log.error("Provider '{}' ({}) quarantined as DEAD. Reason: {}",
                                                                provider.getName(), provider.getId(), deadReason);
                                                    } else {
                                                        log.warn("Provider '{}' ({}) validation failed (streak: {}/{}) — status preserved as '{}'",
                                                                provider.getName(), provider.getId(), streak, ERROR_THRESHOLD, provider.getStatus());
                                                    }
                                                }
                                                return provider;
                                            })
                                            .flatMap(p -> providerRepository.save(p))
                                            .doOnNext(p -> log.info("Provider '{}' validation complete. status={}", p.getName(), p.getStatus()))
                                            .onErrorResume(e -> {
                                                log.error("Error validating provider: {}", e.getMessage(), e);
                                                return Mono.empty();
                                            })
                                            , concurrency);
                })
                .then(Mono.defer(() -> {
                    return providerRepository.findAll()
                        .filter(p -> "active".equalsIgnoreCase(p.getStatus()) || "rotating".equalsIgnoreCase(p.getStatus()))
                        .collectList()
                        .timeout(java.time.Duration.ofSeconds(10))
                        .flatMap(providers -> {
                            List<APIProvider> allProviders = providers != null ? providers : List.of();
                            return Mono.fromRunnable(() -> {
                                try {
                                    generateAndSaveReport(allProviders);
                                } catch (Exception e) {
                                    log.error("Daily API health report generation failed", e);
                                }
                            });
                        })
                        .doOnError(e -> log.error("Daily API health report generation failed", e));
                }))
                .subscribe(
                    null,
                    err -> log.error("validateAllActiveProviders: pipeline failed", err)
                );
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
     * On-demand provider validation — usable from ChatProcessingService RUN_AUDIT command.
     * Scans all active providers (max 10 concurrent), validates each against the discovery
     * service, and stores updated error-streak metadata.  Returns a count summary map.
     */
    public Mono<Map<String, Object>> testAllProviders() {
        log.info("testAllProviders: on-demand validation started");
        return providerRepository.findAll()
                .collectList()
                .flatMap(allProviders -> {
                    if (allProviders == null || allProviders.isEmpty()) {
                        log.info("testAllProviders: no providers found");
                        return Mono.just(Map.of("status", "ok", "total", 0, "valid", 0, "failed", 0));
                    }
                    List<APIProvider> activeProviders = allProviders.stream()
                            .filter(p -> "active".equalsIgnoreCase(p.getStatus()) || "rotating".equalsIgnoreCase(p.getStatus()))
                            .collect(java.util.stream.Collectors.toList());
                    if (activeProviders.isEmpty()) {
                        return Mono.just(Map.of("status", "ok", "total", 0, "valid", 0, "failed", 0,
                                "note", "no active providers to test"));
                    }
                    int concurrency = Math.min(10, activeProviders.size());
                    AtomicInteger validCount = new AtomicInteger(0);
                    AtomicInteger failedCount = new AtomicInteger(0);
                    return Flux.fromIterable(activeProviders)
                            .flatMap(provider ->
                                    discoveryService.validateKey(provider.getType(), provider.getApiKey())
                                            .onErrorReturn(false)
                                            .flatMap(valid -> {
                                                provider.setLastValidated(java.time.LocalDateTime.now());
                                                if (valid) {
                                                    provider.setConsecutiveErrorDays(0);
                                                    provider.setLastErrorDate(null);
                                                    provider.setStatus("active");
                                                    provider.setDeadReason(null);
                                                    provider.setDeadAt(null);
                                                    validCount.incrementAndGet();
                                                } else {
                                                    int streak = Optional.ofNullable(provider.getConsecutiveErrorDays()).orElse(0) + 1;
                                                    provider.setConsecutiveErrorDays(streak);
                                                    provider.setLastErrorDate(java.time.LocalDateTime.now());
                                                    if (streak >= ERROR_THRESHOLD) {
                                                        provider.setStatus("dead");
                                                        provider.setDeadAt(java.time.LocalDateTime.now());
                                                        provider.setDeadReason("Quarantined after " + streak + " consecutive validation failures");
                                                        log.error("Provider '{}' ({}) quarantined. Reason: {}",
                                                                provider.getName(), provider.getId(), provider.getDeadReason());
                                                    } else {
                                                        log.warn("Provider '{}' ({}) validation failed (streak: {}/{})",
                                                                provider.getName(), provider.getId(), streak, ERROR_THRESHOLD);
                                                    }
                                                    failedCount.incrementAndGet();
                                                }
                                                return providerRepository.save(provider);
                                            })
                                            .onErrorResume(e -> {
                                                log.error("testAllProviders: validation error for provider {}: {}", provider.getId(), e.getMessage(), e);
                                                return Mono.empty();
                                            })
                                            , concurrency)
                            .then(Mono.fromSupplier(() -> {
                                Map<String, Object> summary = new HashMap<>();
                                summary.put("status", "completed");
                                summary.put("total", activeProviders.size());
                                summary.put("valid", validCount.get());
                                summary.put("failed", failedCount.get());
                                summary.put("note", "active providers validated; 'dead' providers skipped");
                                log.info("testAllProviders: done — total={}, valid={}, failed={}",
                                        activeProviders.size(), validCount.get(), failedCount.get());
                                return summary;
                            }));
                })
                .doOnError(e -> log.error("testAllProviders: overall error {}", e.getMessage(), e));
    }
}