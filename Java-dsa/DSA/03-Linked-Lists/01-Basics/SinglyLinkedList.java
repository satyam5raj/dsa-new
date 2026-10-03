/*
 * ============================================================================
 *  LINKED LISTS 1.1 : SINGLY LINKED LIST - BUILT FROM SCRATCH
 * ============================================================================
 *
 *  Run:  java SinglyLinkedList.java
 *
 *  WHAT IS A LINKED LIST?
 *  A chain of NODES. Each node holds a value and a POINTER (reference) to the next node.
 *  The list only remembers the FIRST node (head) (and often the last, the tail).
 *
 *     head
 *      |
 *    [10 | *]--->[20 | *]--->[30 | *]--->null
 *     node        node        node
 *
 *  ARRAY vs LINKED LIST
 *  ------------------------------------------------------------------------
 *                         Array / ArrayList          Linked list
 *  ------------------------------------------------------------------------
 *   memory                one contiguous block        nodes scattered on the heap
 *   access list[i]        O(1) (address math)         O(n) (walk from the head)
 *   insert/delete FRONT   O(n) (shift everything)     O(1) (relink the head)
 *   insert/delete MIDDLE  O(n) (shift)                O(1) IF you already hold the
 *                                                     previous node, O(n) to find it
 *   extra memory          none per element            one pointer per node (+ object header)
 *   CPU cache             excellent                   poor (pointer chasing)
 *  ------------------------------------------------------------------------
 *  In everyday Java, ArrayList (and ArrayDeque) win almost always. Linked lists matter for:
 *    - interviews (pointer manipulation is a favourite topic)
 *    - O(1) splicing/removal when you hold the node (LRU cache, see DoublyLinkedList)
 *    - building blocks: hash table chains, adjacency lists, free lists
 *
 *  GOLDEN RULES OF POINTER CODE
 *    1. DRAW IT. Boxes and arrows on paper, before and after.
 *    2. Before overwriting a 'next' pointer, SAVE what it pointed to.
 *    3. Always check: empty list? one node? operating on the head? on the tail?
 *    4. A DUMMY (sentinel) node before the head removes most "is it the head?" special cases.
 */

import java.util.*;

public class SinglyLinkedList implements Iterable<Integer> {

    // ---------------- the node ----------------
    private static class Node {
        int val;
        Node next;

        Node(int val) { this.val = val; }
    }

    private Node head;     // first node (null when empty)
    private Node tail;     // last node  (lets addLast be O(1))
    private int size;

    // =================================================================
    // INSERT
    // =================================================================

    // O(1): new node points to the old head, then becomes the head
    public void addFirst(int val) {
        Node node = new Node(val);
        node.next = head;
        head = node;
        if (tail == null) tail = node;          // the list was empty
        size++;
    }

    // O(1) thanks to the tail pointer (O(n) without it: walk to the end)
    public void addLast(int val) {
        Node node = new Node(val);
        if (tail == null) {
            head = tail = node;
        } else {
            tail.next = node;
            tail = node;
        }
        size++;
    }

    // O(n): walk to the node BEFORE the position, then splice in
    //   before: prev -> next        after: prev -> node -> next
    public void addAt(int index, int val) {
        if (index < 0 || index > size) throw new IndexOutOfBoundsException("index " + index);
        if (index == 0) { addFirst(val); return; }
        if (index == size) { addLast(val); return; }
        Node prev = nodeAt(index - 1);
        Node node = new Node(val);
        node.next = prev.next;                  // 1. new node points to the rest  (do this FIRST)
        prev.next = node;                       // 2. prev points to the new node
        size++;
    }

    // =================================================================
    // DELETE
    // =================================================================

    // O(1)
    public int removeFirst() {
        if (head == null) throw new NoSuchElementException("empty list");
        int val = head.val;
        head = head.next;                       // the old head becomes unreachable -> GC frees it
        if (head == null) tail = null;
        size--;
        return val;
    }

    // O(n) in a SINGLY linked list: we need the node BEFORE the tail, and
    // there's no back pointer. (A doubly linked list does this in O(1).)
    public int removeLast() {
        if (head == null) throw new NoSuchElementException("empty list");
        if (head == tail) return removeFirst();
        Node prev = head;
        while (prev.next != tail) prev = prev.next;
        int val = tail.val;
        prev.next = null;
        tail = prev;
        size--;
        return val;
    }

    // O(n): remove the first node holding 'val'. Uses a DUMMY node so removing
    // the head needs no special case.
    public boolean removeValue(int val) {
        Node dummy = new Node(0);
        dummy.next = head;
        Node prev = dummy;
        while (prev.next != null && prev.next.val != val) prev = prev.next;
        if (prev.next == null) return false;    // not found
        if (prev.next == tail) tail = prev == dummy ? null : prev;
        prev.next = prev.next.next;             // skip over the node
        head = dummy.next;
        size--;
        return true;
    }

    // =================================================================
    // ACCESS / SEARCH: O(n)
    // =================================================================
    public int get(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException("index " + index);
        return nodeAt(index).val;
    }

    public int indexOf(int val) {
        int i = 0;
        for (Node cur = head; cur != null; cur = cur.next, i++) {
            if (cur.val == val) return i;
        }
        return -1;
    }

    private Node nodeAt(int index) {
        Node cur = head;
        for (int i = 0; i < index; i++) cur = cur.next;
        return cur;
    }

    public int size() { return size; }

    // =================================================================
    // REVERSE - THE most important linked list operation (learn it cold!)
    //
    //   prev = null, cur = head
    //   repeat:  next = cur.next      (1. save the rest of the list)
    //            cur.next = prev      (2. flip the arrow backwards)
    //            prev = cur           (3. advance prev)
    //            cur = next           (4. advance cur)
    //
    //   null  1 -> 2 -> 3 -> null
    //   null <- 1    2 -> 3        after step 1
    //   null <- 1 <- 2    3        after step 2
    //   null <- 1 <- 2 <- 3        done: prev (3) is the new head
    // =================================================================
    public void reverse() {
        Node prev = null, cur = head;
        tail = head;                            // the old head becomes the tail
        while (cur != null) {
            Node next = cur.next;
            cur.next = prev;
            prev = cur;
            cur = next;
        }
        head = prev;
    }

    // Recursive version: reverse the rest, then hook the current node at the end.
    //   reverse(1->2->3): reverse(2->3) gives 3->2, and 2 is still 1.next,
    //   so set 2.next = 1 and 1.next = null.  Uses O(n) stack space.
    private Node reverseRecursive(Node node) {
        if (node == null || node.next == null) return node;   // base: the last node is the new head
        Node newHead = reverseRecursive(node.next);
        node.next.next = node;                                // the next node points back to me
        node.next = null;
        return newHead;
    }

    public void reverseUsingRecursion() {
        tail = head;
        head = reverseRecursive(head);
    }

    // Iterable -> for (int x : list)
    @Override
    public Iterator<Integer> iterator() {
        return new Iterator<>() {
            Node cur = head;
            public boolean hasNext() { return cur != null; }
            public Integer next() {
                int v = cur.val;
                cur = cur.next;
                return v;
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Node cur = head; cur != null; cur = cur.next) sb.append(cur.val).append(" -> ");
        return sb.append("null").toString();
    }

    // =================================================================
    // DEMO
    // =================================================================
    public static void main(String[] args) {
        SinglyLinkedList list = new SinglyLinkedList();
        System.out.println("===== INSERT =====");
        list.addLast(20);
        list.addLast(30);
        list.addFirst(10);
        System.out.println("   addLast 20, 30, addFirst 10 : " + list);
        list.addAt(2, 25);
        System.out.println("   addAt(2, 25)                : " + list);

        System.out.println("\n===== ACCESS / SEARCH (O(n)) =====");
        System.out.println("   get(2) = " + list.get(2) + ", indexOf(30) = " + list.indexOf(30) + ", indexOf(99) = " + list.indexOf(99));

        System.out.println("\n===== REVERSE =====");
        list.reverse();
        System.out.println("   iterative : " + list);
        list.reverseUsingRecursion();
        System.out.println("   recursive : " + list + "   (back to the original)");

        System.out.println("\n===== DELETE =====");
        System.out.println("   removeFirst() = " + list.removeFirst() + " -> " + list);
        System.out.println("   removeLast()  = " + list.removeLast() + " -> " + list + "  (O(n): needs the node before the tail)");
        System.out.println("   removeValue(25) = " + list.removeValue(25) + " -> " + list);
        System.out.println("   removeValue(99) = " + list.removeValue(99));

        System.out.print("\n===== ITERATE =====\n   for-each:");
        for (int x : list) System.out.print(" " + x);
        System.out.println("   size = " + list.size());

        // Self-check against java.util.LinkedList with random operations
        Random rnd = new Random(1);
        SinglyLinkedList mine = new SinglyLinkedList();
        LinkedList<Integer> ref = new LinkedList<>();
        boolean ok = true;
        for (int i = 0; i < 5000; i++) {
            int op = rnd.nextInt(6), v = rnd.nextInt(10);
            switch (op) {
                case 0 -> { mine.addFirst(v); ref.addFirst(v); }
                case 1 -> { mine.addLast(v); ref.addLast(v); }
                case 2 -> { int idx = rnd.nextInt(ref.size() + 1); mine.addAt(idx, v); ref.add(idx, v); }
                case 3 -> { if (!ref.isEmpty()) ok &= mine.removeFirst() == ref.removeFirst(); }
                case 4 -> { if (!ref.isEmpty()) ok &= mine.removeLast() == ref.removeLast(); }
                default -> ok &= mine.removeValue(v) == ref.remove(Integer.valueOf(v));
            }
            if (rnd.nextInt(50) == 0) { mine.reverse(); Collections.reverse(ref); }
            List<Integer> got = new ArrayList<>();
            for (int x : mine) got.add(x);
            ok &= got.equals(ref) && mine.size() == ref.size();
        }
        System.out.println("\n===== 5,000 random operations vs java.util.LinkedList: " + (ok ? "all match" : "MISMATCH") + " =====");
        if (!ok) System.exit(1);
    }
}
