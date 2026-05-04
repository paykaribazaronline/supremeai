package com.supremeai.config;

import com.google.cloud.spring.data.firestore.repository.config.EnableReactiveFirestoreRepositories;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@Configuration
@Profile("!local")
@ConditionalOnProperty(name = "spring.cloud.gcp.firestore.enabled", havingValue = "true", matchIfMissing = true)
@EnableReactiveFirestoreRepositories(basePackages = "com.supremeai.repository")
public class FirestoreRepositoryConfig {
}
