package com.smartload.lru;

/**
 * Thread-safe LRU cache implementation.
 * 
 * This class is a convenience wrapper around Cache<K, V> that uses
 * LRUEvictionStrategy by default, providing backward compatibility
 * with the original LRUCache API.
 * 
 * For more flexibility with different eviction strategies, use Cache directly:
 * 
 *   Cache<K, V> cache = new Cache<>(capacity, new FIFOEvictionStrategy<>());
 * 
 * @param <K> Key type
 * @param <V> Value type
 */
public class LRUCache<K, V> {
    private final Cache<K, V> cache;

    /**
     * Creates an LRU cache with the specified capacity.
     * Uses LRUEvictionStrategy for evicting least recently used entries.
     * 
     * @param capacity The maximum number of entries in the cache (must be > 0)
     * @throws IllegalArgumentException if capacity <= 0
     */
    public LRUCache(int capacity) {
        this.cache = new Cache<>(capacity, new LRUEvictionStrategy<>());
    }

    /**
     * Returns the value associated with the key.
     * Marks the entry as most-recently-used.
     * 
     * Backwards-compatibility: if the cache is used with Integer values,
     * returns Integer.valueOf(-1) for cache misses.
     * 
     * @param key The key to look up
     * @return The value associated with the key, or -1 if not found (for Integer caches)
     */
    public V get(K key) {
        return cache.get(key);
    }

    /**
     * Inserts or updates an entry in the cache.
     * If insertion exceeds capacity, evicts the least-recently-used entry.
     * 
     * @param key The key to insert or update
     * @param value The value to associate with the key
     */
    public void put(K key, V value) {
        cache.put(key, value);
    }

    /**
     * Returns the current number of entries in the cache.
     * 
     * @return The current size of the cache
     */
    public int size() {
        return cache.size();
    }

    @Override
    public String toString() {
        return cache.toString();
    }
}
