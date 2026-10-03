/*
 * ============================================================================
 *  HARD 02 : DESIGN SKIPLIST  ⭐ bonus        LeetCode #1206
 *  https://leetcode.com/problems/design-skiplist/
 *  Pattern: skip list WITH DUPLICATES (01-Basics/SkipListBasics was a set)
 * ============================================================================
 *
 *  PROBLEM
 *    Implement search(target), add(num), erase(num) without library structures,
 *    each O(log n) expected. Duplicates ARE allowed; erase removes ONE copy and
 *    returns whether anything was erased.
 *      add 1, add 2, add 3; search(0)=false; add 4; search(1)=true;
 *      erase(0)=false; erase(1)=true; search(1)=false
 *
 *  ---------------------------------------------------------------------------
 *  Differences from the set version in 01-Basics:
 *    add   : DON'T reject an existing key - just insert another node. Because the
 *            search stops BEFORE the first node >= num, the new copy goes in front
 *            of the existing copies; that's fine, order among equals doesn't matter.
 *    erase : find the update[] path to the FIRST node with key == num, and unlink
 *            exactly that node on every level it occupies.
 *  Careful: when unlinking at level i, only unlink if update[i].next[i] IS that
 *  node (a duplicate could sit there instead on a level the target doesn't reach).
 *
 *  Recap of the skip list: level 0 is the full sorted list; each node is promoted
 *  with probability 1/2 per level; search goes right while next < target, then down.
 *
 *  COMPLEXITY   expected O(log n) per operation, O(n) space
 * ============================================================================
 */

import java.util.*;

public class H02_DesignSkiplist {

    static class Skiplist {
        private static final int MAX = 16;
        private static class Node {
            int val;
            Node[] next;
            Node(int val, int levels) { this.val = val; next = new Node[levels]; }
        }

        private final Node head = new Node(-1, MAX);
        private int level = 1;
        private final Random rnd = new Random(7);

        public boolean search(int target) {
            Node x = head;
            for (int i = level - 1; i >= 0; i--) while (x.next[i] != null && x.next[i].val < target) x = x.next[i];
            x = x.next[0];
            return x != null && x.val == target;
        }

        public void add(int num) {
            Node[] update = new Node[MAX];
            Node x = head;
            for (int i = level - 1; i >= 0; i--) {
                while (x.next[i] != null && x.next[i].val < num) x = x.next[i];
                update[i] = x;
            }
            int h = 1;
            while (h < MAX && rnd.nextBoolean()) h++;
            for (int i = level; i < h; i++) update[i] = head;
            level = Math.max(level, h);
            Node n = new Node(num, h);
            for (int i = 0; i < h; i++) { n.next[i] = update[i].next[i]; update[i].next[i] = n; }
        }

        public boolean erase(int num) {
            Node[] update = new Node[MAX];
            Node x = head;
            for (int i = level - 1; i >= 0; i--) {
                while (x.next[i] != null && x.next[i].val < num) x = x.next[i];
                update[i] = x;
            }
            Node target = x.next[0];
            if (target == null || target.val != num) return false;
            for (int i = 0; i < level; i++) {
                if (update[i].next[i] != target) break;          // target doesn't reach this level
                update[i].next[i] = target.next[i];
            }
            while (level > 1 && head.next[level - 1] == null) level--;
            return true;
        }

        List<Integer> toList() {
            List<Integer> out = new ArrayList<>();
            for (Node n = head.next[0]; n != null; n = n.next[0]) out.add(n.val);
            return out;
        }
    }

    public static void main(String[] args) {
        Skiplist s = new Skiplist();
        s.add(1); s.add(2); s.add(3);
        check("search 0", false, s.search(0));
        s.add(4);
        check("search 1", true, s.search(1));
        check("erase 0", false, s.erase(0));
        check("erase 1", true, s.erase(1));
        check("search 1", false, s.search(1));
        Skiplist d = new Skiplist();
        d.add(5); d.add(5); d.add(5);
        d.erase(5);
        check("duplicates: one copy erased", "[5, 5]", d.toList());

        Random rnd = new Random(2);
        Skiplist mine = new Skiplist();
        TreeMap<Integer, Integer> counts = new TreeMap<>();
        boolean ok = true;
        for (int i = 0; i < 100_000; i++) {
            int k = rnd.nextInt(200);
            switch (rnd.nextInt(3)) {
                case 0 -> { mine.add(k); counts.merge(k, 1, Integer::sum); }
                case 1 -> {
                    boolean had = counts.containsKey(k);
                    ok &= mine.erase(k) == had;
                    if (had && counts.merge(k, -1, Integer::sum) == 0) counts.remove(k);
                }
                default -> ok &= mine.search(k) == counts.containsKey(k);
            }
        }
        List<Integer> exp = new ArrayList<>();
        counts.forEach((k, c) -> { for (int i = 0; i < c; i++) exp.add(k); });
        ok &= mine.toList().equals(exp);
        check("100,000 random ops vs a TreeMap multiset", true, ok);
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
