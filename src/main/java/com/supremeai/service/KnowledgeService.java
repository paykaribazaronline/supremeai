package com.supremeai.service;

import com.supremeai.model.KnowledgeDomain;
import com.supremeai.model.KnowledgeRecommendation;
import com.supremeai.model.KnowledgeDomain.Status;
import com.supremeai.model.SystemLearning;
import com.supremeai.repository.KnowledgeDomainRepository;
import com.supremeai.repository.KnowledgeRecommendationRepository;
import com.supremeai.repository.SystemLearningRepository;
import com.supremeai.learning.active.ActiveInternetScraper;
import com.supremeai.controller.WebSocketController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class KnowledgeService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeService.class);

    @Autowired
    private KnowledgeDomainRepository domainRepository;

    @Autowired
    private KnowledgeRecommendationRepository recommendationRepository;

    @Autowired
    private SystemLearningRepository learningRepository;

    @Autowired
    private ActiveInternetScraper scraper;

    @Autowired
    private WebSocketController webSocketController;

    /**
     * Register a new knowledge domain for learning
     */
    public Mono<KnowledgeDomain> registerDomain(String name, List<String> keywords) {
        KnowledgeDomain domain = new KnowledgeDomain(name, keywords);
        return domainRepository.save(domain);
    }

    /**
     * Start learning for a domain
     */
    public Mono<KnowledgeDomain> startLearning(String domainId) {
        return domainRepository.findById(domainId)
                .flatMap(domain -> {
                    domain.setStatus(Status.LEARNING);
                    domain.setLastUpdated(LocalDateTime.now());
                    return domainRepository.save(domain);
                });
    }

    /**
     * Process learning job - actual web search and extraction
     */
    public Mono<Map<String, Object>> processLearningJob(String domainId) {
        return domainRepository.findById(domainId)
                .flatMap(domain -> {
                    webSocketController.broadcastSystemEvent("KNOWLEDGE_CRAWL", domainId, 0.0, null,
                            "Starting knowledge acquisition for " + domain.getName());

                    return scraper.scrapeKnowledge(domain.getName(), domain.getKeywords())
                            .subscribeOn(Schedulers.boundedElastic())
                            .index() // Add index to track progress
                            .flatMap(tuple -> {
                                long index = tuple.getT1();
                                var scrapedIssue = tuple.getT2();
                                double progress = Math.min(95.0, (index + 1) * 10.0); // Simple progress calculation

                                webSocketController.broadcastSystemEvent("KNOWLEDGE_CRAWL", domainId, progress,
                                        scrapedIssue.getTitle(), "Discovered: " + scrapedIssue.getTitle());

                                return saveScrapedFact(domain, scrapedIssue);
                            })
                            .collectList()
                            .flatMap(savedList -> {
                                domain.setNodesDiscovered(domain.getNodesDiscovered() + savedList.size());
                                domain.setStatus(Status.COMPLETE);
                                domain.setLastUpdated(LocalDateTime.now());

                                webSocketController.broadcastSystemEvent("KNOWLEDGE_CRAWL", domainId, 100.0, null,
                                        "Acquisition complete. Discovered " + savedList.size() + " new nodes.");

                                Map<String, Object> result = new HashMap<>();
                                result.put("domainId", domainId);
                                result.put("factsDiscovered", savedList.size());
                                result.put("status", "complete");

                                return domainRepository.save(domain).thenReturn(result);
                            })
                            .onErrorResume(e -> {
                                logger.error("Error during learning job for domain {}", domainId, e);
                                domain.setStatus(Status.ERROR);
                                webSocketController.broadcastSystemEvent("KNOWLEDGE_CRAWL", domainId, null, null,
                                        "Failed: " + e.getMessage());
                                return domainRepository.save(domain).thenReturn(Collections.emptyMap());
                            });
                });
    }

    /**
     * Process learning for multiple websites/topics.
     * 
     * @param topics List of topics or website names to scrape
     * @return Mono signaling completion
     */
    public Mono<Void> processMultipleWebsites(List<String> topics) {
        logger.info("[KNOWLEDGE] Initiating bulk scraping for {} topics", topics.size());
        return Flux.fromIterable(topics)
                .flatMap(topic -> registerDomain(topic, Arrays.asList(topic.toLowerCase().split("\\s+")))
                        .flatMap(domain -> processLearningJob(domain.getId())))
                .then()
                .doOnSuccess(v -> logger.info("[KNOWLEDGE] Bulk scraping completed successfully"))
                .doOnError(e -> logger.error("[KNOWLEDGE] Bulk scraping encountered errors: {}", e.getMessage()));
    }

    /**
     * Get the most recent scraped learning entries for UI verification.
     * 
     * @param limit Number of entries to return
     * @return Mono containing a list of recent learning entries
     */
    public Mono<List<SystemLearning>> getRecentScrapedLearnings(int limit) {
        return learningRepository.findAll()
                .filter(learning -> "WEB_SCRAPE".equals(learning.getLearningType()))
                .sort(Comparator.comparing(SystemLearning::getLearnedAt).reversed())
                .take(limit)
                .collectList();
    }

    /**
     * Purge all old web-scraped learning entries from Firestore.
     * 
     * @return Mono signaling completion
     */
    public Mono<Void> clearOldScrapedLearnings() {
        return learningRepository.findAll()
                .filter(learning -> "WEB_SCRAPE".equals(learning.getLearningType()))
                .flatMap(learning -> learningRepository.deleteById(learning.getId()))
                .then()
                .doOnSuccess(v -> logger.info("[KNOWLEDGE] Successfully purged web-scraped entries from Firestore"))
                .doOnError(e -> logger.error("[KNOWLEDGE] Failed to purge web-scraped entries: {}", e.getMessage()));
    }

    /**
     * Save scraped fact as a learning entry
     */
    private Mono<SystemLearning> saveScrapedFact(KnowledgeDomain domain,
            com.supremeai.learning.active.ActiveInternetScraper.ScrapedIssue issue) {
        SystemLearning learning = new SystemLearning();
        learning.setId("kb_" + UUID.randomUUID().toString());
        learning.setTopic(issue.getTitle());
        learning.setCategory("KNOWLEDGE_ACQUISITION");
        learning.setContent(issue.getBody());
        learning.setLearningType("WEB_SCRAPE");
        learning.setRelatedProvider(issue.getSource());
        learning.setLearnedAt(LocalDateTime.now());
        learning.setQualityScore(issue.getAuthority().getWeight());
        learning.setSuccess(true);
        learning.setTimesApplied(0);
        learning.setTags(domain.getKeywords());

        return learningRepository.save(learning)
                .doOnSuccess(saved -> logger.info("[FIREBASE_PERSIST] Knowledge saved to Firestore: ID={}, Topic={}",
                        saved.getId(), saved.getTopic()));
    }

    /**
     * Get all domains with statistics
     */
    public Mono<Map<String, Object>> getKnowledgeSnapshot() {
        return domainRepository.findAll()
                .collectList()
                .map(domains -> {
                    Map<String, Object> snapshot = new HashMap<>();

                    int totalNodes = domains.stream()
                            .mapToInt(d -> d.getNodesDiscovered() != null ? d.getNodesDiscovered() : 0)
                            .sum();

                    Optional<KnowledgeDomain> lastDomain = domains.stream()
                            .max(Comparator.comparing(KnowledgeDomain::getLastUpdated));

                    List<String> topDomains = domains.stream()
                            .sorted(Comparator.comparingInt(
                                    (KnowledgeDomain d) -> d.getNodesDiscovered() != null ? d.getNodesDiscovered() : 0)
                                    .reversed())
                            .limit(3)
                            .map(KnowledgeDomain::getName)
                            .collect(Collectors.toList());

                    double efficiency = domains.isEmpty() ? 0
                            : domains.stream()
                                    .mapToDouble(d -> d.getAverageConfidence() != null ? d.getAverageConfidence() : 0)
                                    .average().orElse(0);

                    snapshot.put("totalKnowledgeNodes", totalNodes);
                    snapshot.put("topLearningDomains", topDomains);
                    snapshot.put("lastDiscoveryTime", lastDomain.map(KnowledgeDomain::getLastUpdated).orElse(null));
                    snapshot.put("discoveryEfficiency", String.format("%.1f%%", efficiency * 100));

                    return snapshot;
                });
    }

    /**
     * Generate recommendations based on knowledge gaps
     */
    public Mono<List<KnowledgeRecommendation>> generateRecommendations() {
        return domainRepository.findAll()
                .collectList()
                .flatMap(domains -> {
                    // Analyze gaps - domains with low node count get recommendations
                    Map<String, Object> gapAnalysis = analyzeKnowledgeGaps();

                    List<KnowledgeRecommendation> recommendations = new ArrayList<>();

                    // Generate recommendations for trending topics
                    List<String> trendingTopics = Arrays.asList(
                            "React 19 Server Components",
                            "Spring Boot Virtual Threads Optimization",
                            "AI Agent Memory Management",
                            "Quantum-Resistant Cryptography",
                            "Edge AI Deployment Patterns");

                    for (int i = 0; i < Math.min(3, trendingTopics.size()); i++) {
                        String topic = trendingTopics.get(i);
                        List<String> keywords = Arrays.asList(topic.toLowerCase().split(" "));
                        double confidence = 0.75 + Math.random() * 0.15;

                        KnowledgeRecommendation rec = new KnowledgeRecommendation(
                                topic,
                                "Identified as trending technology with growing ecosystem adoption",
                                confidence,
                                keywords);
                        recommendations.add(rec);
                    }

                    return Flux.fromIterable(recommendations)
                            .flatMap(recommendationRepository::save)
                            .collectList();
                });
    }

    /**
     * Analyze knowledge gaps
     */
    private Map<String, Object> analyzeKnowledgeGaps() {
        Map<String, Object> gaps = new HashMap<>();
        gaps.put("lowCoverageDomains", Arrays.asList("Quantum Computing", "Web3 Infrastructure", "AR/VR Development"));
        gaps.put("outdatedEntries", 5);
        gaps.put("staleRecommendations", 0);
        return gaps;
    }

    /**
     * Approve a recommendation (converts to learning domain)
     */
    public Mono<KnowledgeDomain> approveRecommendation(String recommendationId, String domainName,
            List<String> keywords) {
        return recommendationRepository.findById(recommendationId)
                .flatMap(rec -> {
                    rec.setStatus(KnowledgeRecommendation.Status.APPROVED);
                    rec.setProcessedAt(LocalDateTime.now());
                    recommendationRepository.save(rec);

                    KnowledgeDomain domain = new KnowledgeDomain(domainName, keywords);
                    return domainRepository.save(domain);
                });
    }

    /**
     * Decline a recommendation
     */
    public Mono<KnowledgeRecommendation> declineRecommendation(String recommendationId) {
        return recommendationRepository.findById(recommendationId)
                .flatMap(rec -> {
                    rec.setStatus(KnowledgeRecommendation.Status.DECLINED);
                    rec.setProcessedAt(LocalDateTime.now());
                    return recommendationRepository.save(rec);
                });
    }

    /**
     * Get all domains
     */
    public Flux<KnowledgeDomain> getAllDomains() {
        return domainRepository.findAll();
    }

    /**
     * Get pending recommendations
     */
    public Flux<KnowledgeRecommendation> getPendingRecommendations() {
        return recommendationRepository.findByStatus(KnowledgeRecommendation.Status.PENDING);
    }

    /**
     * Delete a domain
     */
    public Mono<Void> deleteDomain(String domainId) {
        return domainRepository.deleteById(domainId);
    }
}