package com.smartload.lru;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Thread-safe cache with TTL (Time-To-Live) support and pluggable eviction strategies.
 * 
 * Features:
 * - Pluggable eviction strategies (LRU, FIFO, LFU, custom)
 * - Per-entry TTL expiration
 * - Lazy expiration (expired entries removed on access)
 * - Thread-safe with ReentrantReadWriteLock
 * - O(1) average time complexity for get/put operations
 * 
 * Example usage:
 * <pre>
 *   TTLCache<String, String> cache = new TTLCache<>(100, new LRUEvictionStrategy<>());
 *   cache.put("key", "value", 5000); // Expires in 5 seconds
 *   String value = cache.get("key"); // Returns value if not expired
 * </pre>
 * 
 * @param <K> Key type
 * @param <V> Value type
 */
public class TTLCache<K, V> {
    private final int capacity;
    private final Map<K, CacheEntry<V>> map;
    private final EvictionStrategy<K, V> evictionStrategy;
    private int size;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    
    // Statistics tracking
    private long totalPuts = 0;
    private long totalGets = 0;
    private long cacheHits = 0;
    private long cacheMisses = 0;
    private long expirations = 0;

    /**
     * Creates a TTL-aware cache with the specified capacity and eviction strategy.
     * 
     * @param capacity The maximum number of entries in the cache (must be > 0)
     * @param evictionStrategy The strategy to use for evicting entries when capacity is exceeded
     * @throws IllegalArgumentException if capacity <= 0
     * @throws NullPointerException if evictionStrategy is null
     */
    public TTLCache(int capacity, EvictionStrategy<K, V> evictionStrategy) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be > 0");
        }
        if (evictionStrategy == null) {
            throw new NullPointerException("Eviction strategy cannot be null");
        }
        this.capacity = capacity;
        this.map = new HashMap<>();
        this.evictionStrategy = evictionStrategy;
        this.size = 0;
    }

    /**
     * Inserts or updates an entry with a TTL of Long.MAX_VALUE (no expiry).
     * 
     * @param key The key
     * @param value The value
     */
    public void put(K key, V value) {
        put(key, value, Long.MAX_VALUE);
    }

    /**
     * Inserts or updates an entry with a specified TTL.
     * 
     * @param key The key
     * @param value The value
     * @param ttlMillis Time-to-live in milliseconds. Use Long.MAX_VALUE for no expiry.
     */
    public void put(K key, V value, long ttlMillis) {
        lock.writeLock().lock();
        try {
            totalPuts++;
            
            // Check if key already exists
            if (map.containsKey(key)) {
                // Update existing entry
                map.put(key, new CacheEntry<>(value, ttlMillis));
                evictionStrategy.recordAccess(key);
                return;
            }

            // New entry - add to map and record insertion
            CacheEntry<V> entry = new CacheEntry<>(value, ttlMillis);
            map.put(key, entry);
            evictionStrategy.recordInsertion(key);
            size++;

            // If capacity exceeded, evict the candidate selected by strategy
            if (size > capacity) {
                K evictionCandidate = evictionStrategy.selectEvictionCandidate();
                if (evictionCandidate != null) {
                    map.remove(evictionCandidate);
                    evictionStrategy.recordRemoval(evictionCandidate);
                    size--;
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Retrieves the value associated with the key.
     * Returns null if the key doesn't exist or if the entry has expired.
     * Uses lazy expiration - expired entries are removed when accessed.
     * 
     * For backward compatibility with Integer-valued caches, returns -1 on miss.
     * 
     * @param key The key to look up
     * @return The value if found and not expired, null otherwise (or -1 for Integer values)
     */
    public V get(K key) {
        lock.writeLock().lock();
        try {
            totalGets++;
            
            CacheEntry<V> entry = map.get(key);
            
            if (entry == null) {
                cacheMisses++;
                // Backward compatibility for Integer-valued caches
                try {
                    @SuppressWarnings("unchecked")
                    V sentinel = (V) Integer.valueOf(-1);
                    return sentinel;
                } catch (ClassCastException e) {
                    return null;
                }
            }

            // Check if entry has expired (lazy eviction)
            if (entry.isExpired()) {
                map.remove(key);
                evictionStrategy.recordRemoval(key);
                size--;
                expirations++;
                cacheMisses++;
                
                // Backward compatibility
                try {
                    @SuppressWarnings("unchecked")
                    V sentinel = (V) Integer.valueOf(-1);
                    return sentinel;
                } catch (ClassCastException e) {
                    return null;
                }
            }

            // Valid entry found
            cacheHits++;
            evictionStrategy.recordAccess(key);
            return entry.getValue();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Removes an entry from the cache if present and not expired.
     * Lazy expiration: expired entries are treated as not present.
     * 
     * @param key The key to remove
     * @return The value that was removed, or null if the key was not in the cache or if it has expired
     */
    public V remove(K key) {
        lock.writeLock().lock();
        try {
            CacheEntry<V> entry = map.get(key);
            if (entry == null) {
                return null;
            }
            
            // If entry is expired, treat it as not present
            if (entry.isExpired()) {
                map.remove(key);
                evictionStrategy.recordRemoval(key);
                size--;
                expirations++;
                return null;
            }
            
            // Valid entry - remove and return it
            map.remove(key);
            evictionStrategy.recordRemoval(key);
            size--;
            return entry.getValue();
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Checks if the cache contains the specified key (and it's not expired).
     * 
     * @param key The key to check
     * @return true if the cache contains the key and it's not expired, false otherwise
     */
    public boolean containsKey(K key) {
        lock.readLock().lock();
        try {
            CacheEntry<V> entry = map.get(key);
            return entry != null && !entry.isExpired();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Removes all expired entries from the cache.
     * This is an active cleanup operation (not lazy).
     * 
     * @return The number of entries that were expired and removed
     */
    public int cleanupExpired() {
        lock.writeLock().lock();
        try {
            int removed = 0;
            var iterator = map.entrySet().iterator();
            
            while (iterator.hasNext()) {
                var entry = iterator.next();
                if (entry.getValue().isExpired()) {
                    iterator.remove();
                    evictionStrategy.recordRemoval(entry.getKey());
                    size--;
                    expirations++;
                    removed++;
                }
            }
            
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns the current number of entries in the cache.
     * Note: This count may include expired entries (removed on lazy access).
     * 
     * @return The current size of the cache
     */
    public int size() {
        lock.readLock().lock();
        try {
            return size;
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the actual number of non-expired entries in the cache.
     * This is a more expensive operation as it checks expiry for all entries.
     * 
     * @return The number of valid, non-expired entries
     */
    public int countValid() {
        lock.readLock().lock();
        try {
            return (int) map.values().stream()
                    .filter(entry -> !entry.isExpired())
                    .count();
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Returns the maximum capacity of the cache.
     * 
     * @return The capacity
     */
    public int capacity() {
        return capacity;
    }

    /**
     * Clears all entries from the cache.
     */
    public void clear() {
        lock.writeLock().lock();
        try {
            map.clear();
            evictionStrategy.clear();
            size = 0;
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Returns the type of eviction strategy being used.
     * 
     * @return The class name of the eviction strategy
     */
    public String getEvictionStrategyName() {
        return evictionStrategy.getClass().getSimpleName();
    }

    /**
     * Gets cache statistics.
     * 
     * @return A string representation of cache stats
     */
    public String getStats() {
        lock.readLock().lock();
        try {
            double hitRate = totalGets > 0 ? (double) cacheHits / totalGets * 100 : 0;
            return String.format(
                    "Stats{" +
                    "totalPuts=%d, totalGets=%d, cacheHits=%d, cacheMisses=%d, " +
                    "expirations=%d, hitRate=%.2f%%, size=%d/%d, strategy=%s" +
                    "}",
                    totalPuts, totalGets, cacheHits, cacheMisses,
                    expirations, hitRate, size, capacity, getEvictionStrategyName()
            );
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * Gets the remaining TTL for a specific entry.
     * 
     * @param key The key to check
     * @return The remaining time in milliseconds, or 0 if the entry doesn't exist or is expired
     */
    public long getRemainingTTL(K key) {
        lock.readLock().lock();
        try {
            CacheEntry<V> entry = map.get(key);
            if (entry == null || entry.isExpired()) {
                return 0;
            }
            return entry.getRemainingTTL();
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public String toString() {
        lock.readLock().lock();
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("TTLCache{")
                    .append("capacity=").append(capacity)
                    .append(", size=").append(size)
                    .append(", strategy=").append(getEvictionStrategyName())
                    .append(", entries=[");
            
            boolean first = true;
            for (Map.Entry<K, CacheEntry<V>> entry : map.entrySet()) {
                if (!first) sb.append(", ");
                sb.append(entry.getKey()).append("=").append(entry.getValue().getValue());
                if (entry.getValue().isExpired()) {
                    sb.append(" [EXPIRED]");
                }
                first = false;
            }
            sb.append("]}");
            return sb.toString();
        } finally {
            lock.readLock().unlock();
        }
    }
}
