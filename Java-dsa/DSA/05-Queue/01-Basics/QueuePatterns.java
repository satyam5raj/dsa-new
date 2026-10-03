/*
 * ============================================================================
 *  QUEUE 1.3 : THE QUEUE PATTERNS
 * ============================================================================
 *
 *  Run:  java QueuePatterns.java
 *
 *  -----------------------------------------------------------------------------------
 *   Pattern                        Clue                                       Problems
 *  -----------------------------------------------------------------------------------
 *   1. BFS (level by level)         shortest path in an UNWEIGHTED grid/graph,  (Trees, Graphs)
 *                                   "minimum number of steps", level order
 *   2. Multi-source BFS             spread from MANY starting points at once    (Rotting Oranges)
 *   3. Sliding TIME window          "in the last N seconds", rate limiting      E02 E03 M03
 *   4. Monotonic DEQUE              max/min of a sliding window, DP over a      M06 M07 H01 H02
 *                                   window of previous states
 *   5. Simulation / round robin     people in a line, turns, card dealing       E04 M04 M05
 *   6. Ring buffer design           fixed capacity, reuse slots                 M01 M02
 *  -----------------------------------------------------------------------------------
 *
 *  WHY BFS GIVES SHORTEST PATHS: the queue processes everything at distance d
 *  before anything at distance d+1 (FIFO keeps the "waves" in order). The first
 *  time BFS reaches a cell, it has used the minimum number of steps.
 */

import java.util.*;

public class QueuePatterns {

    public static void main(String[] args) {
        bfsShortestPath();
        multiSourceBfs();
        slidingTimeWindow();
        monotonicDeque();
        roundRobin();
    }

    // =================================================================
    // 1. BFS on a grid: minimum steps from S to E ('#' = wall)
    //    Mark cells visited WHEN ENQUEUED (not when polled), or the same
    //    cell gets enqueued many times.
    // =================================================================
    static int shortestPath(char[][] grid, int sr, int sc, int er, int ec) {
        int R = grid.length, C = grid[0].length;
        boolean[][] seen = new boolean[R][C];
        Deque<int[]> q = new ArrayDeque<>();
        q.offer(new int[]{sr, sc});
        seen[sr][sc] = true;
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        for (int steps = 0; !q.isEmpty(); steps++) {
            for (int n = q.size(); n > 0; n--) {               // process ONE whole level
                int[] cur = q.poll();
                if (cur[0] == er && cur[1] == ec) return steps;
                for (int[] d : dirs) {
                    int r = cur[0] + d[0], c = cur[1] + d[1];
                    if (r >= 0 && r < R && c >= 0 && c < C && !seen[r][c] && grid[r][c] != '#') {
                        seen[r][c] = true;
                        q.offer(new int[]{r, c});
                    }
                }
            }
        }
        return -1;                                               // unreachable
    }

    static void bfsShortestPath() {
        System.out.println("===== 1. BFS SHORTEST PATH =====");
        char[][] g = {
                "S..#....".toCharArray(),
                ".#.#.##.".toCharArray(),
                ".#...#E.".toCharArray(),
                "...#....".toCharArray()};
        System.out.println("   minimum steps S -> E = " + shortestPath(g, 0, 0, 2, 6));
    }

    // =================================================================
    // 2. MULTI-SOURCE BFS: start with ALL sources in the queue at distance 0.
    //    Example: distance from every cell to its nearest '1' (a "fire spreading" map).
    // =================================================================
    static int[][] distanceToNearestOne(int[][] grid) {
        int R = grid.length, C = grid[0].length;
        int[][] dist = new int[R][C];
        Deque<int[]> q = new ArrayDeque<>();
        for (int r = 0; r < R; r++)
            for (int c = 0; c < C; c++) {
                if (grid[r][c] == 1) q.offer(new int[]{r, c});   // every source starts the race together
                else dist[r][c] = -1;                           // -1 = not reached yet
            }
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        while (!q.isEmpty()) {
            int[] cur = q.poll();
            for (int[] d : dirs) {
                int r = cur[0] + d[0], c = cur[1] + d[1];
                if (r >= 0 && r < R && c >= 0 && c < C && dist[r][c] == -1) {
                    dist[r][c] = dist[cur[0]][cur[1]] + 1;
                    q.offer(new int[]{r, c});
                }
            }
        }
        return dist;
    }

    static void multiSourceBfs() {
        System.out.println("\n===== 2. MULTI-SOURCE BFS =====");
        int[][] dist = distanceToNearestOne(new int[][]{{0, 0, 0, 1}, {0, 0, 0, 0}, {1, 0, 0, 0}});
        for (int[] row : dist) System.out.println("   " + Arrays.toString(row));
    }

    // =================================================================
    // 3. SLIDING TIME WINDOW: keep only the events of the last W time units.
    //    New events go in at the back; expired ones fall out of the front.
    //    Example: allow at most 3 requests per 10 seconds (a simple rate limiter).
    // =================================================================
    static class RateLimiter {
        private final Deque<Integer> times = new ArrayDeque<>();
        private final int limit, window;

        RateLimiter(int limit, int window) { this.limit = limit; this.window = window; }

        boolean allow(int t) {
            while (!times.isEmpty() && times.peekFirst() <= t - window) times.pollFirst();   // expire old requests
            if (times.size() >= limit) return false;
            times.offerLast(t);
            return true;
        }
    }

    static void slidingTimeWindow() {
        System.out.println("\n===== 3. SLIDING TIME WINDOW (rate limiter: 3 per 10s) =====");
        RateLimiter rl = new RateLimiter(3, 10);
        StringBuilder sb = new StringBuilder();
        for (int t : new int[]{1, 2, 3, 4, 11, 12, 13, 14}) sb.append("t=").append(t).append(rl.allow(t) ? " ok  " : " NO  ");
        System.out.println("   " + sb);
    }

    // =================================================================
    // 4. MONOTONIC DEQUE: the max of every window of size k in O(n).
    //    (Fully explained in Arrays/H02 Sliding Window Maximum.)
    //    Keep indices with DECREASING values; the front is the window's max.
    //      - drop from the FRONT the indices that left the window
    //      - drop from the BACK the values <= the new one (they can never be the max again)
    // =================================================================
    static int[] windowMax(int[] a, int k) {
        int[] res = new int[a.length - k + 1];
        Deque<Integer> dq = new ArrayDeque<>();
        for (int i = 0; i < a.length; i++) {
            if (!dq.isEmpty() && dq.peekFirst() <= i - k) dq.pollFirst();
            while (!dq.isEmpty() && a[dq.peekLast()] <= a[i]) dq.pollLast();
            dq.offerLast(i);
            if (i >= k - 1) res[i - k + 1] = a[dq.peekFirst()];
        }
        return res;
    }

    static void monotonicDeque() {
        System.out.println("\n===== 4. MONOTONIC DEQUE =====");
        System.out.println("   window max k=3 of [1,3,-1,-3,5,3,6,7] = " + Arrays.toString(windowMax(new int[]{1, 3, -1, -3, 5, 3, 6, 7}, 3)));
    }

    // =================================================================
    // 5. ROUND ROBIN: serve the front, and if work remains, send it to the BACK
    //    (how a CPU scheduler shares time slices between processes)
    // =================================================================
    static void roundRobin() {
        System.out.println("\n===== 5. ROUND ROBIN (time slice = 2) =====");
        Deque<int[]> q = new ArrayDeque<>();                     // [processId, remaining time]
        q.offer(new int[]{1, 5});
        q.offer(new int[]{2, 2});
        q.offer(new int[]{3, 3});
        int clock = 0;
        StringBuilder log = new StringBuilder();
        while (!q.isEmpty()) {
            int[] p = q.poll();
            int run = Math.min(2, p[1]);
            clock += run;
            p[1] -= run;
            if (p[1] > 0) q.offer(p);                            // not finished: back of the line
            else log.append("P").append(p[0]).append(" done at t=").append(clock).append("  ");
        }
        System.out.println("   " + log);
    }
}
