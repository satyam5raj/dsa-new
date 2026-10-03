/*
 * ============================================================================
 *  HARD 03 : LONGEST DUPLICATE SUBSTRING  ⭐ bonus   LeetCode #1044
 *  https://leetcode.com/problems/longest-duplicate-substring/
 *  Pattern: BINARY SEARCH ON THE ANSWER + ROLLING HASH (Rabin-Karp) in a HashSet
 * ============================================================================
 *
 *  PROBLEM
 *    Return any longest substring that occurs at least TWICE (occurrences may
 *    overlap), or "" if none.
 *      "banana" -> "ana"      "abcd" -> ""
 *
 *  ---------------------------------------------------------------------------
 *  BRUTE FORCE: for each length L, put all substrings of length L in a set -> O(n^2)
 *  substrings of up to n characters each = O(n^3). n = 3 * 10^4 -> impossible.
 *
 *  TWO IDEAS COMBINED
 *  1. BINARY SEARCH ON THE LENGTH. If a duplicate of length L exists, one of
 *     length L-1 exists too (just drop its last character). So "is there a
 *     duplicate of length L?" is MONOTONIC: true, true, ..., true, false, false.
 *     Binary search finds the largest L with O(log n) checks.
 *  2. CHECK A LENGTH L IN O(n) WITH A ROLLING HASH (Strings/01-Basics, Rabin-Karp):
 *     hash every window of length L, sliding in O(1) per step, and store the
 *     hashes in a HashSet. A hash seen twice -> a candidate duplicate.
 *     Collisions are possible: VERIFY the candidate by comparing the actual
 *     characters (store hash -> list of start positions).
 *
 *  DRY RUN "banana"
 *    lo=1, hi=5: L=3 -> windows ban, ana, nan, ana -> "ana" repeats -> yes, lo=4
 *    L=4 -> bana, anan, nana -> all different -> no, hi=3 -> answer length 3 -> "ana"
 *
 *  COMPLEXITY   Time O(n log n) average   Space O(n)
 *  (A suffix array or suffix automaton also solves it, but those are much harder to code.)
 * ============================================================================
 */

import java.util.*;

public class H03_LongestDuplicateSubstring {

    static final long MOD = 1_000_000_007L, BASE = 131;

    static String longestDupSubstring(String s) {
        int lo = 1, hi = s.length() - 1, bestStart = -1, bestLen = 0;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            int start = findDuplicate(s, mid);
            if (start >= 0) {                 // a duplicate of length mid exists -> try longer
                bestStart = start;
                bestLen = mid;
                lo = mid + 1;
            } else {
                hi = mid - 1;                 // none -> try shorter
            }
        }
        return bestStart < 0 ? "" : s.substring(bestStart, bestStart + bestLen);
    }

    // start index of a duplicated substring of length L, or -1
    static int findDuplicate(String s, int L) {
        long hash = 0, power = 1;
        for (int i = 0; i < L; i++) {
            hash = (hash * BASE + s.charAt(i)) % MOD;
            if (i > 0) power = power * BASE % MOD;          // BASE^(L-1)
        }
        Map<Long, List<Integer>> seen = new HashMap<>();
        seen.computeIfAbsent(hash, k -> new ArrayList<>()).add(0);
        for (int start = 1; start + L <= s.length(); start++) {
            hash = (hash - s.charAt(start - 1) * power % MOD + MOD) % MOD;   // drop the left char
            hash = (hash * BASE + s.charAt(start + L - 1)) % MOD;             // add the right char
            List<Integer> candidates = seen.computeIfAbsent(hash, k -> new ArrayList<>());
            for (int other : candidates) {
                if (s.regionMatches(other, s, start, L)) return start;       // verify (rule out a collision)
            }
            candidates.add(start);
        }
        return -1;
    }

    static int bruteLength(String s) {
        int best = 0;
        for (int L = 1; L < s.length(); L++) {
            Set<String> set = new HashSet<>();
            for (int i = 0; i + L <= s.length(); i++) if (!set.add(s.substring(i, i + L))) { best = L; break; }
        }
        return best;
    }

    public static void main(String[] args) {
        check("banana", "ana", longestDupSubstring("banana"));
        check("abcd (none)", "", longestDupSubstring("abcd"));
        check("aaaaa (overlapping)", "aaaa", longestDupSubstring("aaaaa"));
        Random rnd = new Random(10);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0, n = 1 + rnd.nextInt(25); i < n; i++) sb.append((char) ('a' + rnd.nextInt(3)));
            String s = sb.toString(), r = longestDupSubstring(s);
            ok &= r.length() == bruteLength(s) && (r.isEmpty() || s.indexOf(r) != s.lastIndexOf(r));
        }
        check("500 random strings vs brute force", true, ok);
        StringBuilder big = new StringBuilder();
        Random r2 = new Random(11);
        for (int i = 0; i < 30_000; i++) big.append((char) ('a' + r2.nextInt(26)));
        long t0 = System.nanoTime();
        String ans = longestDupSubstring(big.toString());
        System.out.printf("   n = 30,000 random letters -> longest duplicate length %d in %d ms%n", ans.length(), (System.nanoTime() - t0) / 1_000_000);
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
