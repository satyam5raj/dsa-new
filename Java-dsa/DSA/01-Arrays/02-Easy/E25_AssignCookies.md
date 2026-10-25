# E25 · Assign Cookies

| | |
|---|---|
| **LeetCode** | [#455 Assign Cookies](https://leetcode.com/problems/assign-cookies/) |
| **Difficulty** | Easy |
| **Pattern** | Greedy + sort + two pointers |
| **Code** | [E25_AssignCookies.java](E25_AssignCookies.java) |
| **Run** | `java E25_AssignCookies.java` |

---

## 1. The problem

- `g[i]` = the **greed factor** of child i: the minimum cookie size that makes them content.
- `s[j]` = the size of cookie j.
- Each child gets **at most one** cookie, and each cookie goes to at most one child.
- **Maximise** the number of content children.

| g (greed) | s (cookies) | answer | why |
|---|---|---|---|
| `[1, 2, 3]` | `[1, 1]` | 1 | both cookies are size 1, so only the greed-1 child is satisfied |
| `[1, 2]` | `[1, 2, 3]` | 2 | cookie 1 → child 1, cookie 2 → child 2 |
| `[10, 9, 8, 7]` | `[5, 6, 7, 8]` | 2 | 7 → greed 7, 8 → greed 8 |
| `[1]` | `[]` | 0 | no cookies |

**Constraints:** `1 ≤ g.length ≤ 3 × 10⁴`, `0 ≤ s.length ≤ 3 × 10⁴`.

---

## 2. How to think about it

- **Brute force** (try every assignment) explodes combinatorially.
- **Greedy:** satisfy the **least greedy** child first, using the **smallest cookie that's big enough**.
  - Why the least greedy child? They're the easiest to satisfy, so serving them first never blocks anyone.
  - Why the smallest sufficient cookie? Bigger cookies are saved for greedier children. Using a bigger cookie than needed could only hurt.
- **Implementation:** sort both arrays, then use two pointers:
  - `i` → the current (least greedy unsatisfied) child
  - `j` → the current (smallest unused) cookie
  - `s[j] >= g[i]`: it fits → child i is satisfied, move to the next child **and** the next cookie
  - otherwise: cookie j is too small for child i, **and therefore for every child after i** (they're even greedier) → throw that cookie away and try the next one

### Why is greedy correct? (exchange argument)
Take any optimal assignment. If it gives child i (the least greedy one) a cookie bigger than the smallest sufficient cookie c, **swap**: give c to child i, and give the bigger cookie to whoever had c (it still satisfies them, since it's bigger). The number of satisfied children doesn't change. Repeating this turns any optimal solution into the greedy one, so greedy is optimal too. This "**swap a choice in the optimal solution for the greedy choice without making it worse**" proof is the standard way to justify greedy algorithms.

---

## 3. Java concepts used in this problem

### 3.1 Greedy algorithms

A **greedy** algorithm builds the answer step by step, always making the choice that looks best **right now**, and never undoing it. It's fast and simple, but **only correct when you can prove it** (as above). Many problems look greedy-solvable but aren't (e.g. coin change with coins {1, 3, 4} and amount 6: greedy picks 4+1+1, but 3+3 is better).

### 3.2 Sorting the inputs in place

`Arrays.sort(g); Arrays.sort(s);` reorders the **caller's** arrays (see E03 on references). That's acceptable on LeetCode. In production code, sort copies (`g.clone()`) if the caller still needs the original order.

### 3.3 A `while` with two pointers that can move at different speeds

```java
while (i < g.length && j < s.length) {
    if (s[j] >= g[i]) i++;
    j++;
}
```

`j` moves **every** iteration (each cookie is examined once), and `i` moves only on a match. The loop stops when we run out of children **or** cookies.

---

## 4. Approach 1: Brute force (for each child, scan for the smallest fitting cookie)

```java
static int findContentChildrenBrute(int[] g, int[] s) {
    boolean[] used = new boolean[s.length];
    int[] kids = g.clone();
    Arrays.sort(kids);                              // least greedy first
    int content = 0;
    for (int need : kids) {
        int pick = -1;
        for (int j = 0; j < s.length; j++) {        // smallest unused cookie with size >= need
            if (!used[j] && s[j] >= need && (pick == -1 || s[j] < s[pick])) pick = j;
        }
        if (pick != -1) { used[pick] = true; content++; }
    }
    return content;
}
```

This is the same greedy idea **without** sorting the cookies, so each child scans all cookies: **O(n·m)**. Sorting the cookies too turns the inner scan into a single forward-moving pointer.

---

## 5. Approach 2: Sort + two pointers (the file's solution)

### Code ([E25_AssignCookies.java:39-48](E25_AssignCookies.java#L39-L48))

```java
static int findContentChildren(int[] g, int[] s) {
    Arrays.sort(g);
    Arrays.sort(s);
    int i = 0, j = 0;
    while (i < g.length && j < s.length) {
        if (s[j] >= g[i]) i++;       // child satisfied, move to the next child
        j++;                         // this cookie is used (or too small): move on either way
    }
    return i;                        // number of satisfied children
}
```

### Line by line

| Line | What it does |
|---|---|
| `Arrays.sort(g); Arrays.sort(s);` | least greedy children first, smallest cookies first |
| `int i = 0, j = 0;` | `i` = the next child to satisfy, `j` = the next cookie to try |
| `while (i < g.length && j < s.length)` | stop when either children or cookies run out |
| `if (s[j] >= g[i]) i++;` | the cookie satisfies the child → this child is done |
| `j++;` | the cookie is consumed either way: given to child i, or too small for everyone left |
| `return i;` | `i` counts the satisfied children (they're always the first i in sorted order) |

### Dry run 1: `g = [1, 2, 3]`, `s = [1, 1]` (already sorted)

| i (g[i]) | j (s[j]) | fits? | i after | j after |
|---|---|---|---|---|
| 0 (1) | 0 (1) | 1 ≥ 1 ✓ | 1 | 1 |
| 1 (2) | 1 (1) | 1 ≥ 2 ✗ | 1 | 2 |
| — | j = 2 = s.length → stop | | | |

**Return 1** ✓

### Dry run 2: `g = [10, 9, 8, 7]`, `s = [5, 6, 7, 8]`

After sorting: `g = [7, 8, 9, 10]`, `s = [5, 6, 7, 8]`

| i (g[i]) | j (s[j]) | fits? | i | j |
|---|---|---|---|---|
| 0 (7) | 0 (5) | ✗ | 0 | 1 |
| 0 (7) | 1 (6) | ✗ | 0 | 2 |
| 0 (7) | 2 (7) | ✓ | 1 | 3 |
| 1 (8) | 3 (8) | ✓ | 2 | 4 |
| — | stop | | | |

**Return 2** ✓

### Dry run 3: `g = [1, 2]`, `s = [1, 2, 3]`

| i (g[i]) | j (s[j]) | fits? | i | j |
|---|---|---|---|---|
| 0 (1) | 0 (1) | ✓ | 1 | 1 |
| 1 (2) | 1 (2) | ✓ | 2 | 2 |
| i = 2 = g.length → stop | | | | |

**Return 2** ✓ (cookie 3 is left over)

### Complexity
- **Time O(n log n + m log m)** for the sorts; the two-pointer walk is O(n + m).
- **Space O(log n + log m)** for the sorts (in place).

---

## 6. (Bonus) Alternative greedy: serve the greediest child with the biggest cookie

Iterate from the **largest** cookie and the **greediest** child, going down:

```java
static int findContentChildrenFromTop(int[] g, int[] s) {
    Arrays.sort(g); Arrays.sort(s);
    int i = g.length - 1, j = s.length - 1, content = 0;
    while (i >= 0 && j >= 0) {
        if (s[j] >= g[i]) { content++; j--; }   // the biggest cookie satisfies this child
        i--;                                    // otherwise this child can't be satisfied by any cookie
    }
    return content;
}
```

Also correct: if the biggest cookie can't satisfy the greediest child, nothing can, so skip that child. It's the mirror image of the same idea.

---

## 7. Comparing the approaches

| | Brute (scan cookies) | Sort + two pointers |
|---|---|---|
| Time | O(n log n + n·m) | **O(n log n + m log m)** |
| Space | O(m) | O(1) extra |

---

## 8. Edge cases and common mistakes

**Edge cases:** no cookies, more cookies than children, all cookies too small, equal sizes.

**Mistakes:**
1. Advancing `i` on a mismatch (skipping a child) instead of `j`. A cookie that's too small for child i is useless for later children, but a child who doesn't fit cookie j might still fit a later, bigger cookie.
2. Forgetting to sort one of the arrays.
3. Using `>` instead of `>=` (a cookie exactly equal to the greed satisfies the child).

---

## 9. Interview questions and answers

**Q1. Why is the greedy choice safe?**
> Exchange argument: in any optimal solution, I can give the least greedy child the smallest sufficient cookie (swapping if necessary) without lowering the count. So some optimal solution makes the greedy choice, and by induction greedy is optimal.

**Q2. Why do we discard a cookie that's too small?**
> The children are sorted by greed. If cookie j can't satisfy child i, it can't satisfy any later child either, since they're greedier.

**Q3. How do you recognise a greedy problem?**
> There's a natural order (by size, time or deadline) in which choosing the locally best option never closes off a better global solution, and you can back that up with an exchange argument. If you can find a counterexample, use DP instead.

### Follow-up questions

**F1. Each child may get multiple cookies whose sizes add up.**
> That's a bin-packing / assignment problem, which is NP-hard in general. Use DP for small sizes or heuristics.

**F2. Boats to Save People (LC 881): each boat carries ≤ 2 people with a weight limit.**
> Sort, two pointers from both ends: pair the heaviest with the lightest if they fit, otherwise the heaviest goes alone. A greedy with an exchange argument.

**F3. Maximise satisfied children when cookies have costs and you have a budget.**
> A knapsack-like DP, unless the costs have a special structure.

**F4. Other classic greedy problems.**
> Interval scheduling (sort by end time), Jump Game (track the furthest reach), Gas Station, Task Scheduler. See [15-DP M05 Jump Game](../../15-Dynamic-Programming/03-Medium/M05_JumpGame.md) and [08-Heap M03 Task Scheduler](../../08-Heap/03-Medium/M03_TaskScheduler.md).

---

## 10. Running the file

```bash
java E25_AssignCookies.java      # 4/4 tests passed
```

**One-line summary:** *"Sort both; give each least-greedy child the smallest cookie that fits; a too-small cookie is useless for everyone left."*
