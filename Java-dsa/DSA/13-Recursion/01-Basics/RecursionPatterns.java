/*
 * ============================================================================
 *  RECURSION PATTERNS + how to find the complexity of a recursive function
 * ============================================================================
 *
 *  PATTERN 1: LINEAR RECURSION (one call, smaller by 1)       f(n) -> f(n-1)
 *    factorial, sum, reversing a list, checking a palindrome with two indices.
 *    Time O(n), stack O(n).
 *
 *  PATTERN 2: RECURSION ON AN INDEX / RANGE (arrays & strings without copying)
 *    pass (array, i) or (array, lo, hi) instead of creating sub-arrays.
 *    s.substring(1) in every call costs O(n) each -> O(n^2) total; indices cost O(1).
 *
 *  PATTERN 3: DIVIDE AND CONQUER (split in parts, solve each, COMBINE)
 *    binary search (1 half), merge sort (2 halves + merge), max of an array,
 *    fast power. Often O(n log n) or O(log n).
 *
 *  PATTERN 4: TREE RECURSION (several calls per level)          f(n) -> f(n-1), f(n-2)
 *    Fibonacci, counting paths, "pick or not pick" (2 choices per element)
 *    -> exponential unless subproblems repeat AND you memoize (-> Dynamic Programming).
 *
 *  PATTERN 5: PICK / NOT PICK (generate all subsets/subsequences)
 *    for each element: one branch includes it, one excludes it -> 2^n leaves.
 *    This is the doorway to BACKTRACKING (topic 14).
 *
 *  ---------------------------------------------------------------------------
 *  COMPLEXITY OF RECURSION = (number of calls) x (work per call)
 *  Draw the RECURSION TREE: one node per call.
 *    T(n) = T(n-1) + O(1)      -> a chain of n nodes        -> O(n)
 *    T(n) = T(n/2) + O(1)      -> log n levels, 1 node each -> O(log n)     binary search
 *    T(n) = 2T(n/2) + O(n)     -> log n levels, O(n) each   -> O(n log n)   merge sort
 *    T(n) = 2T(n-1) + O(1)     -> 2^n nodes                 -> O(2^n)       Hanoi, naive subsets
 *    T(n) = T(n-1) + T(n-2)    -> ~1.618^n nodes            -> O(phi^n)     naive Fibonacci
 *  MASTER THEOREM (for T(n) = a T(n/b) + O(n^d)):
 *    d > log_b a -> O(n^d);   d == log_b a -> O(n^d log n);   d < log_b a -> O(n^(log_b a))
 *  SPACE = maximum DEPTH of the tree (not the number of nodes!) + any copies made.
 *
 *  Run:  java RecursionPatterns.java
 * ============================================================================
 */

import java.util.*;

public class RecursionPatterns {

    // PATTERN 1 + 2: palindrome check with two indices (no substring copies)
    static boolean isPalindrome(String s, int lo, int hi) {
        if (lo >= hi) return true;
        return s.charAt(lo) == s.charAt(hi) && isPalindrome(s, lo + 1, hi - 1);
    }

    // PATTERN 2: reverse an array in place with two indices
    static void reverse(int[] a, int lo, int hi) {
        if (lo >= hi) return;
        int t = a[lo]; a[lo] = a[hi]; a[hi] = t;
        reverse(a, lo + 1, hi - 1);
    }

    // PATTERN 3: divide and conquer - maximum of a[lo..hi]
    static int max(int[] a, int lo, int hi) {
        if (lo == hi) return a[lo];
        int mid = (lo + hi) >>> 1;
        return Math.max(max(a, lo, mid), max(a, mid + 1, hi));   // solve halves, combine
    }

    // PATTERN 3: recursive binary search (index or -1)
    static int binarySearch(int[] a, int target, int lo, int hi) {
        if (lo > hi) return -1;
        int mid = (lo + hi) >>> 1;
        if (a[mid] == target) return mid;
        return a[mid] < target ? binarySearch(a, target, mid + 1, hi) : binarySearch(a, target, lo, mid - 1);
    }

    // PATTERN 4: tree recursion - number of ways to climb n stairs taking 1 or 2 steps
    static long counter;

    static long climbWays(int n) {
        counter++;
        if (n <= 1) return 1;
        return climbWays(n - 1) + climbWays(n - 2);
    }

    // PATTERN 5: pick / not pick - all subsets
    static void subsets(int[] a, int i, List<Integer> cur, List<List<Integer>> out) {
        if (i == a.length) { out.add(new ArrayList<>(cur)); return; }
        cur.add(a[i]);                         // PICK a[i]
        subsets(a, i + 1, cur, out);
        cur.remove(cur.size() - 1);            // undo
        subsets(a, i + 1, cur, out);           // NOT PICK a[i]
    }

    // Recursion vs a copying version: substring() makes each call O(n)
    static boolean isPalindromeCopying(String s) {
        if (s.length() <= 1) return true;
        return s.charAt(0) == s.charAt(s.length() - 1) && isPalindromeCopying(s.substring(1, s.length() - 1));
    }

    public static void main(String[] args) {
        System.out.println("PATTERN 1-2: linear recursion with indices");
        check("racecar", true, isPalindrome("racecar", 0, 6));
        check("abca", false, isPalindrome("abca", 0, 3));
        int[] arr = {1, 2, 3, 4, 5};
        reverse(arr, 0, 4);
        check("reverse in place", "[5, 4, 3, 2, 1]", arr);

        System.out.println("PATTERN 3: divide and conquer");
        check("max of [3,9,2,7,5]", 9, max(new int[]{3, 9, 2, 7, 5}, 0, 4));
        int[] sorted = {1, 3, 5, 7, 9, 11};
        check("binary search 7", 3, binarySearch(sorted, 7, 0, 5));
        check("binary search 4", -1, binarySearch(sorted, 4, 0, 5));

        System.out.println("PATTERN 4: tree recursion explodes");
        for (int n : new int[]{10, 20, 30}) {
            counter = 0;
            long w = climbWays(n);
            System.out.printf("   climbWays(%d) = %,d using %,d calls (~1.618^n)%n", n, w, counter);
        }
        check("climbWays(5)", 8L, climbWays(5));

        System.out.println("PATTERN 5: pick / not pick");
        List<List<Integer>> subs = new ArrayList<>();
        subsets(new int[]{1, 2, 3}, 0, new ArrayList<>(), subs);
        check("subsets of [1,2,3]", "[[1, 2, 3], [1, 2], [1, 3], [1], [2, 3], [2], [3], []]", subs);
        check("count = 2^n", 8, subs.size());

        System.out.println("Copying costs: substring in every call");
        String big = "a".repeat(5_000);
        long t0 = System.nanoTime();
        boolean r1 = isPalindrome(big, 0, big.length() - 1);
        long idxNs = System.nanoTime() - t0;
        t0 = System.nanoTime();
        boolean r2 = isPalindromeCopying(big);
        long copyNs = System.nanoTime() - t0;
        System.out.printf("   n = 5,000: indices %d us, substring copies %d us%n", idxNs / 1000, copyNs / 1000);
        check("both say palindrome", true, r1 && r2);

        Random rnd = new Random(2);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int[] a = rnd.ints(1 + rnd.nextInt(30), -50, 50).toArray();
            ok &= max(a, 0, a.length - 1) == Arrays.stream(a).max().getAsInt();
            int[] s = rnd.ints(1 + rnd.nextInt(30), 0, 100).distinct().sorted().toArray();
            int target = rnd.nextInt(100);
            int idx = binarySearch(s, target, 0, s.length - 1);
            ok &= idx == -1 ? Arrays.binarySearch(s, target) < 0 : s[idx] == target;
        }
        check("500 random arrays: max + binary search", true, ok);
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
