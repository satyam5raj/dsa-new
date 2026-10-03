/*
 * ============================================================================
 *  MEDIUM 04 : REORGANIZE STRING             LeetCode #767
 *  https://leetcode.com/problems/reorganize-string/
 *  Pattern: GREEDY "most frequent first" with a max-heap, HOLDING BACK the last letter
 * ============================================================================
 *
 *  PROBLEM
 *    Rearrange s so that no two ADJACENT characters are equal; return "" if impossible.
 *      "aab" -> "aba"        "aaab" -> ""
 *
 *  ---------------------------------------------------------------------------
 *  WHEN IS IT POSSIBLE? The most frequent letter needs a different letter between
 *  each pair of its copies:  maxFreq <= (len + 1) / 2.   ("aaab": 3 > 2 -> impossible)
 *
 *  GREEDY WITH A HEAP: at every step, place the MOST FREQUENT letter that is not
 *  the one we just placed. Why most frequent? It's the hardest to fit; using it
 *  early keeps the counts balanced so we don't get stuck with many copies of one
 *  letter at the end.
 *  Implementation trick: after placing a letter, DON'T put it back into the heap
 *  yet - "hold" it for one step, so it can't be chosen twice in a row:
 *      top = poll; append top; top.count--
 *      push the previously held letter back (if it has copies left)
 *      held = top
 *  If the heap runs empty while the held letter still has copies, we're stuck
 *  -> the result is shorter than s -> return "".
 *
 *  DRY RUN "aaabbc" (a3 b2 c1)
 *    a (held a)   -> "a"
 *    b (a back)   -> "ab"       a2 b1 c1
 *    a (b back)   -> "aba"      a1 b1 c1
 *    b ... a ... c              -> "ababac"
 *
 *  O(n) ALTERNATIVE WITHOUT A HEAP: write the most frequent letter at indices
 *  0, 2, 4, ...; then continue with the other letters on the remaining even
 *  indices, wrapping to the odd ones 1, 3, 5, ... Copies of one letter always
 *  land 2 apart (or on different parities), so no two are adjacent.
 *
 *  COMPLEXITY   heap: O(n log 26) = O(n);  even/odd fill: O(n)
 * ============================================================================
 */

import java.util.*;

public class M04_ReorganizeString {

    static String reorganizeString(String s) {
        int[] count = new int[26];
        for (char c : s.toCharArray()) count[c - 'a']++;
        // entries {letter, remaining}; most remaining first, ties by letter
        PriorityQueue<int[]> heap = new PriorityQueue<>((x, y) -> x[1] != y[1] ? Integer.compare(y[1], x[1]) : Integer.compare(x[0], y[0]));
        for (int i = 0; i < 26; i++) if (count[i] > 0) heap.offer(new int[]{i, count[i]});
        StringBuilder sb = new StringBuilder();
        int[] held = null;                                       // the letter just placed
        while (!heap.isEmpty()) {
            int[] top = heap.poll();
            sb.append((char) ('a' + top[0]));
            top[1]--;
            if (held != null && held[1] > 0) heap.offer(held);   // allowed again after one step
            held = top;
        }
        return sb.length() == s.length() ? sb.toString() : "";   // stuck with copies left
    }

    static String reorganizeEvenOdd(String s) {
        int n = s.length();
        int[] count = new int[26];
        for (char c : s.toCharArray()) count[c - 'a']++;
        int maxLetter = 0;
        for (int i = 1; i < 26; i++) if (count[i] > count[maxLetter]) maxLetter = i;
        if (count[maxLetter] > (n + 1) / 2) return "";

        List<Integer> order = new ArrayList<>();
        order.add(maxLetter);                                    // the most frequent goes first
        for (int i = 0; i < 26; i++) if (i != maxLetter) order.add(i);

        char[] out = new char[n];
        int idx = 0;
        for (int letter : order) {
            while (count[letter]-- > 0) {
                if (idx >= n) idx = 1;                           // even slots full -> odd slots
                out[idx] = (char) ('a' + letter);
                idx += 2;
            }
        }
        return new String(out);
    }

    static boolean valid(String original, String r) {
        if (r.length() != original.length()) return false;
        for (int i = 1; i < r.length(); i++) if (r.charAt(i) == r.charAt(i - 1)) return false;
        char[] a = original.toCharArray(), b = r.toCharArray();
        Arrays.sort(a);
        Arrays.sort(b);
        return Arrays.equals(a, b);
    }

    public static void main(String[] args) {
        check("aab", "aba", reorganizeString("aab"));
        check("aaab (impossible)", "", reorganizeString("aaab"));
        check("aaabbc", "ababac", reorganizeString("aaabbc"));
        check("even/odd version valid for aaabbc", true, valid("aaabbc", reorganizeEvenOdd("aaabbc")));
        Random rnd = new Random(4);
        boolean ok = true;
        for (int t = 0; t < 3000; t++) {
            StringBuilder sb = new StringBuilder();
            int letters = 1 + rnd.nextInt(4);
            for (int i = 0, n = 1 + rnd.nextInt(15); i < n; i++) sb.append((char) ('a' + rnd.nextInt(letters)));
            String s = sb.toString();
            int[] cnt = new int[26];
            for (char c : s.toCharArray()) cnt[c - 'a']++;
            boolean possible = Arrays.stream(cnt).max().getAsInt() <= (s.length() + 1) / 2;
            String r1 = reorganizeString(s), r2 = reorganizeEvenOdd(s);
            ok &= possible ? valid(s, r1) && valid(s, r2) : r1.isEmpty() && r2.isEmpty();
        }
        check("3,000 random strings, both versions", true, ok);
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
