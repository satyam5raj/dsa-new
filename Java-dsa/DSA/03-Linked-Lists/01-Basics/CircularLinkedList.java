/*
 * ============================================================================
 *  LINKED LISTS 1.3 : CIRCULAR LINKED LIST (+ the Josephus problem)
 * ============================================================================
 *
 *  Run:  java CircularLinkedList.java
 *
 *  The last node points BACK to the first instead of to null:
 *
 *        +--> [1] --> [2] --> [3] --> [4] --+
 *        |                                  |
 *        +----------------------------------+
 *
 *  THE TRICK: store only a TAIL pointer. Then head = tail.next for free, and
 *    addFirst = insert after tail       O(1)
 *    addLast  = insert after tail, then move tail forward   O(1)
 *    removeFirst = tail.next = tail.next.next               O(1)
 *    rotate by one = tail = tail.next                       O(1)  <- no copying at all!
 *
 *  TRAVERSAL: there's no null at the end! Stop when you're BACK at the start,
 *  otherwise you loop forever:
 *      Node cur = head; do { ...; cur = cur.next; } while (cur != head);
 *
 *  USES: round-robin scheduling (CPU time slices, load balancers cycling
 *  through servers), multiplayer turn order, circular buffers, Josephus-type puzzles.
 */

import java.util.*;

public class CircularLinkedList {

    private static class Node {
        int val;
        Node next;

        Node(int val) { this.val = val; }
    }

    private Node tail;          // tail.next is the head
    private int size;

    public void addFirst(int val) {
        Node n = new Node(val);
        if (tail == null) {
            n.next = n;                 // a single node points to ITSELF
            tail = n;
        } else {
            n.next = tail.next;         // new node -> old head
            tail.next = n;              // tail -> new node (the new head)
        }
        size++;
    }

    public void addLast(int val) {
        addFirst(val);                  // put it at the front...
        tail = tail.next;               // ...then declare it the tail. Same circle, rotated!
    }

    public int removeFirst() {
        if (tail == null) throw new NoSuchElementException();
        Node head = tail.next;
        if (head == tail) tail = null;  // it was the only node
        else tail.next = head.next;
        size--;
        return head.val;
    }

    // Move the first element to the back: O(1), only the tail pointer moves
    public void rotate() {
        if (tail != null) tail = tail.next;
    }

    public int size() { return size; }

    @Override
    public String toString() {
        if (tail == null) return "(empty)";
        StringBuilder sb = new StringBuilder();
        Node cur = tail.next;
        do {                                // do-while: the circle has no null to stop at
            sb.append(cur.val).append(" -> ");
            cur = cur.next;
        } while (cur != tail.next);
        return sb.append("(back to ").append(tail.next.val).append(")").toString();
    }

    // =================================================================
    // JOSEPHUS PROBLEM: n people stand in a circle, every k-th person is
    // eliminated, going around until one remains. Who survives?
    // Simulation with a circular list: O(n * k)
    // (A math recurrence also solves it in O(n): J(1) = 0, J(n) = (J(n-1) + k) % n.)
    // =================================================================
    static int josephusSimulation(int n, int k) {
        CircularLinkedList circle = new CircularLinkedList();
        for (int i = 1; i <= n; i++) circle.addLast(i);
        while (circle.size() > 1) {
            for (int i = 1; i < k; i++) circle.rotate();   // skip k-1 people
            circle.removeFirst();                          // eliminate the k-th
        }
        return circle.removeFirst();
    }

    static int josephusFormula(int n, int k) {
        int pos = 0;                                       // 0-indexed survivor for 1 person
        for (int i = 2; i <= n; i++) pos = (pos + k) % i;
        return pos + 1;
    }

    public static void main(String[] args) {
        CircularLinkedList c = new CircularLinkedList();
        System.out.println("===== BUILD =====");
        c.addLast(2);
        c.addLast(3);
        c.addFirst(1);
        c.addLast(4);
        System.out.println("   " + c);

        System.out.println("\n===== ROTATE (round robin) =====");
        for (int i = 0; i < 3; i++) {
            c.rotate();
            System.out.println("   after rotate: " + c);
        }

        System.out.println("\n===== REMOVE =====");
        System.out.println("   removeFirst() = " + c.removeFirst() + " -> " + c);

        System.out.println("\n===== JOSEPHUS =====");
        System.out.println("   n=7, k=3 -> survivor " + josephusSimulation(7, 3) + " (formula: " + josephusFormula(7, 3) + ")");
        boolean ok = true;
        for (int n = 1; n <= 40; n++)
            for (int k = 1; k <= 6; k++)
                ok &= josephusSimulation(n, k) == josephusFormula(n, k);
        System.out.println("   simulation == formula for all n<=40, k<=6: " + ok);
        if (!ok) System.exit(1);
    }
}
