# H03 · Russian Doll Envelopes

| | |
|---|---|
| **LeetCode** | [#354 Russian Doll Envelopes](https://leetcode.com/problems/russian-doll-envelopes/) |
| **Difficulty** | Hard |
| **Pattern** | 2-D LIS → **sort cleverly**, then a 1-D LIS in O(n log n) |
| **Code** | [H03_RussianDollEnvelopes.java](H03_RussianDollEnvelopes.java) |
| **Run** | `java H03_RussianDollEnvelopes.java` |

The LIS and its O(n log n) "tails" version are explained in [M19](../03-Medium/M19_LongestIncreasingSubsequence.md).

---

## 1. The problem

Envelope (w, h) fits into another if **both** the width and the height are strictly larger. What's the maximum number of envelopes you can nest?

| envelopes | answer | chain |
|---|---|---|
| [[5,4],[6,4],[6,7],[2,3]] | 3 | [2,3] → [5,4] → [6,7] |
| [[1,1],[1,1],[1,1]] | 1 | |
| [[3,3],[3,4]] | 1 | same width: neither fits in the other |

---

## 2. How to think about it

### O(n²) DP
Sort by width, then run LIS where j → i requires w[j] < w[i] **and** h[j] < h[i]. LeetCode has up to 10⁵ envelopes, so that's too slow.

### Sort by width ascending, and equal widths by height **descending**
Then a plain strictly increasing LIS on the **heights alone** gives the answer:
- With widths ascending, any increasing-height subsequence also has non-decreasing widths.
- Envelopes with **equal** widths are sorted by height descending, so two of them can never both appear in a strictly increasing height sequence. So two envelopes with the same width are never chained, which is exactly the "strictly larger width" rule.

Without the descending tie-break, [3,3] and [3,4] would wrongly chain (heights 3, 4 are increasing).

### Dry run: [[5,4],[6,4],[6,7],[2,3]]

Sorted: [2,3], [5,4], [6,7], [6,4] (the two width-6 envelopes in **descending** height). Heights: 3, 4, 7, 4.

| height | tails |
|---|---|
| 3 | [3] |
| 4 | [3, 4] |
| 7 | [3, 4, 7] |
| 4 | [3, 4, 7] (4 replaces the existing 4) |

Length **3**.

### The bug, shown
With heights **ascending** for equal widths, [[3,3],[3,4]] gives heights 3, 4 → LIS 2, which is wrong. The file keeps that version to demonstrate it.

---

## 3. Java concepts used in this problem

### 3.1 A comparator with a tie-break

```java
Arrays.sort(e, (a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(b[1], a[1]));
```

- First compare the widths ascending.
- If they're equal, compare the heights with the arguments swapped (`b[1], a[1]`), which sorts descending.
- `Integer.compare` avoids the overflow risk of `a[0] − b[0]` (harmless here with small values, but a good habit).

### 3.2 Cloning before sorting

`envelopes.clone()` copies the outer array so the caller's order isn't changed. The inner `int[]` pairs are shared, which is fine because they're never modified.

### 3.3 The lower-bound search for a strict LIS

`if (tails[mid] < h) lo = mid + 1; else hi = mid;` finds the first tail ≥ h, exactly as in [M19](../03-Medium/M19_LongestIncreasingSubsequence.md#31-lower-bound-binary-search).

---

## 4. Approach 1: O(n²) DP with both conditions (the referee)

### Code ([H03_RussianDollEnvelopes.java:52-63](H03_RussianDollEnvelopes.java#L52-L63))

```java
static int quadratic(int[][] env) {                  // O(n^2) DP with both conditions
    int[][] e = env.clone();
    Arrays.sort(e, (a, b) -> a[0] - b[0]);
    int[] dp = new int[e.length];
    int best = 0;
    for (int i = 0; i < e.length; i++) {
        dp[i] = 1;
        for (int j = 0; j < i; j++) if (e[j][0] < e[i][0] && e[j][1] < e[i][1]) dp[i] = Math.max(dp[i], dp[j] + 1);
        best = Math.max(best, dp[i]);
    }
    return best;
}
```

LIS with the 2-D "fits inside" test checked directly. **O(n²).**

---

## 5. Approach 2: The wrong tie-break (kept as a warning)

### Code ([H03_RussianDollEnvelopes.java:65-72](H03_RussianDollEnvelopes.java#L65-L72))

The same as the main solution, but sorting equal widths by height **ascending**. On [[3,3],[3,4]] it returns 2.

---

## 6. Approach 3: Sort + LIS on heights (the file's main solution)

### Code ([H03_RussianDollEnvelopes.java:38-50](H03_RussianDollEnvelopes.java#L38-L50))

```java
static int maxEnvelopes(int[][] envelopes) {
    int[][] e = envelopes.clone();
    Arrays.sort(e, (a, b) -> a[0] != b[0] ? Integer.compare(a[0], b[0]) : Integer.compare(b[1], a[1]));
    int[] tails = new int[e.length];
    int len = 0;
    for (int[] env : e) {                             // strictly increasing LIS on heights
        int h = env[1], lo = 0, hi = len;
        while (lo < hi) { int mid = (lo + hi) >>> 1; if (tails[mid] < h) lo = mid + 1; else hi = mid; }
        tails[lo] = h;
        if (lo == len) len++;
    }
    return len;
}
```

| Line | What it does |
|---|---|
| the sort | width ascending, height descending on ties |
| the loop | the tails LIS over the heights |

### Complexity
- **Time O(n log n), space O(n).**

---

## 7. Comparing the approaches

| | O(n²) DP | Sort + tails |
|---|---|---|
| Time | O(n²) | **O(n log n)** |
| Handles equal widths | by the explicit check | by the descending tie-break |

The file checks the fast version against the O(n²) DP on 1,000 random sets (with many equal widths and heights).

---

## 8. Edge cases and common mistakes

**Edge cases:** all envelopes identical (1), all the same width (1), all the same height (1).

**Mistakes:**
1. The ascending tie-break.
2. A non-strict LIS (`≤`).
3. Sorting by area.

---

## 9. Interview questions and answers

**Q1. Why does the descending tie-break work?**
> In a strictly increasing height sequence, a later envelope must be taller. Among equal widths, the later ones are shorter, so at most one envelope of each width can be chosen, matching the "strictly wider" requirement.

**Q2. What about 3-D boxes?**
> Sorting only reduces one dimension. For 3-D you'd sort by one and run an O(n²) DP (or a 2-D data structure, like a Fenwick tree over one coordinate) on the other two.

### Follow-up questions

**F1. Longest Increasing Subsequence.** → [M19](../03-Medium/M19_LongestIncreasingSubsequence.md)

**F2. Maximum Height by Stacking Cuboids (LC 1691).**
> Sort each cuboid's dimensions, then sort the cuboids, then O(n²) DP.

**F3. Return the chain of envelopes.**
> Keep the index of each tail and a parent pointer, as described in [M19's Q2](../03-Medium/M19_LongestIncreasingSubsequence.md#9-interview-questions-and-answers).

---

## 10. Running the file

```bash
java H03_RussianDollEnvelopes.java      # 5/5 tests passed (including the tie-break bug demo and 1000 random vs O(n^2))
```

**One-line summary:** *"Sort by width ascending and, for equal widths, height descending; then the answer is the strictly increasing LIS of the heights, found with tails and binary search."*
