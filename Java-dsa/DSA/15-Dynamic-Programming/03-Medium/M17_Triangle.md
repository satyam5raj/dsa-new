# M17 · Triangle

| | |
|---|---|
| **LeetCode** | [#120 Triangle](https://leetcode.com/problems/triangle/) |
| **Difficulty** | Medium |
| **Pattern** | Grid DP filled **bottom-up** (choosing the direction removes the edge cases) |
| **Code** | [M17_Triangle.java](M17_Triangle.java) |
| **Run** | `java M17_Triangle.java` |

The DP recipe is explained in [E01](../02-Easy/E01_ClimbingStairs.md#3-java-concepts-used-in-this-problem-and-the-dp-recipe); a rectangular min-sum grid is [M16](M16_MinimumPathSum.md).

---

## 1. The problem

Find the minimum path sum from the top of a triangle to the bottom. From index i in one row you may move to index i or i + 1 in the next row.

```
      2
     3 4
    6 5 7
   4 1 8 3          →  11   (2 + 3 + 5 + 1)
```

| triangle | answer |
|---|---|
| [[2],[3,4],[6,5,7],[4,1,8,3]] | 11 |
| [[−10]] | −10 |

---

## 2. How to think about it

### Top-down direction
dp[r][i] = t[r][i] + min(dp[r − 1][i − 1], dp[r − 1][i]) needs care at **both** edges (i = 0 has no left parent, i = r has no right parent), and the answer is the minimum of the whole last row.

### Bottom-up direction is cleaner

| step | for this problem |
|---|---|
| **State** | dp[i] = the minimum path sum from (r, i) **down** to the bottom |
| **Transition** | dp[i] = t[r][i] + min(dp[i], dp[i + 1]) (the two cells below) |
| **Base** | the row below the last one: all 0 |
| **Order** | rows from the bottom up |
| **Answer** | dp[0]: a single apex, no final minimum, no edge cases |

Updating dp[i] left to right is safe: when dp[i] is computed, dp[i + 1] still holds the row below.

### Dry run

| after row | dp |
|---|---|
| (base) | [0, 0, 0, 0, 0] |
| [4, 1, 8, 3] | [4, 1, 8, 3, 0] |
| [6, 5, 7] | [6+1 = 7, 5+1 = 6, 7+3 = 10, …] |
| [3, 4] | [3+6 = 9, 4+6 = 10, …] |
| [2] | [2+9 = **11**, …] |

---

## 3. Java concepts used in this problem

### 3.1 One extra cell instead of a special case

```java
int[] dp = new int[n + 1];                        // one extra 0 so the last row needs no special case
```

Processing the bottom row reads dp[i + 1] for i up to n − 1, so an array of n + 1 zeros acts as a virtual row of zeros below the triangle.

### 3.2 A list of lists

`triangle.get(r).get(i)` reads row r, position i. Each `get` returns an `Integer` that's unboxed automatically in the arithmetic.

### 3.3 `List.of(...)` for test data

`List.of(List.of(2), List.of(3, 4), ...)` builds an immutable nested list in one expression.

---

## 4. Approach 1: Recursion from the apex (the referee)

### Code ([M17_Triangle.java:43-46](M17_Triangle.java#L43-L46))

```java
static int brute(List<List<Integer>> t, int r, int i) {
    if (r == t.size()) return 0;
    return t.get(r).get(i) + Math.min(brute(t, r + 1, i), brute(t, r + 1, i + 1));
}
```

The bottom-up recurrence written top-down: the cost from (r, i) is its value plus the cheaper of the two paths below. **O(2ⁿ)** without memo.

---

## 5. Approach 2: Bottom-up with one row (the file's main solution)

### Code ([M17_Triangle.java:35-41](M17_Triangle.java#L35-L41))

```java
static int minimumTotal(List<List<Integer>> triangle) {
    int n = triangle.size();
    int[] dp = new int[n + 1];                        // one extra 0 so the last row needs no special case
    for (int r = n - 1; r >= 0; r--)
        for (int i = 0; i <= r; i++) dp[i] = triangle.get(r).get(i) + Math.min(dp[i], dp[i + 1]);
    return dp[0];
}
```

| Line | What it does |
|---|---|
| `dp` of n + 1 zeros | the virtual row below the bottom |
| the outer loop | from the last row up to the apex |
| the inner loop | row r has r + 1 cells |
| `dp[0]` | the best from the apex |

- **Time O(n²)** (the number of cells), **space O(n).**

---

## 6. Comparing the approaches

| | Recursion | Top-down DP | Bottom-up DP |
|---|---|---|---|
| Time | O(2ⁿ) | O(n²) | O(n²) |
| Edge cases | | both ends of each row | **none** |
| Final step | | min over the last row | dp[0] |

The file checks the bottom-up DP against the recursion on 500 random triangles with negative values.

---

## 7. Edge cases and common mistakes

**Edge cases:** one row, negative numbers (the minimum can be negative), all equal values.

**Mistakes:**
1. Greedy (picking the smaller child each time).
2. Updating right to left in the bottom-up version: dp[i + 1] would already hold the current row's value when dp[i] reads it.
3. Forgetting the extra slot and reading past the array at the bottom row.

---

## 8. Interview questions and answers

**Q1. Why go bottom-up?**
> Every cell has exactly two children, but the edge cells have only one parent. Going upward means every transition has both inputs, and the apex is a single answer.

**Q2. Can you do it in O(1) extra space?**
> Yes, by overwriting the triangle itself bottom-up, if modifying the input is allowed.

### Follow-up questions

**F1. Minimum Falling Path Sum.** → [M18](M18_MinimumFallingPathSum.md)
> Three predecessors instead of two, on a square matrix.

**F2. Return the path.**
> Store which child was chosen for each cell and follow the choices from the apex.

**F3. Pascal's Triangle.** → [Arrays E15](../../01-Arrays/02-Easy/E15_PascalsTriangle.md)
> The same triangle shape, with + instead of min.

---

## 9. Running the file

```bash
java M17_Triangle.java      # 3/3 tests passed (including 500 random triangles vs recursion)
```

**One-line summary:** *"Fill from the bottom up with dp[i] = t[r][i] + min(dp[i], dp[i + 1]), starting from a row of zeros, so the apex dp[0] is the answer with no edge cases."*
