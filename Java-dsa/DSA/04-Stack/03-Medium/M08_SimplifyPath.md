# M08 · Simplify Path

| | |
|---|---|
| **LeetCode** | [#71 Simplify Path](https://leetcode.com/problems/simplify-path/) |
| **Difficulty** | Medium |
| **Pattern** | A stack of directory names (`..` = pop) |
| **Code** | [M08_SimplifyPath.java](M08_SimplifyPath.java) |
| **Run** | `java M08_SimplifyPath.java` |

---

## 1. The problem

Convert an absolute Unix path to its **canonical** form.
- `.` = the current directory → ignore it
- `..` = the parent directory → go up one level (at the root, stay at the root)
- `//` = the same as `/`
- any other name (**including `...`**) is a normal directory name
- canonical: it starts with `/`, uses single slashes, has no trailing slash, and contains no `.` or `..`

| path | canonical |
|---|---|
| `"/home/"` | `"/home"` |
| `"/../"` | `"/"` |
| `"/home//foo/"` | `"/home/foo"` |
| `"/a/./b/../../c/"` | `"/c"` |
| `"/.../a/../b/c/../d/./"` | `"/.../b/d"` |

---

## 2. How to think about it

The path **is** a stack of directories: entering a directory = **push**, `..` = **pop** (go back to the directory you were in before).

```
split the path by "/" → tokens (empty tokens come from "//" and the leading "/")
for each token:
    "" or "."  → skip
    ".."       → pop, if the stack isn't empty
    otherwise  → push
answer = "/" + the stack joined with "/" (from the bottom to the top)
```

---

## 3. Java concepts used in this problem

### 3.1 `split("/")` and empty tokens

```java
"/a//b/".split("/")    // ["", "a", "", "b"]   (the trailing empty strings are dropped automatically)
```

A leading `/` produces a leading empty string, and `//` produces an empty string in the middle. We simply skip the empty tokens.

### 3.2 `String.equals` for the tokens

`part.equals("..")`, never `part == ".."`: the tokens are new String objects created by `split`.

### 3.3 Iterating a Deque from the bottom: `descendingIterator()`

With `ArrayDeque` used as a stack, the **top** is at the front. To print the path from the root, iterate from the **bottom**: `descendingIterator()`.

```java
Iterator<String> it = stack.descendingIterator();
while (it.hasNext()) sb.append('/').append(it.next());
```

An `Iterator` has `hasNext()` and `next()`. The for-each loop uses one behind the scenes.

### 3.4 `String.join` (alternative)

If you use the Deque as a **queue-like list** (`addLast` / `pollLast`), you can write `"/" + String.join("/", deque)`, since iterating a Deque goes from front to back.

---

## 4. Approach 1: Stack of names (the file's solution)

### Code ([M08_SimplifyPath.java:46-60](M08_SimplifyPath.java#L46-L60))

```java
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
```

### Line by line

| Line | What it does |
|---|---|
| `for (String part : path.split("/"))` | each path component |
| `if (part.isEmpty() \|\| part.equals(".")) continue;` | empty components (from `//`) and `.` change nothing |
| `if (part.equals("..")) { if (!stack.isEmpty()) stack.pop(); }` | go up a level, unless we're already at the root |
| `else stack.push(part);` | a real directory name (even `...`) |
| `descendingIterator()` loop | rebuild from the root down |
| `sb.length() == 0 ? "/" : ...` | an empty stack means the root itself |

### Dry run 1: `"/a/./b/../../c/"`

Tokens: `""`, `a`, `.`, `b`, `..`, `..`, `c` (the trailing empty string is dropped by split).

| token | action | stack (bottom → top) |
|---|---|---|
| "" | skip | |
| a | push | a |
| . | skip | a |
| b | push | a b |
| .. | pop | a |
| .. | pop | (empty) |
| c | push | c |

**Result `"/c"`** ✓

### Dry run 2: `"/.../a/../b/c/../d/./"`
`...` → push (it's a name!); `a` push; `..` pop; `b` push; `c` push; `..` pop; `d` push; `.` skip → stack `[..., b, d]` → **`"/.../b/d"`** ✓

### Dry run 3: `"/../"`
`..` on an empty stack → nothing happens → **`"/"`** ✓

### Complexity
- **Time O(n)**, **Space O(n).**

---

## 5. (Bonus) Approach 2: Manual scanning without split

```java
static String simplifyPathScan(String path) {
    Deque<String> stack = new ArrayDeque<>();
    int i = 0, n = path.length();
    while (i < n) {
        while (i < n && path.charAt(i) == '/') i++;       // skip the slashes
        int start = i;
        while (i < n && path.charAt(i) != '/') i++;       // read one name
        String part = path.substring(start, i);
        if (part.isEmpty() || part.equals(".")) continue;
        if (part.equals("..")) { if (!stack.isEmpty()) stack.pollLast(); }
        else stack.addLast(part);
    }
    return "/" + String.join("/", stack);                 // a front-to-back iteration = root → leaf
}
```

The same logic, avoiding the regex-based `split`. It uses the Deque as a **list** (`addLast` / `pollLast`), so `String.join` naturally produces the root-to-leaf order.

---

## 6. Comparing the approaches

| | split + stack | Manual scan |
|---|---|---|
| Time | O(n) | O(n) |
| Space | O(n) | O(n) |
| Uses regex | yes (`split`) | no |

---

## 7. Edge cases and common mistakes

**Edge cases:** the root only (`"/"`), `..` at the root, `...` or `..a` as names, repeated slashes, a trailing slash.

**Mistakes:**
1. Treating `...` as special (it's a normal name).
2. Popping from an empty stack on `..` at the root.
3. Building the result from the top of the stack (it comes out reversed).
4. Returning `""` instead of `"/"` for the root.

---

## 8. Interview questions and answers

**Q1. Why a stack?**
> Navigating directories is LIFO: `..` undoes the most recent directory entered.

**Q2. How do you handle `//` and a trailing `/`?**
> Splitting on `/` creates empty tokens for them; skipping the empty tokens normalises everything.

### Follow-up questions

**F1. Relative paths (resolve against a current directory).**
> Start the stack with the current directory's components, then process the relative path the same way.

**F2. Symbolic links.**
> Resolving a link replaces it with its target's components, which may contain `..`. You'd need cycle detection for link loops.

**F3. Crawler Log Folder (LC 1598).**
> The same idea, but only the depth matters, so a counter is enough.

---

## 9. Running the file

```bash
java M08_SimplifyPath.java      # 5/5 tests passed
```

**One-line summary:** *"Split by '/', skip empty tokens and '.', pop on '..', push names; join from the root."*
