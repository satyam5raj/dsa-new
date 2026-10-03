/*
 * ============================================================================
 *  EASY 04 : TIME NEEDED TO BUY TICKETS  ⭐ bonus   LeetCode #2073
 *  https://leetcode.com/problems/time-needed-to-buy-tickets/
 *  Pattern: QUEUE SIMULATION -> then a one-pass formula
 * ============================================================================
 *
 *  PROBLEM
 *    People stand in a line; tickets[i] = how many tickets person i wants.
 *    Each second the person at the front buys ONE ticket, then goes to the BACK
 *    of the line if they still need more (or leaves if done).
 *    How many seconds until person k has all their tickets?
 *      tickets [2,3,2], k=2 -> 6
 *      tickets [5,1,1,1], k=0 -> 8
 *
 *  ---------------------------------------------------------------------------
 *  APPROACH 1: SIMULATE with a queue of indices (round robin, like QueuePatterns 5)
 *    poll i; tickets[i]--; time++; if i == k and done -> return time;
 *    if tickets[i] > 0 -> offer i again
 *    Time O(sum of tickets). Fine for small inputs, and it MATCHES the story exactly.
 *
 *  APPROACH 2: COUNT DIRECTLY (O(n))
 *    Person k needs t = tickets[k] rounds. In that time:
 *      - someone AHEAD of k (i <= k) buys min(tickets[i], t) tickets
 *        (they get a turn before k in every round, including k's final one)
 *      - someone BEHIND k (i > k) buys min(tickets[i], t - 1)
 *        (in k's last round, k finishes before they get their turn)
 *    answer = the sum of those amounts
 *
 *  DRY RUN approach 2, [2,3,2], k=2, t=2
 *    i=0 (ahead): min(2,2) = 2;  i=1: min(3,2) = 2;  i=2 (k itself): 2   -> 6
 *
 *  "Simulate first, then find the formula" is a great way to solve these problems.
 * ============================================================================
 */

import java.util.*;

public class E04_TimeNeededToBuyTickets {

    static int timeRequiredToBuySimulate(int[] tickets, int k) {
        int[] t = tickets.clone();
        Deque<Integer> q = new ArrayDeque<>();
        for (int i = 0; i < t.length; i++) q.offer(i);
        int time = 0;
        while (true) {
            int i = q.poll();
            t[i]--;
            time++;
            if (i == k && t[i] == 0) return time;
            if (t[i] > 0) q.offer(i);                 // back of the line
        }
    }

    static int timeRequiredToBuy(int[] tickets, int k) {
        int time = 0, need = tickets[k];
        for (int i = 0; i < tickets.length; i++) {
            time += Math.min(tickets[i], i <= k ? need : need - 1);
        }
        return time;
    }

    public static void main(String[] args) {
        check("[2,3,2] k=2", 6, timeRequiredToBuy(new int[]{2, 3, 2}, 2));
        check("[5,1,1,1] k=0", 8, timeRequiredToBuy(new int[]{5, 1, 1, 1}, 0));
        check("simulation agrees", 8, timeRequiredToBuySimulate(new int[]{5, 1, 1, 1}, 0));
        Random rnd = new Random(4);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            int[] a = rnd.ints(1 + rnd.nextInt(10), 1, 8).toArray();
            int k = rnd.nextInt(a.length);
            ok &= timeRequiredToBuy(a, k) == timeRequiredToBuySimulate(a, k);
        }
        check("formula == simulation on 500 random lines", true, ok);
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
