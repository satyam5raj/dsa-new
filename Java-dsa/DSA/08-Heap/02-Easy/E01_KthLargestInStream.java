/*
 * ============================================================================
 *  EASY 01 : KTH LARGEST ELEMENT IN A STREAM   LeetCode #703
 *  https://leetcode.com/problems/kth-largest-element-in-a-stream/
 *  Pattern: TOP-K with a MIN-heap of size k (the root IS the answer)
 * ============================================================================
 *
 *  PROBLEM
 *    KthLargest(k, nums); add(val) returns the k-th largest of everything seen so far.
 *      k=3, nums [4,5,8,2]
 *      add(3) -> 4   (sorted desc: 8 5 4 3 2)
 *      add(5) -> 5   (8 5 5 4 3 2)
 *      add(10)-> 5   (10 8 5 5 4 3 2)
 *      add(9) -> 8
 *      add(4) -> 8
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: keep a sorted list; insert in O(n) per add. Or sort every time.
 *
 *  KEY INSIGHT: we only care about the k LARGEST values. Everything smaller than
 *  the k-th largest can never become the answer again (values are only added).
 *  Keep exactly those k values in a MIN-heap:
 *     - the root = the smallest of the top k = the k-th largest   -> O(1) answer
 *     - add(val): offer it; if size > k, poll the root (it fell out of the top k)
 *  Why a MIN-heap for "largest"? Because the element we throw away is always the
 *  smallest one we're keeping - and that must be the one at the root.
 *
 *  DRY RUN k=3: init [4,5,8,2] -> heap keeps {4,5,8} (2 polled)
 *    add 3: {3,4,5,8} -> poll 3 -> {4,5,8} root 4
 *    add 5: {4,5,5,8} -> poll 4 -> {5,5,8} root 5
 *    add 10: -> poll 5 -> {5,8,10} root 5
 *
 *  COMPLEXITY   add O(log k)   Space O(k)
 * ============================================================================
 */

import java.util.*;

public class E01_KthLargestInStream {

    static class KthLargest {
        private final int k;
        private final PriorityQueue<Integer> topK = new PriorityQueue<>();   // min-heap

        KthLargest(int k, int[] nums) {
            this.k = k;
            for (int x : nums) add(x);
        }

        int add(int val) {
            topK.offer(val);
            if (topK.size() > k) topK.poll();        // the smallest is no longer in the top k
            return topK.peek();
        }
    }

    public static void main(String[] args) {
        KthLargest kl = new KthLargest(3, new int[]{4, 5, 8, 2});
        check("add(3)", 4, kl.add(3));
        check("add(5)", 5, kl.add(5));
        check("add(10)", 5, kl.add(10));
        check("add(9)", 8, kl.add(9));
        check("add(4)", 8, kl.add(4));
        KthLargest one = new KthLargest(1, new int[]{});
        check("k=1, empty start, add(-3)", -3, one.add(-3));

        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 100; t++) {
            int k = 1 + rnd.nextInt(10);
            int[] init = rnd.ints(k - 1 + rnd.nextInt(5), -50, 50).toArray();   // at least k-1 values
            KthLargest mine = new KthLargest(k, init);
            List<Integer> all = new ArrayList<>();
            for (int x : init) all.add(x);
            for (int i = 0; i < 50; i++) {
                int v = rnd.nextInt(100) - 50;
                all.add(v);
                List<Integer> s = new ArrayList<>(all);
                s.sort(Collections.reverseOrder());
                ok &= mine.add(v) == s.get(k - 1);
            }
        }
        check("100 random streams vs sorting", true, ok);
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
