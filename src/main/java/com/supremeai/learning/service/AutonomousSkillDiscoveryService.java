package com.supremeai.learning.service;

import com.supremeai.model.SystemLearning;
import com.supremeai.repository.SystemLearningRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Autonomous Skill Discovery Service
 * Automatically finds resourceful websites (like skills.sh or AI agent registries),
 * scrapes their skill instructions, and injects them into SupremeAI's knowledge base.
 */
@Service
public class AutonomousSkillDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(AutonomousSkillDiscoveryService.class);

    @Autowired
    private EnhancedWebScraperService scraperService;

    @Autowired
    private SystemLearningRepository learningRepository;

    // A list of seed registries to look for AI skills
    private final List<String> seedRegistries = Arrays.asList(
            "https://skills.sh",
            "https://github.com/vercel/ai-skills" // Example fallback repository
    );

    /**
     * Scheduled job to autonomously discover and learn new skills.
     * Runs every 12 hours.
     */
    @Scheduled(fixedRate = 43200000) // 12 hours
    public void autonomousSkillDiscovery() {
        log.info("[Skill Discovery] Starting autonomous scan for new AI agent skills...");

        for (String registryUrl : seedRegistries) {
            log.info("[Skill Discovery] Scanning registry: {}", registryUrl);
            try {
                EnhancedWebScraperService.ScrapedContent content = scraperService.scrapeUrl(registryUrl);

                if (content != null && content.getContent() != null) {
                    processDiscoveredSkill(content);
                } else {
                    log.warn("[Skill Discovery] Failed to extract content from {}", registryUrl);
                }
            } catch (Exception e) {
                log.error("[Skill Discovery] Error while scanning registry: {}", registryUrl, e);
            }
        }
        
        log.info("[Skill Discovery] Autonomous scan completed.");
    }

    /**
     * Process the scraped skill content, parse the instructions,
     * and persist them into the SystemLearning database.
     */
    private void processDiscoveredSkill(EnhancedWebScraperService.ScrapedContent content) {
        String title = content.getTitle();
        String fullContent = content.getFullContent();

        // Check if we already learned this skill recently
        // For simplicity, we just save it as a new learning if not exact duplicate
        log.info("[Skill Discovery] Discovered new potential skill: {}", title);

        SystemLearning learning = new SystemLearning();
        learning.setTopic(title);
        learning.setCategory("AUTONOMOUS_SKILL");
        learning.setContent(fullContent);
        learning.setSources(Arrays.asList(content.getUrl()));
        learning.setConfidenceScore(content.getQualityScore());
        learning.setLearningType("SKILL_REGISTRY");
        
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("domain", content.getDomain());
        metadata.put("sourceAuthority", content.getSourceAuthority());
        metadata.put("technologies", String.join(",", content.getTechnologies()));
        learning.setMetadata(metadata);
        
        learning.setPermanent(true); // Persist as core knowledge
        learning.setTags(Arrays.asList("autonomous", "skill", content.getDomain()));
        learning.setSuccess(true);
        learning.setQualityScore(content.getQualityScore());

        // Save to Firestore repository
        learningRepository.save(learning).subscribe(
            saved -> log.info("[Skill Discovery] Successfully integrated skill '{}' into SupremeAI Core Engine.", title),
            error -> log.error("[Skill Discovery] Failed to save skill to database.", error)
        );
    }
    
    /**
     * Manual trigger for on-demand skill learning.
     * @param url The URL of the SKILL.md or registry to learn from
     */
    public void learnSkillOnDemand(String url) {
        log.info("[Skill Discovery] On-demand learning triggered for: {}", url);
        try {
            EnhancedWebScraperService.ScrapedContent content = scraperService.scrapeUrl(url);
            if (content != null) {
                processDiscoveredSkill(content);
            }
        } catch (Exception e) {
            log.error("[Skill Discovery] On-demand learning failed for {}", url, e);
        }
    }
}
