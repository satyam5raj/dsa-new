# H04 · Longest Valid Parentheses ⭐

| | |
|---|---|
| **LeetCode** | [#32 Longest Valid Parentheses](https://leetcode.com/problems/longest-valid-parentheses/) |
| **Difficulty** | Hard |
| **Pattern** | A stack of indices with a "last invalid position" sentinel, or two counter scans |
| **Code** | [H04_LongestValidParentheses.java](H04_LongestValidParentheses.java) |
| **Run** | `java H04_LongestValidParentheses.java` |

⭐ = a bonus classic.

---

## 1. The problem

Return the length of the longest **well-formed** (balanced) parentheses **substring**.

| s | answer | substring |
|---|---|---|
| `"(()"` | 2 | `"()"` |
| `")()())"` | 4 | `"()()"` |
| `""` | 0 | |
| `"()(())"` | 6 | the whole string |

**Constraints:** `0 ≤ n ≤ 3 × 10⁴`.

---

## 2. How to think about it

### Brute force
Check every even-length substring with the validity test from [E01](../02-Easy/E01_ValidParentheses.md): O(n³). (A balance counter per start index brings it down to O(n²).)

### A stack of indices with a barrier
The **bottom** of the stack always holds the index of the **last position that can't be part of a valid substring** (a "barrier"). It starts as −1 (a virtual barrier before the string).
- `(` → push its index.
- `)` → pop (it matches the most recent `(`, or it pops the barrier).
  - if the stack is now **empty**: this `)` is unmatched, so push i as the **new barrier**;
  - else: a valid substring ends at i, starting **right after** the stack's top: `length = i − stack.top`.

### Two counter scans (O(1) space)
Left to right, count `open` and `close`: when they're equal, there's a valid substring of length 2·close; when `close > open`, nothing can extend past this point, so reset both. That misses cases like `"(()"` (the counts never become equal), so also scan **right to left** with the mirrored rules.

---

## 3. Java concepts used in this problem

### 3.1 A sentinel index on the stack

`stack.push(-1)` makes "the start of the current valid run" always available as `stack.peek()`. Without it, `i − stack.peek()` would fail for a valid substring starting at index 0. That's the same trick as the −1 left boundary in [Largest Rectangle](../../01-Arrays/04-Hard/H03_LargestRectangleInHistogram.md).

### 3.2 Chained assignment: `open = close = 0;`

The assignment is right-associative: `close = 0` happens first, its value (0) is assigned to `open`.

### 3.3 Testing against a brute force

1000 random strings of `(` and `)` (length < 16) are compared with an O(n²) balance-counting brute force, for **both** methods.

---

## 4. Approach 1: Brute force with a balance counter

### Code ([H04_LongestValidParentheses.java:78-89](H04_LongestValidParentheses.java#L78-L89))

```java
static int brute(String s) {
    int best = 0;
    for (int i = 0; i < s.length(); i++) {
        int bal = 0;
        for (int j = i; j < s.length(); j++) {
            bal += s.charAt(j) == '(' ? 1 : -1;
            if (bal < 0) break;
            if (bal == 0) best = Math.max(best, j - i + 1);
        }
    }
    return best;
}
```

For each start i: a balance below zero means an unmatched `)`, so no longer substring from i can be valid (break); a balance of zero means `s[i..j]` is valid. **O(n²).**

---

## 5. Approach 2: A stack of indices with a barrier (the file's main solution)

### Code ([H04_LongestValidParentheses.java:46-60](H04_LongestValidParentheses.java#L46-L60))

```java
static int longestValidParentheses(String s) {
    Deque<Integer> stack = new ArrayDeque<>();
    stack.push(-1);                                   // the initial barrier
    int best = 0;
    for (int i = 0; i < s.length(); i++) {
        if (s.charAt(i) == '(') {
            stack.push(i);
        } else {
            stack.pop();
            if (stack.isEmpty()) stack.push(i);       // unmatched ')' -> new barrier
            else best = Math.max(best, i - stack.peek());
        }
    }
    return best;
}
```

### Line by line

| Line | What it does |
|---|---|
| `stack.push(-1);` | the barrier before the start |
| `(` → `stack.push(i)` | an open bracket waiting for its match |
| `)` → `stack.pop()` | match the latest `(`, or remove the barrier if there's no `(` |
| `if (stack.isEmpty()) stack.push(i);` | there was no `(` to match: this `)` becomes the new barrier |
| `else best = Math.max(best, i - stack.peek());` | the top is the last unmatched position (a barrier or an unmatched `(`); everything after it up to i is valid |

### Dry run 1: `")()())"`

| i | char | action | stack (bottom → top) | length | best |
|---|---|---|---|---|---|
| — | | start | [−1] | | 0 |
| 0 | ) | pop −1 → empty → push 0 | [0] | | 0 |
| 1 | ( | push | [0, 1] | | 0 |
| 2 | ) | pop 1 → top 0 | [0] | 2 − 0 = 2 | 2 |
| 3 | ( | push | [0, 3] | | 2 |
| 4 | ) | pop 3 → top 0 | [0] | 4 − 0 = **4** | **4** |
| 5 | ) | pop 0 → empty → push 5 | [5] | | 4 |

**Return 4** ✓

### Dry run 2: `"(()"`

| i | char | stack after | length | best |
|---|---|---|---|---|
| 0 | ( | [−1, 0] | | 0 |
| 1 | ( | [−1, 0, 1] | | 0 |
| 2 | ) | pop 1 → [−1, 0] | 2 − 0 = **2** | 2 |

**Return 2** ✓. The unmatched `(` at index 0 acts as the barrier.

### Complexity
- **Time O(n)**, **Space O(n).**

---

## 6. Approach 3: Two counter scans, O(1) space (the file's second method)

### Code ([H04_LongestValidParentheses.java:62-76](H04_LongestValidParentheses.java#L62-L76))

```java
static int longestValidParenthesesTwoScans(String s) {
    int best = 0, open = 0, close = 0;
    for (int i = 0; i < s.length(); i++) {            // left -> right
        if (s.charAt(i) == '(') open++; else close++;
        if (open == close) best = Math.max(best, 2 * close);
        else if (close > open) open = close = 0;      // too many ')': nothing can extend past here
    }
    open = close = 0;
    for (int i = s.length() - 1; i >= 0; i--) {       // right -> left (catches cases like "(()")
        if (s.charAt(i) == '(') open++; else close++;
        if (open == close) best = Math.max(best, 2 * open);
        else if (open > close) open = close = 0;
    }
    return best;
}
```

### Line by line

| Line | What it does |
|---|---|
| left-to-right: `open == close` | the segment since the last reset is balanced → a candidate |
| `close > open` → reset | an unmatched `)` can't be inside any valid substring |
| right-to-left: the mirrored rules | catches the substrings ruined by surplus `(` on the left (the first scan never sees `open == close` for them) |

### Dry run: `"(()"`
- **Left → right:** `(` o=1; `(` o=2; `)` c=1 → never equal → best 0.
- **Right → left:** `)` c=1; `(` o=1 → equal → best 2; `(` o=2 > c → reset.
- **Return 2** ✓. The first scan alone would miss it.

**O(n) time, O(1) space.**

---

## 7. (Mention) Approach 4: DP

`dp[i]` = the length of the longest valid substring **ending** at i. For `s[i] == ')'`:
- if `s[i−1] == '('`: `dp[i] = dp[i−2] + 2`
- else if `s[i − dp[i−1] − 1] == '('`: `dp[i] = dp[i−1] + 2 + dp[i − dp[i−1] − 2]`

O(n) time and space. It's covered in the DP topic.

---

## 8. Comparing the approaches

| | Brute force | Stack of indices | Two scans | DP |
|---|---|---|---|---|
| Time | O(n²) | **O(n)** | **O(n)** | O(n) |
| Space | O(1) | O(n) | **O(1)** | O(n) |

---

## 9. Edge cases and common mistakes

**Edge cases:** an empty string, all `(`, all `)`, a valid substring at the start or end, several valid runs separated by barriers.

**Mistakes:**
1. Forgetting the −1 sentinel, so `i − peek()` fails for runs starting at 0.
2. Not replacing the barrier when the stack becomes empty.
3. Doing only the left-to-right counter scan (misses `"(()"`).
4. Computing `i − peek() + 1` (off by one: peek is the position **before** the run).

---

## 10. Interview questions and answers

**Q1. What does the stack's bottom represent?**
> The index of the last position that can't be part of any valid substring through i: either the virtual −1, or the latest unmatched `)`. The unmatched `(` above it also act as barriers for their own region.

**Q2. Why does the two-scan method need both directions?**
> Left to right handles surplus `)` by resetting, but surplus `(` never let the counts become equal. Scanning right to left handles those symmetrically.

### Follow-up questions

**F1. Minimum Remove to Make Valid Parentheses (LC 1249).**
> A stack of the unmatched `(` indices; remove those and every unmatched `)`.

**F2. Return the substring itself.**
> Track the start (`stack.peek() + 1`) whenever `best` improves.

**F3. Count the valid substrings (not just the longest).**
> DP: `count[i]` = the number of valid substrings ending at i, using the matching index from the stack (`count[i] = 1 + count[match − 1]`).

---

## 11. Running the file

```bash
java H04_LongestValidParentheses.java      # 6/6 tests passed (including 1,000 random strings, both methods)
```

**One-line summary:** *"A stack of indices with a −1 barrier: a `)` pops; if empty, it becomes the new barrier, else length = i − top. Or two counter scans."*
