package com.supremeai.intelligence;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for LightningCache. Tests L1 cache operations, size management, and eviction behavior.
 */
class LightningCacheTest {

  @Test
  void testGetCachedAnalysis_emptyCache() {
    LightningCache cache = new LightningCache();

    String result = cache.getCachedAnalysis("hash123");

    assertNull(result);
  }

  @Test
  void testCacheResult_andRetrieve() {
    LightningCache cache = new LightningCache();

    cache.cacheResult("hash1", "analysis result");
    String retrieved = cache.getCachedAnalysis("hash1");

    assertEquals("analysis result", retrieved);
  }

  @Test
  void testCacheResult_overwritesExisting() {
    LightningCache cache = new LightningCache();

    cache.cacheResult("hash1", "first result");
    cache.cacheResult("hash1", "updated result");

    String retrieved = cache.getCachedAnalysis("hash1");
    assertEquals("updated result", retrieved);
  }

  @Test
  void testCacheResult_multipleHashes() {
    LightningCache cache = new LightningCache();

    cache.cacheResult("hash1", "result1");
    cache.cacheResult("hash2", "result2");
    cache.cacheResult("hash3", "result3");

    assertEquals("result1", cache.getCachedAnalysis("hash1"));
    assertEquals("result2", cache.getCachedAnalysis("hash2"));
    assertEquals("result3", cache.getCachedAnalysis("hash3"));
  }

  @Test
  void testCacheResult_nullHash() {
    LightningCache cache = new LightningCache();

    assertDoesNotThrow(
        () -> {
          cache.cacheResult(null, "result");
        });

    // Null key should put entry with null key
    assertNull(cache.getCachedAnalysis(null));
  }

  @Test
  void testCacheResult_nullValue() {
    LightningCache cache = new LightningCache();

    cache.cacheResult("hash", null);

    assertNull(cache.getCachedAnalysis("hash"));
  }

  @Test
  void testCacheResult_emptyStringKeys() {
    LightningCache cache = new LightningCache();

    cache.cacheResult("", "empty");
    cache.cacheResult("   ", "spaces");

    assertEquals("empty", cache.getCachedAnalysis(""));
    assertEquals("spaces", cache.getCachedAnalysis("   "));
  }

  @Test
  void testCacheEviction_whenSizeExceeds10000() {
    LightningCache cache = new LightningCache();

    // Fill up cache beyond 10000
    for (int i = 0; i < 10001; i++) {
      cache.cacheResult("hash" + i, "value" + i);
    }

    // The last insertion should trigger clear
    // After clear, only the latest entry remains
    assertEquals("value10000", cache.getCachedAnalysis("hash10000"));
    assertNull(cache.getCachedAnalysis("hash1"));
  }

  @Test
  void testCacheEviction_doesNotEvictBeforeLimit() {
    LightningCache cache = new LightningCache();

    for (int i = 0; i < 100; i++) {
      cache.cacheResult("hash" + i, "value" + i);
    }

    assertEquals("value0", cache.getCachedAnalysis("hash0"));
    assertEquals("value99", cache.getCachedAnalysis("hash99"));
  }

  @Test
  void testCacheEviction_multipleEvictionCycles() {
    LightningCache cache = new LightningCache();

    // First batch
    for (int i = 0; i < 10002; i++) {
      cache.cacheResult("batch1_" + i, "val1_" + i);
    }
    // Should be cleared, only last entry remains
    assertEquals("val1_10001", cache.getCachedAnalysis("batch1_10001"));

    // Second batch
    for (int i = 0; i < 10002; i++) {
      cache.cacheResult("batch2_" + i, "val2_" + i);
    }
    assertEquals("val2_10001", cache.getCachedAnalysis("batch2_10001"));
    assertNull(cache.getCachedAnalysis("batch1_0"));
  }

  @Test
  void testEmptyCache() {
    LightningCache cache = new LightningCache();

    String result = cache.getCachedAnalysis("hash123");

    assertNull(result);
  }

  @Test
  void testCacheReusesSameHashKey() {
    LightningCache cache = new LightningCache();

    cache.cacheResult("same", "first");
    assertEquals("first", cache.getCachedAnalysis("same"));

    cache.cacheResult("same", "second");
    assertEquals("second", cache.getCachedAnalysis("same"));
  }
}
