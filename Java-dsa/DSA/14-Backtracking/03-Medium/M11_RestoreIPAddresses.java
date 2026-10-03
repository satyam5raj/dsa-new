/*
 * ============================================================================
 *  MEDIUM 11 : RESTORE IP ADDRESSES          LeetCode #93
 *  https://leetcode.com/problems/restore-ip-addresses/
 *  Pattern: PARTITION into exactly 4 pieces with validity rules + length pruning
 * ============================================================================
 *
 *  PROBLEM
 *    Insert 3 dots into a digit string to make every valid IPv4 address. Each part is
 *    0..255 with NO leading zeros ("0" is fine, "01" is not).
 *      "25525511135" -> ["255.255.11.135","255.255.111.35"]
 *      "101023"      -> ["1.0.10.23","1.0.102.3","10.1.0.23","10.10.2.3","101.0.2.3"]
 *
 *  ---------------------------------------------------------------------------
 *  Palindrome Partitioning's template (M10) with:
 *    - exactly 4 parts; each part 1..3 characters
 *    - valid(part): no leading zero unless it IS "0", and value <= 255
 *    - PRUNING: with `left` parts still to place and `rem` characters remaining,
 *      need left <= rem <= 3 * left; otherwise stop early.
 *  The search space is tiny (at most 3^4 = 81 splits), but the rules are the point.
 *
 *  DRY RUN "0000": only "0.0.0.0" ("00" is invalid as a part)
 *
 *  COMPLEXITY   O(3^4) splits * O(n) to build each -> effectively O(1) for length <= 12
 * ============================================================================
 */

import java.util.*;

public class M11_RestoreIPAddresses {

    static List<String> restoreIpAddresses(String s) {
        List<String> out = new ArrayList<>();
        bt(s, 0, new ArrayList<>(), out);
        return out;
    }

    static void bt(String s, int start, List<String> parts, List<String> out) {
        int left = 4 - parts.size(), rem = s.length() - start;
        if (rem < left || rem > 3 * left) return;               // can't finish with valid lengths
        if (left == 0) { out.add(String.join(".", parts)); return; }
        for (int len = 1; len <= 3 && start + len <= s.length(); len++) {
            String part = s.substring(start, start + len);
            if (!valid(part)) continue;
            parts.add(part);
            bt(s, start + len, parts, out);
            parts.remove(parts.size() - 1);
        }
    }

    static boolean valid(String p) {
        if (p.length() > 1 && p.charAt(0) == '0') return false; // leading zero
        return Integer.parseInt(p) <= 255;
    }

    // referee: three nested loops over the dot positions
    static List<String> brute(String s) {
        List<String> out = new ArrayList<>();
        int n = s.length();
        for (int a = 1; a < n; a++) for (int b = a + 1; b < n; b++) for (int c = b + 1; c < n; c++) {
            String[] p = {s.substring(0, a), s.substring(a, b), s.substring(b, c), s.substring(c)};
            boolean ok = true;
            for (String x : p) ok &= x.length() <= 3 && valid(x);
            if (ok) out.add(String.join(".", p));
        }
        return out;
    }

    public static void main(String[] args) {
        check("25525511135", "[255.255.11.135, 255.255.111.35]", restoreIpAddresses("25525511135"));
        check("0000", "[0.0.0.0]", restoreIpAddresses("0000"));
        check("101023", "[1.0.10.23, 1.0.102.3, 10.1.0.23, 10.10.2.3, 101.0.2.3]", restoreIpAddresses("101023"));
        Random rnd = new Random(1);
        boolean ok = true;
        for (int t = 0; t < 2000; t++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < 1 + rnd.nextInt(14); i++) sb.append(rnd.nextInt(4) == 0 ? '0' : (char) ('0' + rnd.nextInt(10)));
            ok &= new TreeSet<>(restoreIpAddresses(sb.toString())).equals(new TreeSet<>(brute(sb.toString())));
        }
        check("2,000 random digit strings vs triple loop", true, ok);
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
