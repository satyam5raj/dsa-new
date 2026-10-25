# M03 · Combinations ⭐

| | |
|---|---|
| **LeetCode** | [#77 Combinations](https://leetcode.com/problems/combinations/) |
| **Difficulty** | Medium |
| **Pattern** | The **combinations** family: fixed size k, loop from `start`, **feasibility pruning** |
| **Code** | [M03_Combinations.java](M03_Combinations.java) |
| **Run** | `java M03_Combinations.java` |

⭐ = not in `leetcode.md`, added for a key technique. Backtracking basics are explained in [E01](../02-Easy/E01_BinaryWatch.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Return all combinations of k numbers chosen from 1..n.

| n | k | answer |
|---|---|---|
| 4 | 2 | [[1,2], [1,3], [1,4], [2,3], [2,4], [3,4]] |
| 1 | 1 | [[1]] |

---

## 2. How to think about it

### Subsets that stop at size k
It's [M01 Subsets](M01_Subsets.md), but only recording (and stopping) when the path has k numbers:

```
bt(start):
    if path.size() == k: record, return
    for i in start..n:  add i;  bt(i + 1);  remove
```

### Feasibility pruning
Suppose we still need `need = k − path.size()` numbers. Starting at i leaves the numbers i..n, which is n − i + 1 of them. If that's fewer than `need`, no completion exists. So the loop only has to run while

```
i ≤ n − need + 1
```

For n = 20, k = 10 the file measured **616,666** nodes without this pruning and **352,716** with it.

### The tree for n = 4, k = 2 with pruning

```
                   []                    need 2 → i runs 1..3 (4 alone can't make a pair)
         /          |          \
       [1]         [2]         [3]       need 1 → i runs start..4
     /  |  \       /  \          |
 [1,2][1,3][1,4] [2,3][2,4]    [3,4]
```

Without pruning, the root would also try [4], which has no children (start = 5 > 4): a wasted node. The deeper the tree and the larger k, the more such dead branches there are.

### Dry run: n = 4, k = 2

| path | need | loop range | records |
|---|---|---|---|
| [] | 2 | 1..3 | |
| [1] | 1 | 2..4 | [1,2], [1,3], [1,4] |
| [2] | 1 | 3..4 | [2,3], [2,4] |
| [3] | 1 | 4..4 | [3,4] |

---

## 3. Java concepts used in this problem

### 3.1 Computing the loop bound once

```java
int last = prune ? n - (k - path.size()) + 1 : n;
for (int i = start; i <= last; i++) { ... }
```

The bound depends only on the path size, which is the same for every iteration of this call's loop (each child's change is undone before the next iteration), so it's computed once before the loop.

### 3.2 A static counter for measuring work

`static long nodes;` is incremented at the top of every call. Resetting it before each run lets the test compare the two versions. (It's a measurement tool, not part of the algorithm.)

### 3.3 Computing C(n, k) without overflow

```java
static long choose(int n, int k) { long r = 1; for (int i = 1; i <= k; i++) r = r * (n - k + i) / i; return r; }
```

After step i, r equals C(n − k + i, i), which is always an integer, so dividing at each step is exact. Multiplying first and dividing second keeps it exact. This avoids computing huge factorials.

---

## 4. Approach 1: Without pruning

The same code with `prune = false`: the loop always runs to n. It produces the same combinations, just visiting more dead-end nodes. Used in the file only to measure the difference.

---

## 5. Approach 2: With feasibility pruning (the file's main solution)

### Code ([M03_Combinations.java:30-47](M03_Combinations.java#L30-L47))

```java
static long nodes;

static List<List<Integer>> combine(int n, int k) {
    List<List<Integer>> out = new ArrayList<>();
    bt(n, k, 1, new ArrayList<>(), out, true);
    return out;
}

static void bt(int n, int k, int start, List<Integer> path, List<List<Integer>> out, boolean prune) {
    nodes++;
    if (path.size() == k) { out.add(new ArrayList<>(path)); return; }
    int last = prune ? n - (k - path.size()) + 1 : n;     // enough numbers left to finish?
    for (int i = start; i <= last; i++) {
        path.add(i);
        bt(n, k, i + 1, path, out, prune);
        path.remove(path.size() - 1);
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `start = 1` | the numbers are 1..n |
| `path.size() == k` | a complete combination: record a copy and stop going deeper |
| `last` | the largest i from which k − size numbers can still be picked |
| the loop | choose i, explore larger numbers only, un-choose |

### Complexity
- **Time O(k · C(n, k)):** C(n, k) results, each copied in O(k).
- **Space O(k)** recursion and path.

---

## 6. Comparing the approaches

| | Without pruning | With pruning |
|---|---|---|
| Nodes for n = 20, k = 10 | 616,666 | **352,716** |
| Output | the same | the same |

The file checks that, for every n ≤ 12 and 1 ≤ k ≤ n, the number of combinations is C(n, k) and they're all distinct.

---

## 7. Edge cases and common mistakes

**Edge cases:** k = n (one combination), k = 1 (n single-number lists), n = 1.

**Mistakes:**
1. Off-by-one in the bound (it's `n − need + 1`, inclusive).
2. Looping from 0 or 1 every time instead of `start` (permutations instead of combinations).
3. Not returning after recording (it then keeps adding numbers past size k).

---

## 8. Interview questions and answers

**Q1. Derive the pruning bound.**
> With `need` numbers still to choose and the next choice i, the numbers i..n must contain at least `need` values: n − i + 1 ≥ need, so i ≤ n − need + 1.

**Q2. How many combinations are there, and why the extra factor of k?**
> C(n, k) = n! / (k! (n − k)!). Each one costs O(k) to copy into the output.

### Follow-up questions

**F1. Combination Sum III (LC 216): k numbers from 1..9 summing to n.**
> The same template, plus a sum, with pruning when the running sum exceeds n.

**F2. Generate the combinations iteratively.**
> Keep an array of k indices and advance it like a counter (Knuth's Algorithm T / "next combination").

**F3. Combination Sum.** → [M06](M06_CombinationSum.md)
> Combinations with a target sum and reuse allowed.

---

## 9. Running the file

```bash
java M03_Combinations.java      # 4/4 tests passed; prints 616,666 vs 352,716 nodes for n=20, k=10
```

**One-line summary:** *"Run the subsets template but record and stop at size k, and only loop while enough numbers remain (i ≤ n − (k − size) + 1)."*
