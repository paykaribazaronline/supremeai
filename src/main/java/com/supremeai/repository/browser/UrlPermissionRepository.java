package com.supremeai.repository.browser;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.browser.UrlPermission;
import reactor.core.publisher.Flux;

public interface UrlPermissionRepository extends FirestoreReactiveRepository<UrlPermission> {
    Flux<UrlPermission> findByType(String type);
    Flux<UrlPermission> findByUserIdAndType(String userId, String type);
}
