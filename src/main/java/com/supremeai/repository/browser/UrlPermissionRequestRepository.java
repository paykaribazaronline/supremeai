package com.supremeai.repository.browser;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.browser.UrlPermissionRequest;
import reactor.core.publisher.Flux;

public interface UrlPermissionRequestRepository extends FirestoreReactiveRepository<UrlPermissionRequest> {
    Flux<UrlPermissionRequest> findByStatus(String status);
}
