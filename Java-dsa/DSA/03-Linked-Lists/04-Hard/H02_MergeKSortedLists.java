/*
 * ============================================================================
 *  HARD 02 : MERGE K SORTED LISTS            LeetCode #23
 *  https://leetcode.com/problems/merge-k-sorted-lists/
 *  Pattern: MIN-HEAP of list heads  /  DIVIDE & CONQUER pairwise merging
 * ============================================================================
 *
 *  PROBLEM
 *    Merge k sorted lists into one sorted list.
 *      [1->4->5, 1->3->4, 2->6] -> 1->1->2->3->4->4->5->6
 *    N = total number of nodes, k = number of lists.
 *
 *  ---------------------------------------------------------------------------
 *  APPROACH 1: MERGE ONE BY ONE  (result = merge(result, list_i))
 *    The growing result gets re-scanned k times -> O(k * N). Too slow for big k.
 *
 *  APPROACH 2: MIN-HEAP (PriorityQueue)                      O(N log k), O(k) space
 *    The next smallest node overall is always one of the k current HEADS.
 *    Keep those k heads in a min-heap:
 *      push every non-null head
 *      while the heap isn't empty:
 *         node = poll()  (the smallest head)  -> append it to the result
 *         if node.next != null: push node.next  (that list's new head)
 *    Each of the N nodes is pushed and popped once: O(log k) each.
 *
 *  APPROACH 3: DIVIDE & CONQUER                              O(N log k), O(log k) stack
 *    Merge the lists in PAIRS, like the merge step of merge sort:
 *      round 1: (L0+L1), (L2+L3), ...   -> k/2 lists
 *      round 2: pairs again              -> k/4 lists ... -> 1 list
 *    log k rounds, each touching all N nodes once -> O(N log k), and no heap needed.
 *
 *  DRY RUN (heap) lists [1,4,5] [1,3,4] [2,6]
 *    heap {1a,1b,2} -> take 1a, push 4a -> heap {1b,2,4a}
 *    take 1b, push 3 -> {2,3,4a} -> take 2, push 6 -> {3,4a,6} -> take 3, push 4b ...
 *    result 1,1,2,3,4,4,5,6
 *
 *  INTERVIEW TIP: give the heap solution, mention divide & conquer as the
 *  no-extra-structure alternative, and state both complexities.
 * ============================================================================
 */

import java.util.*;

public class H02_MergeKSortedLists {

    // Approach 2: min-heap
    static ListNode mergeKLists(ListNode[] lists) {
        PriorityQueue<ListNode> heap = new PriorityQueue<>(Comparator.comparingInt(n -> n.val));
        for (ListNode head : lists) if (head != null) heap.offer(head);

        ListNode dummy = new ListNode(0), tail = dummy;
        while (!heap.isEmpty()) {
            ListNode smallest = heap.poll();
            tail.next = smallest;
            tail = smallest;
            if (smallest.next != null) heap.offer(smallest.next);   // that list's next head
        }
        return dummy.next;
    }

    // Approach 3: divide & conquer
    static ListNode mergeKListsDivide(ListNode[] lists) {
        if (lists.length == 0) return null;
        return mergeRange(lists, 0, lists.length - 1);
    }

    static ListNode mergeRange(ListNode[] lists, int lo, int hi) {
        if (lo == hi) return lists[lo];
        int mid = (lo + hi) / 2;
        return mergeTwo(mergeRange(lists, lo, mid), mergeRange(lists, mid + 1, hi));
    }

    static ListNode mergeTwo(ListNode a, ListNode b) {
        ListNode dummy = new ListNode(0), t = dummy;
        while (a != null && b != null) {
            if (a.val <= b.val) { t.next = a; a = a.next; }
            else { t.next = b; b = b.next; }
            t = t.next;
        }
        t.next = (a != null) ? a : b;
        return dummy.next;
    }

    public static void main(String[] args) {
        check("[1,4,5],[1,3,4],[2,6]", "[1, 1, 2, 3, 4, 4, 5, 6]",
                mergeKLists(new ListNode[]{build(1, 4, 5), build(1, 3, 4), build(2, 6)}));
        check("[] (no lists)", "null", mergeKLists(new ListNode[]{}));
        check("[[]] (one empty list)", "null", mergeKLists(new ListNode[]{null}));
        check("divide & conquer version", "[1, 1, 2, 3, 4, 4, 5, 6]",
                mergeKListsDivide(new ListNode[]{build(1, 4, 5), build(1, 3, 4), build(2, 6)}));
        Random rnd = new Random(23);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int k = rnd.nextInt(8);
            ListNode[] a = new ListNode[k], b = new ListNode[k];
            List<Integer> all = new ArrayList<>();
            for (int i = 0; i < k; i++) {
                int[] v = rnd.ints(rnd.nextInt(6), -10, 10).sorted().toArray();
                for (int x : v) all.add(x);
                a[i] = build(v);
                b[i] = build(v);
            }
            Collections.sort(all);
            String exp = all.isEmpty() ? "null" : all.toString();
            ok &= String.valueOf(mergeKLists(a)).equals(exp) && String.valueOf(mergeKListsDivide(b)).equals(exp);
        }
        check("300 random cases (both approaches) vs sorting", true, ok);
        summary();
    }

    // ---------------- ListNode (LeetCode's definition) + helpers for building/printing ----------------
    static class ListNode {
        int val;
        ListNode next;
        ListNode(int val) { this.val = val; }
        ListNode(int val, ListNode next) { this.val = val; this.next = next; }

        @Override
        public String toString() {                 // prints the whole list from this node: [1, 2, 3]
            StringBuilder sb = new StringBuilder("[");
            int guard = 0;
            for (ListNode c = this; c != null; c = c.next) {
                if (guard++ > 1000) return sb.append("...cycle?]").toString();
                if (c != this) sb.append(", ");
                sb.append(c.val);
            }
            return sb.append("]").toString();
        }
    }

    static ListNode build(int... vals) {           // build(1, 2, 3) -> 1 -> 2 -> 3
        ListNode dummy = new ListNode(0), t = dummy;
        for (int v : vals) { t.next = new ListNode(v); t = t.next; }
        return dummy.next;
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
