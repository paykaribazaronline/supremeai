package com.supremeai.repository;

import com.supremeai.model.UserGuide;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface UserGuideRepository extends ReactiveCrudRepository<UserGuide, String> {
    
    /**
     * Find all published guides.
     */
    Flux<UserGuide> findByIsPublished(boolean isPublished);
    
    /**
     * Find guides by category and published status.
     */
    Flux<UserGuide> findByCategoryAndIsPublished(String category, boolean isPublished);
    
    /**
     * Find guides by tags (contains check).
     */
    Flux<UserGuide> findByTagsContaining(String tag);
}
