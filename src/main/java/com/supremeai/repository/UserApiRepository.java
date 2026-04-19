package com.supremeai.repository;

import com.supremeai.model.UserApi;
import com.supremeai.model.UserTier;
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.util.List;

@Repository
public interface UserApiRepository extends FirestoreReactiveRepository<UserApi> {
    Flux<UserApi> findByUserId(String userId);
    Flux<UserApi> findByUserIdAndIsActive(String userId, Boolean isActive);
    Mono<UserApi> findByApiKey(String apiKey);
    Flux<UserApi> findByUserTier(UserTier userTier);
}