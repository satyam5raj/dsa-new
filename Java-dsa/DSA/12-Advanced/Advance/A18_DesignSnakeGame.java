/*
 * ============================================================================
 *  ADVANCE 18 : DESIGN SNAKE GAME  ⭐ bonus   LeetCode #353
 *  https://leetcode.com/problems/design-snake-game/
 *  Pattern: DEQUE (the body, head at the front) + HASHSET (which cells are occupied)
 * ============================================================================
 *
 *  PROBLEM
 *    A width x height board. The snake starts at (0,0) with length 1. food[] lists
 *    food positions; only the NEXT piece is on the board at any time. move(dir) with
 *    dir in U/D/L/R returns the score (food eaten) or -1 if the game is over (hit a
 *    wall or its own body). Eating food makes the snake one longer.
 *      SnakeGame(width 3, height 2, food [[1,2],[0,1]])
 *      R -> 0   D -> 0   R -> 1 (eats)   U -> 1   L -> 2 (eats)   U -> -1 (wall)
 *
 *  ---------------------------------------------------------------------------
 *  TWO OPERATIONS PER MOVE, TWO STRUCTURES
 *    move the ends: add a new head, drop the tail     -> a DEQUE, O(1) at both ends
 *    "did the head hit the body?"                     -> a HASHSET of occupied cells, O(1)
 *  A list alone makes the collision check O(length); a set alone forgets which cell
 *  is the tail. Each cell is encoded as one int: row * width + col.
 *
 *  THE SUBTLE RULE: the tail moves away in the same step, so the head MAY move into
 *  the cell the tail is leaving - unless the snake eats (then the tail stays).
 *  So: decide whether it eats, remove the tail if it doesn't, THEN check collision.
 *
 *  DRY RUN width 3, height 2 (cells numbered r*3+c)
 *    start body [0]
 *    R -> (0,1)=1  no food   drop tail 0 -> body [1]           score 0
 *    D -> (1,1)=4  no food   -> [4]                             score 0
 *    R -> (1,2)=5  FOOD      keep tail -> [5,4]                 score 1
 *    U -> (0,2)=2  no food   drop 4 -> [2,5]                    score 1
 *    L -> (0,1)=1  FOOD      -> [1,2,5]                         score 2
 *    U -> (-1,1)   wall                                         -1
 *
 *  COMPLEXITY   O(1) per move   Space O(length + food)
 * ============================================================================
 */

import java.util.*;

public class A18_DesignSnakeGame {

    // ---------------- Approach 1: a list of cells, collision by scanning (the referee) ----------------
    static class ListSnakeGame {
        private final int width, height;
        private final int[][] food;
        private int foodIndex = 0;
        private final List<int[]> snake = new ArrayList<>();               // index 0 = head

        ListSnakeGame(int width, int height, int[][] food) { this.width = width; this.height = height; this.food = food; snake.add(new int[]{0, 0}); }

        public int move(String direction) {
            int r = snake.get(0)[0], c = snake.get(0)[1];
            switch (direction) { case "U" -> r--; case "D" -> r++; case "L" -> c--; default -> c++; }
            if (r < 0 || r >= height || c < 0 || c >= width) return -1;
            boolean eats = foodIndex < food.length && food[foodIndex][0] == r && food[foodIndex][1] == c;
            if (eats) foodIndex++;
            else snake.remove(snake.size() - 1);
            for (int[] cell : snake) if (cell[0] == r && cell[1] == c) return -1;
            snake.add(0, new int[]{r, c});
            return snake.size() - 1;
        }
    }

    // ---------------- Approach 2: deque + hash set (main solution) ----------------
    static class SnakeGame {
        private final int width, height;
        private final int[][] food;
        private int foodIndex = 0;
        private final Deque<Integer> body = new ArrayDeque<>();            // head at the front; cell = r * width + c
        private final Set<Integer> occupied = new HashSet<>();

        SnakeGame(int width, int height, int[][] food) {
            this.width = width; this.height = height; this.food = food;
            body.addFirst(0);
            occupied.add(0);
        }

        public int move(String direction) {
            int head = body.peekFirst(), r = head / width, c = head % width;
            switch (direction) { case "U" -> r--; case "D" -> r++; case "L" -> c--; default -> c++; }
            if (r < 0 || r >= height || c < 0 || c >= width) return -1;      // hit a wall
            int next = r * width + c;
            boolean eats = foodIndex < food.length && food[foodIndex][0] == r && food[foodIndex][1] == c;
            if (eats) foodIndex++;
            else occupied.remove(body.pollLast());                           // the tail moves out of the way first
            if (!occupied.add(next)) return -1;                              // ran into its own body
            body.addFirst(next);
            return body.size() - 1;                                          // score = food eaten = length - 1
        }
    }

    public static void main(String[] args) {
        SnakeGame g = new SnakeGame(3, 2, new int[][]{{1, 2}, {0, 1}});
        StringBuilder sb = new StringBuilder();
        for (String d : "R D R U L U".split(" ")) sb.append(g.move(d)).append(' ');
        check("LeetCode example", "0 0 1 1 2 -1", sb.toString().trim());

        SnakeGame chase = new SnakeGame(2, 2, new int[][]{{0, 1}, {1, 1}, {1, 0}});
        sb.setLength(0);
        for (String d : "R D L U".split(" ")) sb.append(chase.move(d)).append(' ');
        check("full board: head follows the tail", "1 2 3 3", sb.toString().trim());

        SnakeGame back = new SnakeGame(3, 2, new int[][]{{0, 1}, {0, 2}});
        sb.setLength(0);
        for (String d : "R R L".split(" ")) sb.append(back.move(d)).append(' ');
        check("length 3 turning back hits itself", "1 2 -1", sb.toString().trim());

        SnakeGame loop = new SnakeGame(2, 2, new int[][]{{0, 1}});
        sb.setLength(0);
        for (String d : "R D L U R".split(" ")) sb.append(loop.move(d)).append(' ');
        check("head may enter the cell the tail leaves", "1 1 1 1 1", sb.toString().trim());

        Random rnd = new Random(18);
        String[] dirs = {"U", "D", "L", "R"};
        boolean ok = true;
        for (int t = 0; t < 3000; t++) {
            int w = 2 + rnd.nextInt(4), h = 2 + rnd.nextInt(4);
            int[][] food = new int[rnd.nextInt(12)][];
            for (int i = 0; i < food.length; i++) food[i] = new int[]{rnd.nextInt(h), rnd.nextInt(w)};
            SnakeGame main = new SnakeGame(w, h, food);
            ListSnakeGame ref = new ListSnakeGame(w, h, food);
            for (int i = 0; i < 60; i++) {
                String d = dirs[rnd.nextInt(4)];
                int a = main.move(d), b = ref.move(d);
                ok &= a == b;
                if (a == -1) break;                                          // game over
            }
        }
        check("3,000 random games vs list scanning", true, ok);
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
