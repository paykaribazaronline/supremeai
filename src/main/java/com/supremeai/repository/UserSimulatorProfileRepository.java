package com.supremeai.repository;

import com.supremeai.model.UserSimulatorProfile;
import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSimulatorProfileRepository extends FirestoreReactiveRepository<UserSimulatorProfile> {
}
