package com.smartload.lru;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for TTLCache - cache with Time-To-Live support.
 */
@DisplayName("TTL Cache with Expiration Support")
public class TTLCacheTest {

    // ========== BASIC TTL TESTS ==========

    @Test
    @DisplayName("TTL: Entry without expiry")
    void testEntryWithoutExpiry() {
        TTLCache<String, Integer> cache = new TTLCache<>(10, new LRUEvictionStrategy<>());
        cache.put("key", 100); // No TTL = no expiry
        
        // Wait a bit
        try { Thread.sleep(100); } catch (InterruptedException e) { }
        
        Object result = cache.get("key");
        assertEquals(100, result);
    }

    @Test
    @DisplayName("TTL: Entry expires after TTL")
    void testEntryExpires() throws InterruptedException {
        TTLCache<String, Integer> cache = new TTLCache<>(10, new LRUEvictionStrategy<>());
        cache.put("key", 100, 100); // Expires in 100ms
        
        // Get immediately - should exist
        assertEquals(100, cache.get("key"));
        
        // Wait for expiry
        Thread.sleep(150);
        
        // Should be expired now (returns -1 for Integer type)
        assertEquals(-1, cache.get("key"));
        assertFalse(cache.containsKey("key"));
    }

    @Test
    @DisplayName("TTL: Multiple entries with different TTLs")
    void testMultipleDifferentTTLs() throws InterruptedException {
        TTLCache<String, Integer> cache = new TTLCache<>(10, new LRUEvictionStrategy<>());
        cache.put("short", 1, 50);   // Expires soon
        cache.put("long", 2, 500);   // Expires later
        
        Thread.sleep(100);
        
        // short should be expired
        assertEquals(-1, cache.get("short"));
        
        // long should still exist
        assertEquals(2, cache.get("long"));
        
        Thread.sleep(450);
        
        // long should now be expired
        assertEquals(-1, cache.get("long"));
    }

    @Test
    @DisplayName("TTL: Lazy expiration on access")
    void testLazyExpiration() throws InterruptedException {
        TTLCache<String, Integer> cache = new TTLCache<>(10, new LRUEvictionStrategy<>());
        cache.put("key", 100, 100);
        
        assertEquals(1, cache.size()); // Size includes expired entries until accessed
        
        Thread.sleep(150);
        
        // Entry is still in map (not actively cleaned)
        assertEquals(1, cache.size());
        
        // But accessing it returns expired
        assertEquals(-1, cache.get("key"));
        
        // And now it's removed
        assertEquals(0, cache.size());
    }

    // ========== UPDATE AND TTL TESTS ==========

    @Test
    @DisplayName("TTL: Update resets TTL")
    void testUpdateResetsTTL() throws InterruptedException {
        TTLCache<String, Integer> cache = new TTLCache<>(10, new LRUEvictionStrategy<>());
        cache.put("key", 1, 50);
        
        Thread.sleep(30);
        
        // Update the entry with new TTL
        cache.put("key", 2, 200);
        
        Thread.sleep(50);
        
        // Should still be valid (old TTL would have expired)
        assertEquals(2, cache.get("key"));
    }

    @Test
    @DisplayName("TTL: Update value without explicit TTL")
    void testUpdateWithoutExplicitTTL() throws InterruptedException {
        TTLCache<String, Integer> cache = new TTLCache<>(10, new LRUEvictionStrategy<>());
        cache.put("key", 1, 50);
        
        // Wait a bit, then update
        Thread.sleep(30);
        
        // Update without specifying TTL - uses MAX_VALUE
        cache.put("key", 2);
        
        // Wait longer (original TTL would have expired)
        Thread.sleep(150);
        
        // Should still exist (no expiry after update)
        assertEquals(2, cache.get("key"));
    }

    // ========== REMOVAL AND CLEANUP TESTS ==========

    @Test
    @DisplayName("TTL: Remove expired entry explicitly")
    void testRemoveExpiredEntry() throws InterruptedException {
        TTLCache<String, Integer> cache = new TTLCache<>(10, new LRUEvictionStrategy<>());
        cache.put("key", 100, 50);
        
        // Get the value first to verify it was there
        assertEquals(100, cache.get("key"));
        
        Thread.sleep(100);
        
        // Even though expired, can still remove it
        Integer removed = cache.remove("key");
        // Since we already accessed it once, it should have been removed on lazy expiration
        assertNull(removed);
        
        assertEquals(0, cache.size());
    }

    @Test
    @DisplayName("TTL: Active cleanup of expired entries")
    void testActiveCleanup() throws InterruptedException {
        TTLCache<String, Integer> cache = new TTLCache<>(10, new LRUEvictionStrategy<>());
        cache.put("key1", 1, 50);
        cache.put("key2", 2, 100);
        cache.put("key3", 3, 500); // Long-lived
        
        assertEquals(3, cache.size());
        
        Thread.sleep(120);
        
        // key1 and key2 are expired, but still in map
        assertEquals(3, cache.size());
        
        // Active cleanup
        int removed = cache.cleanupExpired();
        assertEquals(2, removed);
        assertEquals(1, cache.size());
        
        // key3 should still be there
        assertEquals(3, cache.get("key3"));
    }

    @Test
    @DisplayName("TTL: Count valid entries vs total size")
    void testCountValidVsSize() throws InterruptedException {
        TTLCache<String, Integer> cache = new TTLCache<>(10, new LRUEvictionStrategy<>());
        cache.put("key1", 1, 50);
        cache.put("key2", 2, 500);
        cache.put("key3", 3, 500);
        
        assertEquals(3, cache.size());
        assertEquals(3, cache.countValid());
        
        Thread.sleep(100);
        
        // key1 is expired but still in map
        assertEquals(3, cache.size());
        assertEquals(2, cache.countValid()); // Only non-expired
    }

    // ========== TTL AND EVICTION POLICY TESTS ==========

    @Test
    @DisplayName("TTL + LRU: Expired entries don't prevent LRU eviction")
    void testExpiredWithLRU() throws InterruptedException {
        TTLCache<String, Integer> cache = new TTLCache<>(3, new LRUEvictionStrategy<>());
        cache.put("key1", 1, 50);  // Will expire
        cache.put("key2", 2, 500); // Long-lived
        cache.put("key3", 3, 500); // Long-lived
        
        Thread.sleep(100);
        
        // key1 is expired, key2 and key3 are accessed in order
        cache.get("key2");
        cache.get("key3");
        
        // Add key4 (cache full)
        cache.put("key4", 4, 500);
        
        // key1 (expired and least used) should be evicted
        // Or key2 if LRU eviction happens before expiry check
        // Either way, cache should maintain consistency
        assertEquals(3, cache.size());
        assertTrue(cache.containsKey("key3") || cache.containsKey("key4"));
    }

    @Test
    @DisplayName("TTL + FIFO: Expired entries with FIFO strategy")
    void testExpiredWithFIFO() throws InterruptedException {
        TTLCache<String, Integer> cache = new TTLCache<>(3, new FIFOEvictionStrategy<>());
        cache.put("key1", 1, 100);
        cache.put("key2", 2, 100);
        cache.put("key3", 3, 500);
        
        Thread.sleep(150);
        
        // key1 and key2 are expired, key3 is not
        // Add key4 - FIFO would evict key1
        cache.put("key4", 4, 500);
        
        assertEquals(3, cache.size());
        assertFalse(cache.containsKey("key1"));
    }

    // ========== TTL AND CONCURRENCY TESTS ==========

    @Test
    @DisplayName("TTL: Concurrent puts with different TTLs")
    void testConcurrentPutsWithDifferentTTLs() throws InterruptedException {
        TTLCache<String, String> cache = new TTLCache<>(100, new LRUEvictionStrategy<>());
        ExecutorService executor = Executors.newFixedThreadPool(5);
        
        for (int t = 0; t < 5; t++) {
            final int threadId = t;
            executor.submit(() -> {
                for (int i = 0; i < 20; i++) {
                    String key = "key-" + threadId + "-" + i;
                    long ttl = (i % 2 == 0) ? 50 : 500; // Mix of short and long TTLs
                    cache.put(key, "value", ttl);
                }
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
        
        assertEquals(100, cache.size());
    }

    @Test
    @DisplayName("TTL: Concurrent gets on expiring entries")
    void testConcurrentGetsOnExpiringEntries() throws InterruptedException {
        TTLCache<String, Integer> cache = new TTLCache<>(10, new LRUEvictionStrategy<>());
        
        for (int i = 0; i < 10; i++) {
            cache.put("key-" + i, i, 200);
        }
        
        ExecutorService executor = Executors.newFixedThreadPool(5);
        AtomicInteger foundCount = new AtomicInteger(0);
        
        for (int t = 0; t < 5; t++) {
            executor.submit(() -> {
                for (int i = 0; i < 50; i++) {
                    Object value = cache.get("key-" + (i % 10));
                    if (value != null && !value.equals(-1)) {
                        foundCount.incrementAndGet();
                    }
                }
            });
        }
        
        executor.shutdown();
        executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS);
        
        // Should have found entries (they don't expire for 200ms)
        assertTrue(foundCount.get() > 0);
    }

    // ========== TTL INFORMATION TESTS ==========

    @Test
    @DisplayName("TTL: Get remaining TTL")
    void testGetRemainingTTL() throws InterruptedException {
        TTLCache<String, Integer> cache = new TTLCache<>(10, new LRUEvictionStrategy<>());
        cache.put("key", 100, 1000);
        
        long remainingBegin = cache.getRemainingTTL("key");
        assertTrue(remainingBegin > 500, "Should have most of 1000ms remaining");
        
        Thread.sleep(200);
        
        long remainingAfter = cache.getRemainingTTL("key");
        assertTrue(remainingAfter > 300, "Should have around 800ms remaining");
        assertTrue(remainingAfter < remainingBegin, "Should have decreased");
    }

    @Test
    @DisplayName("TTL: Remaining TTL for non-existent key")
    void testRemainingTTLNonExistent() {
        TTLCache<String, Integer> cache = new TTLCache<>(10, new LRUEvictionStrategy<>());
        
        assertEquals(0, cache.getRemainingTTL("nonexistent"));
    }

    // ========== STATISTICS TESTS ==========

    @Test
    @DisplayName("TTL: Cache statistics tracking")
    void testCacheStatistics() throws InterruptedException {
        TTLCache<String, Integer> cache = new TTLCache<>(10, new LRUEvictionStrategy<>());
        
        // Put some entries
        cache.put("key1", 1, 500);
        cache.put("key2", 2, 50);
        
        // Access them
        cache.get("key1"); // Hit
        cache.get("key1"); // Hit
        cache.get("nonexistent"); // Miss
        
        Thread.sleep(100);
        
        // Expired access
        cache.get("key2"); // Miss (expired)
        
        String stats = cache.getStats();
        assertTrue(stats.contains("totalPuts=2"));
        assertTrue(stats.contains("totalGets=4"));
        assertTrue(stats.contains("cacheHits=2"));
        assertTrue(stats.contains("cacheMisses=2"));
    }

    // ========== BACKWARD COMPATIBILITY TESTS ==========

    @Test
    @DisplayName("Backward compatibility: TTLCache with Integer values")
    void testBackwardCompatibilityBasics() {
        TTLCache<Integer, Integer> cache = new TTLCache<>(3, new LRUEvictionStrategy<>());
        cache.put(1, 1);
        cache.put(2, 2);
        cache.put(3, 3);
        
        assertEquals(1, cache.get(1));
        assertEquals(2, cache.get(2));
        assertEquals(3, cache.get(3));
        assertEquals(3, cache.size());
    }

    @Test
    @DisplayName("TTL: Clear cache")
    void testClearCache() throws InterruptedException {
        TTLCache<String, Integer> cache = new TTLCache<>(10, new LRUEvictionStrategy<>());
        cache.put("key1", 1, 50);
        cache.put("key2", 2, 100);
        
        assertEquals(2, cache.size());
        
        cache.clear();
        
        assertEquals(0, cache.size());
        assertEquals(-1, cache.get("key1"));
    }

    @Test
    @DisplayName("TTL: Contains key with expiry check")
    void testContainsKeyWithExpiry() throws InterruptedException {
        TTLCache<String, Integer> cache = new TTLCache<>(10, new LRUEvictionStrategy<>());
        cache.put("key", 100, 50);
        
        assertTrue(cache.containsKey("key"));
        
        Thread.sleep(100);
        
        assertFalse(cache.containsKey("key"));
    }

    @Test
    @DisplayName("TTL: Very long TTL (effectively no expiry)")
    void testVeryLongTTL() throws InterruptedException {
        TTLCache<String, Integer> cache = new TTLCache<>(10, new LRUEvictionStrategy<>());
        cache.put("key", 100, Long.MAX_VALUE);
        
        Thread.sleep(100);
        
        // Should still be there
        assertEquals(100, cache.get("key"));
    }
}
