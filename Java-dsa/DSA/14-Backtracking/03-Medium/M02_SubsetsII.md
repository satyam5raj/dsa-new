# M02 · Subsets II

| | |
|---|---|
| **LeetCode** | [#90 Subsets II](https://leetcode.com/problems/subsets-ii/) |
| **Difficulty** | Medium |
| **Pattern** | Subsets + duplicates → **sort**, then skip equal values at the same level |
| **Code** | [M02_SubsetsII.java](M02_SubsetsII.java) |
| **Run** | `java M02_SubsetsII.java` |

Backtracking basics are explained in [E01](../02-Easy/E01_BinaryWatch.md#3-java-concepts-used-in-this-problem); plain subsets are in [M01](M01_Subsets.md).

---

## 1. The problem

`nums` may contain duplicates. Return all **distinct** subsets.

| nums | answer |
|---|---|
| [1, 2, 2] | [[], [1], [1,2], [1,2,2], [2], [2,2]] |
| [0] | [[], [0]] |

Plain [M01](M01_Subsets.md) on [1, 2, 2] would produce [1, 2] twice (once with each 2) and [2] twice.

---

## 2. How to think about it

### The lazy fix
Generate all 2ⁿ subsets, sort each one, and put them in a `Set`. It works, but it wastes time generating duplicates and hashing lists. (It's the file's referee.)

### Sort + skip
After sorting, equal values sit next to each other. At one **level** of the tree (one run of the `for` loop), choosing the first 2 or the second 2 as the next element leads to **identical** subtrees. So at each level, use only the **first** of a run of equal values:

```java
if (i > start && nums[i] == nums[i - 1]) continue;
```

**Why `i > start` matters:** the second 2 is still allowed **deeper**, right after the first 2 was picked one level up. That's how [2, 2] is formed. The condition only skips a value when it's not the first candidate at this level.

### The tree for [1, 2, 2] (sorted)

```
                     []
            /         |        \
          [1]        [2]      (2) skipped: i=2 > start=0 and a[2] == a[1]
         /    \        |
     [1,2]   (2)     [2,2]
       |   skipped
    [1,2,2]
```

### Dry run

| call | start | i | value | skip? | path after choosing |
|---|---|---|---|---|---|
| bt(0) | 0 | 0 | 1 | no (i == start) | [1] |
| bt(1) | 1 | 1 | 2 | no (i == start) | [1, 2] |
| bt(2) | 2 | 2 | 2 | no (i == start) | [1, 2, 2] |
| bt(1) | 1 | 2 | 2 | **yes** (i > start, equal to a[1]) | |
| bt(0) | 0 | 1 | 2 | no (a[1] ≠ a[0]) | [2] |
| bt(2) | 2 | 2 | 2 | no (i == start) | [2, 2] |
| bt(0) | 0 | 2 | 2 | **yes** | |

Recorded: [], [1], [1,2], [1,2,2], [2], [2,2] ✓

---

## 3. Java concepts used in this problem

### 3.1 Sorting a copy

```java
int[] a = nums.clone();
Arrays.sort(a);
```

The skip rule only works if equal values are adjacent. Cloning first leaves the caller's array untouched.

### 3.2 `continue` versus `break`

`continue` skips just this candidate and moves on to the next `i`. `break` would end the loop entirely, which would be wrong here: a later, different value (like a 3 after the 2s) must still be tried.

### 3.3 A set of lists as a referee

```java
Set<List<Integer>> s = new HashSet<>();
Collections.sort(x);
s.add(x);
```

`ArrayList` implements `equals` and `hashCode` by content, so two lists with the same elements in the same order are the same set member. Sorting each subset first makes [2, 1] and [1, 2] identical.

### 3.4 Checking that no duplicates were **generated**

```java
new HashSet<>(got).size() == got.size()
```

If the set is smaller than the list, the algorithm produced some subset twice.

---

## 4. Approach 1: All subsets into a set (the referee)

### Code ([M02_SubsetsII.java:53-62](M02_SubsetsII.java#L53-L62))

```java
static Set<List<Integer>> brute(int[] a) {
    Set<List<Integer>> s = new HashSet<>();
    for (int mask = 0; mask < (1 << a.length); mask++) {
        List<Integer> x = new ArrayList<>();
        for (int i = 0; i < a.length; i++) if ((mask >> i & 1) == 1) x.add(a[i]);
        Collections.sort(x);
        s.add(x);
    }
    return s;
}
```

Every bitmask subset, sorted so that equal multisets match, deduplicated by the set.

- **Time O(n · 2ⁿ)** plus the sorting and hashing, and it always does all 2ⁿ subsets even when most are duplicates.

---

## 5. Approach 2: Sort + skip duplicates at the same level (the file's main solution)

### Code ([M02_SubsetsII.java:35-51](M02_SubsetsII.java#L35-L51))

```java
static List<List<Integer>> subsetsWithDup(int[] nums) {
    int[] a = nums.clone();
    Arrays.sort(a);                                          // equal values adjacent
    List<List<Integer>> out = new ArrayList<>();
    bt(a, 0, new ArrayList<>(), out);
    return out;
}

static void bt(int[] a, int start, List<Integer> path, List<List<Integer>> out) {
    out.add(new ArrayList<>(path));
    for (int i = start; i < a.length; i++) {
        if (i > start && a[i] == a[i - 1]) continue;         // same value, same level: duplicate subtree
        path.add(a[i]);
        bt(a, i + 1, path, out);
        path.remove(path.size() - 1);
    }
}
```

### Line by line

| Line | What it does |
|---|---|
| `Arrays.sort(a)` | group equal values |
| `out.add(...)` | every node is a distinct subset |
| the skip | a repeated value at this level would rebuild an identical subtree |
| the rest | the M01 template |

### Complexity
- **Time O(n · 2ⁿ)** in the worst case (all distinct), but it never generates a duplicate.
- **Space O(n)**, plus the output.

---

## 6. Comparing the approaches

| | All subsets + set | Sort + skip |
|---|---|---|
| Generates duplicates? | yes, then removes them | **never** |
| Extra memory | a hash set of lists | none |

The file checks the fast version on 500 random arrays with values 0–3 (lots of duplicates): the results match the brute-force set, and nothing was generated twice.

---

## 7. Edge cases and common mistakes

**Edge cases:** all elements equal ([2,2,2] → [], [2], [2,2], [2,2,2]), no duplicates (same as M01), an empty array.

**Mistakes:**
1. Forgetting to sort (equal values aren't adjacent, so the skip misses them).
2. `if (i > 0 && ...)` instead of `i > start` (that also blocks [2, 2]).
3. `break` instead of `continue`.

---

## 8. Interview questions and answers

**Q1. Why `i > start` and not `i > 0`?**
> `i == start` is the first candidate at this level, and it must always be allowed, even if it equals the element just before it (that element was chosen one level up). `i > 0` would wrongly skip it, losing subsets like [2, 2].

**Q2. Why does the skip remove exactly the duplicates?**
> At a given level, choosing any copy of a value v leads to the same set of possible extensions (the remaining elements after it are a superset when choosing the first copy). Choosing the first copy covers everything the later copies would.

### Follow-up questions

**F1. Permutations II.** → [M05](M05_PermutationsII.md)
> The same "sort and skip equal values" idea, with a `used[]` condition instead of `start`.

**F2. Combination Sum II.** → [M07](M07_CombinationSumII.md)
> Exactly this skip rule, plus a target.

**F3. Count the distinct subsets without listing them.**
> For each distinct value with count c, there are c + 1 choices (use it 0..c times): the product of (cᵢ + 1).

---

## 9. Running the file

```bash
java M02_SubsetsII.java      # 3/3 tests passed (including 500 random arrays vs the set of all subsets)
```

**One-line summary:** *"Sort, then run the subsets template but skip a value when it equals the previous one at the same level (i > start), so each distinct subset is built exactly once."*
