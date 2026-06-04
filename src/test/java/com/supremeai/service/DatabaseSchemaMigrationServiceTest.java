package com.supremeai.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;

class DatabaseSchemaMigrationServiceTest {

  @Mock private FirebaseRealtimeService firebaseRealtimeService;

  @Mock private Firestore firestore;

  @Mock private CollectionReference collectionReference;

  @Mock private QuerySnapshot querySnapshot;

  @Mock private QueryDocumentSnapshot documentSnapshot;

  @Mock private DocumentReference documentReference;

  @Mock private WriteBatch writeBatch;

  @InjectMocks private DatabaseSchemaMigrationService migrationService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  @SuppressWarnings("unchecked")
  void testRunAutoMigrations_PendingMigration_Success()
      throws ExecutionException, InterruptedException {
    // Mock RTDB showing no previous migration applied
    when(firebaseRealtimeService.getData("migration_status"))
        .thenReturn(Mono.just(Collections.emptyMap()));
    when(firebaseRealtimeService.setData(anyString(), any())).thenReturn(Mono.empty());

    // Mock Firestore structures
    when(firestore.collection("system_learning")).thenReturn(collectionReference);

    ApiFuture<QuerySnapshot> getFuture = mock(ApiFuture.class);
    when(collectionReference.get()).thenReturn(getFuture);
    when(getFuture.get()).thenReturn(querySnapshot);

    when(querySnapshot.getDocuments()).thenReturn(List.of(documentSnapshot));

    // Mock document needing migration
    when(documentSnapshot.getId()).thenReturn("learning_doc_123");
    when(documentSnapshot.contains("schemaVersion")).thenReturn(false);
    when(documentSnapshot.getReference()).thenReturn(documentReference);
    when(documentSnapshot.getLong("timesApplied")).thenReturn(null);
    when(documentSnapshot.getBoolean("permanent")).thenReturn(null);

    // Mock WriteBatch
    when(firestore.batch()).thenReturn(writeBatch);
    when(writeBatch.update(
            any(DocumentReference.class),
            anyString(),
            any(),
            anyString(),
            any(),
            anyString(),
            any()))
        .thenReturn(writeBatch);

    ApiFuture<List<WriteResult>> commitFuture = mock(ApiFuture.class);
    when(writeBatch.commit()).thenReturn(commitFuture);
    try {
      when(commitFuture.get()).thenReturn(Collections.emptyList());
    } catch (Exception e) {
    }

    // CountDownLatch to wait for the async execution thread
    java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
    when(firebaseRealtimeService.setData(eq("migration_status/v2_firestore_applied"), any()))
        .thenAnswer(
            invocation -> {
              latch.countDown();
              return Mono.empty();
            });

    // Run migrations
    migrationService.runAutoMigrations();

    // Wait for async task to trigger the latch countdown
    boolean completed = latch.await(5, java.util.concurrent.TimeUnit.SECONDS);
    org.junit.jupiter.api.Assertions.assertTrue(completed, "Async migration task timed out");

    // Verify Firestore query and batch update were performed
    verify(firestore, times(1)).collection("system_learning");
    verify(writeBatch, times(1)).commit();

    // Verify RTDB state is updated to mark migration as completed
    verify(firebaseRealtimeService, times(1))
        .setData("migration_status/v2_firestore_applied", true);
  }

  @Test
  void testRunAutoMigrations_AlreadyApplied_NoAction() {
    // Mock RTDB showing migration already applied
    when(firebaseRealtimeService.getData("migration_status"))
        .thenReturn(Mono.just(Map.of("v2_firestore_applied", true)));

    // Run migrations
    migrationService.runAutoMigrations();

    // Verify no Firestore batch write was triggered
    verifyNoInteractions(firestore);
  }
}
