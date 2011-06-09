package com.rabbitmq.client.impl;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

/**
 * A generic queue-like implementation (only supporting operations <code>add</code>,
 * <code>poll</code>, <code>contains</code>, and <code>isEmpty</code>)
 * which restricts a queue element to appear at most once.
 * If the element is already present {@link #addIfNotPresent(T)} returns <code><b>false</b></code>.
 * <p/><b>Concurrent Semantics</b><br/>
 * This implementation is <i>not</i> thread-safe.
 * @param <T> type of elements in the queue
 */
public class SetQueue<T> {
    private final Set<T> members = new HashSet<T>();
    private final Queue<T> queue = new LinkedList<T>();

    /**
     * Add an element to the back of the queue, or else return false.
     * @param item to add
     * @return <b><code>true</code></b> if the element was added, <b><code>false</code></b> if it is already present.
     */
    public boolean addIfNotPresent(T item) {
        if (this.members.contains(item)) {
            return false;
        }
        this.members.add(item);
        this.queue.offer(item);
        return true;
    }

    /**
     * Remove the head of the queue and return it.
     * @return head of the queue, or <b><code>null</code></b> if the queue is empty.
     */
    public T poll() {
        T item =  this.queue.poll();
        if (item != null) {
            this.members.remove(item);
        }
        return item;
    }

    /**
     * @param item to check for
     * @return <code><b>true</b></code> if and only if <b>item</b> is in the queue.
     */
    public boolean contains(T item) {
        return this.members.contains(item);
    }

    /**
     * @return <code><b>true</b></code> if and only if the queue is empty.
     */
    public boolean isEmpty() {
        return this.members.isEmpty();
    }
}
