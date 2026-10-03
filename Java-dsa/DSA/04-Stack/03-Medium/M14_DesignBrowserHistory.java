/*
 * ============================================================================
 *  MEDIUM 14 : DESIGN BROWSER HISTORY        LeetCode #1472
 *  https://leetcode.com/problems/design-browser-history/
 *  Pattern: TWO STACKS (back / forward)  or  ARRAY + CURSOR
 * ============================================================================
 *
 *  PROBLEM
 *    BrowserHistory(homepage), visit(url), back(steps), forward(steps).
 *    visit() CLEARS all forward history (like a real browser).
 *    back/forward move at most as far as possible and return the current URL.
 *
 *  ---------------------------------------------------------------------------
 *  APPROACH 1: TWO STACKS (the classic undo/redo design)
 *    back stack    = pages behind us (top = the most recent)
 *    forward stack = pages ahead of us (filled when we go back)
 *    current       = the page we're on
 *    visit(url): push current to back; current = url; CLEAR forward
 *    back(k):    up to k times: push current to forward, current = back.pop()
 *    forward(k): the mirror image
 *    (Text editors implement undo/redo exactly like this.)
 *
 *  APPROACH 2: ARRAY + CURSOR (O(1) per call, any number of steps)
 *    history list, cur index, last = the last valid index
 *    visit: cur++, overwrite history[cur] (or append), last = cur  -> forward pages are "forgotten"
 *    back(k):    cur = max(0, cur - k)
 *    forward(k): cur = min(last, cur + k)
 *  Jumping k steps is O(1) here vs O(k) with stacks.
 *
 *  COMPLEXITY  stacks: visit O(1) amortized, back/forward O(steps)   array: all O(1)
 * ============================================================================
 */

import java.util.*;

public class M14_DesignBrowserHistory {

    static class BrowserHistory {                          // two stacks
        private final Deque<String> back = new ArrayDeque<>(), forward = new ArrayDeque<>();
        private String current;

        BrowserHistory(String homepage) { current = homepage; }

        void visit(String url) {
            back.push(current);
            current = url;
            forward.clear();                                // new page: the forward history is gone
        }

        String back(int steps) {
            while (steps-- > 0 && !back.isEmpty()) {
                forward.push(current);
                current = back.pop();
            }
            return current;
        }

        String forward(int steps) {
            while (steps-- > 0 && !forward.isEmpty()) {
                back.push(current);
                current = forward.pop();
            }
            return current;
        }
    }

    static class BrowserHistoryArray {                      // array + cursor
        private final List<String> history = new ArrayList<>();
        private int cur = 0, last = 0;

        BrowserHistoryArray(String homepage) { history.add(homepage); }

        void visit(String url) {
            cur++;
            if (cur < history.size()) history.set(cur, url); else history.add(url);
            last = cur;                                     // everything after cur is now invalid
        }

        String back(int steps) { cur = Math.max(0, cur - steps); return history.get(cur); }
        String forward(int steps) { cur = Math.min(last, cur + steps); return history.get(cur); }
    }

    public static void main(String[] args) {
        // the LeetCode example
        for (int version = 0; version < 2; version++) {
            String label = version == 0 ? "stacks" : "array";
            BrowserHistory a = version == 0 ? new BrowserHistory("leetcode.com") : null;
            BrowserHistoryArray b = version == 1 ? new BrowserHistoryArray("leetcode.com") : null;
            List<String> out = new ArrayList<>();
            for (String url : List.of("google.com", "facebook.com", "youtube.com")) { if (a != null) a.visit(url); else b.visit(url); }
            out.add(a != null ? a.back(1) : b.back(1));
            out.add(a != null ? a.back(1) : b.back(1));
            out.add(a != null ? a.forward(1) : b.forward(1));
            if (a != null) a.visit("linkedin.com"); else b.visit("linkedin.com");
            out.add(a != null ? a.forward(2) : b.forward(2));
            out.add(a != null ? a.back(2) : b.back(2));
            out.add(a != null ? a.back(7) : b.back(7));
            check(label + ": LeetCode sequence", "[facebook.com, google.com, facebook.com, linkedin.com, google.com, leetcode.com]", out);
        }
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
