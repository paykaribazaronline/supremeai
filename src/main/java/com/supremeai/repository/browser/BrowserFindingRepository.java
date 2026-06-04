package com.supremeai.repository.browser;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.browser.BrowserFinding;
import reactor.core.publisher.Flux;

public interface BrowserFindingRepository extends FirestoreReactiveRepository<BrowserFinding> {
  Flux<BrowserFinding> findByTaskId(String taskId);
}
