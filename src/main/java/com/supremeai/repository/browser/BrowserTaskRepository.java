package com.supremeai.repository.browser;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.browser.BrowserTask;

public interface BrowserTaskRepository extends FirestoreReactiveRepository<BrowserTask> {
}
