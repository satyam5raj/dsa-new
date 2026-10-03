/*
 * ============================================================================
 *  MEDIUM 08 : SIMPLIFY PATH                 LeetCode #71
 *  https://leetcode.com/problems/simplify-path/
 *  Pattern: STACK OF DIRECTORY NAMES ('..' = pop)
 * ============================================================================
 *
 *  PROBLEM
 *    Convert an absolute Unix path to its CANONICAL form.
 *      "."   = current directory  -> ignore
 *      ".."  = parent directory   -> go up one level (at the root, stay at the root)
 *      "//"  = same as "/"
 *      any other name (including "...") is a normal directory name
 *    Canonical: starts with "/", single slashes, no trailing slash, no "." or "..".
 *      "/home/"                -> "/home"
 *      "/../"                  -> "/"
 *      "/home//foo/"           -> "/home/foo"
 *      "/a/./b/../../c/"       -> "/c"
 *      "/.../a/../b/c/../d/./" -> "/.../b/d"
 *
 *  ---------------------------------------------------------------------------
 *  WHY A STACK: the path is a stack of directories. Entering a directory =
 *  push; ".." = pop (go back to the most recently entered directory).
 *
 *  ALGORITHM
 *    split by "/" -> tokens (empty tokens come from "//" or the leading "/")
 *    for each token:
 *      ""  or "."  -> skip
 *      ".."        -> pop if not empty
 *      otherwise   -> push
 *    answer = "/" + the stack joined by "/" (bottom to top)
 *
 *  DRY RUN "/a/./b/../../c/"
 *    tokens: "", a, ., b, .., .., c, ""
 *    a -> [a];  . skip;  b -> [a,b];  .. -> [a];  .. -> [];  c -> [c]
 *    -> "/c"
 *
 *  COMPLEXITY   Time O(n)   Space O(n)
 * ============================================================================
 */

import java.util.*;

public class M08_SimplifyPath {

    static String simplifyPath(String path) {
        Deque<String> stack = new ArrayDeque<>();
        for (String part : path.split("/")) {
            if (part.isEmpty() || part.equals(".")) continue;
            if (part.equals("..")) {
                if (!stack.isEmpty()) stack.pop();          // already at the root: nothing to pop
            } else {
                stack.push(part);
            }
        }
        StringBuilder sb = new StringBuilder();
        Iterator<String> it = stack.descendingIterator();    // bottom (root side) first
        while (it.hasNext()) sb.append('/').append(it.next());
        return sb.length() == 0 ? "/" : sb.toString();
    }

    public static void main(String[] args) {
        check("/home/", "/home", simplifyPath("/home/"));
        check("/../", "/", simplifyPath("/../"));
        check("/home//foo/", "/home/foo", simplifyPath("/home//foo/"));
        check("/a/./b/../../c/", "/c", simplifyPath("/a/./b/../../c/"));
        check("/.../a/../b/c/../d/./", "/.../b/d", simplifyPath("/.../a/../b/c/../d/./"));
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
