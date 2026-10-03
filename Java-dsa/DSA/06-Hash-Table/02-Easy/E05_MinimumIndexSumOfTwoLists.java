/*
 * ============================================================================
 *  EASY 05 : MINIMUM INDEX SUM OF TWO LISTS   LeetCode #599
 *  https://leetcode.com/problems/minimum-index-sum-of-two-lists/
 *  Pattern: HASHMAP value -> index (index one list, scan the other)
 * ============================================================================
 *
 *  PROBLEM
 *    Two people's favourite-restaurant lists. Find the COMMON restaurants with the
 *    smallest (index in list1 + index in list2). Return ALL ties.
 *      ["Shogun","Tapioca Express","Burger King","KFC"],
 *      ["KFC","Shogun","Burger King"]              -> ["Shogun"]   (0 + 1 = 1)
 *      ["happy","sad","good"], ["sad","happy","good"] -> ["sad","happy"]   (both sum to 1)
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: compare every pair of strings -> O(n * m * L)
 *
 *  HASHMAP: store list1 as  name -> index  (O(1) lookups). Walk list2 with index j:
 *    if the name is in the map: sum = i + j
 *       sum <  best -> new best: clear the answer list, add this name
 *       sum == best -> a TIE: add it too
 *  EARLY EXIT: once j alone exceeds best, no later name can do better (indices
 *  only grow), so stop.
 *
 *  DRY RUN ["happy","sad","good"] vs ["sad","happy","good"]
 *    map {happy:0, sad:1, good:2}
 *    j=0 sad:   1+0 = 1 -> best 1, ans [sad]
 *    j=1 happy: 0+1 = 1 -> tie,    ans [sad, happy]
 *    j=2: 2 > best -> stop
 *
 *  COMPLEXITY   Time O(n + m) (times the string length for hashing)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class E05_MinimumIndexSumOfTwoLists {

    static String[] findRestaurant(String[] list1, String[] list2) {
        Map<String, Integer> index1 = new HashMap<>();
        for (int i = 0; i < list1.length; i++) index1.put(list1[i], i);
        List<String> answer = new ArrayList<>();
        int best = Integer.MAX_VALUE;
        for (int j = 0; j < list2.length && j <= best; j++) {      // early exit
            Integer i = index1.get(list2[j]);
            if (i == null) continue;
            int sum = i + j;
            if (sum < best) {
                best = sum;
                answer.clear();
                answer.add(list2[j]);
            } else if (sum == best) {
                answer.add(list2[j]);
            }
        }
        return answer.toArray(new String[0]);
    }

    public static void main(String[] args) {
        check("Shogun example", "[Shogun]", Arrays.toString(findRestaurant(
                new String[]{"Shogun", "Tapioca Express", "Burger King", "KFC"}, new String[]{"KFC", "Shogun", "Burger King"})));
        check("tie: sad & happy", "[sad, happy]", Arrays.toString(findRestaurant(
                new String[]{"happy", "sad", "good"}, new String[]{"sad", "happy", "good"})));
        check("only the last items match", "[z]", Arrays.toString(findRestaurant(
                new String[]{"a", "b", "z"}, new String[]{"x", "y", "z"})));
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
