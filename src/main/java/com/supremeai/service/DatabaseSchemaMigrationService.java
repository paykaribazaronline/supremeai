package com.supremeai.service;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.cloud.firestore.WriteBatch;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;

/**
 * ZM-04: Database Schema Auto-Migration
 * Detects schema changes and applies them automatically via @PostConstruct.
 * Performs a highly-efficient real Firestore batch migration.
 */
@Service
public class DatabaseSchemaMigrationService {
    public DatabaseSchemaMigrationService(FirebaseRealtimeService firebaseRealtimeService, Firestore firestore) {
        this.firebaseRealtimeService = firebaseRealtimeService;
        this.firestore = firestore;
    }


    private static final Logger log = LoggerFactory.getLogger(DatabaseSchemaMigrationService.class);



    @PostConstruct
    public void runAutoMigrations() {
        log.info("🛠️ Running Database Schema Auto-Migrations...");

        firebaseRealtimeService.getData("migration_status")
            .subscribe(
                status -> {
                    if (status == null || !status.containsKey("v2_firestore_applied")) {
                        log.info("🚀 Applying Real Firestore Schema Batch Migration V2: System Learning Schema Versioning...");
                        
                        performFirestoreBatchMigration()
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe(
                                updatedCount -> {
                                    log.info("✅ Firestore Batch Migration completed successfully. Updated {} documents.", updatedCount);
                                    // Mark migration as applied in RTDB for multi-instance coordination
                                    firebaseRealtimeService.setData("migration_status/v2_firestore_applied", true).subscribe();
                                },
                                error -> log.error("❌ Real Firestore Schema Batch Migration failed", error)
                            );
                    } else {
                        log.info("✨ Firestore Schema is up to date. No pending migrations.");
                    }
                },
                error -> log.error("Failed to check database migration status", error)
            );
    }

    /**
     * Performs direct Firestore batch query and writes to update outdated schemas.
     */
    private Mono<Integer> performFirestoreBatchMigration() {
        return Mono.fromCallable(() -> {
            log.info("📖 Fetching documents from 'system_learning' collection...");
            ApiFuture<QuerySnapshot> future = firestore.collection("system_learning").get();
            QuerySnapshot querySnapshot = future.get(); // Blocking call inside boundedElastic
            
            List<QueryDocumentSnapshot> documents = querySnapshot.getDocuments();
            log.info("Found {} total system_learning documents. Analyzing for migration...", documents.size());
            
            int updateCount = 0;
            int totalUpdated = 0;
            WriteBatch batch = firestore.batch();
            
            for (QueryDocumentSnapshot doc : documents) {
                // Migrate document if it doesn't have schemaVersion or tags
                boolean needsMigration = false;
                
                if (!doc.contains("schemaVersion")) {
                    needsMigration = true;
                }
                
                if (needsMigration) {
                    log.info("Document ID '{}' is missing fields. Queueing for batch migration...", doc.getId());
                    
                    // Define updates for this document
                    batch.update(doc.getReference(), 
                        "schemaVersion", 2L,
                        "timesApplied", doc.getLong("timesApplied") == null ? 0L : doc.getLong("timesApplied"),
                        "permanent", doc.getBoolean("permanent") == null ? false : doc.getBoolean("permanent")
                    );
                    
                    updateCount++;
                    totalUpdated++;
                    
                    // Firestore has a hard limit of 500 writes per batch
                    if (updateCount >= 450) {
                        batch.commit().get();
                        batch = firestore.batch();
                        updateCount = 0;
                        log.info("Committed intermediate batch of 450 documents.");
                    }
                }
            }
            
            if (updateCount > 0) {
                batch.commit().get(); // Commit batch blocking inside boundedElastic
            }
            
            log.info("Completed migration. Total documents updated: {}", totalUpdated);
            
            return totalUpdated;
        });
    }
}
