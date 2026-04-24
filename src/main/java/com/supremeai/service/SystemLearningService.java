package com.supremeai.service;

import com.supremeai.model.SystemLearning;
import com.supremeai.repository.SystemLearningRepository;
import com.supremeai.util.IdUtils;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Service for managing system learning with Redis caching.
 */
@Service
public class SystemLearningService {

    private final SystemLearningRepository repository;

    public SystemLearningService(SystemLearningRepository repository) {
        this.repository = repository;
    }

    @Cacheable(value = "system_learning", key = "'all'")
    public List<SystemLearning> getAllLearningSync() {
        return repository.findAll().collectList().block();
    }

    public Flux<SystemLearning> getAllLearning() {
        return repository.findAll();
    }

    @Cacheable(value = "system_learning", key = "#category")
    public List<SystemLearning> getByCategorySync(String category) {
        return repository.findByCategory(category).collectList().block();
    }

    @CacheEvict(value = "system_learning", allEntries = true)
    public Mono<SystemLearning> addLearning(SystemLearning learning) {
        learning.setId(IdUtils.ensureId(learning.getId()));
        return repository.save(learning);
    }

    @CacheEvict(value = "system_learning", allEntries = true)
    public Mono<Void> deleteLearning(String id) {
        return repository.deleteById(id);
    }
}
