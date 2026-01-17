package com.smartload.lru;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * LRU (Least Recently Used) eviction strategy implementation.
 * 
 * Tracks access order using a LinkedHashMap and evicts the least recently used entry
 * (the one that was accessed earliest).
 * 
 * Time Complexity:
 * - recordAccess: O(1)
 * - recordInsertion: O(1)
 * - recordRemoval: O(1)
 * - selectEvictionCandidate: O(1)
 */
public class LRUEvictionStrategy<K, V> implements EvictionStrategy<K, V> {
    
    /**
     * LinkedHashMap maintains insertion order (or access order if accessOrder=true).
     * We use accessOrder=true to maintain LRU order.
     * The eldest entry is automatically placed at the beginning.
     */
    private final Map<K, Long> accessOrder = new LinkedHashMap<K, Long>(16, 0.75f, true) {
        /**
         * Override removeEldestEntry to enable automatic eviction (optional for this strategy).
         * For now, we just track order manually.
         */
        @Override
        protected boolean removeEldestEntry(Map.Entry<K, Long> eldest) {
            return false; // We manage eviction ourselves via selectEvictionCandidate()
        }
    };
    
    @Override
    public void recordAccess(K key) {
        // Update access time - LinkedHashMap will reorder this to the end
        accessOrder.put(key, System.nanoTime());
    }
    
    @Override
    public void recordInsertion(K key) {
        // Record insertion time
        accessOrder.put(key, System.nanoTime());
    }
    
    @Override
    public void recordRemoval(K key) {
        accessOrder.remove(key);
    }
    
    @Override
    public K selectEvictionCandidate() {
        // In LinkedHashMap with accessOrder=true, the first entry is the least recently used
        if (accessOrder.isEmpty()) {
            return null;
        }
        return accessOrder.keySet().iterator().next();
    }
    
    @Override
    public void clear() {
        accessOrder.clear();
    }
}
