/*
 * ============================================================================
 *  ADVANCE 20 : EXAM ROOM  ⭐ bonus   LeetCode #855
 *  https://leetcode.com/problems/exam-room/
 *  Pattern: TreeSet of taken seats + MAX-HEAP of gaps with LAZY DELETION
 * ============================================================================
 *
 *  PROBLEM
 *    n seats in a row (0..n-1). seat(): a student takes the seat that MAXIMISES the
 *    distance to the closest other student (ties: the lowest seat number); in an empty
 *    room that's seat 0. leave(p): the student in seat p leaves.
 *      ExamRoom(10): seat 0, seat 9, seat 4, seat 2, leave(4), seat 5
 *
 *  ---------------------------------------------------------------------------
 *  THINK IN GAPS. Between two neighbouring students a and b the best seat is the middle,
 *  (a + b) / 2, at distance (b - a) / 2. The two ends are special: before the first
 *  student the best seat is 0 (distance = first), after the last it's n-1.
 *  Use sentinels: a = -1 means "the left wall", b = n means "the right wall".
 *
 *  APPROACH 1 (simple): a TreeSet of seats; seat() walks all gaps - O(k) per seat.
 *  APPROACH 2 (main): also keep every gap in a MAX-HEAP ordered by
 *      (distance desc, seat asc) -> the top is the answer.
 *    leave(p) merges the gaps (lo, p) and (p, hi) into (lo, hi). Removing the two old
 *    gaps from a heap is O(n), so DON'T: push the new gap, and when an old gap reaches
 *    the top, check it against the TreeSet - a gap (a, b) is valid only if a is still
 *    taken (or is the wall) and b is the very next taken seat after a. LAZY DELETION.
 *
 *  DRY RUN n = 10      (gap -> seat, distance)
 *    start          heap {(-1,10) -> 0}                    seat 0
 *    push (0,10) -> 9, dist 9                               seat 9
 *    push (0,9) -> 4, dist 4                                seat 4
 *    push (0,4) -> 2 d2, (4,9) -> 6 d2   tie -> lower seat  seat 2
 *    leave 4: push (2,9) -> 5 d3; (2,4) and (4,9) are now stale
 *    top (2,9) valid (next after 2 is 9)                    seat 5
 *
 *  COMPLEXITY   seat / leave amortised O(log n)   Space O(n + operations)
 * ============================================================================
 */

import java.util.*;

public class A20_ExamRoom {

    // ---------------- Referee: try every empty seat ----------------
    static class BruteExamRoom {
        private final boolean[] taken;
        BruteExamRoom(int n) { taken = new boolean[n]; }

        public int seat() {
            int n = taken.length, best = -1, bestDist = -1;
            for (int s = 0; s < n; s++) {
                if (taken[s]) continue;
                int d = Integer.MAX_VALUE;
                for (int o = 0; o < n; o++) if (taken[o]) d = Math.min(d, Math.abs(o - s));
                if (d > bestDist) { bestDist = d; best = s; }               // strict > keeps the lowest seat on ties
            }
            taken[best] = true;
            return best;
        }
        public void leave(int p) { taken[p] = false; }
    }

    // ---------------- Approach 1: TreeSet, scan the gaps ----------------
    static class ScanExamRoom {
        private final int n;
        private final TreeSet<Integer> seats = new TreeSet<>();
        ScanExamRoom(int n) { this.n = n; }

        public int seat() {
            int best = 0;
            if (!seats.isEmpty()) {
                int bestDist = seats.first();                               // sit at 0: distance to the first student
                Integer prev = null;
                for (int s : seats) {
                    if (prev != null && (s - prev) / 2 > bestDist) { bestDist = (s - prev) / 2; best = prev + bestDist; }
                    prev = s;
                }
                if (n - 1 - seats.last() > bestDist) best = n - 1;           // sit at the far end
            }
            seats.add(best);
            return best;
        }
        public void leave(int p) { seats.remove(p); }
    }

    // ---------------- Approach 2: TreeSet + max-heap of gaps, lazy deletion (main solution) ----------------
    static class ExamRoom {
        private final int n;
        private final TreeSet<Integer> seats = new TreeSet<>();
        private final PriorityQueue<int[]> gaps;                            // {a, b}: a = -1 left wall, b = n right wall

        ExamRoom(int n) {
            this.n = n;
            gaps = new PriorityQueue<>((x, y) -> {
                int dx = distance(x), dy = distance(y);
                return dx != dy ? Integer.compare(dy, dx) : Integer.compare(seatIn(x), seatIn(y));
            });
            gaps.add(new int[]{-1, n});
        }

        private int seatIn(int[] g) { return g[0] == -1 ? 0 : g[1] == n ? n - 1 : (g[0] + g[1]) / 2; }

        private int distance(int[] g) {
            if (g[0] == -1 && g[1] == n) return n;                          // empty room: the only gap
            if (g[0] == -1) return g[1];                                    // sit at 0, nearest student at b
            if (g[1] == n) return n - 1 - g[0];                             // sit at n-1, nearest student at a
            return (g[1] - g[0]) / 2;
        }

        private boolean valid(int[] g) {
            if (g[0] != -1 && !seats.contains(g[0])) return false;
            Integer next = g[0] == -1 ? (seats.isEmpty() ? null : seats.first()) : seats.higher(g[0]);
            return (next == null ? n : next) == g[1];                       // b must be the very next student after a
        }

        private void push(int a, int b) { if (b - a > 1) gaps.add(new int[]{a, b}); }   // only gaps with an empty seat

        public int seat() {
            while (!valid(gaps.peek())) gaps.poll();                        // stale gaps from earlier leaves
            int[] g = gaps.poll();
            int s = seatIn(g);
            seats.add(s);
            push(g[0], s);
            push(s, g[1]);
            return s;
        }

        public void leave(int p) {
            seats.remove(p);
            Integer lo = seats.lower(p), hi = seats.higher(p);
            push(lo == null ? -1 : lo, hi == null ? n : hi);                // the merged gap; the two old ones go stale
        }
    }

    public static void main(String[] args) {
        ExamRoom room = new ExamRoom(10);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 4; i++) sb.append(room.seat()).append(' ');
        room.leave(4);
        sb.append(room.seat());
        check("LeetCode example", "0 9 4 2 5", sb.toString());
        ExamRoom one = new ExamRoom(1);
        check("a single seat", 0, one.seat());
        ExamRoom two = new ExamRoom(4);
        two.seat(); two.seat(); two.leave(0);
        check("leaving seat 0 re-opens the wall gap", 0, two.seat());

        Random rnd = new Random(20);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int n = 1 + rnd.nextInt(12);
            ExamRoom main = new ExamRoom(n);
            ScanExamRoom scan = new ScanExamRoom(n);
            BruteExamRoom ref = new BruteExamRoom(n);
            List<Integer> taken = new ArrayList<>();
            for (int i = 0; i < 60; i++) {
                if (taken.size() < n && (taken.isEmpty() || rnd.nextInt(3) > 0)) {
                    int want = ref.seat();
                    ok &= main.seat() == want && scan.seat() == want;
                    taken.add(want);
                } else {
                    int p = taken.remove(rnd.nextInt(taken.size()));
                    main.leave(p); scan.leave(p); ref.leave(p);
                }
            }
        }
        check("60,000 random ops: heap == scan == brute", true, ok);
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
