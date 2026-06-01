package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.InstalledSkill;
import org.springframework.stereotype.Repository;

@Repository
public interface InstalledSkillRepository extends FirestoreReactiveRepository<InstalledSkill> {
}
