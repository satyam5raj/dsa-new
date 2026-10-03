/*
 * ============================================================================
 *  MEDIUM 01 : TOP K FREQUENT ELEMENTS       LeetCode #347
 *  https://leetcode.com/problems/top-k-frequent-elements/
 *  Pattern: COUNT (HashMap) + BUCKET SORT by frequency  (or a size-k min-heap)
 * ============================================================================
 *
 *  PROBLEM
 *    Return the k most frequent elements (any order). Must beat O(n log n).
 *      [1,1,1,2,2,3], k=2 -> [1,2]
 *
 *  ---------------------------------------------------------------------------
 *  STEP 1 (always): count with a HashMap  value -> frequency.   O(n)
 *
 *  STEP 2, OPTION A: sort the distinct values by frequency -> O(d log d). Too slow per the rules.
 *
 *  STEP 2, OPTION B: MIN-HEAP OF SIZE k                            O(n log k)
 *    Push each (value, freq); if the heap grows past k, pop the LEAST frequent.
 *    The heap ends up holding the k most frequent. (Heap topic, 08)
 *
 *  STEP 2, OPTION C: BUCKET SORT                                   O(n)
 *    A frequency is between 1 and n. buckets[f] = the values occurring exactly f times.
 *    Walk the buckets from f = n down to 1, collecting values until we have k.
 *    (Same trick as Strings/M09 Sort Characters by Frequency.)
 *
 *  DRY RUN [1,1,1,2,2,3], k=2
 *    counts {1:3, 2:2, 3:1}
 *    buckets: [3] = [1], [2] = [2], [1] = [3]
 *    from 6 down: f=3 -> 1; f=2 -> 2 -> we have k=2 -> [1,2]
 *
 *  COMPLEXITY   bucket sort: Time O(n), Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M01_TopKFrequentElements {

    static int[] topKFrequent(int[] nums, int k) {
        Map<Integer, Integer> count = new HashMap<>();
        for (int x : nums) count.merge(x, 1, Integer::sum);

        List<List<Integer>> buckets = new ArrayList<>();
        for (int i = 0; i <= nums.length; i++) buckets.add(new ArrayList<>());
        count.forEach((value, freq) -> buckets.get(freq).add(value));

        int[] result = new int[k];
        int idx = 0;
        for (int f = nums.length; f >= 1 && idx < k; f--) {
            for (int value : buckets.get(f)) {
                if (idx == k) break;
                result[idx++] = value;
            }
        }
        return result;
    }

    static int[] topKFrequentHeap(int[] nums, int k) {
        Map<Integer, Integer> count = new HashMap<>();
        for (int x : nums) count.merge(x, 1, Integer::sum);
        PriorityQueue<Map.Entry<Integer, Integer>> heap = new PriorityQueue<>(Map.Entry.comparingByValue());
        for (Map.Entry<Integer, Integer> e : count.entrySet()) {
            heap.offer(e);
            if (heap.size() > k) heap.poll();                 // drop the least frequent
        }
        int[] result = new int[k];
        for (int i = 0; i < k; i++) result[i] = heap.poll().getKey();
        return result;
    }

    static String sorted(int[] a) {
        int[] c = a.clone();
        Arrays.sort(c);
        return Arrays.toString(c);
    }

    public static void main(String[] args) {
        check("[1,1,1,2,2,3] k=2", "[1, 2]", sorted(topKFrequent(new int[]{1, 1, 1, 2, 2, 3}, 2)));
        check("[1] k=1", "[1]", sorted(topKFrequent(new int[]{1}, 1)));
        check("[4,4,-1,-1,-1,7] k=2", "[-1, 4]", sorted(topKFrequent(new int[]{4, 4, -1, -1, -1, 7}, 2)));
        check("heap version", "[1, 2]", sorted(topKFrequentHeap(new int[]{1, 1, 1, 2, 2, 3}, 2)));
        // random check with a unique answer: values 0..9 get distinct frequencies
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 200; t++) {
            List<Integer> freqs = new ArrayList<>();
            for (int f = 1; f <= 10; f++) freqs.add(f);
            Collections.shuffle(freqs, rnd);
            List<Integer> all = new ArrayList<>();
            for (int v = 0; v < 10; v++) for (int c = 0; c < freqs.get(v); c++) all.add(v);
            Collections.shuffle(all, rnd);
            int[] arr = all.stream().mapToInt(Integer::intValue).toArray();
            int k = 1 + rnd.nextInt(10);
            List<Integer> exp = new ArrayList<>();
            for (int v = 0; v < 10; v++) if (freqs.get(v) > 10 - k) exp.add(v);
            ok &= sorted(topKFrequent(arr, k)).equals(exp.toString()) && sorted(topKFrequentHeap(arr, k)).equals(exp.toString());
        }
        check("200 random cases (both approaches)", true, ok);
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
