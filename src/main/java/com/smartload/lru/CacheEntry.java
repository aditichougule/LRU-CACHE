package com.smartload.lru;

/**
 * Wrapper for cache entries that tracks expiration time.
 * Allows lazy eviction of expired entries on access.
 * 
 * @param <V> The value type
 */
public class CacheEntry<V> {
    private final V value;
    private final long expiryTime; // Absolute time in milliseconds when entry expires
    
    /**
     * Creates a cache entry with a TTL.
     * 
     * @param value The value to store
     * @param ttlMillis Time-to-live in milliseconds. Use Long.MAX_VALUE for no expiry.
     */
    public CacheEntry(V value, long ttlMillis) {
        this.value = value;
        // Prevent overflow when ttlMillis is Long.MAX_VALUE
        if (ttlMillis == Long.MAX_VALUE) {
            this.expiryTime = Long.MAX_VALUE;
        } else {
            this.expiryTime = System.currentTimeMillis() + ttlMillis;
        }
    }
    
    /**
     * Creates a cache entry that never expires.
     * 
     * @param value The value to store
     */
    public CacheEntry(V value) {
        this.value = value;
        this.expiryTime = Long.MAX_VALUE;
    }
    
    /**
     * Gets the wrapped value.
     * 
     * @return The value
     */
    public V getValue() {
        return value;
    }
    
    /**
     * Checks if this entry has expired.
     * 
     * @return true if the current time is past the expiry time, false otherwise
     */
    public boolean isExpired() {
        return System.currentTimeMillis() > expiryTime;
    }
    
    /**
     * Gets the remaining time to live in milliseconds.
     * 
     * @return Milliseconds until expiry, or 0 if already expired
     */
    public long getRemainingTTL() {
        long remaining = expiryTime - System.currentTimeMillis();
        return Math.max(0, remaining);
    }
    
    /**
     * Gets the absolute expiry time.
     * 
     * @return Unix timestamp in milliseconds
     */
    public long getExpiryTime() {
        return expiryTime;
    }
    
    @Override
    public String toString() {
        return "CacheEntry{" +
                "value=" + value +
                ", expiryTime=" + expiryTime +
                ", expired=" + isExpired() +
                ", remainingTTL=" + getRemainingTTL() +
                "}";
    }
}
