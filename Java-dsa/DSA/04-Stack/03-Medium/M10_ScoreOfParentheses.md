# M10 · Score of Parentheses

| | |
|---|---|
| **LeetCode** | [#856 Score of Parentheses](https://leetcode.com/problems/score-of-parentheses/) |
| **Difficulty** | Medium |
| **Pattern** | A stack of scores per nesting level, or depth counting (O(1) space) |
| **Code** | [M10_ScoreOfParentheses.java](M10_ScoreOfParentheses.java) |
| **Run** | `java M10_ScoreOfParentheses.java` |

---

## 1. The problem

A balanced string of `(` and `)` is scored by these rules:
- `"()"` = 1
- `AB` = A + B (side by side: add)
- `(A)` = 2 × A (wrapping: double)

| s | score |
|---|---|
| `"()"` | 1 |
| `"(())"` | 2 |
| `"()()"` | 2 |
| `"(()(()))"` | 6 |
| `"((()))()"` | 5 |

**Constraints:** `2 ≤ n ≤ 50`, balanced.

---

## 2. How to think about it

### Approach 1: a stack of scores (one running score per open level)
- The stack starts with `[0]` (the score of the outermost level).
- `(` → push 0 (start a new, deeper level).
- `)` → `inner = pop()` (the finished level's score); add `max(2 · inner, 1)` to the new top. An **empty** `()` has inner = 0 and is worth 1; otherwise wrapping doubles it.
- The answer is the remaining value.

### Approach 2: an O(1) space insight
Only the **`()` pairs** create points. A `()` at depth d (inside d other pairs) gets **doubled d times**, so it's worth **2ᵈ**:

```
answer = Σ 2^depth over every "()"
"(()(()))": the first () is at depth 1 → 2, the second at depth 2 → 4 → total 6
```

Why? Doubling distributes over addition: `(A + B)` = 2A + 2B, so each innermost `()` is simply multiplied by 2 for every enclosing pair.

---

## 3. Java concepts used in this problem

### 3.1 A stack of accumulators

Each stack entry is the running total for one open nesting level. When a level closes, its total is transformed (doubled, or 1 if empty) and **added into its parent**. This "accumulate per level" pattern is the same one used in Decode String ([M05](M05_DecodeString.md)) and Basic Calculator ([H03](../04-Hard/H03_BasicCalculator.md)).

### 3.2 `1 << depth` = 2^depth

The left shift multiplies by 2 for each position: `1 << 0 = 1`, `1 << 1 = 2`, `1 << 3 = 8`. It's the fastest way to compute powers of two for integers (and it's exact, unlike `Math.pow`, which returns a `double`).

### 3.3 Looking at the previous character

```java
if (s.charAt(i - 1) == '(') score += 1 << depth;
```

A `)` right after a `(` closes a **`()` pair**, which is the only kind of pair that scores directly. It's safe, because a `)` is never at index 0 in a balanced string.

---

## 4. Approach 1: Recursion / divide by balance

```java
static int scoreRecursive(String s, int lo, int hi) {      // the score of s[lo..hi)
    int score = 0, bal = 0;
    for (int i = lo; i < hi; i++) {
        bal += s.charAt(i) == '(' ? 1 : -1;
        if (bal == 0) {                                     // s[lo..i] is one top-level group
            score += (i - lo == 1) ? 1 : 2 * scoreRecursive(s, lo + 1, i);
            lo = i + 1;
        }
    }
    return score;
}
```

Split the string into top-level groups (where the balance returns to 0). Each group is either `()` (1) or `(A)` (2 × score(A)). **O(n²)** in the worst case (deep nesting rescans), O(n) stack.

---

## 5. Approach 2: A stack of level scores (the file's main solution)

### Code ([M10_ScoreOfParentheses.java:41-53](M10_ScoreOfParentheses.java#L41-L53))

```java
static int scoreOfParentheses(String s) {
    Deque<Integer> stack = new ArrayDeque<>();
    stack.push(0);
    for (char c : s.toCharArray()) {
        if (c == '(') {
            stack.push(0);
        } else {
            int inner = stack.pop();
            stack.push(stack.pop() + Math.max(2 * inner, 1));
        }
    }
    return stack.pop();
}
```

### Line by line

| Line | What it does |
|---|---|
| `stack.push(0);` | the outermost level starts with score 0 |
| `if (c == '(') stack.push(0);` | a new level, empty so far |
| `int inner = stack.pop();` | the level being closed |
| `stack.push(stack.pop() + Math.max(2 * inner, 1));` | its value (1 if it was empty, otherwise double) is added to the parent level |
| `return stack.pop();` | the outermost level's total |

### Dry run: `"(()(()))"`

| char | action | stack (bottom → top) |
|---|---|---|
| start | | [0] |
| ( | push 0 | [0, 0] |
| ( | push 0 | [0, 0, 0] |
| ) | inner 0 → +1 | [0, 1] |
| ( | push 0 | [0, 1, 0] |
| ( | push 0 | [0, 1, 0, 0] |
| ) | inner 0 → +1 | [0, 1, 1] |
| ) | inner 1 → +2 | [0, 3] |
| ) | inner 3 → +6 | [6] |

**Return 6** ✓

### Complexity
- **Time O(n)**, **Space O(n)** (the stack depth = the nesting depth).

---

## 6. Approach 3: Depth counting, O(1) space (the file's second method)

### Code ([M10_ScoreOfParentheses.java:55-66](M10_ScoreOfParentheses.java#L55-L66))

```java
static int scoreConstantSpace(String s) {
    int score = 0, depth = 0;
    for (int i = 0; i < s.length(); i++) {
        if (s.charAt(i) == '(') {
            depth++;
        } else {
            depth--;
            if (s.charAt(i - 1) == '(') score += 1 << depth;   // a "()" at this depth
        }
    }
    return score;
}
```

| Line | What it does |
|---|---|
| `depth++` / `depth--` | track how deeply nested we are |
| `if (s.charAt(i - 1) == '(')` | this `)` closes a `()` pair |
| `score += 1 << depth;` | the pair is doubled once per enclosing pair (depth after the decrement = the number of enclosing pairs) |

### Dry run: `"((()))()"`

| i | char | depth after | a `()`? | score |
|---|---|---|---|---|
| 0, 1, 2 | ( ( ( | 3 | | 0 |
| 3 | ) | 2 | yes → 2² = 4 | 4 |
| 4, 5 | ) ) | 0 | no | 4 |
| 6 | ( | 1 | | 4 |
| 7 | ) | 0 | yes → 2⁰ = 1 | **5** |

**Return 5** ✓

**O(n) time, O(1) space.**

---

## 7. Comparing the approaches

| | Recursive split | Stack of levels | Depth counting |
|---|---|---|---|
| Time | O(n²) worst case | **O(n)** | **O(n)** |
| Space | O(depth) | O(depth) | **O(1)** |

---

## 8. Edge cases and common mistakes

**Edge cases:** `"()"`, deep nesting, many side-by-side groups.

**Mistakes:**
1. Doubling an empty level (it must be worth 1, hence `max(2 · inner, 1)`).
2. Counting **every** `)` in the O(1) version, not just those that close a `()`.
3. Using `depth` before decrementing it (off by one in the exponent).

---

## 9. Interview questions and answers

**Q1. Why is each `()` worth 2^depth?**
> Wrapping doubles everything inside, and doubling distributes over addition, so each innermost `()` is doubled once for every pair that encloses it.

**Q2. What does each stack entry hold?**
> The score accumulated so far at one open nesting level. When the level closes, its score is converted and added to its parent.

### Follow-up questions

**F1. Valid Parentheses.** → [E01](../02-Easy/E01_ValidParentheses.md)

**F2. Maximum Nesting Depth (LC 1614).**
> Track the max of `depth` during the scan.

**F3. Different scoring rules (e.g. triple when wrapped).**
> Replace `2 * inner` by the new rule, and the 2ᵈ shortcut by kᵈ.

---

## 10. Running the file

```bash
java M10_ScoreOfParentheses.java      # 6/6 tests passed
```

**One-line summary:** *"A stack of level scores (a closing level adds max(2·inner, 1) to its parent), or the sum of 2^depth over every '()'."*
