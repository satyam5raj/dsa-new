# M07 · Combination Sum II

| | |
|---|---|
| **LeetCode** | [#40 Combination Sum II](https://leetcode.com/problems/combination-sum-ii/) |
| **Difficulty** | Medium |
| **Pattern** | Each element **once** (`i + 1`) + **duplicate skipping** + `break` on overshoot |
| **Code** | [M07_CombinationSumII.java](M07_CombinationSumII.java) |
| **Run** | `java M07_CombinationSumII.java` |

Backtracking basics are explained in [E01](../02-Easy/E01_BinaryWatch.md#3-java-concepts-used-in-this-problem). This combines [M06 Combination Sum](M06_CombinationSum.md) with the skip rule from [M02 Subsets II](M02_SubsetsII.md).

---

## 1. The problem

The candidates may contain duplicates, and each element may be used **at most once**. Return all unique combinations that sum to `target`.

| candidates | target | answer |
|---|---|---|
| [10, 1, 2, 7, 6, 1, 5] | 8 | [[1,1,6], [1,2,5], [1,7], [2,6]] |
| [2, 5, 2, 1, 2] | 5 | [[1,2,2], [5]] |

---

## 2. How to think about it

[M06](M06_CombinationSum.md) with two changes:
1. Recurse with **`i + 1`**, so each element is used at most once.
2. Sort, and at the same level **skip equal values**:
   ```java
   if (i > start && c[i] == c[i - 1]) continue;
   ```
   Without it, [1 (the first one), 7] and [1 (the second one), 7] would both appear.

Plus the same `break` once `c[i] > remaining`.

This is the template behind most "unique combinations from an input with duplicates" problems.

### Dry run: sorted [1, 1, 2, 5, 6, 7, 10], target 8

```
bt(start 0, rem 8)
├─ 1 (i=0) → bt(1, 7)
│   ├─ 1 (i=1) → bt(2, 6)
│   │   ├─ 2 → bt(3, 4): 5 > 4 break
│   │   ├─ 5 → bt(4, 1): 6 > 1 break
│   │   ├─ 6 → bt(5, 0): record [1, 1, 6] ✓
│   │   └─ 7 > 6 break
│   ├─ 2 → bt(3, 5)
│   │   ├─ 5 → bt(4, 0): record [1, 2, 5] ✓
│   │   └─ 6 > 5 break
│   ├─ 5 → bt(4, 2): 6 > 2 break
│   ├─ 6 → bt(5, 1): 7 > 1 break
│   ├─ 7 → bt(6, 0): record [1, 7] ✓
│   └─ 10 > 7 break
├─ 1 (i=1): SKIPPED (i > start and equal to c[0])
├─ 2 → bt(3, 6)
│   ├─ 5 → bt(4, 1): break
│   ├─ 6 → bt(5, 0): record [2, 6] ✓
│   └─ 7 > 6 break
├─ 5 → bt(4, 3): 6 > 3 break
├─ 6 → bt(5, 2): 7 > 2 break
├─ 7 → bt(6, 1): 10 > 1 break
└─ 10 > 8 break
```

Note that the second 1 **is** used at depth 1 (inside the first 1's subtree, where i == start), which is how [1, 1, 6] is found. It's only skipped at depth 0.

---

## 3. Java concepts used in this problem

### 3.1 Two loop guards: `continue`, then `break`

```java
if (i > start && c[i] == c[i - 1]) continue;       // duplicate at this level
if (c[i] > remaining) break;
```

Both are safe in either order here (an equal duplicate is also too big if the previous one was), but checking the duplicate first reads naturally: "skip repeats, stop when too big".

### 3.2 Bitmask enumeration with a running sum (the referee)

```java
for (int i = 0; i < c.length; i++) if ((mask >> i & 1) == 1) { x.add(c[i]); sum += c[i]; }
```

Every subset is built and summed. The ones that hit the target are sorted and put into a set to remove duplicates.

---

## 4. Approach 1: All subsets with the right sum (the referee)

### Code ([M07_CombinationSumII.java:54-63](M07_CombinationSumII.java#L54-L63))

```java
static Set<List<Integer>> brute(int[] c, int target) {
    Set<List<Integer>> s = new HashSet<>();
    for (int mask = 0; mask < (1 << c.length); mask++) {
        List<Integer> x = new ArrayList<>();
        int sum = 0;
        for (int i = 0; i < c.length; i++) if ((mask >> i & 1) == 1) { x.add(c[i]); sum += c[i]; }
        if (sum == target) { Collections.sort(x); s.add(x); }
    }
    return s;
}
```

- **Time O(n · 2ⁿ)** always, generating every duplicate before removing it.

---

## 5. Approach 2: Sort + skip + break (the file's main solution)

### Code ([M07_CombinationSumII.java:35-52](M07_CombinationSumII.java#L35-L52))

```java
static List<List<Integer>> combinationSum2(int[] candidates, int target) {
    int[] c = candidates.clone();
    Arrays.sort(c);
    List<List<Integer>> out = new ArrayList<>();
    bt(c, 0, target, new ArrayList<>(), out);
    return out;
}

static void bt(int[] c, int start, int remaining, List<Integer> path, List<List<Integer>> out) {
    if (remaining == 0) { out.add(new ArrayList<>(path)); return; }
    for (int i = start; i < c.length; i++) {
        if (i > start && c[i] == c[i - 1]) continue;       // duplicate at this level
        if (c[i] > remaining) break;
        path.add(c[i]);
        bt(c, i + 1, remaining - c[i], path, out);         // each element once
        path.remove(path.size() - 1);
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| sort | groups duplicates and enables `break` |
| `remaining == 0` | record |
| the skip | the same value at the same level would rebuild the same combinations |
| the `break` | everything from here on is too big |
| `bt(c, i + 1, ...)` | each element at most once |

### Complexity
- **Time O(n · 2ⁿ)** in the worst case, usually far less thanks to the pruning.
- **Space O(n).**

---

## 6. Comparing the problems in this family

| problem | reuse? | duplicates in the input? | recurse with | skip rule |
|---|---|---|---|---|
| [M06](M06_CombinationSum.md) | yes | no | `i` | none |
| **M07** | no | yes | `i + 1` | `i > start && c[i] == c[i−1]` |
| [M02](M02_SubsetsII.md) | no | yes | `i + 1` | the same |

The file checks the fast version against the brute force on 500 random cases with values 1–7 (many duplicates).

---

## 7. Edge cases and common mistakes

**Edge cases:** all candidates equal, no solution, the target equal to a single element, the target needing every element.

**Mistakes:**
1. Recursing with `i` (an element gets reused).
2. Skipping with `i > 0` instead of `i > start` (loses [1, 1, 6]).
3. Deduplicating with a `Set` at the end instead of skipping (correct, but slow).

---

## 8. Interview questions and answers

**Q1. What's the difference between M06 and M07 in one line?**
> M06: distinct candidates, unlimited reuse, recurse with `i`. M07: duplicates in the input, each used once, recurse with `i + 1` and skip equal values at the same level.

**Q2. Why does the skip rule still allow [1, 1, 6]?**
> The second 1 is skipped only when it's a later candidate at the **same** level as the first 1. One level deeper, after the first 1 is chosen, it's the first candidate (`i == start`), so it's allowed.

### Follow-up questions

**F1. Combination Sum III (LC 216).**
> k distinct numbers from 1..9 summing to n: the combinations template with a size limit and a target.

**F2. Partition to K Equal Sum Subsets.** → [M12](M12_PartitionToKEqualSumSubsets.md)
> Several targets at once.

**F3. Count only.**
> 0/1 knapsack-style DP over distinct values with their counts.

---

## 9. Running the file

```bash
java M07_CombinationSumII.java      # 3/3 tests passed (including 500 random cases vs all subsets)
```

**One-line summary:** *"Sort, recurse with i + 1 so each element is used once, skip equal values at the same level (i > start), and break once a candidate exceeds the remaining target."*
