package com.supremeai.controller;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.supremeai.audit.Audited;
import com.supremeai.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Controller to handle generic Firestore document operations for the admin dashboard.
 * Used for dynamic settings like teldrive_settings.
 */
@RestController
@RequestMapping("/api/admin/firestore")
@PreAuthorize("hasRole('ADMIN')")
public class AdminFirestoreController extends BaseAdminController<Object, String> {

    private static final Logger log = LoggerFactory.getLogger(AdminFirestoreController.class);

    @Autowired
    private Firestore firestore;

    /**
     * Get a document from a specific collection.
     * Path example: /api/admin/firestore/system_configs/teldrive_settings
     */
    @GetMapping("/{collection}/{document}")
    public Mono<ResponseEntity<ApiResponse<Object>>> getDocument(
            @PathVariable String collection,
            @PathVariable String document) {
        
        return Mono.fromCallable(() -> {
            DocumentReference docRef = firestore.collection(collection).document(document);
            ApiFuture<DocumentSnapshot> future = docRef.get();
            DocumentSnapshot snapshot = future.get();
            
            if (snapshot.exists()) {
                return ResponseEntity.ok(ApiResponse.<Object>ok(snapshot.getData()));
            } else {
                return ResponseEntity.status(404).body(ApiResponse.<Object>error("Document not found: " + collection + "/" + document));
            }
        }).map(r -> (ResponseEntity<ApiResponse<Object>>) r)
        .onErrorResume(e -> {
            log.error("Failed to fetch firestore document {}/{}: {}", collection, document, e.getMessage());
            return Mono.just(ResponseEntity.status(500).body(ApiResponse.<Object>error("Failed to fetch document: " + e.getMessage())));
        });
    }

    /**
     * Update or create a document in a specific collection.
     */
    @PutMapping("/{collection}/{document}")
    @Audited(resource = "firestore_config", action = "update_document")
    public Mono<ResponseEntity<ApiResponse<Object>>> updateDocument(
            @PathVariable String collection,
            @PathVariable String document,
            @RequestBody Map<String, Object> data) {
        
        return Mono.fromCallable(() -> {
            DocumentReference docRef = firestore.collection(collection).document(document);
            ApiFuture<WriteResult> future = docRef.set(data);
            WriteResult result = future.get();
            
            log.info("Firestore document {}/{} updated at {}", collection, document, result.getUpdateTime());
            return ResponseEntity.ok(ApiResponse.<Object>ok(data));
        }).onErrorResume(e -> {
            log.error("Failed to update firestore document {}/{}: {}", collection, document, e.getMessage());
            return Mono.just(ResponseEntity.status(500).body(ApiResponse.<Object>error("Failed to update document: " + e.getMessage())));
        });
    }

    /**
     * Delete a document from a specific collection.
     */
    @DeleteMapping("/{collection}/{document}")
    @Audited(resource = "firestore_config", action = "delete_document")
    public Mono<ResponseEntity<ApiResponse<String>>> deleteDocument(
            @PathVariable String collection,
            @PathVariable String document) {
        
        return Mono.fromCallable(() -> {
            DocumentReference docRef = firestore.collection(collection).document(document);
            ApiFuture<WriteResult> future = docRef.delete();
            future.get();
            
            return ResponseEntity.ok(ApiResponse.ok("Document deleted successfully"));
        }).onErrorResume(e -> {
            log.error("Failed to delete firestore document {}/{}: {}", collection, document, e.getMessage());
            return Mono.just(ResponseEntity.status(500).body(ApiResponse.error("Failed to delete document: " + e.getMessage())));
        });
    }
}
