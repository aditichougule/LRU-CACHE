# LRU Cache (Java)

This repository contains a simple implementation of an LRU (Least Recently Used) cache in Java.

## Overview

The cache is implemented in `com.smartload.lru.LRUCache` (now generic: `LRUCache<K,V>`) and is optimized for O(1) operations for both reads and writes by combining:

- A HashMap that maps keys to doubly-linked list nodes for O(1) lookup.
- A doubly-linked list to track usage order (most recently used at the head, least recently used at the tail) for O(1) insert and removal.

This design allows get and put operations to update usage order and evict the least-recently-used entry when capacity is exceeded. The implementation now supports any object types for keys and values via Java generics.

## Key classes / files

- `src/main/java/com/smartload/lru/LRUCache.java` — the cache implementation.
- `src/test/java/com/smartload/lru/LRUCacheTest.java` — unit tests that demonstrate expected behavior.

## How the implementation works (step-by-step, practical)

Below is a concrete, operational explanation of exactly what happens inside the code when you call the public methods. The goal is to follow the map and list state change by change.

Terminology and starting state

- `head` and `tail` are sentinel (dummy) nodes. They never hold real data. The list always looks like:

  head <-> tail

- Real nodes are inserted between `head` and `tail`. The node right after `head` is the most-recently-used (MRU). The node right before `tail` is the least-recently-used (LRU).
- `map` is a HashMap from key -> Node. `size` is the number of real entries (not counting sentinels).

Core helper operations (what each method really does)

- addToHead(node):
  - Inserts `node` immediately after `head`.
  - Steps:
    1. node.prev = head
    2. node.next = head.next
    3. head.next.prev = node
    4. head.next = node
  - Effect: node becomes MRU.

- removeNode(node):
  - Splices `node` out of the list by linking its neighbors together.
  - Steps:
    1. node.prev.next = node.next
    2. node.next.prev = node.prev
    3. node.prev = null; node.next = null
  - Effect: node is disconnected from the list.

- moveToHead(node):
  - removeNode(node) then addToHead(node).
  - Effect: node becomes MRU, preserving its key/value but updating usage order.

- removeTail():
  - Finds the real node just before `tail` (the LRU). If that node is `head` then list is empty and returns null.
  - removeNode(lruNode) and return it.

Public methods (exact internal sequence)

- get(key):
  1. Node node = map.get(key)
  2. If node == null -> return miss value
     - New behavior: the generic `get(K)` returns the stored `V` or `null` when not found.
     - Backwards-compatibility: the current implementation will return `Integer.valueOf(-1)` on misses when the cache is used with `Integer` values so existing tests that expect `-1` still pass.
  3. moveToHead(node) // mark as used now
  4. return node.value

  So on a successful get the map is unchanged, but the linked list order is updated so this node becomes MRU.

- put(key, value):
  1. Node node = map.get(key)
  2. If node != null:
     - node.value = value // update stored value
     - moveToHead(node) // this key is now MRU
     - return
  3. Else (new key):
     - Node newNode = new Node(key, value)
     - map.put(key, newNode)
     - addToHead(newNode) // new node is MRU
     - size++
     - if (size > capacity):
         • Node tailPrev = removeTail() // removes LRU node from list
         • if (tailPrev != null) { map.remove(tailPrev.key); size--; }

  Important: removeTail returns the node that was removed from the list so we can remove its key from the map.

Concrete example (capacity = 2)

Start: empty

  head <-> tail
  map = {}
  size = 0

Operation: put(1,1)

  - map.put(1, node1)
  - addToHead(node1)

  head <-> node1 <-> tail
  map = {1 -> node1}
  size = 1

Operation: put(2,2)

  - map.put(2, node2)
  - addToHead(node2)

  head <-> node2 <-> node1 <-> tail   (node2 is MRU)
  map = {1 -> node1, 2 -> node2}
  size = 2

Operation: get(1)

  - map.get(1) -> node1 (found)
  - moveToHead(node1): remove node1, then add to head

  head <-> node1 <-> node2 <-> tail   (node1 becomes MRU)
  map unchanged

Operation: put(3,3)

  - new node3 inserted: addToHead(node3)
  - size becomes 3 which > capacity (2), so removeTail(): this removes node2 (LRU)
  - map.remove(node2.key)

  head <-> node3 <-> node1 <-> tail
  map = {1 -> node1, 3 -> node3}
  size = 2

Notes about correctness and invariants

- map and list are always kept in sync: every real node present in the list has an entry in map, and every map entry points to a node currently in the list.
- size counts the number of real nodes and matches map.size() unless you directly manipulate internals.
- Sentinel head/tail simplify edge cases: you never have to check for null when inserting/removing nodes as neighbors always exist.

Why this gives O(1)

- HashMap lookup (map.get) is O(1) average.
- addToHead/removeNode only reassign a constant number of pointers, O(1).
- So both get and put perform a bounded number of O(1) steps.

Small implementation quirks and suggestions

- The Node class now stores generic `K` and `V` so the cache works with any object types for keys and values.
- `get(K)` returns `V` (or `null`) on miss. To preserve the original tests the implementation currently returns `Integer.valueOf(-1)` on miss if the cache is used with `Integer` values; if you prefer a cleaner API, consider changing `get` to return `Optional<V>` or update callers to expect `null` on miss.
- `toString()` is provided for debugging and prints entries from MRU to LRU.
- The class is not thread-safe. For concurrent use, add synchronization, use concurrent data structures, or provide a thread-safe wrapper.

## Complexity

- get: O(1) average
- put: O(1) average

(These hold because HashMap provides O(1) lookup and the linked list operations are O(1).)

## Usage example (generic)

```java
LRUCache<Integer, Integer> cache = new LRUCache<>(2);
cache.put(1, 1);
cache.put(2, 2);
int v1 = cache.get(1); // returns 1, key 1 becomes most-recently-used
cache.put(3, 3); // evicts key 2 because capacity is 2
int v2 = cache.get(2); // returns -1 (compatibility behavior for Integer-valued cache)

// If you use a non-Integer value type, get() returns null on miss:
LRUCache<String, String> sCache = new LRUCache<>(2);
sCache.put("a", "A");
String s = sCache.get("missing"); // returns null
```

## Running tests

This project uses Maven. From the project root run:

```bash
mvn test
```

The tests are located at `src/test/java/com/smartload/lru/LRUCacheTest.java` and validate the expected LRU behavior and edge cases.

## Notes and possible improvements

- The implementation uses `int` keys and values like the original tests. It could be generalized with generics to support arbitrary key/value types.
- The class is not thread-safe. For concurrent use, add synchronization or use concurrent data structures and fine-grained locking.
- Add optional eviction callbacks, persistence, or size-by-memory instead of count-based capacity as advanced features.

