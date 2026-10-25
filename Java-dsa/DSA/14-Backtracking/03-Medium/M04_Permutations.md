# M04 · Permutations

| | |
|---|---|
| **LeetCode** | [#46 Permutations](https://leetcode.com/problems/permutations/) |
| **Difficulty** | Medium |
| **Pattern** | The **permutations** family: loop over **all** elements and skip the `used` ones |
| **Code** | [M04_Permutations.java](M04_Permutations.java) |
| **Run** | `java M04_Permutations.java` |

Backtracking basics are explained in [E01](../02-Easy/E01_BinaryWatch.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Given an array of **distinct** integers, return all n! orderings.

| nums | answer |
|---|---|
| [1, 2, 3] | [[1,2,3], [1,3,2], [2,1,3], [2,3,1], [3,1,2], [3,2,1]] |
| [0, 1] | [[0,1], [1,0]] |

---

## 2. How to think about it

### The difference from subsets
**Order matters.** After picking 2, picking 1 next must still be allowed. So instead of looping from `start`, loop from 0 every time and skip the elements that are already in the path:

```
bt():
    if path.size() == n: record
    for i in 0..n−1:
        if used[i]: continue
        mark i used, add a[i];   bt();   remove a[i], unmark i
```

### The tree for [1, 2, 3]

```
                  []
       /          |          \
     [1]         [2]         [3]
    /   \       /   \       /   \
 [1,2] [1,3] [2,1] [2,3] [3,1] [3,2]
   |     |     |     |     |     |
[1,2,3][1,3,2][2,1,3][2,3,1][3,1,2][3,2,1]
```

n choices at the first level, n − 1 at the second, and so on: n! leaves.

### The swap version
No `used[]` array and no separate path. Fix position `pos` by swapping each candidate a[i] (for i ≥ pos) into it, recurse on pos + 1, then swap back. The prefix a[0..pos−1] is the path, and a[pos..] holds the unused elements.

### Dry run: the swap version on [1, 2, 3]

| pos 0 swap | array | pos 1 swap | array | recorded |
|---|---|---|---|---|
| (0, 0) | 1 2 3 | (1, 1) | 1 2 3 | [1, 2, 3] |
| | | (1, 2) | 1 3 2 | [1, 3, 2] |
| (0, 1) | 2 1 3 | (1, 1) | 2 1 3 | [2, 1, 3] |
| | | (1, 2) | 2 3 1 | [2, 3, 1] |
| (0, 2) | 3 2 1 | (1, 1) | 3 2 1 | [3, 2, 1] |
| | | (1, 2) | 3 1 2 | [3, 1, 2] |

(Each swap is undone before the next one at the same level.) The order differs from the `used[]` version, but it's the same six permutations.

---

## 3. Java concepts used in this problem

### 3.1 A `boolean[]` for "already used"

```java
bt(nums, new boolean[nums.length], new ArrayList<>(), out);
```

A new `boolean[]` starts all `false`. Each call sets `used[i] = true` before recursing and `false` afterwards, so the array always describes exactly the current path.

### 3.2 The undo order mirrors the do order

```java
used[i] = true;  path.add(a[i]);           // do
bt(...);
path.remove(path.size() - 1);  used[i] = false;   // undo, in reverse
```

Every change made before the recursive call is reversed after it. Forgetting to reset `used[i]` is a common bug: the element then stays unavailable in sibling branches.

### 3.3 Swapping in place, and copying out

```java
static void swap(int[] a, int i, int j) { int t = a[i]; a[i] = a[j]; a[j] = t; }
```

The swap version changes the array itself, so at a leaf it must **copy** the whole array into a new list (`for (int x : a) p.add(x)`). It also clones the input first (`nums.clone()`) so the caller's array isn't scrambled during the search.

### 3.4 Comparing results as sets

`new HashSet<>(p1).equals(new HashSet<>(p2))` checks that both versions produced the same permutations, in any order.

---

## 4. Approach 1: `used[]` array (the file's main solution)

### Code ([M04_Permutations.java:31-47](M04_Permutations.java#L31-L47))

```java
static List<List<Integer>> permute(int[] nums) {
    List<List<Integer>> out = new ArrayList<>();
    bt(nums, new boolean[nums.length], new ArrayList<>(), out);
    return out;
}

static void bt(int[] a, boolean[] used, List<Integer> path, List<List<Integer>> out) {
    if (path.size() == a.length) { out.add(new ArrayList<>(path)); return; }
    for (int i = 0; i < a.length; i++) {
        if (used[i]) continue;
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
| `path.size() == a.length` | every element is placed: record a copy |
| `for (int i = 0; ...)` | any unused element can come next (order matters) |
| `if (used[i]) continue` | already in the path |
| mark + add | choose |
| `bt(...)` | fill the remaining positions |
| remove + unmark | un-choose |

### Complexity
- **Time O(n · n!):** n! leaves, each copied in O(n). (The internal nodes add a constant factor: the total is about e · n! nodes.)
- **Space O(n)** for the recursion, `used` and the path.

---

## 5. Approach 2: Swap in place

### Code ([M04_Permutations.java:49-64](M04_Permutations.java#L49-L64))

```java
static List<List<Integer>> permuteSwap(int[] nums) {
    List<List<Integer>> out = new ArrayList<>();
    swapBt(nums.clone(), 0, out);
    return out;
}

static void swapBt(int[] a, int pos, List<List<Integer>> out) {
    if (pos == a.length) { List<Integer> p = new ArrayList<>(); for (int x : a) p.add(x); out.add(p); return; }
    for (int i = pos; i < a.length; i++) {
        swap(a, pos, i);                                    // put a[i] at position pos
        swapBt(a, pos + 1, out);
        swap(a, pos, i);                                    // undo
    }
}

static void swap(int[] a, int i, int j) { int t = a[i]; a[i] = a[j]; a[j] = t; }
```

### Line by line

| Line | What it does |
|---|---|
| `pos == a.length` | every position is fixed: copy the array out |
| `for (int i = pos; ...)` | the candidates for position `pos` are the elements not yet placed, a[pos..] |
| the first swap | place a[i] at `pos` |
| the second swap | restore the array for the next candidate |

- **Time O(n · n!), space O(n)** recursion, with no `used[]` or path list.

---

## 6. Comparing the approaches

| | `used[]` | Swap |
|---|---|---|
| Extra arrays | `used` + path | none |
| Output order | lexicographic (for sorted input) | not lexicographic |
| Handles duplicates easily? | **yes** (see [M05](M05_PermutationsII.md)) | needs a per-level set |

The file checks, for n = 1..8, that there are n! distinct permutations and both versions agree.

---

## 7. Edge cases and common mistakes

**Edge cases:** one element, two elements, negative numbers.

**Mistakes:**
1. Looping from `start` (that gives combinations, not permutations).
2. Forgetting `used[i] = false` after the call.
3. In the swap version, forgetting the second swap or not copying the array at the leaf.
4. Recording `path` without copying.

---

## 8. Interview questions and answers

**Q1. How does the permutations template differ from subsets and combinations?**
> Subsets and combinations loop from `start` because order doesn't matter. Permutations loop over every index and use `used[]`, because a smaller index may legitimately come later.

**Q2. Why does the `used[]` version produce lexicographic order?**
> At each position it tries the remaining elements in index order, so with a sorted input the permutations come out in sorted order.

### Follow-up questions

**F1. Permutations II (with duplicates).** → [M05](M05_PermutationsII.md)

**F2. Next Permutation (LC 31).**
> In place, in O(n): find the rightmost i with a[i] < a[i + 1], swap a[i] with the rightmost element larger than it, then reverse the suffix after i.

**F3. The k-th permutation (LC 60).**
> Use the factorial number system: the first element is index (k − 1) / (n − 1)! among the remaining ones, and so on. O(n²) without generating the others.

---

## 9. Running the file

```bash
java M04_Permutations.java      # 3/3 tests passed (including n = 1..8: n! distinct, both versions agree)
```

**One-line summary:** *"Loop over every index, skip the used ones, and mark/add then remove/unmark around the recursive call; or swap each remaining element into the current position and swap back."*
