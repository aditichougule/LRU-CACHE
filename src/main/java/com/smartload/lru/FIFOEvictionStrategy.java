package com.smartload.lru;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * FIFO (First In First Out) eviction strategy implementation.
 * 
 * Evicts the entry that was inserted first, regardless of access patterns.
 * This is simpler and more predictable than LRU for certain use cases.
 * 
 * Time Complexity:
 * - recordAccess: O(1)
 * - recordInsertion: O(1)
 * - recordRemoval: O(1)
 * - selectEvictionCandidate: O(1)
 */
public class FIFOEvictionStrategy<K, V> implements EvictionStrategy<K, V> {
    
    /**
     * LinkedHashMap maintains insertion order (natural order).
     * The first entry inserted is at the beginning (FIFO).
     */
    private final Map<K, Long> insertionOrder = new LinkedHashMap<K, Long>() {
        @Override
        protected boolean removeEldestEntry(Map.Entry<K, Long> eldest) {
            return false; // We manage eviction ourselves
        }
    };
    
    @Override
    public void recordAccess(K key) {
        // FIFO doesn't track access, only insertion order
        // Do nothing
    }
    
    @Override
    public void recordInsertion(K key) {
        // Record insertion order - LinkedHashMap maintains this automatically
        insertionOrder.put(key, System.nanoTime());
    }
    
    @Override
    public void recordRemoval(K key) {
        insertionOrder.remove(key);
    }
    
    @Override
    public K selectEvictionCandidate() {
        // In LinkedHashMap, the first entry is the oldest inserted (FIFO)
        if (insertionOrder.isEmpty()) {
            return null;
        }
        return insertionOrder.keySet().iterator().next();
    }
    
    @Override
    public void clear() {
        insertionOrder.clear();
    }
}
