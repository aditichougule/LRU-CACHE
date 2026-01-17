package com.smartload.lru;

/**
 * Demo/Example file showing how to use the Cache with different eviction strategies.
 * This file is not part of the main library but demonstrates practical usage patterns.
 */
public class CacheDemo {

    public static void main(String[] args) {
        System.out.println("=== LRU Cache with Pluggable Eviction Strategies Demo ===\n");

        lruStrategyDemo();
        System.out.println("\n" + "=".repeat(60) + "\n");

        fifoStrategyDemo();
        System.out.println("\n" + "=".repeat(60) + "\n");

        lfuStrategyDemo();
        System.out.println("\n" + "=".repeat(60) + "\n");

        concurrencyDemo();
    }

    /**
     * Demo: LRU (Least Recently Used) Strategy
     * Evicts entries that haven't been accessed recently.
     * Great for general-purpose caching.
     */
    static void lruStrategyDemo() {
        System.out.println("1. LRU (Least Recently Used) Strategy Demo");
        System.out.println("-----------------------------------------");

        Cache<String, String> cache = new Cache<>(3, new LRUEvictionStrategy<>());
        System.out.println("Created cache with capacity 3, using LRUEvictionStrategy");

        cache.put("user:1", "Alice");
        cache.put("user:2", "Bob");
        cache.put("user:3", "Charlie");
        System.out.println("\nPut 3 entries: user:1, user:2, user:3");
        System.out.println("Cache size: " + cache.size());

        System.out.println("\nAccess user:1 (marks it as recently used)");
        cache.get("user:1");

        System.out.println("Access user:2 (marks it as recently used)");
        cache.get("user:2");

        System.out.println("\nAdd user:4 (cache full, must evict)");
        cache.put("user:4", "Diana");
        System.out.println("Cache size: " + cache.size());

        System.out.println("Checking if user:3 is in cache: " + cache.containsKey("user:3"));
        System.out.println("(user:3 was evicted because it was least recently used)");

        System.out.println("\nRemaining entries:");
        System.out.println("  user:1: " + cache.get("user:1"));
        System.out.println("  user:2: " + cache.get("user:2"));
        System.out.println("  user:4: " + cache.get("user:4"));
    }

    /**
     * Demo: FIFO (First In First Out) Strategy
     * Evicts entries in the order they were inserted, regardless of access patterns.
     * Good for streaming data or simple queue-like behavior.
     */
    static void fifoStrategyDemo() {
        System.out.println("2. FIFO (First In First Out) Strategy Demo");
        System.out.println("-----------------------------------------");

        Cache<Integer, String> cache = new Cache<>(3, new FIFOEvictionStrategy<>());
        System.out.println("Created cache with capacity 3, using FIFOEvictionStrategy");

        cache.put(1, "First");
        cache.put(2, "Second");
        cache.put(3, "Third");
        System.out.println("\nInserted 3 entries in order: 1, 2, 3");

        System.out.println("\nAccess entry 3 many times");
        for (int i = 0; i < 5; i++) {
            cache.get(3);
        }
        System.out.println("(In FIFO, access frequency doesn't matter)");

        System.out.println("\nAdd entry 4 (cache full, must evict)");
        cache.put(4, "Fourth");
        System.out.println("Cache size: " + cache.size());

        System.out.println("Checking entries:");
        System.out.println("  Entry 1 in cache: " + cache.containsKey(1) + " (First inserted, first evicted)");
        System.out.println("  Entry 2 in cache: " + cache.containsKey(2));
        System.out.println("  Entry 3 in cache: " + cache.containsKey(3) + " (despite frequent access)");
        System.out.println("  Entry 4 in cache: " + cache.containsKey(4));

        System.out.println("\nAdd entry 5");
        cache.put(5, "Fifth");
        System.out.println("Entry 2 now evicted (second inserted, second evicted)");
    }

    /**
     * Demo: LFU (Least Frequently Used) Strategy
     * Evicts entries that have been accessed the least number of times.
     * Good for protecting frequently accessed data.
     */
    static void lfuStrategyDemo() {
        System.out.println("3. LFU (Least Frequently Used) Strategy Demo");
        System.out.println("-------------------------------------------");

        Cache<String, Integer> cache = new Cache<>(3, new LFUEvictionStrategy<>());
        System.out.println("Created cache with capacity 3, using LFUEvictionStrategy");

        cache.put("popular", 1);
        cache.put("medium", 2);
        cache.put("rare", 3);
        System.out.println("\nInserted 3 entries: popular, medium, rare");
        System.out.println("All have frequency = 1 (just inserted)");

        System.out.println("\nAccess 'popular' 10 times");
        for (int i = 0; i < 10; i++) {
            cache.get("popular");
        }

        System.out.println("Access 'medium' 5 times");
        for (int i = 0; i < 5; i++) {
            cache.get("medium");
        }

        System.out.println("Don't access 'rare' again (frequency stays at 1)");

        System.out.println("\nCurrent frequencies:");
        System.out.println("  'popular': 11 (accessed 10 times + 1 initial)");
        System.out.println("  'medium': 6 (accessed 5 times + 1 initial)");
        System.out.println("  'rare': 1 (least frequently used)");

        System.out.println("\nAdd 'new' entry (cache full, must evict)");
        cache.put("new", 4);
        System.out.println("Cache size: " + cache.size());

        System.out.println("Checking entries:");
        System.out.println("  'popular' in cache: " + cache.containsKey("popular"));
        System.out.println("  'medium' in cache: " + cache.containsKey("medium"));
        System.out.println("  'rare' in cache: " + cache.containsKey("rare")
            + " (evicted - least frequently used)");
        System.out.println("  'new' in cache: " + cache.containsKey("new"));
    }

    /**
     * Demo: Concurrent access to cache
     * Shows thread safety with different strategies.
     */
    static void concurrencyDemo() {
        System.out.println("4. Concurrent Access Demo");
        System.out.println("-------------------------");

        Cache<String, String> cache = new Cache<>(100, new LRUEvictionStrategy<>());
        System.out.println("Created thread-safe cache with capacity 100");

        Thread writerThread = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                cache.put("key-write-" + i, "value-" + i);
            }
            System.out.println("Writer thread: inserted 50 entries");
        }, "WriterThread");

        Thread readerThread = new Thread(() -> {
            for (int i = 0; i < 50; i++) {
                cache.get("key-write-" + (i % 25));
            }
            System.out.println("Reader thread: read 50 entries");
        }, "ReaderThread");

        writerThread.start();
        readerThread.start();

        try {
            writerThread.join();
            readerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("\nBoth threads completed safely");
        System.out.println("Final cache size: " + cache.size());
        System.out.println("Cache strategy: " + cache.getEvictionStrategyName());
    }

    /**
     * Demonstrates switching strategies for different use cases.
     */
    static void strategySelectionDemo() {
        System.out.println("5. Strategy Selection for Different Use Cases");
        System.out.println("--------------------------------------------");

        // Use case 1: Web server caching
        System.out.println("\nUse case 1: Web server response cache");
        Cache<String, String> webCache = new Cache<>(1000, new LRUEvictionStrategy<>());
        System.out.println("  Strategy: LRU (evict responses not accessed recently)");

        // Use case 2: API rate limiting
        System.out.println("\nUse case 2: Request rate limiter");
        Cache<String, Long> rateLimitCache = new Cache<>(10000, new FIFOEvictionStrategy<>());
        System.out.println("  Strategy: FIFO (simple, predictable cleanup)");

        // Use case 3: Frequently accessed data
        System.out.println("\nUse case 3: Frequently accessed data (hot data set)");
        Cache<Integer, String> hotDataCache = new Cache<>(100, new LFUEvictionStrategy<>());
        System.out.println("  Strategy: LFU (protect popular items)");

        System.out.println("\nAll three caches operate independently with different eviction semantics");
    }
}
