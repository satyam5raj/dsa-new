/*
 * ============================================================================
 *  HARD 08 : COURSE SCHEDULE III             LeetCode #630
 *  https://leetcode.com/problems/course-schedule-iii/
 *  Pattern: SORT by deadline + MAX-HEAP of durations ("swap out the longest")
 * ============================================================================
 *
 *  PROBLEM
 *    Course i takes duration[i] days and must FINISH by lastDay[i]. You take them
 *    one at a time starting on day 1. Maximum number of courses?
 *      [[100,200],[200,1300],[1000,1250],[2000,3200]] -> 3
 *
 *  ---------------------------------------------------------------------------
 *  STEP 1: if you take a set of courses, the best order is EARLIEST DEADLINE FIRST
 *  (exchange argument: swapping two courses that are out of deadline order never
 *  makes things worse). So sort by lastDay and decide course by course.
 *
 *  STEP 2 (the greedy): take every course; keep `time` = total duration taken.
 *    If time > lastDay of the current course, we must DROP one course. Drop the
 *    LONGEST one taken so far (maybe the current one): same count lost (1), but it
 *    frees the most time for the future. A MAX-heap of durations finds it.
 *      for (d, last) sorted by last:
 *        heap.offer(d); time += d
 *        if time > last: time -= heap.poll()     // drop the longest
 *    The answer is heap.size().
 *  Dropping one course is always enough: before adding the current course the
 *  schedule was feasible, and the longest duration is >= the current one, so after
 *  dropping it, time <= previous time <= previous deadline <= the current deadline.
 *
 *  DRY RUN sorted by deadline: [100,200] [1000,1250] [200,1300] [2000,3200]
 *    +100  time 100  <= 200 ok        heap {100}
 *    +1000 time 1100 <= 1250 ok       heap {1000,100}
 *    +200  time 1300 <= 1300 ok       heap {1000,200,100}
 *    +2000 time 3300 > 3200 -> drop 2000 -> time 1300     -> 3 courses
 *
 *  COMPLEXITY   Time O(n log n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class H08_CourseScheduleIII {

    static int scheduleCourse(int[][] courses) {
        int[][] c = courses.clone();
        Arrays.sort(c, Comparator.comparingInt(x -> x[1]));                 // earliest deadline first
        PriorityQueue<Integer> taken = new PriorityQueue<>(Comparator.reverseOrder());   // durations, max-heap
        int time = 0;
        for (int[] course : c) {
            taken.offer(course[0]);
            time += course[0];
            if (time > course[1]) time -= taken.poll();                     // drop the longest course
        }
        return taken.size();
    }

    // brute force: every subset, check it in deadline order
    static int brute(int[][] courses) {
        int n = courses.length, best = 0;
        int[][] c = courses.clone();
        Arrays.sort(c, Comparator.comparingInt(x -> x[1]));
        for (int mask = 0; mask < (1 << n); mask++) {
            int time = 0, count = 0;
            boolean ok = true;
            for (int i = 0; i < n && ok; i++) {
                if ((mask >> i & 1) == 0) continue;
                time += c[i][0];
                count++;
                if (time > c[i][1]) ok = false;
            }
            if (ok) best = Math.max(best, count);
        }
        return best;
    }

    public static void main(String[] args) {
        check("example", 3, scheduleCourse(new int[][]{{100, 200}, {200, 1300}, {1000, 1250}, {2000, 3200}}));
        check("[[1,2]]", 1, scheduleCourse(new int[][]{{1, 2}}));
        check("[[3,2],[4,3]] (none fit)", 0, scheduleCourse(new int[][]{{3, 2}, {4, 3}}));
        check("[[5,5],[4,6],[2,6]] (swap out the 5)", 2, scheduleCourse(new int[][]{{5, 5}, {4, 6}, {2, 6}}));
        Random rnd = new Random(8);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int n = 1 + rnd.nextInt(10);
            int[][] cs = new int[n][];
            for (int i = 0; i < n; i++) cs[i] = new int[]{1 + rnd.nextInt(8), 1 + rnd.nextInt(25)};
            ok &= scheduleCourse(cs) == brute(cs);
        }
        check("1,000 random cases vs all subsets", true, ok);
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
