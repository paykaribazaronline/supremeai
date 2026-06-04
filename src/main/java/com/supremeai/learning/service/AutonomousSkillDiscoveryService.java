package com.supremeai.learning.service;

import com.supremeai.model.SystemLearning;
import com.supremeai.repository.SystemLearningRepository;
import com.supremeai.service.MCPMarketplaceService;
import com.supremeai.service.SystemLearningService;
import com.supremeai.service.ConfigService;
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
 * scrapes their skill instructions, and injects them into SupremeAI's marketplace + knowledge base.
 */
@Service
public class AutonomousSkillDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(AutonomousSkillDiscoveryService.class);

    @Autowired
    private EnhancedWebScraperService scraperService;

    @Autowired
    private SystemLearningRepository learningRepository;

    @Autowired(required = false)
    private SystemLearningService systemLearningService;

    @Autowired(required = false)
    private MCPMarketplaceService mcpMarketplaceService;

    // Added ConfigService to fetch registries dynamically from Database
    @Autowired
    private ConfigService configService;

    /**
     * Scheduled job to autonomously discover and learn new skills.
     * Runs every 12 hours.
     */
    @Scheduled(fixedRate = 43200000)
    public void autonomousSkillDiscovery() {
        log.info("[Skill Discovery] Starting autonomous scan for new AI agent skills...");

        // Fetch dynamic registries from database (comma separated)
        String registriesStr = configService.getEffectiveSetting("skill_registries", 
                "https://skills.sh,https://github.com/vercel/ai-skills,https://smith.langchain.com/hub,https://llamahub.ai,https://github.com/modelcontextprotocol/servers");
        
        List<String> dynamicRegistries = Arrays.stream(registriesStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        for (String registryUrl : dynamicRegistries) {
            log.info("[Skill Discovery] Scanning registry: {}", registryUrl);
            try {
                EnhancedWebScraperService.ScrapedContent content = scraperService.scrapeUrl(registryUrl);

                if (content != null && content.getContent() != null) {
                    proposeSkillToMarketplace(content);
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
     * Propose a discovered skill to MCPMarketplaceService for admin approval.
     */
    private void proposeSkillToMarketplace(EnhancedWebScraperService.ScrapedContent content) {
        if (mcpMarketplaceService == null) {
            log.warn("[Skill Discovery] MCPMarketplaceService unavailable; skipping marketplace proposal.");
            return;
        }

        String title = content.getTitle();
        String description = content.getFullContent();
        if (description != null && description.length() > 400) {
            description = description.substring(0, 400) + "...";
        }
        if (description == null) description = "Auto-discovered skill from " + content.getUrl();

        List<String> triggers = List.of(content.getDomain(), content.getUrl());
        List<String> steps = List.of(content.getFullContent());
        Map<String, Object> metadata = Map.of(
                "url", content.getUrl(),
                "domain", content.getDomain(),
                "sourceAuthority", content.getSourceAuthority()
        );

        mcpMarketplaceService.proposeDiscoveredSkill(
                content.getUrl(),
                title,
                description,
                triggers,
                steps,
                metadata
        );
        log.info("[Skill Discovery] Proposed skill to marketplace: {}", title);
    }

    /**
     * Manual trigger for on-demand skill learning.
     */
    public void learnSkillOnDemand(String url) {
        log.info("[Skill Discovery] On-demand learning triggered for: {}", url);
        try {
            EnhancedWebScraperService.ScrapedContent content = scraperService.scrapeUrl(url);
            if (content != null) {
                proposeSkillToMarketplace(content);
                processDiscoveredSkill(content);
            }
        } catch (Exception e) {
            log.error("[Skill Discovery] On-demand learning failed for {}", url, e);
        }
    }

    private void processDiscoveredSkill(EnhancedWebScraperService.ScrapedContent content) {
        String title = content.getTitle();
        String fullContent = content.getFullContent();

        log.info("[Skill Discovery] Persisting skill to knowledge base: {}", title);

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

        learning.setPermanent(true);
        learning.setTags(Arrays.asList("autonomous", "skill", content.getDomain()));
        learning.setSuccess(true);
        learning.setQualityScore(content.getQualityScore());

        learningRepository.save(learning).subscribe(
                saved -> log.info("[Skill Discovery] Saved skill to knowledge base: {}", title),
                error -> log.error("[Skill Discovery] Failed to save skill to knowledge base", error)
        );
    }

    /**
     * Scheduled job to autonomously discover new free AI models/providers.
     * Runs every 24 hours to find new models on platforms like HuggingFace.
     */
    @Scheduled(fixedRate = 86400000)
    public void autonomousProviderDiscovery() {
        log.info("[Provider Discovery] Scanning for new AI models and web interfaces...");
        String aiRegistriesStr = configService.getEffectiveSetting("ai_model_registries", 
                "https://huggingface.co/models?pipeline_tag=text-generation,https://openrouter.ai/models");
        
        List<String> registries = Arrays.stream(aiRegistriesStr.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
                
        for (String url : registries) {
            try {
                EnhancedWebScraperService.ScrapedContent content = scraperService.scrapeUrl(url);
                if (content != null && content.getContent() != null) {
                    // Pushes discovery to Admin pending queue so you can approve new free models
                    proposeSkillToMarketplace(content); 
                }
            } catch (Exception e) {
                log.error("[Provider Discovery] Failed to scan AI model registry: {}", url, e);
            }
        }
    }
}
