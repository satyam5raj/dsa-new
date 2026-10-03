/*
 * ============================================================================
 *  MEDIUM 05 : REVEAL CARDS IN INCREASING ORDER  ⭐ bonus   LeetCode #950
 *  https://leetcode.com/problems/reveal-cards-in-increasing-order/
 *  Pattern: SIMULATE THE PROCESS ON POSITIONS (a queue of indices)
 * ============================================================================
 *
 *  PROBLEM
 *    Reveal process: take the top card and reveal it, then move the NEXT top card
 *    to the BOTTOM; repeat until no cards are left. Return an ordering of 'deck'
 *    so the cards are revealed in INCREASING order.
 *      [17,13,11,2,3,5,7] -> [2,13,3,11,5,17,7]
 *
 *  ---------------------------------------------------------------------------
 *  KEY IDEA: we don't know where each VALUE goes, but we can simulate the
 *  process on the POSITIONS 0..n-1 to find out in which ORDER the positions get
 *  revealed. Then put the smallest card at the first revealed position, the
 *  second smallest at the second, and so on.
 *
 *    queue of positions: [0,1,2,3,4,5,6]
 *    repeat: reveal = poll()            (this position gets the next smallest card)
 *            if not empty: offer(poll())   (the next position goes to the bottom)
 *    reveal order of positions: 0, 2, 4, 6, 3, 1, 5
 *    sorted cards:              2, 3, 5, 7, 11, 13, 17
 *    result[0]=2, result[2]=3, result[4]=5, result[6]=7, result[3]=11, result[1]=13, result[5]=17
 *    -> [2,13,3,11,5,17,7]
 *
 *  "Simulate on indices, then assign values" is a reusable trick for any
 *  "arrange the input so this process outputs a given order" puzzle.
 *  COMPLEXITY   Time O(n log n) (sorting)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M05_RevealCardsInIncreasingOrder {

    static int[] deckRevealedIncreasing(int[] deck) {
        int n = deck.length;
        int[] sorted = deck.clone();
        Arrays.sort(sorted);
        Deque<Integer> positions = new ArrayDeque<>();
        for (int i = 0; i < n; i++) positions.offer(i);
        int[] result = new int[n];
        for (int card : sorted) {
            result[positions.poll()] = card;                 // this position is revealed next
            if (!positions.isEmpty()) positions.offer(positions.poll());   // next one goes to the bottom
        }
        return result;
    }

    // Run the real reveal process on an ordering, to verify it reveals increasing cards
    static int[] reveal(int[] deckOrder) {
        Deque<Integer> d = new ArrayDeque<>();
        for (int c : deckOrder) d.offer(c);
        int[] out = new int[deckOrder.length];
        for (int i = 0; !d.isEmpty(); i++) {
            out[i] = d.poll();
            if (!d.isEmpty()) d.offer(d.poll());
        }
        return out;
    }

    public static void main(String[] args) {
        int[] r = deckRevealedIncreasing(new int[]{17, 13, 11, 2, 3, 5, 7});
        check("[17,13,11,2,3,5,7]", new int[]{2, 13, 3, 11, 5, 17, 7}, r);
        check("revealing it gives sorted order", new int[]{2, 3, 5, 7, 11, 13, 17}, reveal(r));
        check("[1,1000]", new int[]{1, 1000}, deckRevealedIncreasing(new int[]{1, 1000}));
        Random rnd = new Random(5);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            int[] deck = rnd.ints(1 + rnd.nextInt(30), 0, 1000).distinct().toArray();
            int[] s = deck.clone();
            Arrays.sort(s);
            ok &= Arrays.equals(reveal(deckRevealedIncreasing(deck)), s);
        }
        check("300 random decks reveal in order", true, ok);
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
