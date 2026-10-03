/*
 * ============================================================================
 *  SKIP LIST: a sorted linked list with "express lanes"
 * ============================================================================
 *
 *  PROBLEM WITH A SORTED LINKED LIST: search is O(n) - no random access, so no
 *  binary search.
 *
 *  IDEA: add EXPRESS LANES. Level 0 is the full sorted list. Each node is also
 *  promoted to level 1 with probability 1/2, to level 2 with probability 1/4, ...
 *
 *    level 3:  head ------------------------------> 50 -----------------> nil
 *    level 2:  head ----------> 20 ---------------> 50 -----------------> nil
 *    level 1:  head ----> 10 -> 20 ------> 35 ----> 50 ------> 70 ------> nil
 *    level 0:  head -> 5 -> 10 -> 20 -> 30 -> 35 -> 50 -> 60 -> 70 -> 80 -> nil
 *
 *  SEARCH(x): start at the TOP level of head; move right while the next key < x;
 *  then drop down a level; repeat. At level 0, the next node is x or doesn't exist.
 *  Like binary search: each level roughly halves the remaining distance -> expected
 *  O(log n) steps.
 *
 *  INSERT(x): do the search, remembering at each level the LAST node before x
 *  ("update[level]"). Flip coins for x's height h; splice x in after update[0..h-1].
 *  DELETE(x): same search; unlink x at every level where it appears.
 *  All expected O(log n); space expected O(n) (n + n/2 + n/4 + ... = 2n pointers).
 *
 *  WHY USE IT INSTEAD OF A BALANCED BST?
 *    - much simpler code: no rotations (compare AVLTree in 07-Trees)
 *    - easy to make CONCURRENT: inserts change only local pointers
 *      -> Java's ConcurrentSkipListMap / ConcurrentSkipListSet
 *    - Redis sorted sets (ZSET) and LevelDB/RocksDB memtables use skip lists
 *  The price: guarantees are only EXPECTED (probabilistic), not worst case.
 *
 *  Run:  java SkipListBasics.java
 * ============================================================================
 */

import java.util.*;

public class SkipListBasics {

    static class SkipList {
        static final int MAX_LEVEL = 32;

        static class Node {
            final int key;
            final Node[] next;
            Node(int key, int levels) { this.key = key; next = new Node[levels]; }
        }

        private final Node head = new Node(Integer.MIN_VALUE, MAX_LEVEL);
        private int level = 1;                           // levels currently in use
        private int size;
        private final Random rnd;
        long steps;                                      // pointer moves during searches (for the experiment)

        SkipList(long seed) { rnd = new Random(seed); }

        private int randomLevel() {
            int l = 1;
            while (l < MAX_LEVEL && rnd.nextBoolean()) l++;   // promote with probability 1/2 each time
            return l;
        }

        boolean contains(int key) {
            Node x = head;
            for (int i = level - 1; i >= 0; i--) {
                while (x.next[i] != null && x.next[i].key < key) { x = x.next[i]; steps++; }   // move right
            }                                                                               // then drop down
            x = x.next[0];
            return x != null && x.key == key;
        }

        boolean add(int key) {
            Node[] update = new Node[MAX_LEVEL];
            Node x = head;
            for (int i = level - 1; i >= 0; i--) {
                while (x.next[i] != null && x.next[i].key < key) x = x.next[i];
                update[i] = x;                                  // last node before key on level i
            }
            if (x.next[0] != null && x.next[0].key == key) return false;   // a set: no duplicates
            int h = randomLevel();
            if (h > level) {
                for (int i = level; i < h; i++) update[i] = head;
                level = h;
            }
            Node n = new Node(key, h);
            for (int i = 0; i < h; i++) {                       // splice in on every level of its height
                n.next[i] = update[i].next[i];
                update[i].next[i] = n;
            }
            size++;
            return true;
        }

        boolean remove(int key) {
            Node[] update = new Node[MAX_LEVEL];
            Node x = head;
            for (int i = level - 1; i >= 0; i--) {
                while (x.next[i] != null && x.next[i].key < key) x = x.next[i];
                update[i] = x;
            }
            Node target = x.next[0];
            if (target == null || target.key != key) return false;
            for (int i = 0; i < target.next.length; i++) update[i].next[i] = target.next[i];   // unlink everywhere
            while (level > 1 && head.next[level - 1] == null) level--;                          // drop empty top levels
            size--;
            return true;
        }

        List<Integer> toList() {
            List<Integer> out = new ArrayList<>();
            for (Node n = head.next[0]; n != null; n = n.next[0]) out.add(n.key);
            return out;
        }

        int size() { return size; }
        int levels() { return level; }
    }

    public static void main(String[] args) {
        System.out.println("1. Basic operations");
        SkipList s = new SkipList(1);
        for (int x : new int[]{30, 10, 50, 20, 70, 5}) s.add(x);
        check("sorted at level 0", "[5, 10, 20, 30, 50, 70]", s.toList());
        check("contains 20", true, s.contains(20));
        check("contains 25", false, s.contains(25));
        check("add duplicate 30", false, s.add(30));
        check("remove 20", true, s.remove(20));
        check("after remove", "[5, 10, 30, 50, 70]", s.toList());

        System.out.println("2. 200,000 random ops vs TreeSet");
        Random rnd = new Random(2);
        SkipList mine = new SkipList(3);
        TreeSet<Integer> ref = new TreeSet<>();
        boolean ok = true;
        for (int i = 0; i < 200_000; i++) {
            int k = rnd.nextInt(5000);
            switch (rnd.nextInt(3)) {
                case 0 -> ok &= mine.add(k) == ref.add(k);
                case 1 -> ok &= mine.remove(k) == ref.remove(k);
                default -> ok &= mine.contains(k) == ref.contains(k);
            }
        }
        ok &= mine.toList().equals(new ArrayList<>(ref)) && mine.size() == ref.size();
        check("skip list == TreeSet", true, ok);

        System.out.println("3. Search cost grows like log n");
        for (int n : new int[]{1_000, 10_000, 100_000, 1_000_000}) {
            SkipList big = new SkipList(4);
            for (int i = 0; i < n; i++) big.add(i * 2);
            big.steps = 0;
            int queries = 10_000;
            for (int q = 0; q < queries; q++) big.contains(rnd.nextInt(2 * n));
            double avg = big.steps / (double) queries;
            System.out.printf("   n = %,9d   levels = %2d   avg moves per search = %5.1f   (log2 n = %.1f)%n",
                    n, big.levels(), avg, Math.log(n) / Math.log(2));
            if (n == 1_000_000) check("avg moves for n = 10^6 stays small (< 60)", true, avg < 60);
        }
        summary();
    }

    // ---------------- tiny test helper (same in every problem file) ----------------
    static int passed = 0, total = 0;

    static void check(String label, Object expected, Object actual) {
        String e = fmt(expected), a = fmt(actual);
        total++;
        if (e.equals(a)) passed++;
        System.out.printf("   %s  %-34s -> %s%s%n", e.equals(a) ? "PASS" : "FAIL", label, a, e.equals(a) ? "" : "   (expected " + e + ")");
    }

    static String fmt(Object o) {
        if (o instanceof int[] x) return Arrays.toString(x);
        if (o instanceof int[][] x) return Arrays.deepToString(x);
        if (o instanceof char[] x) return Arrays.toString(x);
        if (o instanceof char[][] x) return Arrays.deepToString(x);
        return String.valueOf(o);
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " tests passed");
        if (passed != total) System.exit(1);
    }
}
