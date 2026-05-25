package com.supremeai.repository;

import com.supremeai.model.BenchmarkResult;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Mono;

/**
 * Repository for benchmark results stored in Firestore.
 */
public interface BenchmarkResultRepository extends ReactiveCrudRepository<BenchmarkResult, String> {
    Mono<BenchmarkResult> findTopByOrderByTimestampDesc();
}