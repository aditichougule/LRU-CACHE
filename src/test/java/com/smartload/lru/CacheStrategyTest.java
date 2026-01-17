package com.smartload.lru;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test suite for the pluggable Cache implementation with different eviction strategies.
 */
@DisplayName("Cache with Pluggable Eviction Strategies")
public class CacheStrategyTest {

    // ========== LRU STRATEGY TESTS ==========

    @Test
    @DisplayName("LRU: Basic put and get operations")
    void testLRUBasicPutGet() {
        Cache<Integer, Integer> cache = new Cache<>(2, new LRUEvictionStrategy<>());
        cache.put(1, 1);
        cache.put(2, 2);
        assertEquals(1, cache.get(1));
        cache.put(3, 3);
        // Key 2 should be evicted (least recently used)
        assertEquals(-1, cache.get(2));
        cache.put(4, 4);
        // Key 1 should be evicted
        assertEquals(-1, cache.get(1));
        assertEquals(3, cache.get(3));
        assertEquals(4, cache.get(4));
    }

    @Test
    @DisplayName("LRU: Update moves to head")
    void testLRUUpdateMovesToHead() {
        Cache<Integer, Integer> cache = new Cache<>(2, new LRUEvictionStrategy<>());
        cache.put(1, 1);
        cache.put(2, 2);
        // Access key 1, making it most recently used
        cache.get(1);
        cache.put(3, 3);
        // Key 2 should be evicted (was least recently used)
        assertEquals(-1, cache.get(2));
        assertEquals(1, cache.get(1));
        assertEquals(3, cache.get(3));
    }

    @Test
    @DisplayName("LRU: Capacity one")
    void testLRUCapacityOne() {
        Cache<Integer, Integer> cache = new Cache<>(1, new LRUEvictionStrategy<>());
        cache.put(1, 1);
        cache.put(2, 2);
        assertEquals(-1, cache.get(1));
        assertEquals(2, cache.get(2));
    }

    // ========== FIFO STRATEGY TESTS ==========

    @Test
    @DisplayName("FIFO: Basic put and get operations")
    void testFIFOBasicPutGet() {
        Cache<Integer, Integer> cache = new Cache<>(2, new FIFOEvictionStrategy<>());
        cache.put(1, 1);
        cache.put(2, 2);
        assertEquals(1, cache.get(1));
        cache.put(3, 3);
        // Key 1 should be evicted (first inserted, FIFO)
        assertEquals(-1, cache.get(1));
        // Even accessing key 2 doesn't prevent it from being evicted
        cache.put(4, 4);
        assertEquals(-1, cache.get(2)); // Key 2 evicted (oldest after key 1)
        assertEquals(3, cache.get(3));
        assertEquals(4, cache.get(4));
    }

    @Test
    @DisplayName("FIFO: Access doesn't affect eviction order")
    void testFIFOAccessDoesntAffectOrder() {
        Cache<Integer, Integer> cache = new Cache<>(2, new FIFOEvictionStrategy<>());
        cache.put(1, 1);
        cache.put(2, 2);
        // Access key 1 multiple times
        cache.get(1);
        cache.get(1);
        cache.get(1);
        // Still, key 1 should be evicted because it was inserted first (FIFO)
        cache.put(3, 3);
        assertEquals(-1, cache.get(1));
        assertEquals(2, cache.get(2));
        assertEquals(3, cache.get(3));
    }

    @Test
    @DisplayName("FIFO: Capacity one")
    void testFIFOCapacityOne() {
        Cache<Integer, Integer> cache = new Cache<>(1, new FIFOEvictionStrategy<>());
        cache.put(1, 1);
        cache.put(2, 2);
        assertEquals(-1, cache.get(1));
        assertEquals(2, cache.get(2));
    }

    // ========== LFU STRATEGY TESTS ==========

    @Test
    @DisplayName("LFU: Basic put and get operations")
    void testLFUBasicPutGet() {
        Cache<Integer, Integer> cache = new Cache<>(2, new LFUEvictionStrategy<>());
        cache.put(1, 1);
        cache.put(2, 2);
        // Access key 1 multiple times (frequency = 2)
        cache.get(1);
        cache.get(1);
        cache.put(3, 3);
        // Key 2 should be evicted (frequency = 1, less frequent than key 1)
        assertEquals(-1, cache.get(2));
        assertEquals(1, cache.get(1));
        assertEquals(3, cache.get(3));
    }

    @Test
    @DisplayName("LFU: Frequency tracking")
    void testLFUFrequencyTracking() {
        Cache<Integer, Integer> cache = new Cache<>(3, new LFUEvictionStrategy<>());
        cache.put(1, 1);
        cache.put(2, 2);
        cache.put(3, 3);
        
        // Access key 1 three times
        cache.get(1);
        cache.get(1);
        cache.get(1);
        
        // Access key 2 twice
        cache.get(2);
        cache.get(2);
        
        // Access key 3 once (not at all after insertion)
        
        cache.put(4, 4);
        // Key 3 should be evicted (least frequently used)
        assertEquals(-1, cache.get(3));
        assertEquals(1, cache.get(1));
        assertEquals(2, cache.get(2));
        assertEquals(4, cache.get(4));
    }

    // ========== GENERAL CACHE TESTS ==========

    @Test
    @DisplayName("Cache: Size tracking")
    void testCacheSize() {
        Cache<Integer, Integer> cache = new Cache<>(3, new LRUEvictionStrategy<>());
        assertEquals(0, cache.size());
        cache.put(1, 1);
        assertEquals(1, cache.size());
        cache.put(2, 2);
        assertEquals(2, cache.size());
        cache.put(3, 3);
        assertEquals(3, cache.size());
        cache.put(4, 4);
        assertEquals(3, cache.size()); // Size doesn't exceed capacity
    }

    @Test
    @DisplayName("Cache: ContainsKey method")
    void testCacheContainsKey() {
        Cache<Integer, Integer> cache = new Cache<>(2, new LRUEvictionStrategy<>());
        assertFalse(cache.containsKey(1));
        cache.put(1, 1);
        assertTrue(cache.containsKey(1));
        cache.put(2, 2);
        cache.put(3, 3); // Evicts key 1
        assertFalse(cache.containsKey(1));
        assertTrue(cache.containsKey(2));
        assertTrue(cache.containsKey(3));
    }

    @Test
    @DisplayName("Cache: Remove method")
    void testCacheRemove() {
        Cache<Integer, Integer> cache = new Cache<>(3, new LRUEvictionStrategy<>());
        cache.put(1, 10);
        cache.put(2, 20);
        cache.put(3, 30);
        
        assertEquals(10, cache.remove(1));
        assertEquals(-1, cache.get(1));
        assertEquals(2, cache.size());
        
        assertNull(cache.remove(999)); // Non-existent key
        assertEquals(2, cache.size());
    }

    @Test
    @DisplayName("Cache: Clear method")
    void testCacheClear() {
        Cache<Integer, Integer> cache = new Cache<>(3, new LRUEvictionStrategy<>());
        cache.put(1, 1);
        cache.put(2, 2);
        cache.put(3, 3);
        assertEquals(3, cache.size());
        
        cache.clear();
        assertEquals(0, cache.size());
        assertEquals(-1, cache.get(1));
        assertEquals(-1, cache.get(2));
        assertEquals(-1, cache.get(3));
    }

    @Test
    @DisplayName("Cache: Capacity and strategy names")
    void testCacheCapacityAndStrategy() {
        Cache<Integer, Integer> lruCache = new Cache<>(5, new LRUEvictionStrategy<>());
        assertEquals(5, lruCache.capacity());
        assertEquals("LRUEvictionStrategy", lruCache.getEvictionStrategyName());
        
        Cache<Integer, Integer> fifoCache = new Cache<>(10, new FIFOEvictionStrategy<>());
        assertEquals(10, fifoCache.capacity());
        assertEquals("FIFOEvictionStrategy", fifoCache.getEvictionStrategyName());
        
        Cache<Integer, Integer> lfuCache = new Cache<>(7, new LFUEvictionStrategy<>());
        assertEquals(7, lfuCache.capacity());
        assertEquals("LFUEvictionStrategy", lfuCache.getEvictionStrategyName());
    }

    // ========== CONCURRENT TESTS ==========

    @Test
    @DisplayName("Concurrent: Multiple threads putting")
    void testConcurrentPuts() throws InterruptedException {
        Cache<Integer, Integer> cache = new Cache<>(150, new LRUEvictionStrategy<>());
        int numThreads = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                for (int i = 0; i < operationsPerThread; i++) {
                    int key = threadId * operationsPerThread + i;
                    cache.put(key, key * 2);
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);

        assertEquals(150, cache.size()); // Should reach capacity due to eviction
    }

    @Test
    @DisplayName("Concurrent: Multiple threads getting")
    void testConcurrentGets() throws InterruptedException {
        Cache<Integer, Integer> cache = new Cache<>(50, new LRUEvictionStrategy<>());

        // Populate cache
        for (int i = 0; i < 50; i++) {
            cache.put(i, i * 2);
        }

        int numThreads = 10;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        AtomicInteger successfulGets = new AtomicInteger(0);

        for (int t = 0; t < numThreads; t++) {
            executor.submit(() -> {
                for (int i = 0; i < operationsPerThread; i++) {
                    int key = i % 50;
                    Object value = cache.get(key);
                    if (value != null && !value.equals(-1)) {
                        successfulGets.incrementAndGet();
                    }
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(10, java.util.concurrent.TimeUnit.SECONDS);

        assertEquals(numThreads * operationsPerThread, successfulGets.get());
    }

    @Test
    @DisplayName("Concurrent: Mixed puts and gets")
    void testConcurrentPutsAndGets() throws InterruptedException {
        Cache<Integer, Integer> cache = new Cache<>(50, new FIFOEvictionStrategy<>());
        int numThreads = 15;
        int operationsPerThread = 200;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                for (int i = 0; i < operationsPerThread; i++) {
                    if (i % 2 == 0) {
                        int key = (threadId + i) % 100;
                        cache.put(key, key * 3);
                    } else {
                        int key = (threadId + i) % 100;
                        cache.get(key);
                    }
                }
            });
        }

        executor.shutdown();
        executor.awaitTermination(20, java.util.concurrent.TimeUnit.SECONDS);

        assertTrue(cache.size() <= 50, "Cache size should not exceed capacity");
        assertTrue(cache.size() > 0, "Cache should contain some elements");
    }

    @Test
    @DisplayName("Concurrent: Strategy-specific behavior with LFU")
    void testConcurrentWithLFUStrategy() throws InterruptedException {
        Cache<Integer, Integer> cache = new Cache<>(30, new LFUEvictionStrategy<>());
        ExecutorService executor = Executors.newFixedThreadPool(5);
        CountDownLatch latch = new CountDownLatch(5);

        // Thread 1: Frequent accesses to key 1
        executor.submit(() -> {
            for (int i = 0; i < 100; i++) {
                cache.put(1, 1);
                cache.get(1);
            }
            latch.countDown();
        });

        // Thread 2: Moderate accesses to key 2
        executor.submit(() -> {
            for (int i = 0; i < 50; i++) {
                cache.put(2, 2);
                cache.get(2);
            }
            latch.countDown();
        });

        // Threads 3-5: Light accesses to other keys
        for (int t = 0; t < 3; t++) {
            final int threadId = t + 3;
            executor.submit(() -> {
                for (int i = 0; i < 15; i++) {
                    cache.put(threadId * 10 + i, threadId * 10 + i);
                }
                latch.countDown();
            });
        }

        latch.await();
        executor.shutdown();
        executor.awaitTermination(20, java.util.concurrent.TimeUnit.SECONDS);

        // Key 1 and 2 should still be in cache (most frequently accessed)
        assertTrue(cache.containsKey(1) || cache.containsKey(2), 
            "Frequently accessed keys should be retained");
    }

    // ========== BACKWARD COMPATIBILITY TESTS ==========

    @Test
    @DisplayName("Backward compatibility: LRUCache wrapper")
    void testLRUCacheBackwardCompatibility() {
        LRUCache<Integer, Integer> cache = new LRUCache<>(2);
        cache.put(1, 1);
        cache.put(2, 2);
        assertEquals(1, cache.get(1));
        cache.put(3, 3);
        assertEquals(-1, cache.get(2));
        assertEquals(2, cache.size());
    }

    @Test
    @DisplayName("Backward compatibility: LRUCache update moves to head")
    void testLRUCacheUpdateMovesToHead() {
        LRUCache<Integer, Integer> cache = new LRUCache<>(2);
        cache.put(1, 1);
        cache.put(2, 2);
        cache.put(1, 10); // update and move to head
        cache.put(3, 3);
        assertEquals(10, cache.get(1));
        assertEquals(-1, cache.get(2));
    }
}
