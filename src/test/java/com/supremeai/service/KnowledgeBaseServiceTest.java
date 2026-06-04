package com.supremeai.service;

import static org.junit.jupiter.api.Assertions.*;

import com.supremeai.model.KnowledgeEntry;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseServiceTest {

  private KnowledgeBaseService service;

  @BeforeEach
  void setUp() {
    service = new KnowledgeBaseService();
  }

  @Test
  void learn_shouldStoreEntryInKnowledgeStore() {
    String topic = "TestTopic";
    String pattern = "Pattern";
    String solution = "Solution";
    String provider = "openai";
    double score = 0.9;

    service.learn(topic, pattern, solution, provider, score);

    List<KnowledgeEntry> all = service.getAllKnowledge();
    assertEquals(1, all.size());
    KnowledgeEntry entry = all.get(0);
    assertEquals(topic, entry.getTopic());
    assertEquals(pattern, entry.getPattern());
    assertEquals(solution, entry.getSolution());
    assertEquals(provider, entry.getSourceProvider());
    assertEquals(score, entry.getConfidenceScore(), 0.01);
  }

  @Test
  void searchKnowledge_shouldFindSimilarEntries() {
    service.learn("java", "sorting", "use Arrays.sort()", "openai", 0.9);
    service.learn("python", "loops", "use for loops", "openai", 0.8);

    List<KnowledgeEntry> results = service.searchKnowledge("java sorting");

    assertNotNull(results);
    assertFalse(results.isEmpty());
  }

  @Test
  void searchKnowledge_shouldReturnEmptyForNoMatch() {
    service.learn("topic1", "pattern1", "solution1", "openai", 0.9);

    List<KnowledgeEntry> results = service.searchKnowledge("completely unrelated query xyzzy");

    assertNotNull(results);
    assertTrue(results.isEmpty());
  }

  @Test
  void getAllKnowledge_shouldReturnEmptyInitially() {
    List<KnowledgeEntry> all = service.getAllKnowledge();

    assertNotNull(all);
    assertTrue(all.isEmpty());
  }

  @Test
  void learn_multipleEntries_shouldReturnAll() {
    service.learn("t1", "p1", "s1", "p", 0.9);
    service.learn("t2", "p2", "s2", "p", 0.8);
    service.learn("t3", "p3", "s3", "p", 0.7);

    List<KnowledgeEntry> all = service.getAllKnowledge();

    assertEquals(3, all.size());
  }
}
