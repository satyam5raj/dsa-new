# E05 · Backspace String Compare ⭐

| | |
|---|---|
| **LeetCode** | [#844 Backspace String Compare](https://leetcode.com/problems/backspace-string-compare/) |
| **Difficulty** | Easy |
| **Pattern** | Stack simulation → follow-up: two pointers from the back (O(1) space) |
| **Code** | [E05_BackspaceStringCompare.java](E05_BackspaceStringCompare.java) |
| **Run** | `java E05_BackspaceStringCompare.java` |

⭐ = a bonus classic.

---

## 1. The problem

`#` means **backspace**. Are `s` and `t` equal after typing them into a text editor? (A backspace on empty text does nothing.)

| s | t | typed s | typed t | answer |
|---|---|---|---|---|
| `"ab#c"` | `"ad#c"` | `ac` | `ac` | true |
| `"ab##"` | `"c#d#"` | `` | `` | true |
| `"a#c"` | `"b"` | `c` | `b` | false |
| `"a##c"` | `"#a#c"` | `c` | `c` | true (extra backspaces are harmless) |

**Constraints:** `1 ≤ lengths ≤ 200`. Follow-up: *O(n) time and O(1) space?*

---

## 2. How to think about it

### Approach 1: simulate with a stack
A letter → push; `#` → pop (if the stack isn't empty). Then compare the two results. It's simple: O(n + m) time and space.

### Approach 2: walk backwards (O(1) space)
Reading from the **end**, a `#` tells you to **skip the next letter you meet** (the one to its left). Keep a `skip` counter per string:
- `#` → `skip++`
- a letter with `skip > 0` → `skip--`, and ignore the letter
- a letter with `skip == 0` → this letter **survives**: compare it with the other string's next survivor

Compare the survivors one pair at a time, from the end.

"Solve it with a stack first, then optimise the stack away" is a common interview flow.

---

## 3. Java concepts used in this problem

### 3.1 `StringBuilder` as a char stack (again)

`append` = push, `deleteCharAt(length − 1)` = pop (see [E04](E04_RemoveAllAdjacentDuplicates.md)).

### 3.2 A helper that returns an index

```java
static int nextSurvivor(String s, int i)   // the index of the next surviving char to the left of (or at) i, or −1
```

Returning −1 for "none" is the standard index convention. Factoring the skip logic into a helper keeps the main loop short and symmetric for both strings.

### 3.3 Randomised equivalence testing

`main` compares the O(1) version with the stack version on 2000 random strings built from `"ab##"` (a high density of `#`). When you optimise a correct solution, check the optimised version against the simple one.

---

## 4. Approach 1: Stack simulation (the file's first method)

### Code ([E05_BackspaceStringCompare.java:38-49](E05_BackspaceStringCompare.java#L38-L49))

```java
static boolean backspaceCompareStack(String s, String t) {
    return type(s).equals(type(t));
}

static String type(String s) {
    StringBuilder sb = new StringBuilder();
    for (char c : s.toCharArray()) {
        if (c != '#') sb.append(c);
        else if (sb.length() > 0) sb.deleteCharAt(sb.length() - 1);
    }
    return sb.toString();
}
```

| Line | What it does |
|---|---|
| `type(s).equals(type(t))` | type both strings out, then compare the results (`.equals`, not `==`!) |
| `if (c != '#') sb.append(c);` | a letter is typed |
| `else if (sb.length() > 0) sb.deleteCharAt(...)` | a backspace deletes the last typed letter, if there is one |

**Dry run** `"ab#c"`: a → ab → (#) a → ac. `"ad#c"`: a → ad → a → ac. Equal → **true**.

**O(n + m) time and space.**

---

## 5. Approach 2: Two pointers from the back (the file's main solution)

### Code ([E05_BackspaceStringCompare.java:51-74](E05_BackspaceStringCompare.java#L51-L74))

```java
static boolean backspaceCompare(String s, String t) {
    int i = s.length() - 1, j = t.length() - 1;
    while (i >= 0 || j >= 0) {
        i = nextSurvivor(s, i);
        j = nextSurvivor(t, j);
        if (i < 0 && j < 0) return true;                       // both fully consumed
        if (i < 0 || j < 0 || s.charAt(i) != t.charAt(j)) return false;
        i--;
        j--;
    }
    return true;
}

// index of the next character (moving left from i) that survives the backspaces, or -1
static int nextSurvivor(String s, int i) {
    int skip = 0;
    while (i >= 0) {
        if (s.charAt(i) == '#') skip++;
        else if (skip > 0) skip--;
        else break;                                            // a surviving letter
        i--;
    }
    return i;
}
```

### Line by line

| Line | What it does |
|---|---|
| `int i = s.length() - 1, j = t.length() - 1;` | start at both ends |
| `i = nextSurvivor(s, i); j = nextSurvivor(t, j);` | jump to each string's next real (surviving) character |
| `if (i < 0 && j < 0) return true;` | both strings are exhausted at the same time → equal |
| `if (i < 0 \|\| j < 0 \|\| s[i] != t[j]) return false;` | one ran out early, or the survivors differ |
| `i--; j--;` | this pair matched; continue to the left |
| `nextSurvivor`: `#` → skip++ | one more letter to the left must be ignored |
| letter with skip > 0 → skip−− | this letter is erased |
| letter with skip == 0 → break | found a survivor; return its index |

### Dry run: `"ab##"` vs `"c#d#"`

| string | scan from the end | result |
|---|---|---|
| s = `ab##` | `#` (skip 1), `#` (skip 2), `b` (erased, skip 1), `a` (erased, skip 0) → i = −1 | no survivor |
| t = `c#d#` | `#` (skip 1), `d` (erased), `#` (skip 1), `c` (erased) → j = −1 | no survivor |

Both −1 → **true** ✓

### Dry run 2: `"a#c"` vs `"b"`
s: `c` survives (i = 2); t: `b` survives (j = 0). `c ≠ b` → **false** ✓

### Dry run 3: `"ab#c"` vs `"ad#c"`

| round | s survivor | t survivor | equal? |
|---|---|---|---|
| 1 | c (index 3) | c (index 3) | yes |
| 2 | `#` skips b → **a** (index 0) | `#` skips d → **a** (index 0) | yes |
| 3 | −1 | −1 | both done → **true** |

### Complexity
- **Time O(n + m):** each index is visited once.
- **Space O(1).**

---

## 6. Comparing the approaches

| | Stack simulation | Two pointers from the back |
|---|---|---|
| Time | O(n + m) | O(n + m) |
| Space | O(n + m) | **O(1)** |
| Early exit on a mismatch | no (it types everything first) | **yes** |

---

## 7. Edge cases and common mistakes

**Edge cases:** more backspaces than letters, everything erased, strings ending in `#`, different lengths that type to the same text.

**Mistakes:**
1. Scanning from the **front** in the O(1) version (a `#` affects letters to its **left**, so you'd need lookahead).
2. Returning true as soon as one string is exhausted (the other may still have survivors).
3. Comparing the typed strings with `==` instead of `.equals`.

---

## 8. Interview questions and answers

**Q1. Why scan from the end for O(1) space?**
> A backspace only affects characters before it. Reading backwards, we see each `#` before the letters it deletes, so a counter is enough to know which letters survive.

**Q2. Walk me through `nextSurvivor`.**
> Move left: each `#` adds one pending deletion; each letter either uses up a pending deletion or, if there are none, is a survivor, so return its index.

### Follow-up questions

**F1. Return the final typed string efficiently.**
> The stack version (StringBuilder) is the natural answer: O(n).

**F2. Support "undo" or cursor movements (a text editor).**
> Two stacks (left of the cursor, right of the cursor), or a gap buffer / rope for big texts.

**F3. Crawler Log Folder / Simplify Path.** → [M08](../03-Medium/M08_SimplifyPath.md)
> The same "stack of tokens, `..` pops" idea.

---

## 9. Running the file

```bash
java E05_BackspaceStringCompare.java      # 5/5 tests passed (including 2,000 random pairs)
```

**One-line summary:** *"Simulate typing with a stack, or walk both strings backwards with skip counters and compare the survivors."*
