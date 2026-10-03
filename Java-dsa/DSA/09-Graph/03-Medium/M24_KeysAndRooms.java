/*
 * ============================================================================
 *  MEDIUM 24 : KEYS AND ROOMS                LeetCode #841
 *  https://leetcode.com/problems/keys-and-rooms/
 *  Pattern: REACHABILITY from one source (the keys ARE the edges)
 * ============================================================================
 *
 *  PROBLEM
 *    Rooms 0..n-1; only room 0 is open. rooms[i] = the keys found in room i.
 *    Can you visit every room?
 *      [[1],[2],[3],[]]          -> true
 *      [[1,3],[3,0,1],[2],[0]]   -> false (the key to room 2 is inside room 2)
 *
 *  ---------------------------------------------------------------------------
 *  MODEL: room i -> room k for every key k in room i (a DIRECTED graph).
 *  "Visit every room" = "every vertex is reachable from 0".
 *  One DFS/BFS from 0 with visited[]; count the visited rooms.
 *
 *  The skill here is RECOGNIZING the graph: no edges are given explicitly, but
 *  "having X lets you reach Y" is always an edge X -> Y.
 *
 *  COMPLEXITY   Time O(rooms + keys)   Space O(rooms)
 * ============================================================================
 */

import java.util.*;

public class M24_KeysAndRooms {

    static boolean canVisitAllRooms(List<List<Integer>> rooms) {
        boolean[] visited = new boolean[rooms.size()];
        Deque<Integer> stack = new ArrayDeque<>(List.of(0));
        visited[0] = true;
        int count = 1;
        while (!stack.isEmpty()) {
            for (int key : rooms.get(stack.pop())) {
                if (!visited[key]) { visited[key] = true; count++; stack.push(key); }
            }
        }
        return count == rooms.size();
    }

    // referee: repeat "open every room we have a key for" until nothing changes
    static boolean fixpoint(List<List<Integer>> rooms) {
        Set<Integer> open = new HashSet<>(List.of(0));
        boolean changed = true;
        while (changed) {
            changed = false;
            for (int r : new ArrayList<>(open)) for (int k : rooms.get(r)) changed |= open.add(k);
        }
        return open.size() == rooms.size();
    }

    public static void main(String[] args) {
        check("[[1],[2],[3],[]]", true, canVisitAllRooms(List.of(List.of(1), List.of(2), List.of(3), List.of())));
        check("key locked inside", false, canVisitAllRooms(List.of(List.of(1, 3), List.of(3, 0, 1), List.of(2), List.of(0))));
        Random rnd = new Random(24);
        boolean ok = true;
        for (int t = 0; t < 1000; t++) {
            int n = 1 + rnd.nextInt(8);
            List<List<Integer>> rooms = new ArrayList<>();
            for (int i = 0; i < n; i++) {
                List<Integer> keys = new ArrayList<>();
                for (int j = 0; j < rnd.nextInt(3); j++) keys.add(rnd.nextInt(n));
                rooms.add(keys);
            }
            ok &= canVisitAllRooms(rooms) == fixpoint(rooms);
        }
        check("1,000 random buildings vs fixpoint", true, ok);
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
