package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.VPNConnection;
import org.springframework.stereotype.Repository;

@Repository
public interface VPNRepository extends FirestoreReactiveRepository<VPNConnection> {
}
