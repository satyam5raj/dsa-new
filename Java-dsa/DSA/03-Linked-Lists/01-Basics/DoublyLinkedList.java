/*
 * ============================================================================
 *  LINKED LISTS 1.2 : DOUBLY LINKED LIST (with SENTINEL nodes)
 * ============================================================================
 *
 *  Run:  java DoublyLinkedList.java
 *
 *  Each node has TWO pointers: prev and next.
 *
 *    HEAD(sentinel) <-> [10] <-> [20] <-> [30] <-> TAIL(sentinel)
 *
 *  WHAT THE EXTRA POINTER BUYS
 *    - removeLast() in O(1) (the tail knows its prev)
 *    - remove(node) in O(1) when you HOLD the node: unlink it from both neighbours
 *    - walk in both directions
 *  COST: one more pointer per node, and every insert/delete updates 4 pointers.
 *
 *  SENTINELS (dummy head and tail that never hold data)
 *    Without them, every operation needs "is the list empty? is this the first
 *    node? the last?" checks. With them, EVERY real node always has a real prev
 *    and next, so insert and remove are the same 4 lines everywhere.
 *
 *  THIS IS THE DATA STRUCTURE BEHIND:
 *    - java.util.LinkedList (a doubly linked list)
 *    - the LRU cache (HashMap key -> node, plus a DLL ordered by recency: O(1) get and put)
 *    - LinkedHashMap's ordering
 *    - browser back/forward history, text editor undo lists
 */

import java.util.*;

public class DoublyLinkedList {

    static class Node {
        int val;
        Node prev, next;

        Node(int val) { this.val = val; }
    }

    private final Node head = new Node(0);      // sentinel before the first real node
    private final Node tail = new Node(0);      // sentinel after the last real node
    private int size;

    public DoublyLinkedList() {
        head.next = tail;
        tail.prev = head;
    }

    // ---------------------------------------------------------------
    // The ONE insert primitive: put 'node' right after 'prev'.
    //   before:  prev <-> nxt
    //   after:   prev <-> node <-> nxt
    // ---------------------------------------------------------------
    private void insertAfter(Node prev, Node node) {
        Node nxt = prev.next;
        node.prev = prev;
        node.next = nxt;
        prev.next = node;
        nxt.prev = node;
        size++;
    }

    // ---------------------------------------------------------------
    // The ONE remove primitive: unlink a node in O(1). Its neighbours point
    // at each other, skipping it.
    // ---------------------------------------------------------------
    public void remove(Node node) {
        node.prev.next = node.next;
        node.next.prev = node.prev;
        node.prev = node.next = null;           // tidy (helps catch bugs and the GC)
        size--;
    }

    // Everything else is a one-liner built on those two primitives: all O(1)
    public Node addFirst(int val) { Node n = new Node(val); insertAfter(head, n); return n; }
    public Node addLast(int val) { Node n = new Node(val); insertAfter(tail.prev, n); return n; }

    public int removeFirst() {
        if (size == 0) throw new NoSuchElementException();
        Node n = head.next;
        remove(n);
        return n.val;
    }

    public int removeLast() {
        if (size == 0) throw new NoSuchElementException();
        Node n = tail.prev;
        remove(n);
        return n.val;
    }

    // Move an existing node to the front: O(1). (The LRU cache's "mark as recently used".)
    public void moveToFront(Node node) {
        remove(node);
        insertAfter(head, node);
    }

    // Reverse: swap prev/next in every node, then swap the sentinels' roles
    public void reverse() {
        // EDGE CASE (found by the random test below!): with 0 or 1 real nodes there's
        // nothing to reverse. With 0 nodes, the sentinel rewiring below would make
        // head.next point at head itself -> an infinite loop when traversing.
        if (size < 2) return;
        Node cur = head;
        while (cur != null) {
            Node t = cur.next;
            cur.next = cur.prev;
            cur.prev = t;
            cur = t;                            // t is the ORIGINAL next
        }
        // now the old tail sentinel is at the front; rewire the sentinels
        Node first = tail.next, last = head.prev;
        head.next = first;   first.prev = head;
        tail.prev = last;    last.next = tail;
        head.prev = null;    tail.next = null;
    }

    public int size() { return size; }

    public String forward() {
        StringBuilder sb = new StringBuilder("HEAD");
        for (Node c = head.next; c != tail; c = c.next) sb.append(" <-> ").append(c.val);
        return sb.append(" <-> TAIL").toString();
    }

    public String backward() {
        StringBuilder sb = new StringBuilder("TAIL");
        for (Node c = tail.prev; c != head; c = c.prev) sb.append(" <-> ").append(c.val);
        return sb.append(" <-> HEAD").toString();
    }

    List<Integer> toList() {
        List<Integer> l = new ArrayList<>();
        for (Node c = head.next; c != tail; c = c.next) l.add(c.val);
        return l;
    }

    public static void main(String[] args) {
        DoublyLinkedList dll = new DoublyLinkedList();
        System.out.println("===== BUILD =====");
        dll.addLast(20);
        Node thirty = dll.addLast(30);
        dll.addFirst(10);
        dll.addLast(40);
        System.out.println("   forward : " + dll.forward());
        System.out.println("   backward: " + dll.backward() + "   (prev pointers work)");

        System.out.println("\n===== O(1) OPERATIONS =====");
        dll.moveToFront(thirty);
        System.out.println("   moveToFront(30)   : " + dll.forward());
        System.out.println("   removeLast() = " + dll.removeLast() + "  : " + dll.forward() + "  (O(1) here, O(n) in a singly list)");
        dll.remove(thirty);
        System.out.println("   remove(node 30)   : " + dll.forward());

        System.out.println("\n===== REVERSE =====");
        dll.addLast(50);
        dll.addLast(60);
        dll.reverse();
        System.out.println("   reversed          : " + dll.forward());
        System.out.println("   backward still OK : " + dll.backward());

        // self-check vs java.util.ArrayDeque
        Random rnd = new Random(2);
        DoublyLinkedList mine = new DoublyLinkedList();
        Deque<Integer> ref = new ArrayDeque<>();
        boolean ok = true;
        for (int i = 0; i < 5000; i++) {
            int v = rnd.nextInt(100);
            switch (rnd.nextInt(5)) {
                case 0 -> { mine.addFirst(v); ref.addFirst(v); }
                case 1 -> { mine.addLast(v); ref.addLast(v); }
                case 2 -> { if (!ref.isEmpty()) ok &= mine.removeFirst() == ref.pollFirst(); }
                case 3 -> { if (!ref.isEmpty()) ok &= mine.removeLast() == ref.pollLast(); }
                default -> {
                    mine.reverse();
                    List<Integer> r = new ArrayList<>(ref);
                    Collections.reverse(r);
                    ref = new ArrayDeque<>(r);
                }
            }
            ok &= mine.toList().equals(new ArrayList<>(ref)) && mine.size() == ref.size();
        }
        System.out.println("\n===== 5,000 random operations vs ArrayDeque: " + (ok ? "all match" : "MISMATCH") + " =====");
        if (!ok) System.exit(1);
    }
}
