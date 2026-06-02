package com.supremeai.service.validation;

import com.supremeai.model.APIProvider;
import com.supremeai.repository.ProviderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * BV-04: Provider Tournament Mode
 * Monthly automated head-to-head provider comparison.
 * Auto-demotes consistently underperforming providers.
 */
@Service
public class ProviderTournamentService {
    public ProviderTournamentService(ProviderRepository providerRepository, SWEBenchValidationService sweBenchService) {
        this.providerRepository = providerRepository;
        this.sweBenchService = sweBenchService;
    }


    private static final Logger log = LoggerFactory.getLogger(ProviderTournamentService.class);



    /**
     * Run the tournament monthly (1st of every month at 3 AM)
     */
    @Scheduled(cron = "0 0 3 1 * *")
    public void runMonthlyTournament() {
        log.info("🏆 Starting Monthly Provider Tournament...");
        
        providerRepository.findAll()
            .filter(p -> "active".equalsIgnoreCase(p.getStatus()))
            .map(APIProvider::getName)
            .collectList()
            .flatMap(activeProviders -> {
                if (activeProviders.size() < 2) {
                    log.warn("Not enough active providers for a tournament.");
                    return Mono.empty();
                }
                
                log.info("Tournament participants: {}", activeProviders);
                return sweBenchService.runSweBenchSuite(activeProviders);
            })
            .subscribe(
                results -> evaluateTournamentResults((java.util.Map<String, Object>) results),
                error -> log.error("Tournament failed", error)
            );
    }

    private void evaluateTournamentResults(java.util.Map<String, Object> results) {
        if (results == null || !results.containsKey("baselinePassRates")) return;
        
        @SuppressWarnings("unchecked")
        java.util.Map<String, Double> rates = (java.util.Map<String, Double>) results.get("baselinePassRates");
        
        String winner = null;
        double maxRate = -1;
        String loser = null;
        double minRate = 2.0;
        
        for (java.util.Map.Entry<String, Double> entry : rates.entrySet()) {
            if (entry.getValue() > maxRate) {
                maxRate = entry.getValue();
                winner = entry.getKey();
            }
            if (entry.getValue() < minRate) {
                minRate = entry.getValue();
                loser = entry.getKey();
            }
        }
        
        log.info("Tournament Winner: {} with pass rate {}", winner, maxRate);
        
        // Auto-demote underperforming provider
        if (loser != null && minRate < 0.40) { // e.g., < 40% pass rate
            log.warn("Auto-demoting consistently underperforming provider: {} (rate: {})", loser, minRate);
            providerRepository.findByName(loser)
                .flatMap(provider -> {
                    provider.setStatus("demoted");
                    provider.setCanParticipateInVoting(false);
                    return providerRepository.save(provider);
                })
                .subscribe();
        }
    }
}
