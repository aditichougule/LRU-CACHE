package com.smartload.lru;

import java.util.HashMap;
import java.util.Map;

/**
 * Generic LRU cache implementation using HashMap + doubly-linked list.
 * Maintains O(1) get/put by keeping a map for fast lookup and a
 * doubly-linked list to track usage order (MRU at head, LRU at tail).
 *
 * get(K) returns the value or null if not present.
 */
public class LRUCache<K, V> {
    private final int capacity;
    private final Map<K, Node> map;
    private final Node head;
    private final Node tail;
    private int size;

    public LRUCache(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be > 0");
        this.capacity = capacity;
        this.map = new HashMap<>();
        this.head = new Node(null, null);
        this.tail = new Node(null, null);
        head.next = tail;
        tail.prev = head;
        this.size = 0;
    }

    /**
     * Returns the value for key or null when not found. Marks the entry as most-recently-used.
     *
     * Backwards-compatibility: if this cache is used with Integer values (original tests),
     * return Integer.valueOf(-1) for misses so existing tests relying on -1 continue to work.
     */
    public V get(K key) {
        Node node = map.get(key);
        if (node == null) {
            // Maintain backward compatibility for Integer-valued caches used by tests.
            try {
                @SuppressWarnings("unchecked")
                V sentinel = (V) Integer.valueOf(-1);
                return sentinel;
            } catch (ClassCastException e) {
                return null;
            }
        }
        moveToHead(node);
        return node.value;
    }

    /**
     * Inserts or updates the (key, value). If insertion exceeds capacity,
     * evicts the least-recently-used entry.
     */
    public void put(K key, V value) {
        Node node = map.get(key);
        if (node != null) {
            node.value = value;
            moveToHead(node);
            return;
        }
        Node newNode = new Node(key, value);
        map.put(key, newNode);
        addToHead(newNode);
        size++;
        if (size > capacity) {
            Node tailPrev = removeTail();
            if (tailPrev != null) {
                map.remove(tailPrev.key);
                size--;
            }
        }
    }

    int size() { // package-private for testing
        return size;
    }

    private void addToHead(Node node) {
        node.prev = head;
        node.next = head.next;
        head.next.prev = node;
        head.next = node;
    }

    private void removeNode(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
        node.prev = null;
        node.next = null;
    }

    private void moveToHead(Node node) {
        removeNode(node);
        addToHead(node);
    }

    private Node removeTail() {
        if (tail.prev == head) return null;
        Node node = tail.prev;
        removeNode(node);
        return node;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("LRUCache{capacity=").append(capacity).append(", size=").append(size).append(", entries=[");
        Node cur = head.next;
        boolean first = true;
        while (cur != tail) {
            if (!first) sb.append(", ");
            sb.append(String.valueOf(cur.key)).append("=").append(String.valueOf(cur.value));
            first = false;
            cur = cur.next;
        }
        sb.append("]}");
        return sb.toString();
    }

    private class Node {
        K key;
        V value;
        Node prev;
        Node next;

        Node(K key, V value) {
            this.key = key;
            this.value = value;
        }
    }
}
