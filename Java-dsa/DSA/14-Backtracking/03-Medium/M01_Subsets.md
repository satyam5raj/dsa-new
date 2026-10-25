# M01 · Subsets

| | |
|---|---|
| **LeetCode** | [#78 Subsets](https://leetcode.com/problems/subsets/) |
| **Difficulty** | Medium |
| **Pattern** | The **subsets** family: record at every node, loop from `start` |
| **Code** | [M01_Subsets.java](M01_Subsets.java) |
| **Run** | `java M01_Subsets.java` |

Backtracking basics (the template, `start`, recording a copy) are explained in [E01](../02-Easy/E01_BinaryWatch.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Given an array of **distinct** integers, return all 2ⁿ subsets (the power set), in any order.

| nums | answer |
|---|---|
| [1, 2, 3] | [[], [1], [1,2], [1,2,3], [1,3], [2], [2,3], [3]] |
| [0] | [[], [0]] |

---

## 2. How to think about it

There are four standard ways, all O(n · 2ⁿ):

1. **Backtracking with a start index:** every node of the tree is a subset; its children add one **later** element.
2. **Pick / not pick:** decide include-or-skip for each element in turn; record at depth n (as in [E02](../02-Easy/E02_SumOfAllSubsetXorTotals.md)).
3. **Bitmask:** subset number `mask` contains `a[i]` exactly when bit i of `mask` is set, for mask = 0..2ⁿ − 1.
4. **Iterative:** start with [[]]; for each element x, copy every existing subset and add x to the copy.

### The start-index tree for [1, 2, 3]

```
                    []
          /          |        \
        [1]         [2]       [3]
       /    \        |
   [1,2]   [1,3]   [2,3]
     |
  [1,2,3]
```

Every node is recorded. Looping from `start` (not from 0) is what prevents [2, 1] from appearing after [1, 2]: a child can only add elements to the right of the last one chosen.

### Dry run: backtracking on [1, 2, 3]

| step | action | path | recorded |
|---|---|---|---|
| bt(0) | record | [] | [] |
| | add 1 → bt(1) | [1] | [1] |
| | add 2 → bt(2) | [1, 2] | [1, 2] |
| | add 3 → bt(3) | [1, 2, 3] | [1, 2, 3] |
| | remove 3, remove 2; add 3 → bt(3) | [1, 3] | [1, 3] |
| | remove 3, remove 1; add 2 → bt(2) | [2] | [2] |
| | add 3 → bt(3) | [2, 3] | [2, 3] |
| | remove 3, remove 2; add 3 → bt(3) | [3] | [3] |

Output order: [], [1], [1,2], [1,2,3], [1,3], [2], [2,3], [3] (exactly the test's expected string).

### Dry run: iterative on [1, 2, 3]

| x | new subsets (copies + x) | all so far |
|---|---|---|
| (start) | | [] |
| 1 | [1] | [], [1] |
| 2 | [2], [1,2] | [], [1], [2], [1,2] |
| 3 | [3], [1,3], [2,3], [1,2,3] | all 8 |

### Bitmask example
mask = 5 = `101` → bits 0 and 2 are set → [a[0], a[2]] = [1, 3].

---

## 3. Java concepts used in this problem

### 3.1 A list of lists

`List<List<Integer>> out` holds the subsets. Each inner list must be a **separate** object, which is why every recording is `new ArrayList<>(path)` ([E01 §3.3](../02-Easy/E01_BinaryWatch.md#33-record-a-copy-of-the-path)).

### 3.2 Fixing the loop bound before modifying a list

```java
int size = out.size();
for (int i = 0; i < size; i++) { ...; out.add(s); }
```

The iterative version appends to `out` while looping over it. Saving `size` first means the loop only visits the subsets that existed before this element; otherwise it would keep visiting the new ones forever.

### 3.3 Comparing results that come in any order

```java
static Set<List<Integer>> norm(List<List<Integer>> l) { ... }   // sort each subset, put them in a set
```

`List.equals` compares element by element, and `HashSet` of lists uses that. Sorting each subset and collecting them into a set makes two answers comparable regardless of order.

---

## 4. Approach 1: Backtracking with a start index (the file's main solution)

### Code ([M01_Subsets.java:32-45](M01_Subsets.java#L32-L45))

```java
static List<List<Integer>> subsets(int[] nums) {
    List<List<Integer>> out = new ArrayList<>();
    bt(nums, 0, new ArrayList<>(), out);
    return out;
}

static void bt(int[] a, int start, List<Integer> path, List<List<Integer>> out) {
    out.add(new ArrayList<>(path));
    for (int i = start; i < a.length; i++) {
        path.add(a[i]);
        bt(a, i + 1, path, out);
        path.remove(path.size() - 1);
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `out.add(new ArrayList<>(path))` | every node is a subset, including the root's empty one |
| `for (int i = start; ...)` | the candidates are the elements after the last one chosen |
| `path.add(a[i])` | choose |
| `bt(a, i + 1, ...)` | explore subsets that extend this one with later elements |
| `path.remove(path.size() - 1)` | un-choose |

There's no explicit base case: when `start == a.length`, the loop doesn't run.

### Complexity
- **Time O(n · 2ⁿ):** 2ⁿ subsets, and copying each costs up to n.
- **Space O(n)** recursion and path, plus the output.

---

## 5. Approach 2: Pick / not pick

This version isn't in the `.java` file; it was checked against the bitmask enumeration for n = 0..12 before being included here.

```java
class PickSkip {
    static void dfs(int[] a, int i, List<Integer> path, List<List<Integer>> out) {
        if (i == a.length) { out.add(new ArrayList<>(path)); return; }
        path.add(a[i]);                       // pick a[i]
        dfs(a, i + 1, path, out);
        path.remove(path.size() - 1);         // undo
        dfs(a, i + 1, path, out);             // skip a[i]
    }
}
```

Only the leaves (i == n) are recorded. For [1, 2, 3] the output order is [1,2,3], [1,2], [1,3], [1], [2,3], [2], [3], [].

---

## 6. Approach 3: Bitmask

### Code ([M01_Subsets.java:47-55](M01_Subsets.java#L47-L55))

```java
static List<List<Integer>> bitmask(int[] a) {
    List<List<Integer>> out = new ArrayList<>();
    for (int mask = 0; mask < (1 << a.length); mask++) {
        List<Integer> s = new ArrayList<>();
        for (int i = 0; i < a.length; i++) if ((mask >> i & 1) == 1) s.add(a[i]);
        out.add(s);
    }
    return out;
}
```

Each of the 2ⁿ masks is one subset. No recursion at all. It works for n ≤ 30 (with `int` masks), which is far beyond what the output size allows anyway.

---

## 7. Approach 4: Iterative doubling

### Code ([M01_Subsets.java:57-65](M01_Subsets.java#L57-L65))

```java
static List<List<Integer>> iterative(int[] a) {
    List<List<Integer>> out = new ArrayList<>();
    out.add(new ArrayList<>());
    for (int x : a) {
        int size = out.size();
        for (int i = 0; i < size; i++) { List<Integer> s = new ArrayList<>(out.get(i)); s.add(x); out.add(s); }
    }
    return out;
}
```

Each element doubles the number of subsets: the old ones (without x) plus a copy of each with x. The dry run is in section 2.

---

## 8. Comparing the approaches

| | Start index | Pick / not pick | Bitmask | Iterative |
|---|---|---|---|---|
| Time | O(n · 2ⁿ) | O(n · 2ⁿ) | O(n · 2ⁿ) | O(n · 2ⁿ) |
| Recursion | yes | yes | no | no |
| Extends to duplicates (M02)? | **easily** | awkward | no | with care |

The file checks the backtracking, bitmask and iterative versions against each other for n = 0..10 (also checking there are exactly 2ⁿ distinct subsets).

---

## 9. Edge cases and common mistakes

**Edge cases:** an empty array ([[]]), a single element, negative numbers.

**Mistakes:**
1. `out.add(path)` without copying.
2. Looping from 0 instead of `start` (duplicates in a different order, and infinite growth).
3. Recursing with `start + 1` instead of `i + 1`.
4. In the iterative version, looping to `out.size()` while adding to `out`.

---

## 10. Interview questions and answers

**Q1. Why does the start index prevent duplicates?**
> Each subset is generated in exactly one way: with its elements in increasing index order. A child only adds elements to the right of the last one, so [2, 1] can never be built.

**Q2. Why O(n · 2ⁿ) and not O(2ⁿ)?**
> There are 2ⁿ subsets, and copying each one into the output costs up to n.

### Follow-up questions

**F1. Subsets II (with duplicates).** → [M02](M02_SubsetsII.md)
> Sort, then skip equal values at the same level.

**F2. Only subsets of size k.** → [M03 Combinations](M03_Combinations.md)
> Record only when the path has k elements, and prune when too few elements remain.

**F3. Generate the subsets in Gray-code order (each differs from the previous by one element).**
> Use the Gray code from [Recursion M06](../../13-Recursion/03-Medium/M06_GrayCode.md) as the sequence of masks.

---

## 11. Running the file

```bash
java M01_Subsets.java      # 3/3 tests passed (including n = 0..10: backtracking == bitmask == iterative)
```

**One-line summary:** *"Record the path at every node and extend it only with elements after the last one chosen; equivalently, use pick/not-pick, bitmasks 0..2ⁿ − 1, or double the list for each element."*
