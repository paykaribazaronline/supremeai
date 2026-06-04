package com.supremeai.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import com.supremeai.model.APIHealthReport;
import org.springframework.stereotype.Repository;

@Repository
public interface APIHealthReportRepository extends FirestoreReactiveRepository<APIHealthReport> {}
