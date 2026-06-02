package com.supremeai.service;

import com.supremeai.model.SystemLearning;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Orchestrates all knowledge seed providers. Each seed type is in a dedicated class.
 */
@Component
public class KnowledgeSeedDataProvider {

    private final SystemLearningFactory factory;
public KnowledgeSeedDataProvider(SystemLearningFactory factory) {
        this.factory = factory;
    }

    public Flux<SystemLearning> provideAllSeeds() {
        return Flux.merge(
                new OfflineOperationSeeds(factory).provideSeeds(),
                new ErrorSolutionsSeeds(factory).provideSeeds(),
                new AiPatternsSeeds(factory).provideSeeds(),
                new BestPracticesSeeds(factory).provideSeeds(),
                new LifecyclePoliciesSeeds(factory).provideSeeds(),
                new BrowserAutomationSeeds(factory).provideSeeds()
        );
    }

    /**
     * Factory for creating SystemLearning objects to avoid code duplication.
     */
    public static class SystemLearningFactory {
        public SystemLearning create(String id, String title, String category,
                String content, List<String> tags, boolean isCritical, double confidence) {
            SystemLearning learning = new SystemLearning();
            learning.setId(id);
            learning.setTitle(title);
            learning.setCategory(category);
            learning.setContent(content);
            learning.setTags(tags);
            learning.setCritical(isCritical);
            learning.setConfidence(confidence);
            learning.setVersion(1L);
            learning.setCreatedAt(LocalDateTime.now());
            learning.setUpdatedAt(LocalDateTime.now());
            return learning;
        }
    }
}