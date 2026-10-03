/*
 * ============================================================================
 *  HARD 01 : WORD LADDER                     LeetCode #127
 *  https://leetcode.com/problems/word-ladder/
 *  Pattern: BFS on an IMPLICIT graph (words differing by one letter) + BIDIRECTIONAL BFS
 * ============================================================================
 *
 *  PROBLEM
 *    Transform beginWord into endWord changing ONE letter at a time; every
 *    intermediate word must be in wordList. Return the number of WORDS in the
 *    shortest sequence (0 if impossible).
 *      hit -> hot -> dot -> dog -> cog   = 5
 *
 *  ---------------------------------------------------------------------------
 *  GRAPH: vertices = words; an edge joins two words that differ in exactly one
 *  position. All edges cost 1 -> BFS.
 *
 *  GENERATING NEIGHBOURS (the real problem)
 *    compare against every word: O(N * L) per word -> O(N^2 * L) total. Slow for N = 5000.
 *    BETTER: for each position, try all 26 letters and look the result up in a
 *    HashSet: O(26 * L) candidates per word, each an O(L) hash -> O(N * 26 * L^2).
 *    REMOVE a word from the set when it's enqueued: that's the visited marker.
 *
 *  BIDIRECTIONAL BFS: grow from BOTH ends, always expanding the SMALLER frontier;
 *  stop when they meet. If each word has b neighbours and the answer is d,
 *  one-sided BFS explores ~b^d words, two-sided ~2 * b^(d/2). Huge win.
 *
 *  DRY RUN one-sided: level 1 {hit} -> 2 {hot} -> 3 {dot, lot} -> 4 {dog, log}
 *  -> 5 {cog} = endWord -> 5
 *
 *  COMPLEXITY   Time O(N * 26 * L^2)   Space O(N * L)
 * ============================================================================
 */

import java.util.*;

public class H01_WordLadder {

    static int ladderLength(String beginWord, String endWord, List<String> wordList) {
        Set<String> unvisited = new HashSet<>(wordList);
        if (!unvisited.contains(endWord)) return 0;
        Queue<String> q = new ArrayDeque<>(List.of(beginWord));
        unvisited.remove(beginWord);
        int words = 1;
        while (!q.isEmpty()) {
            for (int i = q.size(); i > 0; i--) {
                String w = q.poll();
                if (w.equals(endWord)) return words;
                char[] cs = w.toCharArray();
                for (int p = 0; p < cs.length; p++) {
                    char orig = cs[p];
                    for (char c = 'a'; c <= 'z'; c++) {
                        if (c == orig) continue;
                        cs[p] = c;
                        String nb = new String(cs);
                        if (unvisited.remove(nb)) q.add(nb);      // in the list and not seen yet
                    }
                    cs[p] = orig;
                }
            }
            words++;
        }
        return 0;
    }

    static int bidirectional(String beginWord, String endWord, List<String> wordList) {
        Set<String> dict = new HashSet<>(wordList);
        if (!dict.contains(endWord)) return 0;
        Set<String> front = new HashSet<>(List.of(beginWord)), back = new HashSet<>(List.of(endWord));
        dict.remove(beginWord);
        dict.remove(endWord);
        int words = 1;
        while (!front.isEmpty() && !back.isEmpty()) {
            if (front.size() > back.size()) { Set<String> t = front; front = back; back = t; }   // expand the smaller side
            Set<String> next = new HashSet<>();
            for (String w : front) {
                char[] cs = w.toCharArray();
                for (int p = 0; p < cs.length; p++) {
                    char orig = cs[p];
                    for (char c = 'a'; c <= 'z'; c++) {
                        if (c == orig) continue;
                        cs[p] = c;
                        String nb = new String(cs);
                        if (back.contains(nb)) return words + 1;      // the two searches meet
                        if (dict.remove(nb)) next.add(nb);
                    }
                    cs[p] = orig;
                }
            }
            front = next;
            words++;
        }
        return 0;
    }

    public static void main(String[] args) {
        List<String> list = List.of("hot", "dot", "dog", "lot", "log", "cog");
        check("hit -> cog", 5, ladderLength("hit", "cog", list));
        check("cog missing", 0, ladderLength("hit", "cog", List.of("hot", "dot", "dog", "lot", "log")));
        check("bidirectional", 5, bidirectional("hit", "cog", list));
        check("one step", 2, ladderLength("a", "c", List.of("a", "b", "c")));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            Set<String> ws = new LinkedHashSet<>();
            for (int i = 0; i < 2 + rnd.nextInt(30); i++) ws.add("" + (char) ('a' + rnd.nextInt(3)) + (char) ('a' + rnd.nextInt(3)) + (char) ('a' + rnd.nextInt(3)));
            List<String> wl = new ArrayList<>(ws);
            String b = "" + (char) ('a' + rnd.nextInt(3)) + (char) ('a' + rnd.nextInt(3)) + (char) ('a' + rnd.nextInt(3));
            String e = wl.get(rnd.nextInt(wl.size()));
            if (b.equals(e)) continue;
            ok &= ladderLength(b, e, wl) == bidirectional(b, e, wl);
        }
        check("500 random dictionaries: BFS == bidirectional", true, ok);
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
