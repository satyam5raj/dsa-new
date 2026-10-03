/*
 * ============================================================================
 *  EASY 11 : INTERSECTION OF TWO ARRAYS      LeetCode #349 (+ #350 variant)
 *  https://leetcode.com/problems/intersection-of-two-arrays/
 *  Pattern: HASHING  /  SORT + TWO POINTERS
 * ============================================================================
 *
 *  PROBLEM (#349)
 *    Return the UNIQUE elements that appear in BOTH arrays (any order).
 *      [1,2,2,1], [2,2]        -> [2]
 *      [4,9,5],   [9,4,9,8,4]  -> [4,9]
 *
 *  ---------------------------------------------------------------------------
 *  APPROACH 1: TWO SETS                                O(n + m) time, O(n) space
 *    Put nums1 into a set. Walk nums2: if x is in the set, add it to the result
 *    set (a set again, so duplicates in the answer disappear).
 *
 *  APPROACH 2: SORT BOTH + TWO POINTERS                O(n log n + m log m), O(1) extra
 *    i over a, j over b:
 *      a[i] < b[j] -> i++        a[i] > b[j] -> j++
 *      equal -> record (if not the same as the last recorded), i++, j++
 *    Better when the arrays are already sorted, or memory is tight.
 *
 *  VARIANT #350 (Intersection II): keep DUPLICATES as many times as they appear
 *  in both: [1,2,2,1],[2,2] -> [2,2]. Use a COUNT map instead of a set:
 *  count nums1, then for each x in nums2 with count > 0: output x, decrement.
 * ============================================================================
 */

import java.util.*;

public class E11_IntersectionOfTwoArrays {

    // #349 with sets
    static int[] intersection(int[] nums1, int[] nums2) {
        Set<Integer> first = new HashSet<>();
        for (int x : nums1) first.add(x);
        Set<Integer> common = new TreeSet<>();          // TreeSet only to make the output order predictable
        for (int x : nums2) {
            if (first.contains(x)) common.add(x);
        }
        return common.stream().mapToInt(Integer::intValue).toArray();
    }

    // #349 with sorting + two pointers
    static int[] intersectionSorted(int[] nums1, int[] nums2) {
        int[] a = nums1.clone(), b = nums2.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        List<Integer> out = new ArrayList<>();
        int i = 0, j = 0;
        while (i < a.length && j < b.length) {
            if (a[i] < b[j]) i++;
            else if (a[i] > b[j]) j++;
            else {
                if (out.isEmpty() || out.get(out.size() - 1) != a[i]) out.add(a[i]);   // skip repeats
                i++;
                j++;
            }
        }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }

    // #350: keep duplicates
    static int[] intersectWithDuplicates(int[] nums1, int[] nums2) {
        Map<Integer, Integer> count = new HashMap<>();
        for (int x : nums1) count.merge(x, 1, Integer::sum);
        List<Integer> out = new ArrayList<>();
        for (int x : nums2) {
            int c = count.getOrDefault(x, 0);
            if (c > 0) {
                out.add(x);
                count.put(x, c - 1);                   // use one copy up
            }
        }
        return out.stream().mapToInt(Integer::intValue).toArray();
    }

    public static void main(String[] args) {
        check("[1,2,2,1] & [2,2]", new int[]{2}, intersection(new int[]{1, 2, 2, 1}, new int[]{2, 2}));
        check("[4,9,5] & [9,4,9,8,4]", new int[]{4, 9}, intersection(new int[]{4, 9, 5}, new int[]{9, 4, 9, 8, 4}));
        check("no overlap", new int[]{}, intersection(new int[]{1, 2}, new int[]{3, 4}));
        check("sorted approach", new int[]{4, 9}, intersectionSorted(new int[]{4, 9, 5}, new int[]{9, 4, 9, 8, 4}));
        check("#350 keep duplicates", new int[]{2, 2}, intersectWithDuplicates(new int[]{1, 2, 2, 1}, new int[]{2, 2}));
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
        if (o instanceof char[][] x) return Arrays.deepToString(x);
        return String.valueOf(o);
    }

    static void summary() {
        System.out.println("   " + passed + "/" + total + " tests passed");
        if (passed != total) System.exit(1);
    }
}
