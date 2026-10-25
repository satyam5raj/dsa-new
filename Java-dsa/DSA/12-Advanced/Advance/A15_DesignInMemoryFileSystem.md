# A15 · Design In-Memory File System ⭐

| | |
|---|---|
| **LeetCode** | [#588 Design In-Memory File System](https://leetcode.com/problems/design-in-memory-file-system/) |
| **Difficulty** | Hard |
| **Mix of structures** | **Trie** of path parts + **TreeMap** children (sorted for `ls`) + **StringBuilder** file contents |
| **Code** | [A15_DesignInMemoryFileSystem.java](A15_DesignInMemoryFileSystem.java) |
| **Run** | `java A15_DesignInMemoryFileSystem.java` |

⭐ = not in `leetcode.md`, added because it combines a trie with sorted maps in a design problem.

---

## 1. The problem

Design a `FileSystem`:
- `ls(path)`: if `path` is a **file**, return a list with just its name; if it's a **directory**, return the names of its files and subdirectories in **lexicographic order**.
- `mkdir(path)`: create the directory, and any missing parent directories (like `mkdir -p`).
- `addContentToFile(filePath, content)`: create the file if it doesn't exist, then **append** `content`.
- `readContentFromFile(filePath)`: return the file's content.

```
ls("/")                                  → []
mkdir("/a/b/c")
addContentToFile("/a/b/c/d", "hello")
ls("/")                                  → ["a"]
readContentFromFile("/a/b/c/d")          → "hello"
```

Paths are absolute, start with `/`, and a name is never both a file and a directory in the same folder.

---

## 2. How to think about it

### A path is a word; its parts are letters
`/a/b/c/d` is the sequence `a → b → c → d`. Every path shares a prefix with its parent. That's exactly a **trie** ([Trie M01](../../10-Trie/03-Medium/M01_ImplementTrie.md)), with **path parts** instead of characters:

```
root
 └── a
      └── b
           ├── aa          (directory)
           ├── c
           │    └── d      (file: "hello")
           └── zz          (file: "x")
```

Each node:
```
Node:  children = TreeMap<String, Node>      content = StringBuilder (non-null → it's a file)
```

### Two small design choices
| Choice | Why |
|---|---|
| `TreeMap` for children | `ls` must return names **sorted**; a TreeMap keeps them sorted all the time, so `ls` is just "list the keys" with no sort per call |
| `StringBuilder` for content | `append` is cheap; with `String`, `content += more` copies the whole file every time (O(size²) for many appends) |

### One helper does everything
```
walk(path, create):  start at root; for each part, go to that child
                     (creating it if create == true, returning null otherwise)
```
- `mkdir` = `walk(path, true)`
- `addContentToFile` = `walk(path, true)`, then mark it as a file and append
- `ls` / `read` = `walk(path, false)`

### The flat alternative (Approach 1)
Store `Map<fullPath, content>` and a `Set` of directory paths. `mkdir` and `addContentToFile` are easy, but `ls("/a")` must **scan every path** to find the ones directly under `/a`, then sort them. The trie answers `ls` by looking at one node.

### Dry run
| Call | What happens | Result |
|---|---|---|
| `ls("/")` | root has no children | `[]` |
| `mkdir("/a/b/c")` | creates a, then b, then c | |
| `addContentToFile("/a/b/c/d", "hello")` | walks to c, creates d, content "hello" | |
| `ls("/")` | root's children: {a} | `[a]` |
| `readContentFromFile("/a/b/c/d")` | | `hello` |
| `addContentToFile("/a/b/c/d", " world")` | d exists: append | |
| `ls("/a/b/c/d")` | d is a file | `[d]` |
| `mkdir("/a/b/aa")`, `addContentToFile("/a/b/zz", "x")`, `ls("/a/b")` | TreeMap keys | `[aa, c, zz]` |

---

## 3. Java concepts used in this problem

### 3.1 `String.split("/")` and the empty first part
```java
for (String part : path.split("/")) {
    if (part.isEmpty()) continue;                            // "/a/b".split("/") = ["", "a", "b"]
```
A leading `/` produces an empty string before it. `"/".split("/")` gives an **empty array** (trailing empty strings are dropped), so `walk("/")` returns the root. Both cases are handled by skipping empty parts.

### 3.2 A private static nested class
```java
private static class Node {
    final TreeMap<String, Node> children = new TreeMap<>();
    StringBuilder content;                                       // null = directory, non-null = file
}
```
`static` means a `Node` doesn't need a reference to its `FileSystem` (it saves memory, and it's the right default for helper classes). `private` hides it from outside code.

### 3.3 `TreeMap.keySet()` is sorted
`new ArrayList<>(node.children.keySet())` copies the keys in sorted order. Copying matters: returning the live `keySet()` would let callers change the directory by removing items from the list.

### 3.4 `substring(lastIndexOf('/') + 1)`
For `/a/b/c/d`, `lastIndexOf('/')` is the index of the last slash; everything after it, `d`, is the file name.

### 3.5 `null` as "not a file"
`content == null` marks a directory. The first `addContentToFile` sets `content = new StringBuilder()`. This avoids a separate `isFile` flag that could get out of sync.

---

## 4. Approach 1: A flat map of full paths (the referee)

### Code ([A15_DesignInMemoryFileSystem.java:43-72](A15_DesignInMemoryFileSystem.java#L43-L72))
```java
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
```

| Part | What it does |
|---|---|
| `dirs`, `files` | every directory path and every file path, in full |
| `ls`: `prefix` | children of `/a` start with `/a/`; children of `/` start with `/` |
| `addChild` | a direct child starts with the prefix and has **no further `/`** after it |
| `TreeSet` | sorts the names and removes duplicates |
| `mkdir` | adds every prefix ending at a `/` or the end, so parents exist too |
| `addContentToFile` | makes sure the parent directory exists, then appends |

**Cost:** `ls` is **O(total paths × path length)** per call, plus sorting.

---

## 5. Approach 2: A trie of path parts (main solution)

### Code ([A15_DesignInMemoryFileSystem.java:75-114](A15_DesignInMemoryFileSystem.java#L75-L114))
```java
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
```

### Line by line
| Line | What it does |
|---|---|
| `walk`: the loop | one map lookup per path part |
| `if (!create) return null` | reading a path that doesn't exist |
| `children.put(part, next)` | create a missing directory (or the file node) on the way |
| `ls`: `content != null` | a file: just its name |
| `ls`: `keySet()` | a directory: children's names, already sorted |
| `mkdir` | creating the nodes is all mkdir does |
| `addContentToFile` | create the node if needed, turn it into a file, append |

### Complexity
- Every call walks L path parts with a TreeMap lookup each: **O(L · log k)** for k entries per directory, plus the output size for `ls` and `read`.
- Space: one node per distinct path part, plus the contents.

---

## 6. Comparing the approaches

| | mkdir / add | ls | read |
|---|---|---|---|
| Flat map | O(L) | O(P · L + output sort) for P paths | O(L) |
| **Trie + TreeMap** | O(L log k) | O(L log k + output) | O(L log k + size) |

The file runs 18,000 random operations and checks the trie against the flat map, including `ls` on directories and files.

---

## 7. Edge cases and common mistakes

**Edge cases:** `ls("/")` on an empty system, `ls` on a file, appending to an existing file, a directory with both files and subdirectories (sorted together), `mkdir` on an existing path (a no-op).

**Mistakes:**
1. Not skipping the empty first part from `split("/")`, which creates a child named `""`.
2. Sorting children on every `ls` (use a TreeMap) or returning them unsorted (HashMap).
3. `String content` with `+=` for appends.
4. Returning the live `keySet()` instead of a copy.
5. Forgetting that `ls` on a **file** returns `[fileName]`, not its parent's listing.

---

## 8. Interview questions and answers

**Q1. Why a trie?**
> Paths share prefixes, and every operation walks a path from the root. A trie of path parts makes each operation O(path length), and `ls` only has to look at one node's children.

**Q2. Why a TreeMap for children?**
> `ls` must return sorted names. A TreeMap keeps children sorted at O(log k) per insert, so `ls` never sorts. A HashMap would need a sort on every `ls`.

**Q3. How do you tell files from directories?**
> A node with non-null `content` is a file. The problem guarantees no name is both, so one field is enough.

### Follow-up questions

**F1. "Add `rm` (remove a file or directory)."**
> Walk to the parent and remove the child from its map; the whole subtree goes with it.

**F2. "Add `mv(src, dst)`."**
> Detach the node from `src`'s parent and attach it under `dst`'s parent with the new name: O(L log k), and no copying of the subtree.

**F3. "Total size of a directory (like `du`)."**
> Keep a `size` per node, updated along the path on every append (+length), or compute it on demand with a DFS of the subtree.

**F4. "Thread safety."**
> A read-write lock per node, or one global `ReentrantReadWriteLock`: many readers, one writer.

---

## 9. Running the file

```bash
java A15_DesignInMemoryFileSystem.java     # 7/7 tests passed (LeetCode example, append, ls file, sorted ls, 18,000 random ops)
```

**One-line summary:** *"Model paths as a trie of path parts whose children are TreeMaps (so ls is already sorted) and whose file nodes hold a StringBuilder; one walk(path, create) helper implements mkdir, addContentToFile, ls and read."*
