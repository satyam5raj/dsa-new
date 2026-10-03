/*
 * ============================================================================
 *  MEDIUM 02 : DAILY TEMPERATURES            LeetCode #739
 *  https://leetcode.com/problems/daily-temperatures/
 *  Pattern: MONOTONIC (decreasing) STACK OF INDICES - "how far to the next greater?"
 * ============================================================================
 *
 *  PROBLEM
 *    For each day, how many days until a WARMER temperature? 0 if never.
 *      [73,74,75,71,69,72,76,73] -> [1,1,4,2,1,1,0,0]
 *
 *  ---------------------------------------------------------------------------
 *  This is "next greater element", but the answer is a DISTANCE, so the stack
 *  must hold INDICES (values alone can't tell us how far apart two days are).
 *
 *  ALGORITHM
 *    stack = indices of days still waiting for a warmer day (temperatures decreasing)
 *    for each day i:
 *      while temps[i] > temps[stack.top]:     // day i is the warmer day they waited for
 *          j = stack.pop(); answer[j] = i - j
 *      push i
 *    days left on the stack never got a warmer day -> answer stays 0
 *
 *  DRY RUN [73,74,75,71,69,72,76,73]   (stack shows indices(temps))
 *    i0 73: push                         [0(73)]
 *    i1 74: pop 0 -> ans[0]=1; push      [1(74)]
 *    i2 75: pop 1 -> ans[1]=1; push      [2(75)]
 *    i3 71: push                         [2,3(71)]
 *    i4 69: push                         [2,3,4(69)]
 *    i5 72: pop 4 -> 1, pop 3 -> 2; push [2,5(72)]
 *    i6 76: pop 5 -> 1, pop 2 -> 4; push [6(76)]
 *    i7 73: push                         [6,7]   -> both stay 0
 *
 *  COMPLEXITY   Time O(n) (each index pushed and popped once)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M02_DailyTemperatures {

    static int[] dailyTemperatures(int[] temps) {
        int[] answer = new int[temps.length];
        Deque<Integer> stack = new ArrayDeque<>();
        for (int i = 0; i < temps.length; i++) {
            while (!stack.isEmpty() && temps[i] > temps[stack.peek()]) {
                int j = stack.pop();
                answer[j] = i - j;
            }
            stack.push(i);
        }
        return answer;
    }

    static int[] brute(int[] t) {
        int[] a = new int[t.length];
        for (int i = 0; i < t.length; i++)
            for (int j = i + 1; j < t.length; j++)
                if (t[j] > t[i]) { a[i] = j - i; break; }
        return a;
    }

    public static void main(String[] args) {
        check("[73,74,75,71,69,72,76,73]", new int[]{1, 1, 4, 2, 1, 1, 0, 0}, dailyTemperatures(new int[]{73, 74, 75, 71, 69, 72, 76, 73}));
        check("[30,40,50,60]", new int[]{1, 1, 1, 0}, dailyTemperatures(new int[]{30, 40, 50, 60}));
        check("[30,60,90]", new int[]{1, 1, 0}, dailyTemperatures(new int[]{30, 60, 90}));
        check("[50,50,50] (equal is not warmer)", new int[]{0, 0, 0}, dailyTemperatures(new int[]{50, 50, 50}));
        Random rnd = new Random(2);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int[] a = rnd.ints(1 + rnd.nextInt(30), 30, 40).toArray();
            ok &= Arrays.equals(dailyTemperatures(a), brute(a));
        }
        check("500 random arrays vs brute force", true, ok);
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
