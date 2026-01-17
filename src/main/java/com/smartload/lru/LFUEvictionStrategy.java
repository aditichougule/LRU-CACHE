package com.smartload.lru;

import java.util.HashMap;
import java.util.Map;

/**
 * LFU (Least Frequently Used) eviction strategy implementation.
 * 
 * Evicts the entry that has been accessed the least number of times.
 * When there's a tie, evicts the least recently used among tied entries.
 * 
 * Time Complexity:
 * - recordAccess: O(1) amortized
 * - recordInsertion: O(1)
 * - recordRemoval: O(1)
 * - selectEvictionCandidate: O(n) where n is the number of entries
 */
public class LFUEvictionStrategy<K, V> implements EvictionStrategy<K, V> {
    
    private static class AccessInfo {
        long frequency;
        long lastAccessTime;
        
        AccessInfo() {
            this.frequency = 1;
            this.lastAccessTime = System.nanoTime();
        }
    }
    
    private final Map<K, AccessInfo> accessInfo = new HashMap<>();
    
    @Override
    public void recordAccess(K key) {
        if (accessInfo.containsKey(key)) {
            AccessInfo info = accessInfo.get(key);
            info.frequency++;
            info.lastAccessTime = System.nanoTime();
        }
    }
    
    @Override
    public void recordInsertion(K key) {
        accessInfo.put(key, new AccessInfo());
    }
    
    @Override
    public void recordRemoval(K key) {
        accessInfo.remove(key);
    }
    
    @Override
    public K selectEvictionCandidate() {
        if (accessInfo.isEmpty()) {
            return null;
        }
        
        K candidateKey = null;
        long minFrequency = Long.MAX_VALUE;
        long oldestAccessTime = Long.MAX_VALUE;
        
        for (Map.Entry<K, AccessInfo> entry : accessInfo.entrySet()) {
            AccessInfo info = entry.getValue();
            
            // Select the key with minimum frequency
            // In case of tie, select the one with earliest access time
            if (info.frequency < minFrequency || 
                (info.frequency == minFrequency && info.lastAccessTime < oldestAccessTime)) {
                candidateKey = entry.getKey();
                minFrequency = info.frequency;
                oldestAccessTime = info.lastAccessTime;
            }
        }
        
        return candidateKey;
    }
    
    @Override
    public void clear() {
        accessInfo.clear();
    }
}
