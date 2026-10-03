/*
 * ============================================================================
 *  TOPIC 3.4 : QUEUE, DEQUE, STACK, PRIORITYQUEUE
 * ============================================================================
 *
 *  Run:  java QueueDequeDemo.java
 *
 *  QUEUE  = FIFO (first in, first out)  -> BFS, task scheduling
 *  STACK  = LIFO (last in, first out)   -> DFS, undo, parentheses matching
 *  DEQUE  = "double-ended queue": add/remove at BOTH ends -> can be either.
 *  PRIORITYQUEUE = always removes the SMALLEST (by default) element -> heap.
 *
 *  TWO STYLES OF METHODS IN Queue:
 *  ------------------------------------------------------------
 *                  throws exception      returns special value
 *   insert         add(e)                offer(e)   -> false
 *   remove         remove()              poll()     -> null
 *   examine        element()             peek()     -> null
 *  ------------------------------------------------------------
 *  Prefer offer/poll/peek in algorithms.
 *
 *  WHICH CLASS?
 *   - Queue  -> ArrayDeque  (faster than LinkedList, circular array inside)
 *   - Stack  -> ArrayDeque  (java.util.Stack is legacy: extends Vector,
 *                            synchronized, slow)
 *   - Heap   -> PriorityQueue (binary heap in an array)
 *  ArrayDeque does NOT allow null elements (null is used as "empty" marker).
 *
 *  PRIORITYQUEUE COMPLEXITY
 *   offer/poll   O(log n)   (sift up / sift down)
 *   peek         O(1)
 *   remove(obj)  O(n)
 *   building from a collection: O(n) (heapify)
 *   Iterating does NOT give sorted order! Only poll() order is sorted.
 */

import java.util.*;

public class QueueDequeDemo {

    public static void main(String[] args) {
        queue();
        stack();
        deque();
        priorityQueue();
        dsaPatterns();
    }

    // =================================================================
    // 1. Queue (FIFO)
    // =================================================================
    static void queue() {
        System.out.println("===== 1. QUEUE =====");
        Queue<String> q = new ArrayDeque<>();
        q.offer("A");                 // add at the tail
        q.offer("B");
        q.offer("C");
        System.out.println("queue = " + q);
        System.out.println("peek  = " + q.peek());     // look at head, don't remove
        System.out.println("poll  = " + q.poll());     // remove head -> "A"
        System.out.println("after = " + q);

        Queue<String> empty = new ArrayDeque<>();
        System.out.println("poll on empty = " + empty.poll());   // null, no exception
        try {
            empty.remove();
        } catch (NoSuchElementException e) {
            System.out.println("remove on empty -> NoSuchElementException");
        }
    }

    // =================================================================
    // 2. Stack (LIFO) using ArrayDeque
    //    push = addFirst, pop = removeFirst, peek = peekFirst
    // =================================================================
    static void stack() {
        System.out.println("\n===== 2. STACK =====");
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(1);
        stack.push(2);
        stack.push(3);
        System.out.println("stack (top first) = " + stack);
        System.out.println("peek = " + stack.peek());
        System.out.println("pop  = " + stack.pop());
        System.out.println("after = " + stack + ", isEmpty = " + stack.isEmpty());
    }

    // =================================================================
    // 3. Deque - both ends
    // =================================================================
    static void deque() {
        System.out.println("\n===== 3. DEQUE =====");
        Deque<Integer> dq = new ArrayDeque<>();
        dq.offerFirst(2);    // [2]
        dq.offerFirst(1);    // [1, 2]
        dq.offerLast(3);     // [1, 2, 3]
        System.out.println("deque = " + dq);
        System.out.println("pollFirst = " + dq.pollFirst() + ", pollLast = " + dq.pollLast() + " -> " + dq);
    }

    // =================================================================
    // 4. PriorityQueue (binary heap)
    //    A binary heap is a complete binary tree stored in an array:
    //       parent(i) = (i - 1) / 2,  left(i) = 2i + 1,  right(i) = 2i + 2
    //    Min-heap property: parent <= children, so the root is the minimum.
    // =================================================================
    static void priorityQueue() {
        System.out.println("\n===== 4. PRIORITYQUEUE =====");

        PriorityQueue<Integer> minHeap = new PriorityQueue<>();          // smallest first
        PriorityQueue<Integer> maxHeap = new PriorityQueue<>(Collections.reverseOrder());
        for (int n : new int[]{5, 1, 8, 3, 9, 2}) {
            minHeap.offer(n);
            maxHeap.offer(n);
        }
        System.out.println("internal array (NOT sorted) = " + minHeap);
        System.out.print("minHeap poll order: ");
        while (!minHeap.isEmpty()) System.out.print(minHeap.poll() + " ");
        System.out.print("\nmaxHeap poll order: ");
        while (!maxHeap.isEmpty()) System.out.print(maxHeap.poll() + " ");
        System.out.println();

        // Custom objects: tasks by priority, then by name
        record Task(String name, int priority) {}
        PriorityQueue<Task> tasks = new PriorityQueue<>(
                Comparator.comparingInt(Task::priority).thenComparing(Task::name));
        tasks.offer(new Task("write tests", 2));
        tasks.offer(new Task("fix prod bug", 1));
        tasks.offer(new Task("code review", 2));
        System.out.print("tasks: ");
        while (!tasks.isEmpty()) System.out.print(tasks.poll().name() + " | ");
        System.out.println();

        // int[] in a heap: e.g. [distance, node] for Dijkstra
        PriorityQueue<int[]> pq = new PriorityQueue<>((a, b) -> Integer.compare(a[0], b[0]));
        pq.offer(new int[]{7, 1});
        pq.offer(new int[]{3, 2});
        System.out.println("smallest distance entry = " + Arrays.toString(pq.peek()));
    }

    // =================================================================
    // 5. Classic DSA patterns using these structures
    // =================================================================
    static void dsaPatterns() {
        System.out.println("\n===== 5. DSA PATTERNS =====");
        System.out.println("valid parentheses \"{[()]}\" = " + isValid("{[()]}"));
        System.out.println("valid parentheses \"([)]\"   = " + isValid("([)]"));
        System.out.println("kth largest (k=2) in [3,2,1,5,6,4] = " + kthLargest(new int[]{3, 2, 1, 5, 6, 4}, 2));
        System.out.println("BFS order from 0 = " + bfs());
        System.out.println("next greater [2,1,2,4,3] = " + Arrays.toString(nextGreater(new int[]{2, 1, 2, 4, 3})));
    }

    // STACK: push openings; on a closing bracket, top must be its matching opening.
    static boolean isValid(String s) {
        Deque<Character> st = new ArrayDeque<>();
        for (char c : s.toCharArray()) {
            if (c == '(' || c == '{' || c == '[') {
                st.push(c);
            } else {
                if (st.isEmpty()) return false;
                char open = st.pop();
                if ((c == ')' && open != '(') || (c == '}' && open != '{') || (c == ']' && open != '[')) {
                    return false;
                }
            }
        }
        return st.isEmpty();   // leftover openings -> invalid
    }

    // HEAP: keep a MIN-heap of size k. Its root is the k-th largest. O(n log k).
    static int kthLargest(int[] nums, int k) {
        PriorityQueue<Integer> heap = new PriorityQueue<>();
        for (int n : nums) {
            heap.offer(n);
            if (heap.size() > k) heap.poll();   // drop the smallest
        }
        return heap.peek();
    }

    // QUEUE: breadth-first search on a small graph
    static List<Integer> bfs() {
        List<List<Integer>> adj = List.of(
                List.of(1, 2),   // 0 -> 1, 2
                List.of(3),      // 1 -> 3
                List.of(3),      // 2 -> 3
                List.of());      // 3
        List<Integer> order = new ArrayList<>();
        boolean[] visited = new boolean[adj.size()];
        Queue<Integer> q = new ArrayDeque<>();
        q.offer(0);
        visited[0] = true;          // mark when ENQUEUED, not when dequeued (avoids duplicates)
        while (!q.isEmpty()) {
            int node = q.poll();
            order.add(node);
            for (int nb : adj.get(node)) {
                if (!visited[nb]) {
                    visited[nb] = true;
                    q.offer(nb);
                }
            }
        }
        return order;
    }

    // MONOTONIC STACK: for each element, the next element to its right that is greater.
    // Stack holds indices whose answer is not found yet (values decreasing). O(n).
    static int[] nextGreater(int[] nums) {
        int[] res = new int[nums.length];
        Arrays.fill(res, -1);
        Deque<Integer> st = new ArrayDeque<>();
        for (int i = 0; i < nums.length; i++) {
            while (!st.isEmpty() && nums[i] > nums[st.peek()]) {
                res[st.pop()] = nums[i];   // nums[i] is the answer for that index
            }
            st.push(i);
        }
        return res;
    }
}
