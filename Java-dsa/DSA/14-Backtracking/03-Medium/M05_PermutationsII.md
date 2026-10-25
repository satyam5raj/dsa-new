# M05 · Permutations II

| | |
|---|---|
| **LeetCode** | [#47 Permutations II](https://leetcode.com/problems/permutations-ii/) |
| **Difficulty** | Medium |
| **Pattern** | Permutations + duplicates → sort, and skip a value if its equal twin is **unused** |
| **Code** | [M05_PermutationsII.java](M05_PermutationsII.java) |
| **Run** | `java M05_PermutationsII.java` |

Backtracking basics are explained in [E01](../02-Easy/E01_BinaryWatch.md#3-java-concepts-used-in-this-problem); plain permutations are in [M04](M04_Permutations.md).

---

## 1. The problem

`nums` may contain duplicates. Return all **distinct** permutations.

| nums | answer |
|---|---|
| [1, 1, 2] | [[1,1,2], [1,2,1], [2,1,1]] |
| [1, 2, 3] | 6 permutations |
| [2, 2, 2] | [[2,2,2]] |

---

## 2. How to think about it

### Why duplicates appear
Label the equal values 1a and 1b. Then [1a, 1b, 2] and [1b, 1a, 2] are different orderings of the indices but the **same** permutation of values. Plain [M04](M04_Permutations.md) would produce both.

### Force equal values to be used in order
If 1a must always be placed before 1b, each arrangement of the values appears exactly once. After sorting (so equal values are adjacent):

```java
if (i > 0 && a[i] == a[i - 1] && !used[i - 1]) continue;
```

"Don't use the second 1 unless the first 1 is already in the path."

### Why `!used[i − 1]` rather than `used[i − 1]`?
Both conditions give correct output: one forces equal values to be used left to right, the other right to left. But `!used[i − 1]` prunes at the **top** of the tree, while `used[i − 1]` only discovers the duplicates deep down. Measured on [1, 1, 1, 1, 1, 1, 2, 2] (28 distinct permutations):

| condition | nodes visited |
|---|---|
| `!used[i − 1]` | **119** |
| `used[i − 1]` | 4,830 |

### Dry run: [1, 1, 2] (sorted, indices 0, 1, 2)

| path so far | candidate i | used before | decision |
|---|---|---|---|
| [] | 0 (1) | none | take → [1] |
| [1] (i=0 used) | 1 (1) | used[0] = true | allowed (its twin is used) → [1, 1] → [1, 1, 2] ✓ |
| [1] | 2 (2) | | take → [1, 2] → then i=1 (1): twin used → [1, 2, 1] ✓ |
| [] | 1 (1) | used[0] = false | **skip**: twin unused |
| [] | 2 (2) | | take → [2] → i=0 → [2, 1]; i=1 (twin used) → [2, 1, 1] ✓ |
| [2] | 1 (1) | used[0] = false | **skip** (would give [2, 1b, 1a]) |

Result: [[1,1,2], [1,2,1], [2,1,1]] ✓

---

## 3. Java concepts used in this problem

### 3.1 Two `continue` conditions

```java
if (used[i]) continue;
if (i > 0 && a[i] == a[i - 1] && !used[i - 1]) continue;
```

The first is the normal permutation rule. The second is the duplicate rule. `i > 0` comes first so that `a[i − 1]` is never read out of bounds: `&&` stops as soon as one part is false.

### 3.2 Sorting a clone

`int[] a = nums.clone(); Arrays.sort(a);` groups the equal values without changing the caller's array.

### 3.3 A `Set` for the referee

The referee generates **all** n! index permutations by swapping and puts each one into a `HashSet<List<Integer>>`, which keeps one copy of each distinct value list.

---

## 4. Approach 1: All permutations, deduplicated (the referee)

### Code ([M05_PermutationsII.java:58-71](M05_PermutationsII.java#L58-L71))

```java
static Set<List<Integer>> brute(int[] a) {
    Set<List<Integer>> s = new HashSet<>();
    perm(a, 0, s);
    return s;
}

static void perm(int[] a, int pos, Set<List<Integer>> s) {
    if (pos == a.length) { List<Integer> p = new ArrayList<>(); for (int x : a) p.add(x); s.add(p); return; }
    for (int i = pos; i < a.length; i++) {
        int t = a[pos]; a[pos] = a[i]; a[i] = t;
        perm(a, pos + 1, s);
        t = a[pos]; a[pos] = a[i]; a[i] = t;
    }
}
```

The swap version from [M04](M04_Permutations.md#5-approach-2-swap-in-place), with the results put into a set.

- **Time O(n · n!)** regardless of how many duplicates there are.

---

## 5. Approach 2: Sort + "use equal values in order" (the file's main solution)

### Code ([M05_PermutationsII.java:36-55](M05_PermutationsII.java#L36-L55))

```java
static List<List<Integer>> permuteUnique(int[] nums) {
    int[] a = nums.clone();
    Arrays.sort(a);
    List<List<Integer>> out = new ArrayList<>();
    bt(a, new boolean[a.length], new ArrayList<>(), out);
    return out;
}

static void bt(int[] a, boolean[] used, List<Integer> path, List<List<Integer>> out) {
    if (path.size() == a.length) { out.add(new ArrayList<>(path)); return; }
    for (int i = 0; i < a.length; i++) {
        if (used[i]) continue;
        if (i > 0 && a[i] == a[i - 1] && !used[i - 1]) continue;   // use equal values in order only
        used[i] = true;
        path.add(a[i]);
        bt(a, used, path, out);
        path.remove(path.size() - 1);
        used[i] = false;
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| sort | equal values become neighbours |
| `used[i]` | already placed |
| the duplicate rule | a copy may only be used after the copy before it |
| the rest | the [M04](M04_Permutations.md) template |

### Complexity
- **Time O(n · n!)** in the worst case (all distinct); far less with many duplicates, and it never generates a duplicate.
- **Space O(n).**

---

## 6. Comparing the approaches

| | All + set | Sort + skip |
|---|---|---|
| Generates duplicates? | yes | **never** |
| Nodes for [1,1,1,1,1,1,2,2] | all 8! = 40,320 leaves | 119 nodes |

The file checks the fast version against the deduplicated brute force on 300 random arrays with values 0–2.

---

## 7. Edge cases and common mistakes

**Edge cases:** all values equal (one permutation), no duplicates (n! permutations), a single element.

**Mistakes:**
1. Forgetting to sort.
2. Using the M02 rule `i > start` (permutations have no `start`).
3. Omitting the `!used[i − 1]` part (then the second copy is never used at all, and permutations are lost).
4. Applying the duplicate rule in the swap version (it doesn't work there; the swap version needs a per-level `HashSet` of tried values).

---

## 8. Interview questions and answers

**Q1. Explain `!used[i − 1]`.**
> Among equal values, we only allow them to be placed in index order. If the previous copy isn't in the path yet, using this copy now would start an ordering that's just a relabelling of one we'll generate anyway.

**Q2. Why is `!used[i − 1]` faster than `used[i − 1]`?**
> `!used[i − 1]` cuts duplicate branches at the first level where they could start. `used[i − 1]` allows them to begin and only blocks them later, after much of the subtree has been explored.

### Follow-up questions

**F1. Count the distinct permutations without listing them.**
> n! / (c₁! · c₂! · …) for the value counts cᵢ.

**F2. Subsets II.** → [M02](M02_SubsetsII.md)
> The same "sort and skip equal values" idea for subsets, with `i > start`.

**F3. Palindrome Permutation II (LC 267).**
> Build half of the palindrome from half the counts, generate its distinct permutations with this template, and mirror each one.

---

## 9. Running the file

```bash
java M05_PermutationsII.java      # 4/4 tests passed (including 300 random arrays vs deduplicated brute force)
```

**One-line summary:** *"Sort, then run the permutations template but skip a value when it equals the previous value and that previous copy is not yet used, so equal values are always placed in order."*
