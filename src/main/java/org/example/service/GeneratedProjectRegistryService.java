package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GeneratedProjectRegistryService {
    private static final Logger logger = LoggerFactory.getLogger(GeneratedProjectRegistryService.class);
    private static final String LOCAL_CACHE_PATH = "generated-projects/metadata.json";
    private static final String FIREBASE_PATH = "generated-projects/metadata";
    private static final String FIREBASE_RUNNING_PATH = "generated-projects/running";
    private static final String FIREBASE_FINISHED_PATH = "generated-projects/finished";

    private final FirebaseService firebaseService;
    private final LocalJsonStoreService jsonStore;
    private final Map<String, Map<String, Object>> projectStatuses = new ConcurrentHashMap<>();

    public GeneratedProjectRegistryService(FirebaseService firebaseService, LocalJsonStoreService jsonStore) {
        this.firebaseService = firebaseService;
        this.jsonStore = jsonStore;
    }

    @PostConstruct
    public void init() {
        if (!syncFromCloud()) {
            restoreFromLocal();
            logger.info("GeneratedProjectRegistryService restored {} generated projects from local cache", projectStatuses.size());
            return;
        }
        logger.info("GeneratedProjectRegistryService restored {} generated projects from Firebase", projectStatuses.size());
    }

    public synchronized List<Map<String, Object>> listProjects() {
        syncFromCloudIfAvailable();
        return new ArrayList<>(projectStatuses.values().stream().map(this::copyProject).toList());
    }

    public synchronized List<Map<String, Object>> listRunningProjects() {
        syncFromCloudIfAvailable();
        return projectStatuses.values().stream()
                .filter(this::isRunningStatus)
                .map(this::copyProject)
                .toList();
    }

    public synchronized List<Map<String, Object>> listFinishedProjects() {
        syncFromCloudIfAvailable();
        return projectStatuses.values().stream()
                .filter(this::isFinishedStatus)
                .map(this::copyProject)
                .toList();
    }

    public boolean isCloudStorageActive() {
        return firebaseService != null && firebaseService.isInitialized();
    }

    public synchronized Map<String, Object> getProject(String projectId) {
        syncFromCloudIfAvailable();
        Map<String, Object> project = projectStatuses.get(projectId);
        return project == null ? null : copyProject(project);
    }

    public synchronized void saveProject(Map<String, Object> projectStatus) {
        Map<String, Object> copy = copyProject(projectStatus);
        String projectId = String.valueOf(copy.get("projectId"));
        if (projectId == null || projectId.isBlank() || "null".equals(projectId)) {
            throw new IllegalArgumentException("projectId is required");
        }
        projectStatuses.put(projectId, copy);
        persistCloudFirst(projectId, copy);
        persistLocal();
    }

    public synchronized boolean removeProject(String projectId) {
        syncFromCloudIfAvailable();
        boolean removed = projectStatuses.remove(projectId) != null;
        if (firebaseService != null && firebaseService.isInitialized()) {
            try {
                firebaseService.getDatabase().getReference(FIREBASE_PATH).child(projectId).removeValueAsync().get();
                firebaseService.getDatabase().getReference(FIREBASE_RUNNING_PATH).child(projectId).removeValueAsync().get();
                firebaseService.getDatabase().getReference(FIREBASE_FINISHED_PATH).child(projectId).removeValueAsync().get();
            } catch (Exception exception) {
                logger.warn("Failed to delete generated project {} from Firebase: {}", projectId, exception.getMessage());
            }
        }
        persistLocal();
        return removed;
    }

    private void persistCloudFirst(String projectId, Map<String, Object> projectStatus) {
        if (firebaseService != null && firebaseService.isInitialized()) {
            try {
                firebaseService.getDatabase().getReference(FIREBASE_PATH).child(projectId).setValueAsync(projectStatus).get();
                if (isFinishedStatus(projectStatus)) {
                    firebaseService.getDatabase().getReference(FIREBASE_FINISHED_PATH).child(projectId).setValueAsync(projectStatus).get();
                    firebaseService.getDatabase().getReference(FIREBASE_RUNNING_PATH).child(projectId).removeValueAsync().get();
                } else {
                    firebaseService.getDatabase().getReference(FIREBASE_RUNNING_PATH).child(projectId).setValueAsync(projectStatus).get();
                    firebaseService.getDatabase().getReference(FIREBASE_FINISHED_PATH).child(projectId).removeValueAsync().get();
                }
            } catch (Exception exception) {
                logger.warn("Failed to write generated project {} to Firebase, keeping local cache in sync: {}", projectId, exception.getMessage());
            }
        }
    }

    private void persistLocal() {
        jsonStore.write(LOCAL_CACHE_PATH, new ArrayList<>(projectStatuses.values()));
    }

    private void restoreFromLocal() {
        List<Map<String, Object>> saved = jsonStore.read(
                LOCAL_CACHE_PATH,
                new TypeReference<List<Map<String, Object>>>() {},
                List.of());
        projectStatuses.clear();
        for (Map<String, Object> project : saved) {
            Map<String, Object> copy = copyProject(project);
            Object projectId = copy.get("projectId");
            if (projectId != null) {
                projectStatuses.put(String.valueOf(projectId), copy);
            }
        }
    }

    private void syncFromCloudIfAvailable() {
        if (firebaseService != null && firebaseService.isInitialized()) {
            syncFromCloud();
        }
    }

    private boolean syncFromCloud() {
        if (firebaseService == null || !firebaseService.isInitialized()) {
            return false;
        }
        try {
            CompletableFuture<DataSnapshot> future = new CompletableFuture<>();
            firebaseService.getDatabase().getReference(FIREBASE_PATH)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot snapshot) {
                            future.complete(snapshot);
                        }

                        @Override
                        public void onCancelled(DatabaseError error) {
                            future.completeExceptionally(error.toException());
                        }
                    });

            DataSnapshot snapshot = future.get();
            Map<String, Map<String, Object>> cloudProjects = new LinkedHashMap<>();
            if (snapshot.exists()) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    Object rawValue = child.getValue();
                    Map<String, Object> project = new LinkedHashMap<>();
                    if (rawValue instanceof Map<?, ?> rawMap) {
                        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
                            if (entry.getKey() != null) {
                                project.put(String.valueOf(entry.getKey()), entry.getValue());
                            }
                        }
                    }
                    project.put("projectId", child.getKey());
                    cloudProjects.put(child.getKey(), project);
                }
            }
            projectStatuses.clear();
            projectStatuses.putAll(cloudProjects);
            persistLocal();
            return true;
        } catch (Exception exception) {
            logger.warn("Failed to sync generated projects from Firebase: {}", exception.getMessage());
            return false;
        }
    }

    private Map<String, Object> copyProject(Map<String, Object> project) {
        return new LinkedHashMap<>(project);
    }

    private boolean isRunningStatus(Map<String, Object> project) {
        String status = String.valueOf(project.getOrDefault("status", ""));
        return "GENERATING".equals(status) || "TEMPLATE_INITIALIZED".equals(status) || "RUNNING".equals(status);
    }

    private boolean isFinishedStatus(Map<String, Object> project) {
        String status = String.valueOf(project.getOrDefault("status", ""));
        return "COMPLETED".equals(status) || "PUSHED_TO_REPO".equals(status) || "PUSH_FAILED".equals(status) || "FAILED".equals(status);
    }
}