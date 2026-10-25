/*
 * ============================================================================
 *  ADVANCE 15 : DESIGN IN-MEMORY FILE SYSTEM  ⭐ bonus   LeetCode #588
 *  https://leetcode.com/problems/design-in-memory-file-system/
 *  Pattern: a TRIE of path parts, each node's children in a TreeMap (sorted for ls)
 * ============================================================================
 *
 *  PROBLEM
 *    ls(path)                      a file path -> [its name]; a directory -> its children's
 *                                  names in lexicographic order
 *    mkdir(path)                   create the directory, and any missing parents (mkdir -p)
 *    addContentToFile(path, text)  create the file if needed, then APPEND text
 *    readContentFromFile(path)     the file's content
 *      ls("/") -> []    mkdir("/a/b/c")    addContentToFile("/a/b/c/d", "hello")
 *      ls("/") -> ["a"]                    readContentFromFile("/a/b/c/d") -> "hello"
 *
 *  ---------------------------------------------------------------------------
 *  A PATH IS A WORD, ITS PARTS ARE LETTERS: "/a/b/c" = a -> b -> c. Exactly a trie:
 *    Node { TreeMap<String, Node> children;  StringBuilder content (non-null = a file) }
 *  - TreeMap (not HashMap) so ls() gets the names ALREADY SORTED: no sort per call.
 *  - StringBuilder so appending is cheap (String += copies the whole file every time).
 *  Every operation walks the path once: O(path length) map lookups.
 *
 *  ANOTHER WAY (Approach 1): a flat Map<fullPath, content> plus a Set of directories.
 *  mkdir/add are easy, but ls() must SCAN EVERY PATH to find children and then sort:
 *  O(total paths) per ls. The trie answers ls by looking at one node.
 *
 *  DRY RUN
 *    mkdir("/a/b/c"):   root -> a -> b -> c   (created on the way)
 *    add("/a/b/c/d", "hello"): walk to c, create child d with content "hello"
 *    ls("/"):  root's children {a} -> ["a"]
 *    ls("/a/b/c/d"): d is a file -> ["d"]
 *
 *  COMPLEXITY   O(L log k) per call (L parts, k children per directory) + output size
 * ============================================================================
 */

import java.util.*;

public class A15_DesignInMemoryFileSystem {

    // ---------------- Approach 1: flat map of full paths (the referee) ----------------
    static class FlatFileSystem {
        private final Set<String> dirs = new HashSet<>(Set.of("/"));
        private final Map<String, StringBuilder> files = new HashMap<>();

        public List<String> ls(String path) {
            if (files.containsKey(path)) return List.of(path.substring(path.lastIndexOf('/') + 1));
            String prefix = path.equals("/") ? "/" : path + "/";
            TreeSet<String> names = new TreeSet<>();
            for (String p : dirs) addChild(p, prefix, names);
            for (String p : files.keySet()) addChild(p, prefix, names);
            return new ArrayList<>(names);
        }

        private static void addChild(String p, String prefix, Set<String> names) {
            if (p.length() > prefix.length() && p.startsWith(prefix) && p.indexOf('/', prefix.length()) == -1)
                names.add(p.substring(prefix.length()));                // a direct child, not a grandchild
        }

        public void mkdir(String path) {
            for (int i = 1; i <= path.length(); i++)                     // every prefix "/a", "/a/b", ...
                if (i == path.length() || path.charAt(i) == '/') dirs.add(path.substring(0, i));
        }

        public void addContentToFile(String filePath, String content) {
            mkdir(filePath.substring(0, Math.max(1, filePath.lastIndexOf('/'))));
            files.computeIfAbsent(filePath, k -> new StringBuilder()).append(content);
        }

        public String readContentFromFile(String filePath) { return files.get(filePath).toString(); }
    }

    // ---------------- Approach 2: trie of path parts (main solution) ----------------
    static class FileSystem {
        private static class Node {
            final TreeMap<String, Node> children = new TreeMap<>();
            StringBuilder content;                                       // null = directory, non-null = file
        }

        private final Node root = new Node();

        /** Walk to the node for 'path', creating missing nodes when create is true. */
        private Node walk(String path, boolean create) {
            Node cur = root;
            for (String part : path.split("/")) {
                if (part.isEmpty()) continue;                            // "/a/b".split("/") = ["", "a", "b"]
                Node next = cur.children.get(part);
                if (next == null) {
                    if (!create) return null;
                    next = new Node();
                    cur.children.put(part, next);
                }
                cur = next;
            }
            return cur;
        }

        public List<String> ls(String path) {
            Node node = walk(path, false);
            if (node.content != null) return List.of(path.substring(path.lastIndexOf('/') + 1));
            return new ArrayList<>(node.children.keySet());              // already sorted by the TreeMap
        }

        public void mkdir(String path) { walk(path, true); }

        public void addContentToFile(String filePath, String content) {
            Node file = walk(filePath, true);
            if (file.content == null) file.content = new StringBuilder();
            file.content.append(content);
        }

        public String readContentFromFile(String filePath) { return walk(filePath, false).content.toString(); }
    }

    public static void main(String[] args) {
        FileSystem fs = new FileSystem();
        check("ls / on an empty system", "[]", fs.ls("/"));
        fs.mkdir("/a/b/c");
        fs.addContentToFile("/a/b/c/d", "hello");
        check("ls /", "[a]", fs.ls("/"));
        check("read", "hello", fs.readContentFromFile("/a/b/c/d"));
        fs.addContentToFile("/a/b/c/d", " world");
        check("append", "hello world", fs.readContentFromFile("/a/b/c/d"));
        check("ls on a file", "[d]", fs.ls("/a/b/c/d"));
        fs.mkdir("/a/b/aa"); fs.addContentToFile("/a/b/zz", "x");
        check("ls is sorted, dirs and files mixed", "[aa, c, zz]", fs.ls("/a/b"));

        Random rnd = new Random(15);
        String[] dirNames = {"a", "b", "c"}, fileNames = {"f1", "f2"};
        boolean ok = true;
        for (int t = 0; t < 300; t++) {
            FileSystem main = new FileSystem();
            FlatFileSystem ref = new FlatFileSystem();
            List<String> knownDirs = new ArrayList<>(List.of("/")), knownFiles = new ArrayList<>();
            for (int i = 0; i < 60; i++) {
                StringBuilder dir = new StringBuilder();
                for (int d = rnd.nextInt(3); d >= 0; d--) dir.append('/').append(dirNames[rnd.nextInt(3)]);
                int op = rnd.nextInt(4);
                if (op == 0) { main.mkdir(dir.toString()); ref.mkdir(dir.toString()); knownDirs.add(dir.toString()); }
                else if (op == 1) {
                    String f = dir + "/" + fileNames[rnd.nextInt(2)], text = "" + (char) ('p' + rnd.nextInt(5));
                    main.addContentToFile(f, text); ref.addContentToFile(f, text); knownFiles.add(f);
                }
                else if (op == 2) { String d = knownDirs.get(rnd.nextInt(knownDirs.size())); ok &= main.ls(d).equals(ref.ls(d)); }
                else if (!knownFiles.isEmpty()) {
                    String f = knownFiles.get(rnd.nextInt(knownFiles.size()));
                    ok &= main.readContentFromFile(f).equals(ref.readContentFromFile(f)) && main.ls(f).equals(ref.ls(f));
                }
            }
        }
        check("18,000 random ops: trie == flat map", true, ok);
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
