/*
 * ============================================================================
 *  MEDIUM 04 : DOTA2 SENATE  ⭐ bonus   LeetCode #649
 *  https://leetcode.com/problems/dota2-senate/
 *  Pattern: TWO QUEUES OF TURN ORDER (greedy round-robin simulation)
 * ============================================================================
 *
 *  PROBLEM
 *    Senators from two parties, Radiant ('R') and Dire ('D'), act in order, round
 *    after round. On their turn a senator may BAN one opposing senator (who loses
 *    all future turns). The party left standing wins.
 *      "RD"  -> "Radiant"     (R acts first and bans D)
 *      "RDD" -> "Dire"
 *
 *  ---------------------------------------------------------------------------
 *  GREEDY: on your turn, ban the opponent who would act NEXT (the earliest
 *  upcoming opponent). That's the one about to hurt your party soonest.
 *
 *  TWO QUEUES holding the TURN INDEX of each living senator, per party.
 *    Compare the two fronts: the SMALLER index acts first.
 *      the winner bans the other front (poll it, gone forever)
 *      the winner goes to the BACK of its queue with index + n (it acts again next round)
 *    When one queue is empty, the other party wins.
 *
 *  DRY RUN "RDD" (n=3)    R:[0]  D:[1,2]
 *    R0 vs D1: R0 acts first -> bans D1; R0 -> 3        R:[3]  D:[2]
 *    R3 vs D2: D2 acts first -> bans R3; D2 -> 5        R:[]   D:[5]   -> "Dire"
 *
 *  WHY + n? Using index + n keeps the global order correct: everyone in round 1
 *  (indices < n) acts before anyone's round 2 (indices >= n).
 *  COMPLEXITY   Time O(n) (each comparison removes one senator)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M04_Dota2Senate {

    static String predictPartyVictory(String senate) {
        int n = senate.length();
        Deque<Integer> radiant = new ArrayDeque<>(), dire = new ArrayDeque<>();
        for (int i = 0; i < n; i++) {
            if (senate.charAt(i) == 'R') radiant.offer(i); else dire.offer(i);
        }
        while (!radiant.isEmpty() && !dire.isEmpty()) {
            int r = radiant.poll(), d = dire.poll();
            if (r < d) radiant.offer(r + n);    // R acts first: bans d, and comes back next round
            else dire.offer(d + n);             // D acts first: bans r
        }
        return radiant.isEmpty() ? "Dire" : "Radiant";
    }

    public static void main(String[] args) {
        check("RD", "Radiant", predictPartyVictory("RD"));
        check("RDD", "Dire", predictPartyVictory("RDD"));
        check("DDRRR", "Dire", predictPartyVictory("DDRRR"));
        check("RRDDD", "Radiant", predictPartyVictory("RRDDD"));
        check("R (alone)", "Radiant", predictPartyVictory("R"));
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
