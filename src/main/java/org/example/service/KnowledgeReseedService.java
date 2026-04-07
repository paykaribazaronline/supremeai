package org.example.service;

import org.example.learning.CIErrorLearningInitializer;
import org.example.learning.EngineeringExcellenceKnowledgeInitializer;
import org.example.learning.OperationalTechniqueLearningInitializer;
import org.example.learning.StrategicKnowledgeLearningInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * Orchestrates reseeding of all knowledge sources on demand.
 */
@Service
public class KnowledgeReseedService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeReseedService.class);

    @Autowired
    private OperationalTechniqueLearningInitializer operationalTechniqueLearningInitializer;

    @Autowired
    private StrategicKnowledgeLearningInitializer strategicKnowledgeLearningInitializer;

    @Autowired
    private CIErrorLearningInitializer ciErrorLearningInitializer;

    @Autowired
    private EngineeringExcellenceKnowledgeInitializer engineeringExcellenceKnowledgeInitializer;

    @Autowired
    private SystemLearningService systemLearningService;

    public Map<String, Object> reseedAllKnowledge(String trigger) {
        long startedAt = System.currentTimeMillis();
        logger.info("🧠 Reseeding all knowledge. trigger={}", trigger);

        operationalTechniqueLearningInitializer.seedOperationalTechniques();
        strategicKnowledgeLearningInitializer.seedStrategicKnowledge();
        ciErrorLearningInitializer.teachCIErrors();
        engineeringExcellenceKnowledgeInitializer.seedEngineeringExcellence();

        Map<String, Object> result = new HashMap<>();
        result.put("status", "success");
        result.put("trigger", trigger);
        result.put("durationMs", System.currentTimeMillis() - startedAt);
        result.put("learningStats", systemLearningService.getLearningStats());
        return result;
    }
}