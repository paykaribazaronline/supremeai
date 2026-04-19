package com.supremeai.repository;

import com.supremeai.model.User;
import com.google.cloud.spring.data.firestore.FirestoreRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface UserRepository extends FirestoreRepository<User> {
    Mono<User> findByEmail(String email);
    Mono<User> findByFirebaseUid(String firebaseUid);
}