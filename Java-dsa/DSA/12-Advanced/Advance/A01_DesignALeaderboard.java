/*
 * ============================================================================
 *  ADVANCE 01 : DESIGN A LEADERBOARD  ⭐ bonus   LeetCode #1244
 *  https://leetcode.com/problems/design-a-leaderboard/
 *  Pattern: HashMap (player -> score) + TreeMap (score -> how many players)
 * ============================================================================
 *
 *  PROBLEM
 *    addScore(playerId, score): add score to the player's total (a new player starts at 0)
 *    top(K):                    the SUM of the K highest totals
 *    reset(playerId):           the player's total goes back to 0 (they leave the board)
 *      addScore(1,73) (2,56) (3,39) (4,51) (5,4)   top(1) = 73
 *      reset(1) reset(2) addScore(2,51)            top(3) = 51 + 51 + 39 = 141
 *
 *  ---------------------------------------------------------------------------
 *  WHY TWO STRUCTURES: a HashMap finds a player's score in O(1) but knows nothing
 *  about ORDER. A sorted structure knows the order but can't find a player. Keep both:
 *    score:        playerId -> current total             (find / update a player)
 *    countByScore: total -> number of players with it    (sorted, highest first)
 *  Counts (not player ids) because two players can have the same total.
 *
 *  addScore: remove the old total from countByScore, add the new one   O(log n)
 *  reset:    remove the old total                                      O(log n)
 *  top(K):   walk countByScore from the highest total, taking up to K  O(K) at worst
 *
 *  Other ways: sort all totals on every top()   O(n log n)
 *              a min-heap of size K on every top()   O(n log K)
 *
 *  DRY RUN of the example after reset(1), reset(2), addScore(2,51):
 *    score {2:51, 3:39, 4:51, 5:4}     countByScore {51:2, 39:1, 4:1}
 *    top(3): 51 x2 -> sum 102, K=1;  39 x1 -> sum 141, K=0  -> 141
 *
 *  COMPLEXITY   addScore / reset O(log n)   top O(K)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class A01_DesignALeaderboard {

    // ---------------- Approach 1: sort everything on top() (the referee) ----------------
    static class SortLeaderboard {
        private final Map<Integer, Integer> score = new HashMap<>();
        public void addScore(int playerId, int s) { score.merge(playerId, s, Integer::sum); }
        public int top(int K) {
            List<Integer> all = new ArrayList<>(score.values());
            all.sort(Collections.reverseOrder());
            int sum = 0;
            for (int i = 0; i < K; i++) sum += all.get(i);
            return sum;
        }
        public void reset(int playerId) { score.remove(playerId); }
    }

    // ---------------- Approach 2: min-heap of size K on top() ----------------
    static class HeapLeaderboard {
        private final Map<Integer, Integer> score = new HashMap<>();
        public void addScore(int playerId, int s) { score.merge(playerId, s, Integer::sum); }
        public int top(int K) {
            PriorityQueue<Integer> best = new PriorityQueue<>();      // min-heap: the smallest of the best K on top
            for (int s : score.values()) {
                best.add(s);
                if (best.size() > K) best.poll();                    // drop the smallest, keep the K largest
            }
            int sum = 0;
            for (int s : best) sum += s;
            return sum;
        }
        public void reset(int playerId) { score.remove(playerId); }
    }

    // ---------------- Approach 3: HashMap + TreeMap of counts (main solution) ----------------
    static class Leaderboard {
        private final Map<Integer, Integer> score = new HashMap<>();
        private final TreeMap<Integer, Integer> countByScore = new TreeMap<>(Comparator.reverseOrder());

        public void addScore(int playerId, int s) {
            Integer old = score.get(playerId);
            if (old != null) decrement(old);
            int now = (old == null ? 0 : old) + s;
            score.put(playerId, now);
            countByScore.merge(now, 1, Integer::sum);
        }

        public int top(int K) {
            int sum = 0;
            for (Map.Entry<Integer, Integer> e : countByScore.entrySet()) {
                int take = Math.min(K, e.getValue());
                sum += take * e.getKey();
                K -= take;
                if (K == 0) break;
            }
            return sum;
        }

        public void reset(int playerId) {
            Integer old = score.remove(playerId);
            if (old != null) decrement(old);
        }

        private void decrement(int s) {
            if (countByScore.merge(s, -1, Integer::sum) == 0) countByScore.remove(s);
        }
    }

    public static void main(String[] args) {
        Leaderboard lb = new Leaderboard();
        lb.addScore(1, 73); lb.addScore(2, 56); lb.addScore(3, 39); lb.addScore(4, 51); lb.addScore(5, 4);
        check("top(1)", 73, lb.top(1));
        lb.reset(1); lb.reset(2); lb.addScore(2, 51);
        check("top(3) after resets", 141, lb.top(3));

        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 200; t++) {
            Leaderboard main = new Leaderboard();
            HeapLeaderboard heap = new HeapLeaderboard();
            SortLeaderboard ref = new SortLeaderboard();
            Set<Integer> players = new HashSet<>();
            for (int i = 0; i < 300; i++) {
                int op = rnd.nextInt(10), id = rnd.nextInt(15);
                if (op < 6) { int s = 1 + rnd.nextInt(20); main.addScore(id, s); heap.addScore(id, s); ref.addScore(id, s); players.add(id); }
                else if (op < 8 && players.contains(id)) { main.reset(id); heap.reset(id); ref.reset(id); players.remove(id); }
                else if (!players.isEmpty()) {
                    int k = 1 + rnd.nextInt(players.size());
                    int want = ref.top(k);
                    ok &= main.top(k) == want && heap.top(k) == want;
                }
            }
        }
        check("60,000 random ops: TreeMap == heap == sort", true, ok);
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
