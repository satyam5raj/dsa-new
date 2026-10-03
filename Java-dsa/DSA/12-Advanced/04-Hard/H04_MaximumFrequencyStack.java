/*
 * ============================================================================
 *  HARD 04 : MAXIMUM FREQUENCY STACK  ⭐ bonus   LeetCode #895
 *  https://leetcode.com/problems/maximum-frequency-stack/
 *  Pattern: one STACK PER FREQUENCY level + maxFreq
 * ============================================================================
 *
 *  PROBLEM
 *    push(val); pop() removes and returns the MOST FREQUENT element; on a tie, the
 *    one closest to the top (pushed most recently).
 *      push 5,7,5,7,4,5 -> pop 5 (freq 3), pop 7 (5 and 7 both freq 2; 7 is newer),
 *                          pop 5, pop 4
 *
 *  ---------------------------------------------------------------------------
 *  HEAP IDEA: max-heap by (frequency, push time) -> O(log n). Works, not optimal.
 *
 *  O(1) IDEA: think of the stack as LAYERS.
 *    freq:   val -> how many copies are currently in the stack
 *    group:  f -> a stack of values that reached frequency f (in push order)
 *    maxFreq
 *  push(x): f = ++freq[x]; group[f].push(x); maxFreq = max(maxFreq, f)
 *  pop():   x = group[maxFreq].pop(); freq[x]--; if group[maxFreq] is empty: maxFreq--
 *  Why it works: group[maxFreq] contains exactly the values at the top frequency,
 *  ordered by WHEN they reached it -> its top is the right tie-break. A value with
 *  frequency 3 sits in group[1], group[2] AND group[3]; popping it from group[3]
 *  leaves its copy in group[2] still correct.
 *  maxFreq-- is enough because if group[maxFreq] has any values, group[maxFreq-1]
 *  contains those same values too, so it's non-empty.
 *
 *  DRY RUN push 5,7,5,7,4,5
 *    group1 [5,7,4]  group2 [5,7]  group3 [5]   maxFreq 3
 *    pop -> 5 (group3 empty -> maxFreq 2);  pop -> 7;  pop -> 5 (maxFreq 1);  pop -> 4
 *
 *  COMPLEXITY   O(1) per operation   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class H04_MaximumFrequencyStack {

    static class FreqStack {
        private final Map<Integer, Integer> freq = new HashMap<>();
        private final Map<Integer, Deque<Integer>> group = new HashMap<>();
        private int maxFreq = 0;

        public void push(int val) {
            int f = freq.merge(val, 1, Integer::sum);
            group.computeIfAbsent(f, k -> new ArrayDeque<>()).push(val);
            maxFreq = Math.max(maxFreq, f);
        }

        public int pop() {
            int val = group.get(maxFreq).pop();
            freq.merge(val, -1, Integer::sum);
            if (group.get(maxFreq).isEmpty()) maxFreq--;
            return val;
        }
    }

    // referee: scan the actual stack from the top for the first element with the max count
    static class Brute {
        final List<Integer> stack = new ArrayList<>();
        void push(int v) { stack.add(v); }
        int pop() {
            Map<Integer, Integer> c = new HashMap<>();
            for (int v : stack) c.merge(v, 1, Integer::sum);
            int max = Collections.max(c.values());
            for (int i = stack.size() - 1; i >= 0; i--) if (c.get(stack.get(i)) == max) return stack.remove(i);
            throw new IllegalStateException();
        }
    }

    public static void main(String[] args) {
        FreqStack fs = new FreqStack();
        for (int v : new int[]{5, 7, 5, 7, 4, 5}) fs.push(v);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) sb.append(fs.pop()).append(' ');
        check("example", "5 7 5 4", sb.toString().trim());
        Random rnd = new Random(4);
        boolean ok = true;
        for (int t = 0; t < 100; t++) {
            FreqStack mine = new FreqStack();
            Brute ref = new Brute();
            int size = 0;
            for (int i = 0; i < 1000; i++) {
                if (size == 0 || rnd.nextInt(3) > 0) { int v = rnd.nextInt(6); mine.push(v); ref.push(v); size++; }
                else { ok &= mine.pop() == ref.pop(); size--; }
            }
        }
        check("100,000 random ops vs scanning the stack", true, ok);
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
