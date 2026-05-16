package com.supremeai.repository.analysis;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.analysis.CodeChunk;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface CodeChunkRepository extends FirestoreReactiveRepository<CodeChunk> {
    Flux<CodeChunk> findByProjectId(String projectId);
    Flux<CodeChunk> findByProjectIdAndFile(String projectId, String file);
    Flux<CodeChunk> findByProjectIdAndLanguage(String projectId, String language);
}
