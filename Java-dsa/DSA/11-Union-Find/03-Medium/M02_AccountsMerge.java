/*
 * ============================================================================
 *  MEDIUM 02 : ACCOUNTS MERGE                LeetCode #721
 *  https://leetcode.com/problems/accounts-merge/
 *  Pattern: Union-Find over EMAILS (strings); union all emails of one account
 * ============================================================================
 *
 *  PROBLEM
 *    accounts[i] = [name, email1, email2, ...]. Two accounts belong to the same
 *    person if they share ANY email (transitively). Merge them; output each person
 *    as [name, sorted emails...].
 *      [["John","a@x","b@x"], ["John","b@x","c@x"], ["Mary","m@x"], ["John","j@x"]]
 *      -> [["John","a@x","b@x","c@x"], ["Mary","m@x"], ["John","j@x"]]
 *      (two different Johns: the same name does NOT mean the same person)
 *
 *  ---------------------------------------------------------------------------
 *  WHAT ARE THE ELEMENTS? Emails. Accounts only tell us "these emails are one person":
 *     for each account: union(first email, every other email of that account)
 *  Account 1 links a-b, account 2 links b-c -> a, b, c end up in one set (via b).
 *
 *  STEPS
 *    1. map email -> int id (and remember the owner's name)
 *    2. union the emails inside each account
 *    3. group emails by find(id); sort each group; prepend the name
 *
 *  GRAPH VIEW (also fine): emails are vertices, each account adds edges; DFS
 *  each component. Union-Find is shorter and has no recursion depth issues.
 *
 *  COMPLEXITY   Time O(E log E) (sorting dominates; unions are ~O(1))   Space O(E)
 * ============================================================================
 */

import java.util.*;

public class M02_AccountsMerge {

    static int[] parent;

    static int find(int x) {
        while (parent[x] != x) { parent[x] = parent[parent[x]]; x = parent[x]; }
        return x;
    }

    static List<List<String>> accountsMerge(List<List<String>> accounts) {
        Map<String, Integer> id = new HashMap<>();
        Map<String, String> owner = new HashMap<>();
        for (List<String> acc : accounts)
            for (int i = 1; i < acc.size(); i++)
                if (id.putIfAbsent(acc.get(i), id.size()) == null) owner.put(acc.get(i), acc.get(0));
        parent = new int[id.size()];
        for (int i = 0; i < parent.length; i++) parent[i] = i;
        for (List<String> acc : accounts) {
            int first = id.get(acc.get(1));
            for (int i = 2; i < acc.size(); i++) parent[find(id.get(acc.get(i)))] = find(first);   // same person
        }
        Map<Integer, TreeSet<String>> groups = new HashMap<>();
        for (String email : id.keySet()) groups.computeIfAbsent(find(id.get(email)), k -> new TreeSet<>()).add(email);
        List<List<String>> result = new ArrayList<>();
        for (TreeSet<String> g : groups.values()) {
            List<String> row = new ArrayList<>();
            row.add(owner.get(g.first()));
            row.addAll(g);
            result.add(row);
        }
        return result;
    }

    static List<String> norm(List<List<String>> r) {
        List<String> out = new ArrayList<>();
        for (List<String> row : r) out.add(row.toString());
        Collections.sort(out);
        return out;
    }

    // referee: repeatedly merge any two groups sharing an email until nothing changes
    static List<List<String>> brute(List<List<String>> accounts) {
        List<String> names = new ArrayList<>();
        List<Set<String>> sets = new ArrayList<>();
        for (List<String> a : accounts) { names.add(a.get(0)); sets.add(new TreeSet<>(a.subList(1, a.size()))); }
        boolean merged = true;
        while (merged) {
            merged = false;
            outer:
            for (int i = 0; i < sets.size(); i++) for (int j = i + 1; j < sets.size(); j++) {
                if (!Collections.disjoint(sets.get(i), sets.get(j))) {
                    sets.get(i).addAll(sets.remove(j));
                    names.remove(j);
                    merged = true;
                    break outer;
                }
            }
        }
        List<List<String>> out = new ArrayList<>();
        for (int i = 0; i < sets.size(); i++) { List<String> row = new ArrayList<>(List.of(names.get(i))); row.addAll(sets.get(i)); out.add(row); }
        return out;
    }

    public static void main(String[] args) {
        List<List<String>> acc = List.of(
                List.of("John", "a@x", "b@x"), List.of("John", "b@x", "c@x"),
                List.of("Mary", "m@x"), List.of("John", "j@x"));
        check("example", "[[John, a@x, b@x, c@x], [John, j@x], [Mary, m@x]]", norm(accountsMerge(acc)));
        Random rnd = new Random(3);
        boolean ok = true;
        for (int t = 0; t < 500; t++) {
            List<List<String>> as = new ArrayList<>();
            for (int i = 0; i < 1 + rnd.nextInt(6); i++) {
                List<String> row = new ArrayList<>(List.of("P" + rnd.nextInt(2)));
                for (int j = 0; j < 1 + rnd.nextInt(3); j++) row.add("e" + rnd.nextInt(10));
                as.add(row);
            }
            // keep the input valid: an email belongs to exactly one name
            Map<String, String> ownerOf = new HashMap<>();
            for (List<String> row : as) for (int j = 1; j < row.size(); j++) ownerOf.putIfAbsent(row.get(j), row.get(0));
            for (List<String> row : as) for (int j = 1; j < row.size(); j++) if (!ownerOf.get(row.get(j)).equals(row.get(0))) row.set(j, row.get(j) + row.get(0));
            ok &= norm(accountsMerge(as)).equals(norm(brute(as)));
        }
        check("500 random account lists vs repeated merging", true, ok);
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
