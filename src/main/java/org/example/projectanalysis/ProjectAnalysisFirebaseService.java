package org.example.projectanalysis;

import com.google.firebase.database.*;
import org.example.service.FirebaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.CompletableFuture;

@Service
public class ProjectAnalysisFirebaseService {
    
    @Autowired
    private FirebaseService firebaseService;
    
    private static final String ANALYSIS_PATH = "project_analyses";
    
    public void saveAnalysis(ProjectAnalysis analysis) {
        if (!firebaseService.isInitialized()) {
            System.out.println("⚠️ Firebase not initialized, skipping save");
            return;
        }
        
        DatabaseReference ref = firebaseService.getDatabase().getReference(ANALYSIS_PATH).child(analysis.getId());
        ref.setValueAsync(analysis.toFirebaseMap());
        
        // Save detailed data as child nodes
        ref.child("issues").setValueAsync(analysis.getIssues());
        ref.child("suggestions").setValueAsync(analysis.getSuggestions());
        ref.child("architecture").setValueAsync(analysis.getArchitecture());
        ref.child("qualityMetrics").setValueAsync(analysis.getQualityMetrics());
        
        System.out.println("✅ Analysis saved to Firebase: " + analysis.getId());
    }
    
    public ProjectAnalysis getAnalysis(String id) throws Exception {
        CompletableFuture<DataSnapshot> future = new CompletableFuture<>();
        firebaseService.getDatabase().getReference(ANALYSIS_PATH).child(id)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(DataSnapshot snapshot) { future.complete(snapshot); }
                @Override public void onCancelled(DatabaseError error) { future.completeExceptionally(error.toException()); }
            });
        
        DataSnapshot snapshot = future.get();
        return snapshot.getValue(ProjectAnalysis.class);
    }
    
    public List<Map<String, Object>> getAllAnalyses() throws Exception {
        CompletableFuture<DataSnapshot> future = new CompletableFuture<>();
        firebaseService.getDatabase().getReference(ANALYSIS_PATH)
            .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override public void onDataChange(DataSnapshot snapshot) { future.complete(snapshot); }
                @Override public void onCancelled(DatabaseError error) { future.completeExceptionally(error.toException()); }
            });
        
        DataSnapshot snapshot = future.get();
        List<Map<String, Object>> analyses = new ArrayList<>();
        
        for (DataSnapshot child : snapshot.getChildren()) {
            analyses.add((Map<String, Object>) child.getValue());
        }
        return analyses;
    }
}
