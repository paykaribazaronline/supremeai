package com.supremeai.service;

import com.supremeai.model.SystemLearning;
import com.supremeai.repository.SystemLearningRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalService {

    private final SystemLearningRepository systemLearningRepository;

    /**
     * Searches the system's learned knowledge for relevant entries based on a query.
     * This can be used by AI agents to access local knowledge.
     *
     * @param query The search query.
     * @return A Flux of relevant SystemLearning entries.
     */
    public Flux<SystemLearning> searchLocalKnowledge(String query) {
        // For a basic implementation, we'll do a simple keyword search on title and content.
        // In a more advanced setup, this would involve vector search (Turbovec, Pinecone)
        // or more sophisticated text analysis.
        String lowerCaseQuery = query.toLowerCase();
        return systemLearningRepository.findAll()
                .filter(sl -> sl.getTitle().toLowerCase().contains(lowerCaseQuery) ||
                               sl.getContent().toLowerCase().contains(lowerCaseQuery))
                .take(5); // Limit to top 5 relevant results for brevity
    }

    /**
     * Retrieves a specific SystemLearning entry by its ID.
     * @param id The ID of the SystemLearning entry.
     * @return A Mono containing the SystemLearning entry, or empty if not found.
     */
    public Mono<SystemLearning> getKnowledgeById(String id) {
        return systemLearningRepository.findById(id);
    }
}