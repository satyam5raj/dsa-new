# E04 · Remove All Adjacent Duplicates In String (+ #1209 with k) ⭐

| | |
|---|---|
| **LeetCode** | [#1047 Remove All Adjacent Duplicates In String](https://leetcode.com/problems/remove-all-adjacent-duplicates-in-string/), follow-up [#1209](https://leetcode.com/problems/remove-all-adjacent-duplicates-in-string-ii/) |
| **Difficulty** | Easy (#1209 is Medium) |
| **Pattern** | Cancel with the top of a stack (chain reactions come for free) |
| **Code** | [E04_RemoveAllAdjacentDuplicates.java](E04_RemoveAllAdjacentDuplicates.java) |
| **Run** | `java E04_RemoveAllAdjacentDuplicates.java` |

⭐ = a bonus classic.

---

## 1. The problem

Repeatedly remove **two adjacent equal letters** until none are left.

| s | result | how |
|---|---|---|
| `"abbaca"` | `"ca"` | `bb` goes → `"aaca"`, then `aa` goes → `"ca"` |
| `"azxxzy"` | `"ay"` | `xx` → `"azzy"`, then `zz` → `"ay"` |
| `"aaaa"` | `""` | |
| `"abc"` | `"abc"` | |

**#1209 (follow-up):** remove groups of **k** equal adjacent letters. `"deeedbbcccbdaa"`, k = 3 → `"aa"`.

**Constraints:** `1 ≤ n ≤ 10⁵`, lowercase letters.

---

## 2. How to think about it

- **Naive:** scan for a pair, remove it, rescan from the start: O(n²). The removals can **chain**: removing `bb` makes the two a's neighbours.
- **Stack:** process left to right. The **top** of the stack is the character that's currently **to the left** of the new one (after all removals so far).
  - `c == top` → **pop** (the pair cancels). The **new** top is now the next neighbour on the left, so chain reactions happen automatically.
  - otherwise → **push** c.

A `StringBuilder` is the perfect stack of characters here: `append` = push, `deleteCharAt(length − 1)` = pop, and the result is already in left-to-right order.

### #1209 (k-groups)
Store **(char, run length)** pairs on the stack. If c matches the top's char, increment its count; when the count reaches k, pop the whole run.

---

## 3. Java concepts used in this problem

### 3.1 `StringBuilder` as a stack of chars

```java
StringBuilder stack = new StringBuilder();
stack.append(c);                       // push
stack.charAt(stack.length() - 1)       // peek
stack.deleteCharAt(stack.length() - 1) // pop: O(1) at the end
stack.toString()                       // the remaining characters, in order
```

Deleting the **last** character is O(1) (nothing needs to shift). Deleting from the middle would be O(n). (See the StringBuilder notes from the Basics.)

### 3.2 `int[]` as a small pair

```java
stack.push(new int[]{c, 1});     // [character code, run length]
stack.peek()[1]                  // the run length of the top entry
++stack.peek()[1]                // increments the count INSIDE the array on the stack
```

The `char` is stored as an `int`. `++stack.peek()[1]` modifies the array object that's on the stack (it's a reference), so there's no need to pop and push again. A `record Run(char c, int count)` would be more readable but immutable, so you'd need to replace it.

### 3.3 `descendingIterator()`

`ArrayDeque` used as a stack has its top at the **front**. `descendingIterator()` walks from the **bottom to the top**, i.e. left-to-right in the original string order, which is exactly the order we need to rebuild the result.

---

## 4. Approach 1: Repeated scanning (naive)

```java
static String removeDuplicatesNaive(String s) {
    StringBuilder sb = new StringBuilder(s);
    boolean changed = true;
    while (changed) {
        changed = false;
        for (int i = 0; i + 1 < sb.length(); i++) {
            if (sb.charAt(i) == sb.charAt(i + 1)) {
                sb.delete(i, i + 2);                // remove the pair (shifts the rest: O(n))
                changed = true;
                break;                              // restart the scan
            }
        }
    }
    return sb.toString();
}
```

**O(n²)** in the worst case (e.g. `"abccba"`-like nesting).

---

## 5. Approach 2: Stack via StringBuilder (the file's main solution)

### Code ([E04_RemoveAllAdjacentDuplicates.java:38-46](E04_RemoveAllAdjacentDuplicates.java#L38-L46))

```java
static String removeDuplicates(String s) {
    StringBuilder stack = new StringBuilder();
    for (char c : s.toCharArray()) {
        int n = stack.length();
        if (n > 0 && stack.charAt(n - 1) == c) stack.deleteCharAt(n - 1);   // cancel the pair
        else stack.append(c);
    }
    return stack.toString();
}
```

### Line by line

| Line | What it does |
|---|---|
| `StringBuilder stack` | the characters that have survived so far |
| `int n = stack.length();` | the current stack size |
| `if (n > 0 && stack.charAt(n - 1) == c)` | the new char equals the surviving char to its left |
| `stack.deleteCharAt(n - 1);` | cancel both (pop the top; c is never pushed) |
| `else stack.append(c);` | c survives for now |
| `return stack.toString();` | the final survivors, in order |

### Dry run 1: `"abbaca"`

| c | top | action | stack |
|---|---|---|---|
| a | — | push | a |
| b | a | push | ab |
| b | b | **cancel** | a |
| a | a | **cancel** (a chain reaction) | (empty) |
| c | — | push | c |
| a | c | push | ca |

**Result `"ca"`** ✓

### Dry run 2: `"azxxzy"`
a → az → azx → (x cancels) az → (z cancels) a → ay → **`"ay"`** ✓

**O(n) time, O(n) space.**

---

## 6. Approach 3 (#1209): a stack of (char, count) runs (the file's second method)

### Code ([E04_RemoveAllAdjacentDuplicates.java:49-65](E04_RemoveAllAdjacentDuplicates.java#L49-L65))

```java
static String removeDuplicatesK(String s, int k) {
    Deque<int[]> stack = new ArrayDeque<>();                 // [char, run length]
    for (char c : s.toCharArray()) {
        if (!stack.isEmpty() && stack.peek()[0] == c) {
            if (++stack.peek()[1] == k) stack.pop();         // the run reached k -> remove it
        } else {
            stack.push(new int[]{c, 1});
        }
    }
    StringBuilder sb = new StringBuilder();
    Iterator<int[]> it = stack.descendingIterator();         // bottom to top = left to right
    while (it.hasNext()) {
        int[] e = it.next();
        sb.append(String.valueOf((char) e[0]).repeat(e[1]));
    }
    return sb.toString();
}
```

### Line by line

| Line | What it does |
|---|---|
| `stack.peek()[0] == c` | the same character as the current run |
| `if (++stack.peek()[1] == k) stack.pop();` | extend the run; at k, the whole run disappears (and the run below may continue: a chain reaction) |
| `stack.push(new int[]{c, 1})` | a new run of length 1 |
| `descendingIterator()` + `repeat` | rebuild the string from the bottom run to the top run |

### Dry run: `"deeedbbcccbdaa"`, k = 3

| c | action | stack (bottom → top) |
|---|---|---|
| d | push | d1 |
| e | push | d1 e1 |
| e | count | d1 e2 |
| e | reaches 3 → pop | d1 |
| d | count (it joins the earlier d!) | d2 |
| b | push | d2 b1 |
| b | count | d2 b2 |
| c | push | d2 b2 c1 |
| c | count | d2 b2 c2 |
| c | reaches 3 → pop | d2 b2 |
| b | reaches 3 → pop | d2 |
| d | reaches 3 → pop | (empty) |
| a | push | a1 |
| a | count | a2 |

**Result `"aa"`** ✓. Three chain reactions happened automatically.

**O(n) time, O(n) space.**

---

## 7. Comparing the approaches

| | Naive rescan | Stack (k = 2) | Stack of runs (any k) |
|---|---|---|---|
| Time | O(n²) | **O(n)** | **O(n)** |
| Space | O(n) | O(n) | O(n) |

---

## 8. Edge cases and common mistakes

**Edge cases:** everything cancels, nothing cancels, long chain reactions, k = 1 for #1209 (everything is removed).

**Mistakes:**
1. Comparing with the **original** previous character instead of the stack's top (it misses chains).
2. Building the result with `+=` in a loop (O(n²)).
3. In #1209, rebuilding the output top-to-bottom (it comes out reversed).

---

## 9. Interview questions and answers

**Q1. Why does the stack handle chain reactions?**
> After a pair cancels, the new top is the character that's now adjacent to whatever comes next, which is exactly the neighbour after the removal. So the next comparison naturally checks the newly formed pair.

**Q2. How do you generalise to k?**
> Store run lengths with the characters, increment on a match, and pop when the run reaches k.

### Follow-up questions

**F1. Make the String Great (LC 1544): remove the pairs like "aA".**
> The same stack, cancelling when `|top − c| == 'a' − 'A'` (the same letter, opposite case).

**F2. Asteroid Collision (LC 735).** → [M12](../03-Medium/M12_AsteroidCollision.md)
> The new element "fights" the top; the stack resolves chain collisions.

**F3. Remove K Digits (LC 402).** → [M09](../03-Medium/M09_RemoveKDigits.md)
> A monotonic stack: pop the larger previous digits while removals remain.

---

## 10. Running the file

```bash
java E04_RemoveAllAdjacentDuplicates.java      # 5/5 tests passed
```

**One-line summary:** *"Push each char; if it equals the top, pop instead: the chain reactions come for free."*
