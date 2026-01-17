package com.smartload.lru;

/**
 * Strategy interface for cache eviction policies.
 * Allows pluggable eviction strategies (LRU, FIFO, LFU, etc.).
 * 
 * This interface defines the contract for deciding which cache entry
 * should be evicted when the cache reaches capacity.
 */
public interface EvictionStrategy<K, V> {
    
    /**
     * Called when an entry is accessed (get or update).
     * Allows the strategy to track access patterns.
     * 
     * @param key The key being accessed
     */
    void recordAccess(K key);
    
    /**
     * Called when a new entry is added to the cache.
     * Allows the strategy to track insertion order or other metadata.
     * 
     * @param key The key being added
     */
    void recordInsertion(K key);
    
    /**
     * Called when an entry is removed from the cache.
     * Allows the strategy to clean up any metadata about this entry.
     * 
     * @param key The key being removed
     */
    void recordRemoval(K key);
    
    /**
     * Determines which key should be evicted when the cache is full.
     * 
     * @return The key to be evicted, or null if no entry should be evicted
     */
    K selectEvictionCandidate();
    
    /**
     * Clears all strategy state (used when cache is cleared).
     */
    void clear();
}
