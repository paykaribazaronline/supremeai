package com.supremeai.codeflow.repository;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.supremeai.codeflow.model.CodeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CodeFlowRepositoryTest {

    private CodeFlowRepository repository;

    @Mock
    private Firestore mockFirestore;

    @Mock
    private DocumentReference mockDocRef;

    @Mock
    private DocumentSnapshot mockDocSnapshot;

    @Mock
    private AggregateQuerySnapshot mockAggregateSnapshot;

    private MockedStatic<FirestoreClient> firestoreClientMock;

    @BeforeEach
    void setUp() {
        repository = new CodeFlowRepository();
        firestoreClientMock = mockStatic(FirestoreClient.class);
        firestoreClientMock.when(FirestoreClient::getFirestore).thenReturn(mockFirestore);
    }

    @AfterEach
    void tearDown() {
        firestoreClientMock.close();
    }

    @Test
    void save_shouldGenerateIdAndPersist() throws ExecutionException, InterruptedException {
        CodeRepository repo = CodeRepository.builder()
                .name("test-repo")
                .fullName("owner/test-repo")
                .build();

        com.google.cloud.firestore.CollectionReference mockColl = mock(com.google.cloud.firestore.CollectionReference.class);
        when(mockFirestore.collection("codeflow/repositories")).thenReturn(mockColl);
        when(mockColl.document(anyString())).thenReturn(mockDocRef);
        when(mockDocRef.set(any(CodeRepository.class))).thenReturn(mock(ApiFuture.class));

        CodeRepository saved = repository.save(repo);

        assertNotNull(saved.getId());
        assertEquals("test-repo", saved.getName());
        verify(mockColl).document(saved.getId());
    }

    @Test
    void save_shouldUseExistingId_whenIdProvided() throws ExecutionException, InterruptedException {
        CodeRepository repo = CodeRepository.builder()
                .id("existing-id")
                .name("existing-repo")
                .build();

        com.google.cloud.firestore.CollectionReference mockColl = mock(com.google.cloud.firestore.CollectionReference.class);
        when(mockFirestore.collection("codeflow/repositories")).thenReturn(mockColl);
        when(mockColl.document("existing-id")).thenReturn(mockDocRef);
        when(mockDocRef.set(any(CodeRepository.class))).thenReturn(mock(ApiFuture.class));

        CodeRepository saved = repository.save(repo);

        assertEquals("existing-id", saved.getId());
        verify(mockColl).document("existing-id");
    }

    @Test
    void findById_shouldReturnRepository_whenExists() throws ExecutionException, InterruptedException {
        CodeRepository expected = CodeRepository.builder()
                .id("repo-1")
                .name("my-repo")
                .build();

        com.google.cloud.firestore.CollectionReference mockColl = mock(com.google.cloud.firestore.CollectionReference.class);
        when(mockFirestore.collection("codeflow/repositories")).thenReturn(mockColl);
        when(mockColl.document("repo-1")).thenReturn(mockDocRef);

        ApiFuture<DocumentSnapshot> future = mock(ApiFuture.class);
        when(mockDocRef.get()).thenReturn(future);
        when(future.get()).thenReturn(mockDocSnapshot);
        when(mockDocSnapshot.exists()).thenReturn(true);
        when(mockDocSnapshot.toObject(CodeRepository.class)).thenReturn(expected);
        when(mockDocSnapshot.getId()).thenReturn("repo-1");

        Optional<CodeRepository> result = repository.findById("repo-1");

        assertTrue(result.isPresent());
        assertEquals("repo-1", result.get().getId());
        assertEquals("my-repo", result.get().getName());
    }

    @Test
    void findById_shouldReturnEmpty_whenNotFound() throws ExecutionException, InterruptedException {
        com.google.cloud.firestore.CollectionReference mockColl = mock(com.google.cloud.firestore.CollectionReference.class);
        when(mockFirestore.collection("codeflow/repositories")).thenReturn(mockColl);
        when(mockColl.document("missing")).thenReturn(mockDocRef);

        ApiFuture<DocumentSnapshot> future = mock(ApiFuture.class);
        when(mockDocRef.get()).thenReturn(future);
        when(future.get()).thenReturn(mockDocSnapshot);
        when(mockDocSnapshot.exists()).thenReturn(false);

        Optional<CodeRepository> result = repository.findById("missing");

        assertFalse(result.isPresent());
    }

    @Test
    void findBySourceId_shouldReturnRepository_whenFound() throws ExecutionException, InterruptedException {
        CodeRepository expected = CodeRepository.builder()
                .id("repo-2")
                .sourceId("github-123")
                .name("source-repo")
                .build();

        com.google.cloud.firestore.CollectionReference mockColl = mock(com.google.cloud.firestore.CollectionReference.class);
        Query mockQuery = mock(Query.class);
        QuerySnapshot mockQuerySnapshot = mock(QuerySnapshot.class);
        QueryDocumentSnapshot doc = mock(QueryDocumentSnapshot.class);

        when(mockFirestore.collection("codeflow/repositories")).thenReturn(mockColl);
        when(mockColl.whereEqualTo("sourceId", "github-123")).thenReturn(mockQuery);
        when(mockQuery.limit(1)).thenReturn(mockQuery);

        ApiFuture<QuerySnapshot> future = mock(ApiFuture.class);
        when(mockQuery.get()).thenReturn(future);
        when(future.get()).thenReturn(mockQuerySnapshot);
        when(mockQuerySnapshot.isEmpty()).thenReturn(false);
        when(mockQuerySnapshot.getDocuments()).thenReturn(List.of(doc));
        when(doc.toObject(CodeRepository.class)).thenReturn(expected);
        when(doc.getId()).thenReturn("repo-2");

        Optional<CodeRepository> result = repository.findBySourceId("github-123");

        assertTrue(result.isPresent());
        assertEquals("github-123", result.get().getSourceId());
    }

    @Test
    void findBySourceId_shouldReturnEmpty_whenNotFound() throws ExecutionException, InterruptedException {
        com.google.cloud.firestore.CollectionReference mockColl = mock(com.google.cloud.firestore.CollectionReference.class);
        Query mockQuery = mock(Query.class);
        QuerySnapshot mockQuerySnapshot = mock(QuerySnapshot.class);

        when(mockFirestore.collection("codeflow/repositories")).thenReturn(mockColl);
        when(mockColl.whereEqualTo("sourceId", "unknown")).thenReturn(mockQuery);
        when(mockQuery.limit(1)).thenReturn(mockQuery);

        ApiFuture<QuerySnapshot> future = mock(ApiFuture.class);
        when(mockQuery.get()).thenReturn(future);
        when(future.get()).thenReturn(mockQuerySnapshot);
        when(mockQuerySnapshot.isEmpty()).thenReturn(true);

        Optional<CodeRepository> result = repository.findBySourceId("unknown");

        assertFalse(result.isPresent());
    }

    @Test
    void findByOwnerId_shouldReturnRepositories() throws ExecutionException, InterruptedException {
        CodeRepository r1 = CodeRepository.builder().id("r1").name("repo-a").build();
        CodeRepository r2 = CodeRepository.builder().id("r2").name("repo-b").build();

        com.google.cloud.firestore.CollectionReference mockColl = mock(com.google.cloud.firestore.CollectionReference.class);
        Query mockQuery = mock(Query.class);
        QuerySnapshot mockQuerySnapshot = mock(QuerySnapshot.class);
        QueryDocumentSnapshot d1 = mock(QueryDocumentSnapshot.class);
        QueryDocumentSnapshot d2 = mock(QueryDocumentSnapshot.class);

        when(mockFirestore.collection("codeflow/repositories")).thenReturn(mockColl);
        when(mockColl.whereEqualTo("ownerId", "owner-1")).thenReturn(mockQuery);

        ApiFuture<QuerySnapshot> future = mock(ApiFuture.class);
        when(mockQuery.get()).thenReturn(future);
        when(future.get()).thenReturn(mockQuerySnapshot);
        when(mockQuerySnapshot.getDocuments()).thenReturn(List.of(d1, d2));
        when(d1.toObject(CodeRepository.class)).thenReturn(r1);
        when(d1.getId()).thenReturn("r1");
        when(d2.toObject(CodeRepository.class)).thenReturn(r2);
        when(d2.getId()).thenReturn("r2");

        List<CodeRepository> result = repository.findByOwnerId("owner-1");

        assertEquals(2, result.size());
    }

    @Test
    void findByOwnerId_shouldReturnEmpty_whenNoneFound() throws ExecutionException, InterruptedException {
        com.google.cloud.firestore.CollectionReference mockColl = mock(com.google.cloud.firestore.CollectionReference.class);
        Query mockQuery = mock(Query.class);
        QuerySnapshot mockQuerySnapshot = mock(QuerySnapshot.class);

        when(mockFirestore.collection("codeflow/repositories")).thenReturn(mockColl);
        when(mockColl.whereEqualTo("ownerId", "no-owner")).thenReturn(mockQuery);

        ApiFuture<QuerySnapshot> future = mock(ApiFuture.class);
        when(mockQuery.get()).thenReturn(future);
        when(future.get()).thenReturn(mockQuerySnapshot);
        when(mockQuerySnapshot.getDocuments()).thenReturn(new ArrayList<>());

        List<CodeRepository> result = repository.findByOwnerId("no-owner");

        assertTrue(result.isEmpty());
    }

    @Test
    void findByAnalysisStatus_shouldReturnMatchingRepositories() throws ExecutionException, InterruptedException {
        CodeRepository r = CodeRepository.builder().id("r3").name("analyzed-repo").build();

        com.google.cloud.firestore.CollectionReference mockColl = mock(com.google.cloud.firestore.CollectionReference.class);
        Query mockQuery = mock(Query.class);
        QuerySnapshot mockQuerySnapshot = mock(QuerySnapshot.class);
        QueryDocumentSnapshot queryDoc = mock(QueryDocumentSnapshot.class);

        when(mockFirestore.collection("codeflow/repositories")).thenReturn(mockColl);
        when(mockColl.whereEqualTo("analysisStatus", "COMPLETED")).thenReturn(mockQuery);

        ApiFuture<QuerySnapshot> future = mock(ApiFuture.class);
        when(mockQuery.get()).thenReturn(future);
        when(future.get()).thenReturn(mockQuerySnapshot);
        when(mockQuerySnapshot.getDocuments()).thenReturn(List.of(queryDoc));
        when(queryDoc.toObject(CodeRepository.class)).thenReturn(r);
        when(queryDoc.getId()).thenReturn("r3");

        List<CodeRepository> result = repository.findByAnalysisStatus(CodeRepository.AnalysisStatus.COMPLETED);

        assertEquals(1, result.size());
    }

    @Test
    void deleteById_shouldRemoveDocument() throws ExecutionException, InterruptedException {
        com.google.cloud.firestore.CollectionReference mockColl = mock(com.google.cloud.firestore.CollectionReference.class);
        when(mockFirestore.collection("codeflow/repositories")).thenReturn(mockColl);
        when(mockColl.document("del-1")).thenReturn(mockDocRef);
        when(mockDocRef.delete()).thenReturn(mock(ApiFuture.class));

        repository.deleteById("del-1");

        verify(mockDocRef).delete();
    }

    @Test
    void updateAnalysisStatus_shouldUpdateFields() throws ExecutionException, InterruptedException {
        com.google.cloud.firestore.CollectionReference mockColl = mock(com.google.cloud.firestore.CollectionReference.class);

        when(mockFirestore.collection("codeflow/repositories")).thenReturn(mockColl);
        when(mockColl.document("upd-1")).thenReturn(mockDocRef);
        when(mockDocRef.update(eq("analysisStatus"), eq("ANALYZING"), eq("updatedAt"), any(Date.class)))
                .thenReturn(mock(ApiFuture.class));

        repository.updateAnalysisStatus("upd-1", CodeRepository.AnalysisStatus.ANALYZING);

        verify(mockDocRef).update(eq("analysisStatus"), eq("ANALYZING"), eq("updatedAt"), any(Date.class));
    }

    @Test
    void updateHealthScore_shouldUpdateScoreAndGrade() throws ExecutionException, InterruptedException {
        com.google.cloud.firestore.CollectionReference mockColl = mock(com.google.cloud.firestore.CollectionReference.class);

        when(mockFirestore.collection("codeflow/repositories")).thenReturn(mockColl);
        when(mockColl.document("hlt-1")).thenReturn(mockDocRef);
        when(mockDocRef.update(anyMap())).thenReturn(mock(ApiFuture.class));

        repository.updateHealthScore("hlt-1", 85, "B");

        verify(mockDocRef).update(argThat((Map<String, Object> map) ->
                map.containsKey("healthScore") && map.containsKey("healthGrade") && map.containsKey("updatedAt")
        ));
    }

    @Test
    void count_shouldReturnDocumentCount() throws ExecutionException, InterruptedException {
        com.google.cloud.firestore.CollectionReference mockColl = mock(com.google.cloud.firestore.CollectionReference.class);
        AggregateQuery mockAggQuery = mock(AggregateQuery.class);

        when(mockFirestore.collection("codeflow/repositories")).thenReturn(mockColl);
        when(mockColl.count()).thenReturn(mockAggQuery);

        ApiFuture<AggregateQuerySnapshot> future = mock(ApiFuture.class);
        when(mockAggQuery.get()).thenReturn(future);
        when(future.get()).thenReturn(mockAggregateSnapshot);
        when(mockAggregateSnapshot.getCount()).thenReturn(42L);

        long count = repository.count();

        assertEquals(42L, count);
    }
}
