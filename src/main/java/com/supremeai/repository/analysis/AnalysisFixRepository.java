package com.supremeai.repository.analysis;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.analysis.AnalysisFix;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface AnalysisFixRepository extends FirestoreReactiveRepository<AnalysisFix> {
  Flux<AnalysisFix> findByJobId(String jobId);

  Flux<AnalysisFix> findByFindingId(String findingId);
}
