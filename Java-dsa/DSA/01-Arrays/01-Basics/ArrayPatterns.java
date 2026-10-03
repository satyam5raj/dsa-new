/*
 * ============================================================================
 *  ARRAYS 1.3 : THE CORE PATTERNS - LEARN THESE BEFORE THE PROBLEMS
 * ============================================================================
 *
 *  Run:  java ArrayPatterns.java
 *
 *  Almost every array problem is one (or a mix) of these patterns. Learn to
 *  RECOGNISE the pattern from the problem statement; the code then follows.
 *
 *  -----------------------------------------------------------------------------------
 *   Pattern              Clue in the problem                         Typical cost
 *  -----------------------------------------------------------------------------------
 *   1. Hashing           "find a pair", "seen before?", "count"      O(n) time, O(n) space
 *   2. Prefix sum        "sum of subarray / range", many queries     O(n) build, O(1) query
 *   3. Two pointers      SORTED array, pairs, "in-place", reverse    O(n) time, O(1) space
 *      (opposite ends / slow-fast)
 *   4. Sliding window    "contiguous subarray" + longest/shortest/   O(n)
 *                        max sum of size k
 *   5. Kadane            "maximum sum subarray"                      O(n)
 *   6. Index marking     values in range 1..n, "missing/duplicate",  O(n) time, O(1) space
 *                        O(1) extra space
 *   7. Dutch flag /      group values into 2-3 categories in-place   O(n)
 *      partitioning
 *  -----------------------------------------------------------------------------------
 *
 *  HOW TO APPROACH ANY PROBLEM (say this out loud in interviews)
 *    1. Understand: restate the problem, ask about edge cases (empty? negatives? duplicates?)
 *    2. Brute force first: the obvious solution and its complexity
 *    3. Find the bottleneck: "what am I recomputing?" -> pick a pattern to remove it
 *    4. Code the optimal solution
 *    5. Dry-run a small example by hand, then the edge cases
 */

import java.util.*;

public class ArrayPatterns {

    public static void main(String[] args) {
        hashingPattern();
        prefixSumPattern();
        twoPointersOppositeEnds();
        twoPointersSlowFast();
        slidingWindowFixed();
        slidingWindowVariable();
        kadanePattern();
        indexMarkingPattern();
        dutchFlagPattern();
    }

    // =================================================================
    // 1. HASHING: trade memory for speed
    //    "For each element, have I seen X before?" -> HashSet/HashMap gives O(1) lookups.
    //    Example: does any pair sum to target?
    //      brute force: check all pairs, O(n^2)
    //      hashing    : for each x, ask "have I seen (target - x)?", O(n)
    // =================================================================
    static boolean hasPairWithSum(int[] arr, int target) {
        Set<Integer> seen = new HashSet<>();
        for (int x : arr) {
            if (seen.contains(target - x)) return true;   // the partner was seen earlier
            seen.add(x);
        }
        return false;
    }

    static void hashingPattern() {
        System.out.println("===== 1. HASHING =====");
        System.out.println("   pair summing to 9 in [2,7,11,15]? " + hasPairWithSum(new int[]{2, 7, 11, 15}, 9));
        System.out.println("   pair summing to 100?              " + hasPairWithSum(new int[]{2, 7, 11, 15}, 100));
    }

    // =================================================================
    // 2. PREFIX SUM: precompute running totals once, answer range sums in O(1)
    //    prefix[i] = sum of arr[0 .. i-1]      (prefix has length n+1, prefix[0] = 0)
    //    sum(l..r) = prefix[r+1] - prefix[l]
    //
    //    arr    =    [3,  1,  4,  1,  5]
    //    prefix = [0, 3,  4,  8,  9, 14]
    //    sum(1..3) = prefix[4] - prefix[1] = 9 - 3 = 6   (1+4+1)
    // =================================================================
    static int[] buildPrefix(int[] arr) {
        int[] prefix = new int[arr.length + 1];
        for (int i = 0; i < arr.length; i++) prefix[i + 1] = prefix[i] + arr[i];
        return prefix;
    }

    static void prefixSumPattern() {
        System.out.println("\n===== 2. PREFIX SUM =====");
        int[] arr = {3, 1, 4, 1, 5};
        int[] prefix = buildPrefix(arr);
        System.out.println("   prefix = " + Arrays.toString(prefix));
        System.out.println("   sum(1..3) = " + (prefix[4] - prefix[1]) + "  (O(1) per query after O(n) setup)");
    }

    // =================================================================
    // 3a. TWO POINTERS - OPPOSITE ENDS (usually on a SORTED array)
    //     left starts at 0, right at n-1. Each step moves ONE pointer inward
    //     based on a comparison -> at most n steps -> O(n).
    //     Pair with sum target in a SORTED array:
    //       sum too small -> move left right (bigger numbers)
    //       sum too big   -> move right left (smaller numbers)
    // =================================================================
    static int[] pairInSorted(int[] sorted, int target) {
        int left = 0, right = sorted.length - 1;
        while (left < right) {
            int sum = sorted[left] + sorted[right];
            if (sum == target) return new int[]{left, right};
            if (sum < target) left++;
            else right--;
        }
        return new int[]{-1, -1};
    }

    static void twoPointersOppositeEnds() {
        System.out.println("\n===== 3a. TWO POINTERS (opposite ends) =====");
        System.out.println("   pair = 10 in [1,3,4,6,8,11] -> indices " + Arrays.toString(pairInSorted(new int[]{1, 3, 4, 6, 8, 11}, 10)));
    }

    // =================================================================
    // 3b. TWO POINTERS - SLOW / FAST (same direction)
    //     'fast' scans every element; 'slow' marks where the next KEPT element goes.
    //     The classic way to filter an array IN-PLACE.
    //     Remove all zeros (keep order):
    //       [0,1,0,3,12] -> fast finds 1,3,12 -> written at slow=0,1,2 -> [1,3,12,...]
    // =================================================================
    static int removeZerosInPlace(int[] arr) {
        int slow = 0;
        for (int fast = 0; fast < arr.length; fast++) {
            if (arr[fast] != 0) {
                arr[slow++] = arr[fast];   // keep it
            }
        }
        return slow;                       // new logical length
    }

    static void twoPointersSlowFast() {
        System.out.println("\n===== 3b. TWO POINTERS (slow/fast) =====");
        int[] arr = {0, 1, 0, 3, 12};
        int len = removeZerosInPlace(arr);
        System.out.println("   remove zeros -> first " + len + " elements: " + Arrays.toString(Arrays.copyOf(arr, len)));
    }

    // =================================================================
    // 4a. SLIDING WINDOW - FIXED SIZE k
    //     Instead of re-summing each window (O(n*k)), SLIDE: add the element
    //     entering on the right, subtract the one leaving on the left. O(n)
    //
    //     [2,1,5,1,3,2], k=3
    //      [2,1,5]        sum 8
    //        [1,5,1]      8 - 2 + 1 = 7
    //          [5,1,3]    7 - 1 + 3 = 9   <- max
    //            [1,3,2]  9 - 5 + 2 = 6
    // =================================================================
    static int maxSumOfSizeK(int[] arr, int k) {
        int window = 0;
        for (int i = 0; i < k; i++) window += arr[i];        // first window
        int best = window;
        for (int right = k; right < arr.length; right++) {
            window += arr[right] - arr[right - k];          // slide by one
            best = Math.max(best, window);
        }
        return best;
    }

    static void slidingWindowFixed() {
        System.out.println("\n===== 4a. SLIDING WINDOW (fixed size) =====");
        System.out.println("   max sum of 3 consecutive in [2,1,5,1,3,2] = " + maxSumOfSizeK(new int[]{2, 1, 5, 1, 3, 2}, 3));
    }

    // =================================================================
    // 4b. SLIDING WINDOW - VARIABLE SIZE
    //     EXPAND the right edge every step; SHRINK the left edge while the
    //     window breaks the rule. Each element enters once and leaves once -> O(n).
    //
    //     TEMPLATE:
    //       left = 0
    //       for right in 0..n-1:
    //           add arr[right] to the window
    //           while (window is invalid): remove arr[left], left++
    //           update the answer with the window [left..right]
    //
    //     Example: shortest subarray with sum >= target (positive numbers)
    // =================================================================
    static int shortestWithSumAtLeast(int[] arr, int target) {
        int left = 0, sum = 0, best = Integer.MAX_VALUE;
        for (int right = 0; right < arr.length; right++) {
            sum += arr[right];                              // expand
            while (sum >= target) {                         // valid -> try to shrink
                best = Math.min(best, right - left + 1);
                sum -= arr[left++];
            }
        }
        return best == Integer.MAX_VALUE ? 0 : best;
    }

    static void slidingWindowVariable() {
        System.out.println("\n===== 4b. SLIDING WINDOW (variable size) =====");
        System.out.println("   shortest subarray with sum >= 7 in [2,3,1,2,4,3] = " + shortestWithSumAtLeast(new int[]{2, 3, 1, 2, 4, 3}, 7) + "  ([4,3])");
    }

    // =================================================================
    // 5. KADANE: maximum sum of a contiguous subarray in O(n)
    //    At each element decide: EXTEND the current subarray, or START FRESH here?
    //      current = max(x, current + x)
    //    If the running sum went negative, it can only hurt what follows -> drop it.
    // =================================================================
    static int kadane(int[] arr) {
        int current = arr[0], best = arr[0];
        for (int i = 1; i < arr.length; i++) {
            current = Math.max(arr[i], current + arr[i]);
            best = Math.max(best, current);
        }
        return best;
    }

    static void kadanePattern() {
        System.out.println("\n===== 5. KADANE =====");
        System.out.println("   max subarray of [-2,1,-3,4,-1,2,1,-5,4] = " + kadane(new int[]{-2, 1, -3, 4, -1, 2, 1, -5, 4}) + "  ([4,-1,2,1])");
    }

    // =================================================================
    // 6. INDEX MARKING: when values are in the range 1..n, the ARRAY ITSELF can be
    //    used as a hash table. Value v "marks" index v-1 by making it negative.
    //    Gives O(1) extra space instead of a HashSet.
    //    Find the numbers missing from 1..n:
    //      [4,3,2,7,8,2,3,1] -> mark indices 3,2,1,6,7,1,2,0 -> still positive at 4,5 -> missing 5,6
    // =================================================================
    static List<Integer> findMissing(int[] arr) {
        for (int x : arr) {
            int idx = Math.abs(x) - 1;                     // abs: the value may already be marked
            if (arr[idx] > 0) arr[idx] = -arr[idx];
        }
        List<Integer> missing = new ArrayList<>();
        for (int i = 0; i < arr.length; i++) if (arr[i] > 0) missing.add(i + 1);
        return missing;
    }

    static void indexMarkingPattern() {
        System.out.println("\n===== 6. INDEX MARKING =====");
        System.out.println("   missing from [4,3,2,7,8,2,3,1] = " + findMissing(new int[]{4, 3, 2, 7, 8, 2, 3, 1}));
    }

    // =================================================================
    // 7. DUTCH NATIONAL FLAG: sort 3 categories (0s, 1s, 2s) in ONE pass, in place
    //    low  : boundary of the 0s       [0 .. low-1]   are 0
    //    mid  : the current element       [low .. mid-1] are 1
    //    high : boundary of the 2s       [high+1 .. n-1] are 2
    // =================================================================
    static void dutchFlag(int[] arr) {
        int low = 0, mid = 0, high = arr.length - 1;
        while (mid <= high) {
            if (arr[mid] == 0) swap(arr, low++, mid++);
            else if (arr[mid] == 1) mid++;
            else swap(arr, mid, high--);                   // don't advance mid: the swapped-in value is unchecked
        }
    }

    static void dutchFlagPattern() {
        System.out.println("\n===== 7. DUTCH NATIONAL FLAG =====");
        int[] arr = {2, 0, 2, 1, 1, 0};
        dutchFlag(arr);
        System.out.println("   sort colors [2,0,2,1,1,0] -> " + Arrays.toString(arr));
    }

    static void swap(int[] a, int i, int j) {
        int t = a[i]; a[i] = a[j]; a[j] = t;
    }
}
