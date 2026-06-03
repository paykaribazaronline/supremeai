package com.supremeai.repository.browser;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.browser.BrowserActivity;
import reactor.core.publisher.Flux;

public interface BrowserActivityRepository extends FirestoreReactiveRepository<BrowserActivity> {
    Flux<BrowserActivity> findAllByOrderByTimestampDesc();
}
