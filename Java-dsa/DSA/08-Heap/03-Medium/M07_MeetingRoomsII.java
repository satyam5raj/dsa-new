/*
 * ============================================================================
 *  MEDIUM 07 : MEETING ROOMS II              LeetCode #253 (premium)
 *  https://leetcode.com/problems/meeting-rooms-ii/
 *  Pattern: SORT by start + MIN-HEAP of end times  (or a sweep over +1/-1 events)
 * ============================================================================
 *
 *  PROBLEM
 *    Meeting intervals [start, end). Minimum number of rooms so none overlap in a room?
 *      [[0,30],[5,10],[15,20]] -> 2        [[7,10],[2,4]] -> 1
 *    (A meeting ending at 10 frees its room for one starting at 10.)
 *
 *  ---------------------------------------------------------------------------
 *  HEAP APPROACH (simulate a building manager)
 *    sort meetings by START
 *    min-heap = END times of meetings currently holding a room
 *    for each meeting:
 *       if the earliest-ending room is free by now (heap.peek() <= start): reuse it (poll)
 *       put this meeting's end into the heap
 *    rooms = the largest heap size reached (= heap size at the end, since we only
 *    poll when we immediately push again)
 *
 *  SWEEP / TWO SORTED ARRAYS (no heap)
 *    starts sorted, ends sorted. Walk the starts; for each start, if the next end
 *    <= start, a meeting finished (end pointer++), else we need one more room.
 *    Equivalently: +1 at every start, -1 at every end, the max running sum.
 *
 *  DRY RUN heap [[0,30],[5,10],[15,20]]
 *    [0,30]: heap {30}
 *    [5,10]: 30 > 5, new room -> {10,30}
 *    [15,20]: 10 <= 15, reuse -> poll 10, push 20 -> {20,30}  -> 2 rooms
 *
 *  COMPLEXITY   Time O(n log n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M07_MeetingRoomsII {

    static int minMeetingRooms(int[][] intervals) {
        int[][] m = intervals.clone();
        Arrays.sort(m, Comparator.comparingInt(iv -> iv[0]));
        PriorityQueue<Integer> ends = new PriorityQueue<>();
        for (int[] iv : m) {
            if (!ends.isEmpty() && ends.peek() <= iv[0]) ends.poll();   // reuse the room that frees first
            ends.offer(iv[1]);
        }
        return ends.size();
    }

    static int minMeetingRoomsSweep(int[][] intervals) {
        int n = intervals.length;
        int[] starts = new int[n], ends = new int[n];
        for (int i = 0; i < n; i++) { starts[i] = intervals[i][0]; ends[i] = intervals[i][1]; }
        Arrays.sort(starts);
        Arrays.sort(ends);
        int rooms = 0, e = 0;
        for (int s : starts) {
            if (ends[e] <= s) e++;          // a meeting ended: its room is reused
            else rooms++;                   // all busy: need a new room
        }
        return rooms;
    }

    static int brute(int[][] iv) {          // max overlap at any integer time point
        int best = 0;
        for (int t = 0; t <= 60; t++) {
            int c = 0;
            for (int[] x : iv) if (x[0] <= t && t < x[1]) c++;
            best = Math.max(best, c);
        }
        return best;
    }

    public static void main(String[] args) {
        check("[[0,30],[5,10],[15,20]]", 2, minMeetingRooms(new int[][]{{0, 30}, {5, 10}, {15, 20}}));
        check("[[7,10],[2,4]]", 1, minMeetingRooms(new int[][]{{7, 10}, {2, 4}}));
        check("back to back [[1,5],[5,9]]", 1, minMeetingRooms(new int[][]{{1, 5}, {5, 9}}));
        check("sweep version", 2, minMeetingRoomsSweep(new int[][]{{0, 30}, {5, 10}, {15, 20}}));
        Random rnd = new Random(7);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int n = 1 + rnd.nextInt(10);
            int[][] iv = new int[n][];
            for (int i = 0; i < n; i++) { int s = rnd.nextInt(50); iv[i] = new int[]{s, s + 1 + rnd.nextInt(10)}; }
            int b = brute(iv);
            ok &= minMeetingRooms(iv) == b && minMeetingRoomsSweep(iv) == b;
        }
        check("1,000 random schedules vs max overlap", true, ok);
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
