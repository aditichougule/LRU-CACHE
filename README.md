# LRU Cache (Java)

This repository contains a production-ready, thread-safe cache implementation in Java with **pluggable eviction strategies**.

## Overview

The cache is implemented with the **Strategy Design Pattern**, allowing you to choose eviction policies:
- **LRU** (Least Recently Used) - default, evicts least recently accessed entries
- **FIFO** (First In First Out) - evicts oldest inserted entries
- **LFU** (Least Frequently Used) - evicts least frequently accessed entries
- **Custom** - easily implement your own eviction strategy

### Architecture

The cache is optimized for O(1) operations:

1. **HashMap** for O(1) key lookups
2. **EvictionStrategy** interface for pluggable eviction policies
3. **ReentrantReadWriteLock** for thread-safe concurrent access
4. **Capacity management** with automatic eviction when full

## Key Classes

- `Cache<K, V>` — The main generic cache with pluggable strategy support
- `EvictionStrategy<K, V>` — Interface for eviction policies
- `LRUEvictionStrategy<K, V>` — LRU eviction (default)
- `FIFOEvictionStrategy<K, V>` — FIFO eviction
- `LFUEvictionStrategy<K, V>` — LFU eviction
- `LRUCache<K, V>` — Backward-compatible wrapper (uses LRU by default)

## Strategy Pattern Implementation

The `EvictionStrategy` interface defines the contract for eviction algorithms:

```java
public interface EvictionStrategy<K, V> {
    void recordAccess(K key);           // Track when entry is accessed
    void recordInsertion(K key);        // Track when entry is inserted
    void recordRemoval(K key);          // Clean up when entry is removed
    K selectEvictionCandidate();        // Choose which entry to evict
    void clear();                       // Clear strategy state
}
```

This design follows SOLID principles:
- **Single Responsibility**: Each strategy handles one eviction policy
- **Open/Closed**: Easy to add new strategies without modifying existing code
- **Liskov Substitution**: All strategies implement the same interface
- **Interface Segregation**: Clean, focused interface
- **Dependency Inversion**: Cache depends on abstraction, not concrete strategies

## Usage Examples

### LRU (Default - Least Recently Used)

```java
// Using LRUCache wrapper (backward compatible)
LRUCache<String, String> cache = new LRUCache<>(2);
cache.put("key1", "value1");
cache.put("key2", "value2");
cache.get("key1");  // Mark key1 as recently used
cache.put("key3", "value3");  // key2 is evicted (least recently used)

// Using Cache directly with explicit strategy
Cache<String, String> cache = new Cache<>(2, new LRUEvictionStrategy<>());
```

### FIFO (First In First Out)

```java
Cache<String, String> cache = new Cache<>(2, new FIFOEvictionStrategy<>());
cache.put("key1", "value1");  // First
cache.put("key2", "value2");  // Second
cache.get("key1");  // Access doesn't matter in FIFO
cache.get("key2");  // Still won't prevent eviction
cache.put("key3", "value3");  // key1 is evicted (first inserted)
```

### LFU (Least Frequently Used)

```java
Cache<String, String> cache = new Cache<>(2, new LFUEvictionStrategy<>());
cache.put("key1", "value1");
cache.put("key2", "value2");
cache.get("key1");  // Increase frequency of key1
cache.get("key1");
cache.get("key1");
cache.put("key3", "value3");  // key2 is evicted (frequency=1 vs key1 frequency=4)
```

### Custom Strategy

Implement the `EvictionStrategy` interface:

```java
public class CustomEvictionStrategy<K, V> implements EvictionStrategy<K, V> {
    // Your custom eviction logic
    @Override
    public K selectEvictionCandidate() {
        // Your implementation
    }
    // ... implement other methods
}

Cache<String, String> cache = new Cache<>(2, new CustomEvictionStrategy<>());
```

## Core Operations

### Cache Methods

- `get(K key)` — Retrieve value, updates eviction strategy
- `put(K key, V value)` — Insert/update entry, may trigger eviction
- `remove(K key)` — Remove specific entry
- `containsKey(K key)` — Check if key exists
- `size()` — Get current entry count
- `capacity()` — Get max capacity
- `clear()` — Remove all entries
- `getEvictionStrategyName()` — Get strategy being used

### Thread Safety

The cache is fully thread-safe using `ReentrantReadWriteLock`:

- **Write locks** for `put()`, `remove()`, `clear()` (exclusive access)
- **Write locks** for `get()` (to update eviction state consistently)
- **Read locks** for `size()`, `containsKey()`, read-only operations

This allows multiple readers to execute concurrently while maintaining strong consistency for writes.

## Time Complexity

| Operation | Time |
|-----------|------|
| get       | O(1) average |
| put       | O(1) average |
| remove    | O(1) average |
| size      | O(1) average |
| containsKey | O(1) average |

**Note**: LFU strategy's `selectEvictionCandidate()` is O(n) but called only on capacity overflow.

## Eviction Strategy Comparison

| Strategy | Use Case | Overhead | Complexity |
|----------|----------|----------|-----------|
| **LRU** | General purpose, cache-like behavior | Low | O(1) |
| **FIFO** | Simple, predictable, streaming data | Very Low | O(1) |
| **LFU** | Protect frequently accessed items | Medium | O(n) candidate selection |
| **Custom** | Domain-specific requirements | Varies | Depends on implementation |

## Design Patterns Used

1. **Strategy Pattern** - Pluggable eviction strategies via `EvictionStrategy` interface
2. **Template Method** - `Cache` class orchestrates strategy calls
3. **Wrapper/Adapter** - `LRUCache` provides backward compatibility

## Thread Safety & Concurrency

```java
// Safe for concurrent use
Cache<String, String> cache = new Cache<>(100, new LRUEvictionStrategy<>());

ExecutorService executor = Executors.newFixedThreadPool(10);
for (int i = 0; i < 10; i++) {
    final int threadId = i;
    executor.submit(() -> {
        for (int j = 0; j < 100; j++) {
            cache.put("key-" + threadId + "-" + j, "value");
            cache.get("key-" + threadId + "-" + j);
        }
    });
}
executor.shutdown();
executor.awaitTermination(30, TimeUnit.SECONDS);

// All operations complete safely without race conditions
```

## Running Tests

```bash
# Run all tests (LRUCacheTest and CacheStrategyTest)
mvn test

# Run specific test class
mvn test -Dtest=CacheStrategyTest

# Run with verbose output
mvn test -v
```

Test suites include:
- **LRUCacheTest**: Basic cache functionality and backward compatibility
- **CacheStrategyTest**: Strategy pattern tests for LRU, FIFO, LFU with:
  - Basic operations for each strategy
  - Concurrent operations (thread safety)
  - Strategy-specific behavior validation
  - Eviction correctness verification

## Implementation Details

### How LRU Strategy Works

LRU uses a LinkedHashMap with access-order tracking:
- On `recordAccess(key)`: LinkedHashMap reorders the key to the end (most recent)
- On `selectEvictionCandidate()`: Return the first key (least recent)
- Time: O(1) for access tracking, O(1) for candidate selection

### How FIFO Strategy Works

FIFO uses a LinkedHashMap with insertion-order tracking:
- On `recordInsertion(key)`: Add key to LinkedHashMap
- On `selectEvictionCandidate()`: Return the first key (oldest inserted)
- `recordAccess()`: No-op (doesn't affect eviction order)
- Time: O(1) for all operations

### How LFU Strategy Works

LFU maintains a frequency map:
- On `recordAccess(key)`: Increment frequency counter
- On `selectEvictionCandidate()`: Scan all entries, find minimum frequency (with timestamp tie-breaking)
- Time: O(1) for access, O(n) for candidate selection

## Known Limitations & Future Enhancements

- **LFU overhead**: `selectEvictionCandidate()` scans all entries. Consider using a min-heap for large caches
- **No eviction callbacks**: Currently no hooks for custom eviction events
- **No persistence**: Cache is in-memory only
- **No statistics**: No built-in monitoring of hit/miss rates

Potential enhancements:
- Add `WeakHashMap` support for garbage-collected entries
- Implement `Clock` eviction strategy
- Add callback hooks for eviction events
- Support for weighted/sized entries
- Metrics collection (hit rate, eviction count)

## Backward Compatibility

The original `LRUCache<K, V>` API is maintained:

```java
// Old code still works
LRUCache<Integer, Integer> cache = new LRUCache<>(10);
cache.put(1, "value");
Integer value = cache.get(1);

// But you can now switch strategies if needed
Cache<Integer, Integer> cache = new Cache<>(10, new FIFOEvictionStrategy<>());
```

## References

- Strategy Pattern: https://refactoring.guru/design-patterns/strategy
- SOLID Principles: https://en.wikipedia.org/wiki/SOLID
- Java Concurrency: https://docs.oracle.com/javase/tutorial/essential/concurrency/

