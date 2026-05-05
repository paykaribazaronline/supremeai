package com.supremeai.codeflow.repository;

import com.supremeai.codeflow.model.CodeRepository;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Firestore repository for CodeFlow module
 * Handles CRUD operations for code analysis data
 */
@Repository
public class CodeFlowRepository {
    
    private static final String COLLECTION_NAME = "codeflow/repositories";
    
    /**
     * Save or update a code repository analysis
     */
    public CodeRepository save(CodeRepository repository) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        
        if (repository.getId() == null) {
            repository.setId(UUID.randomUUID().toString());
        }
        
        DocumentReference docRef = db.collection(COLLECTION_NAME).document(repository.getId());
        docRef.set(repository).get();
        
        return repository;
    }
    
    /**
     * Find repository by ID
     */
    public Optional<CodeRepository> findById(String id) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        DocumentSnapshot document = db.collection(COLLECTION_NAME).document(id).get().get();
        
        if (document.exists()) {
            CodeRepository repo = document.toObject(CodeRepository.class);
            repo.setId(document.getId());
            return Optional.of(repo);
        }
        return Optional.empty();
    }
    
    /**
     * Find repository by source ID (GitHub repo ID, etc.)
     */
    public Optional<CodeRepository> findBySourceId(String sourceId) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        QuerySnapshot query = db.collection(COLLECTION_NAME)
            .whereEqualTo("sourceId", sourceId)
            .limit(1)
            .get().get();
        
        if (!query.isEmpty()) {
            DocumentSnapshot doc = query.getDocuments().get(0);
            CodeRepository repo = doc.toObject(CodeRepository.class);
            repo.setId(doc.getId());
            return Optional.of(repo);
        }
        return Optional.empty();
    }
    
    /**
     * Find all repositories for an owner
     */
    public List<CodeRepository> findByOwnerId(String ownerId) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        QuerySnapshot query = db.collection(COLLECTION_NAME)
            .whereEqualTo("ownerId", ownerId)
            .get().get();
        
        return query.getDocuments().stream()
            .map(doc -> {
                CodeRepository repo = doc.toObject(CodeRepository.class);
                repo.setId(doc.getId());
                return repo;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Find repositories by analysis status
     */
    public List<CodeRepository> findByAnalysisStatus(CodeRepository.AnalysisStatus status) 
            throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        QuerySnapshot query = db.collection(COLLECTION_NAME)
            .whereEqualTo("analysisStatus", status.toString())
            .get().get();
        
        return query.getDocuments().stream()
            .map(doc -> {
                CodeRepository repo = doc.toObject(CodeRepository.class);
                repo.setId(doc.getId());
                return repo;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Find repositories needing re-analysis (cache expired)
     */
    public List<CodeRepository> findExpiredCache() throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        QuerySnapshot query = db.collection(COLLECTION_NAME)
            .whereEqualTo("cached", true)
            .whereLessThan("cacheExpiresAt", new Date())
            .get().get();
        
        return query.getDocuments().stream()
            .map(doc -> {
                CodeRepository repo = doc.toObject(CodeRepository.class);
                repo.setId(doc.getId());
                return repo;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Find repositories with security issues above threshold
     */
    public List<CodeRepository> findBySecuritySeverity(String severity) 
            throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        
        // Note: Firestore doesn't support array-contains-any for nested fields easily
        // This would need a composite index or denormalized field
        QuerySnapshot query = db.collection(COLLECTION_NAME)
            .get().get();
        
        return query.getDocuments().stream()
            .map(doc -> {
                CodeRepository repo = doc.toObject(CodeRepository.class);
                repo.setId(doc.getId());
                return repo;
            })
            .filter(repo -> repo.getSecurityIssues() != null &&
                repo.getSecurityIssues().stream().anyMatch(issue ->
                    issue.getSeverity().equals(severity)))
            .collect(Collectors.toList());
    }
    
    /**
     * Delete repository
     */
    public void deleteById(String id) throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        db.collection(COLLECTION_NAME).document(id).delete().get();
    }
    
    /**
     * Update analysis status
     */
    public void updateAnalysisStatus(String id, CodeRepository.AnalysisStatus status) 
            throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        db.collection(COLLECTION_NAME).document(id)
            .update("analysisStatus", status.toString(), "updatedAt", new Date())
            .get();
    }
    
    /**
     * Update health score
     */
    public void updateHealthScore(String id, Integer score, String grade) 
            throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        Map<String, Object> updates = new HashMap<>();
        updates.put("healthScore", score);
        updates.put("healthGrade", grade);
        updates.put("updatedAt", new Date());
        
        db.collection(COLLECTION_NAME).document(id).update(updates).get();
    }
    
    /**
     * Add security issue
     */
    public void addSecurityIssue(String id, CodeRepository.SecurityIssue issue) 
            throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        db.collection(COLLECTION_NAME).document(id)
            .update("securityIssues", FieldValue.arrayUnion(issue))
            .get();
    }
    
    /**
     * Get repository count
     */
    public long count() throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        AggregateQuerySnapshot snapshot = db.collection(COLLECTION_NAME)
            .count()
            .get().get();
        return snapshot.getCount();
    }
}