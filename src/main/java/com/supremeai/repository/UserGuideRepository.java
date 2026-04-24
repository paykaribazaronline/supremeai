package com.supremeai.repository;

import com.supremeai.model.UserGuide;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.List;

@Repository
public interface UserGuideRepository extends ReactiveCrudRepository<UserGuide, String> {
    
    /**
     * Find all published guides ordered by the 'order' field.
     */
    Flux<UserGuide> findByIsPublishedTrue();
    
    /**
     * Find guides by category.
     */
    Flux<UserGuide> findByCategoryAndIsPublished(String category, Boolean isPublished);
    
    /**
     * Find guides by tags (contains check).
     */
    Flux<UserGuide> findByTagsContaining(String tag);
}
