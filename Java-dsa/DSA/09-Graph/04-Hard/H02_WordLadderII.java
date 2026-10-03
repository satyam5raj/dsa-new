/*
 * ============================================================================
 *  HARD 02 : WORD LADDER II                  LeetCode #126
 *  https://leetcode.com/problems/word-ladder-ii/
 *  Pattern: BFS LEVEL BY LEVEL recording PARENTS, then DFS BACK from the end
 * ============================================================================
 *
 *  PROBLEM
 *    Like H01, but return ALL shortest transformation sequences.
 *      hit, cog, [hot,dot,dog,lot,log,cog] ->
 *        [[hit,hot,dot,dog,cog],[hit,hot,lot,log,cog]]
 *
 *  ---------------------------------------------------------------------------
 *  NAIVE: DFS over all paths, keep the shortest -> exponential, times out.
 *
 *  TWO PHASES
 *  1. BFS, LEVEL BY LEVEL, building a "parents" map: word -> the words on the
 *     PREVIOUS level that lead to it. Two subtleties:
 *       - a word can have SEVERAL parents on the previous level: cog is reached
 *         from BOTH dog and log. So don't block a word the moment it's first
 *         seen - remove the whole level from the dictionary only AFTER the level
 *         is done (otherwise the path through log would be lost).
 *       - stop after the level where endWord appears (only shortest paths).
 *  2. DFS from endWord back to beginWord through the parents map, building each
 *     path in reverse. Every path found is shortest by construction.
 *  Going backwards from the end is the key: it only walks edges that are on
 *  some shortest path, so no work is wasted on dead ends.
 *
 *  COMPLEXITY   BFS O(N * 26 * L^2) + output size
 * ============================================================================
 */

import java.util.*;

public class H02_WordLadderII {

    static List<List<String>> findLadders(String beginWord, String endWord, List<String> wordList) {
        Set<String> dict = new HashSet<>(wordList);
        List<List<String>> result = new ArrayList<>();
        if (!dict.contains(endWord)) return result;
        Map<String, List<String>> parents = new HashMap<>();
        Set<String> level = new HashSet<>(List.of(beginWord));
        dict.remove(beginWord);
        boolean found = false;
        while (!level.isEmpty() && !found) {
            Set<String> next = new HashSet<>();
            for (String w : level) {
                char[] cs = w.toCharArray();
                for (int p = 0; p < cs.length; p++) {
                    char orig = cs[p];
                    for (char c = 'a'; c <= 'z'; c++) {
                        if (c == orig) continue;
                        cs[p] = c;
                        String nb = new String(cs);
                        if (dict.contains(nb)) {                   // not used on an EARLIER level
                            next.add(nb);
                            parents.computeIfAbsent(nb, k -> new ArrayList<>()).add(w);
                            if (nb.equals(endWord)) found = true;
                        }
                    }
                    cs[p] = orig;
                }
            }
            dict.removeAll(next);                                  // block this level only now
            level = next;
        }
        if (found) {
            LinkedList<String> path = new LinkedList<>(List.of(endWord));
            backtrack(endWord, beginWord, parents, path, result);
        }
        return result;
    }

    static void backtrack(String word, String begin, Map<String, List<String>> parents, LinkedList<String> path, List<List<String>> result) {
        if (word.equals(begin)) { result.add(new ArrayList<>(path)); return; }
        for (String p : parents.getOrDefault(word, List.of())) {
            path.addFirst(p);
            backtrack(p, begin, parents, path, result);
            path.removeFirst();
        }
    }

    // referee: distances from both ends; a word w lies on a shortest path iff dFrom[w] + dTo[w] == best
    static Set<List<String>> referee(String b, String e, List<String> wl) {
        Set<String> all = new HashSet<>(wl);
        all.add(b);
        Map<String, Integer> dFrom = bfs(b, all), dTo = bfs(e, all);
        Set<List<String>> out = new HashSet<>();
        if (!new HashSet<>(wl).contains(e) || !dFrom.containsKey(e)) return out;
        int best = dFrom.get(e);
        enumerate(b, e, best, dFrom, dTo, all, new ArrayList<>(List.of(b)), out);
        return out;
    }

    static void enumerate(String w, String e, int best, Map<String, Integer> dFrom, Map<String, Integer> dTo, Set<String> all, List<String> path, Set<List<String>> out) {
        if (w.equals(e)) { out.add(new ArrayList<>(path)); return; }
        for (String nb : all) {
            if (oneApart(w, nb) && dFrom.getOrDefault(nb, -1) == dFrom.get(w) + 1 && dTo.containsKey(nb) && dFrom.get(nb) + dTo.get(nb) == best) {
                path.add(nb);
                enumerate(nb, e, best, dFrom, dTo, all, path, out);
                path.remove(path.size() - 1);
            }
        }
    }

    static Map<String, Integer> bfs(String s, Set<String> all) {
        Map<String, Integer> d = new HashMap<>(Map.of(s, 0));
        Queue<String> q = new ArrayDeque<>(List.of(s));
        while (!q.isEmpty()) {
            String w = q.poll();
            for (String nb : all) if (!d.containsKey(nb) && oneApart(w, nb)) { d.put(nb, d.get(w) + 1); q.add(nb); }
        }
        return d;
    }

    static boolean oneApart(String a, String b) {
        int diff = 0;
        for (int i = 0; i < a.length(); i++) if (a.charAt(i) != b.charAt(i)) diff++;
        return diff == 1;
    }

    public static void main(String[] args) {
        List<List<String>> r = findLadders("hit", "cog", List.of("hot", "dot", "dog", "lot", "log", "cog"));
        r.sort(Comparator.comparing(Object::toString));
        check("example", "[[hit, hot, dot, dog, cog], [hit, hot, lot, log, cog]]", r);
        check("unreachable", "[]", findLadders("hit", "cog", List.of("hot", "dot", "dog", "lot", "log")));
        Random rnd = new Random(2);
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            Set<String> ws = new LinkedHashSet<>();
            for (int i = 0; i < 2 + rnd.nextInt(20); i++) ws.add("" + (char) ('a' + rnd.nextInt(3)) + (char) ('a' + rnd.nextInt(3)) + (char) ('a' + rnd.nextInt(2)));
            List<String> wl = new ArrayList<>(ws);
            String b = "" + (char) ('a' + rnd.nextInt(3)) + (char) ('a' + rnd.nextInt(3)) + (char) ('a' + rnd.nextInt(2));
            String e = wl.get(rnd.nextInt(wl.size()));
            if (b.equals(e)) continue;
            ok &= new HashSet<>(findLadders(b, e, wl)).equals(referee(b, e, wl));
        }
        check("300 random dictionaries vs distance referee", true, ok);
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
