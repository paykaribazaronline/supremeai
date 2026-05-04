package com.supremeai.config;

import com.google.cloud.spring.data.firestore.repository.config.EnableReactiveFirestoreRepositories;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!local")
@EnableReactiveFirestoreRepositories(basePackages = "com.supremeai.repository")
public class FirestoreRepositoryConfig {
}
