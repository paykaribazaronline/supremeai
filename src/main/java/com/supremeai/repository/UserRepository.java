package com.supremeai.repository;

import com.supremeai.model.User;
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends FirestoreReactiveRepository<User> {
    Mono<User> findByEmail(String email);
    Mono<User> findByFirebaseUid(String firebaseUid);
}