# M06 · Combination Sum

| | |
|---|---|
| **LeetCode** | [#39 Combination Sum](https://leetcode.com/problems/combination-sum/) |
| **Difficulty** | Medium |
| **Pattern** | Combinations with **unlimited reuse**: recurse with `i` (not `i + 1`), plus `break` on overshoot |
| **Code** | [M06_CombinationSum.java](M06_CombinationSum.java) |
| **Run** | `java M06_CombinationSum.java` |

Backtracking basics are explained in [E01](../02-Easy/E01_BinaryWatch.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

The candidates are distinct positive integers, and each may be used **any number of times**. Return all unique combinations that sum to `target`.

| candidates | target | answer |
|---|---|---|
| [2, 3, 6, 7] | 7 | [[2,2,3], [7]] |
| [2, 3, 5] | 8 | [[2,2,2,2], [2,3,3], [3,5]] |
| [2] | 1 | [] |

---

## 2. How to think about it

The combinations template from [M03](M03_Combinations.md), with two tweaks.

### 1. Reuse: recurse with `start = i`
After picking `candidates[i]`, recurse with `start = i` (not `i + 1`) so the same candidate can be picked again. We still never go **back** to smaller indices, which is what prevents [3, 2, 2] from appearing after [2, 2, 3]: every combination is built in non-decreasing order.

### 2. Bound pruning: sort, then `break`
Sort the candidates. Once `candidates[i] > remaining`, **break**: every later candidate is bigger, so none of them fits either.

```
bt(start, remaining):
    remaining == 0 → record
    for i in start..:
        if c[i] > remaining: break
        add c[i];  bt(i, remaining − c[i]);  remove
```

### Dry run: [2, 3, 6, 7], target 7

```
bt(start 0, rem 7)
├─ 2 → bt(0, 5)
│   ├─ 2 → bt(0, 3)
│   │   ├─ 2 → bt(0, 1): 2 > 1 break
│   │   ├─ 3 → bt(1, 0): record [2, 2, 3] ✓
│   │   └─ 6 > 3 break
│   ├─ 3 → bt(1, 2): 3 > 2 break
│   └─ 6 > 5 break
├─ 3 → bt(1, 4)
│   ├─ 3 → bt(1, 1): 3 > 1 break
│   └─ 6 > 4 break
├─ 6 → bt(2, 1): 6 > 1 break
└─ 7 → bt(3, 0): record [7] ✓
```

---

## 3. Java concepts used in this problem

### 3.1 `break` versus `continue` on sorted candidates

`break` leaves the loop entirely. That's only correct because the array is sorted: if `c[i]` is too big, so is everything after it. On an unsorted array you'd have to `continue` instead and check every candidate.

### 3.2 Passing the remaining target down

`bt(c, i, remaining - c[i], path, out)` gives the child a new value. The parent's `remaining` is unchanged, so there's nothing to undo for it (only the `path` needs the explicit remove).

### 3.3 Summing a list with a stream (in the test)

```java
l.stream().mapToInt(Integer::intValue).sum()
```

`stream()` turns the list into a stream, `mapToInt(Integer::intValue)` unboxes each `Integer` into an `int`, and `sum()` adds them.

### 3.4 `.distinct()` on an `IntStream`

`rnd.ints(count, 2, 12).distinct().toArray()` drops repeated values, because this problem requires distinct candidates.

---

## 4. Approach 1: Count with DP (the referee)

### Code ([M06_CombinationSum.java:59-64](M06_CombinationSum.java#L59-L64))

```java
static long countWays(int[] c, int target) {
    long[] ways = new long[target + 1];
    ways[0] = 1;
    for (int coin : c) for (int s = coin; s <= target; s++) ways[s] += ways[s - coin];
    return ways[target];
}
```

This counts the combinations without listing them (it's Coin Change II, a DP problem from topic 15). `ways[s]` is the number of multisets of the candidates processed so far that sum to s. Putting the candidates in the **outer** loop counts each multiset once (not each ordering).

The test checks that the backtracking produces exactly `countWays` lists, all distinct, each summing to the target.

---

## 5. Approach 2: Backtracking with reuse (the file's main solution)

### Code ([M06_CombinationSum.java:40-56](M06_CombinationSum.java#L40-L56))

```java
static List<List<Integer>> combinationSum(int[] candidates, int target) {
    int[] c = candidates.clone();
    Arrays.sort(c);
    List<List<Integer>> out = new ArrayList<>();
    bt(c, 0, target, new ArrayList<>(), out);
    return out;
}

static void bt(int[] c, int start, int remaining, List<Integer> path, List<List<Integer>> out) {
    if (remaining == 0) { out.add(new ArrayList<>(path)); return; }
    for (int i = start; i < c.length; i++) {
        if (c[i] > remaining) break;                       // sorted: nothing later fits either
        path.add(c[i]);
        bt(c, i, remaining - c[i], path, out);             // i, not i + 1: reuse allowed
        path.remove(path.size() - 1);
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| sort a clone | needed for the `break` |
| `remaining == 0` | the path sums exactly to the target |
| the `break` | the bound pruning |
| `bt(c, i, ...)` | the same candidate may be chosen again |
| `path.remove(...)` | un-choose |

`remaining` can never go negative, because the `break` stops before any candidate that's too big.

### Complexity
- **Time:** exponential. In the worst case the depth is T / min (using the smallest candidate repeatedly) and each level branches up to N ways: O(N^(T/min + 1)).
- **Space O(T / min)** for the recursion depth and the path.

---

## 6. Comparing the approaches

| | DP count | Backtracking |
|---|---|---|
| Output | how many | the actual lists |
| Time | O(N · T) | exponential (output-sensitive) |

The file checks 300 random cases: the number of lists equals the DP count, all lists are distinct, and every one sums to the target.

---

## 7. Edge cases and common mistakes

**Edge cases:** no solution, the target equal to a candidate, a single candidate that divides the target.

**Mistakes:**
1. Recursing with `i + 1` (no reuse: [2, 2, 3] is lost).
2. Recursing with `0` (every ordering appears: [2, 3, 2], [3, 2, 2], …).
3. `break` without sorting first.
4. Recording when `remaining < 0` instead of preventing it.

---

## 8. Interview questions and answers

**Q1. Why `start = i` and not `0`?**
> With `0`, the same multiset would be generated in every order. With `i`, each combination is built in non-decreasing order, which happens exactly once, while still allowing repeats of the current candidate.

**Q2. When is `break` valid?**
> Only when the candidates are sorted and positive: then a too-big candidate means every later one is too big as well.

### Follow-up questions

**F1. Combination Sum II (each element once, duplicates in the input).** → [M07](M07_CombinationSumII.md)

**F2. Count the combinations only.**
> Coin Change II DP (the referee above): O(N · T).

**F3. Combination Sum IV (LC 377): count ordered sequences.**
> Put the target in the **outer** loop and the candidates in the inner one, so different orders count separately.

---

## 9. Running the file

```bash
java M06_CombinationSum.java      # 4/4 tests passed (including 300 random cases: count == DP count, sums correct)
```

**One-line summary:** *"Sort the candidates, recurse with the same index to allow reuse, record when the remaining target hits 0, and break as soon as a candidate exceeds what remains."*
