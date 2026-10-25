# E02 · Sum of All Subset XOR Totals ⭐

| | |
|---|---|
| **LeetCode** | [#1863 Sum of All Subset XOR Totals](https://leetcode.com/problems/sum-of-all-subset-xor-totals/) |
| **Difficulty** | Easy |
| **Pattern** | **Pick / not pick** over every element, carrying a running value (plus an O(n) trick) |
| **Code** | [E02_SumOfAllSubsetXorTotals.java](E02_SumOfAllSubsetXorTotals.java) |
| **Run** | `java E02_SumOfAllSubsetXorTotals.java` |

⭐ = not in `leetcode.md`, added for a key technique. Backtracking basics are explained in [E01](E01_BinaryWatch.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

For every subset of `nums`, take the XOR of its elements (the empty subset has XOR 0). Return the sum of all those XORs.

```
[1, 3]:  []  → 0
         [1] → 1
         [3] → 3
         [1, 3] → 1 ^ 3 = 2
         sum = 6
```

| nums | answer |
|---|---|
| [1, 3] | 6 |
| [5, 1, 6] | 28 |
| [3, 4, 5, 6, 7, 8] | 480 |

---

## 2. How to think about it

### Pick / not pick
Each element is either in the subset or not, so there are two branches per element:

```
dfs(i, xorSoFar):
    if i == n: return xorSoFar                          one complete subset
    return dfs(i + 1, xorSoFar ^ a[i])                  pick a[i]
         + dfs(i + 1, xorSoFar)                         don't pick it
```

There's no list to maintain: the "path" is just one number, the XOR so far. Passing a new value down **is** the undo (see [E01 §3.5](E01_BinaryWatch.md#35-passing-values-instead-of-a-path-nothing-to-undo)). There are 2ⁿ leaves, which is fine for n ≤ 12.

This is the second way to generate subsets. The first is the `start`-index loop in [E01 §3.1](E01_BinaryWatch.md#31-the-backtracking-template); pick / not pick makes exactly one binary decision per element.

### Dry run: [1, 3]

```
dfs(0, 0)
├─ pick 1     → dfs(1, 1)
│   ├─ pick 3     → dfs(2, 1 ^ 3 = 2) → 2
│   └─ skip 3     → dfs(2, 1)         → 1          dfs(1, 1) = 3
└─ skip 1     → dfs(1, 0)
    ├─ pick 3     → dfs(2, 3)         → 3
    └─ skip 3     → dfs(2, 0)         → 0          dfs(1, 0) = 3
                                                   dfs(0, 0) = 6 ✓
```

### The O(n) trick
A bit that is set in **any** element is set in exactly **half** of all 2ⁿ subset XORs. So:

```
answer = (OR of all elements) · 2ⁿ⁻¹
```

**Why half:** take an element x that has bit b set. Pair every subset S that doesn't contain x with S ∪ {x}. Adding x flips bit b of the XOR, so in each pair exactly one subset has bit b set. That's 2ⁿ⁻¹ subsets with bit b. Bits that no element has are never set.

[1, 3]: OR = 3, and 3 · 2¹ = **6** ✓. [5, 1, 6]: OR = 7, and 7 · 2² = **28** ✓.

---

## 3. Java concepts used in this problem

### 3.1 XOR `^` and OR `|`

- `a ^ b`: 1 where the bits **differ**. XOR is its own inverse: x ^ y ^ y = x.
- `a | b`: 1 where **either** has a 1.
- `or |= x` is short for `or = or | x`.

### 3.2 `<<` as multiplication by a power of two

`or << (n − 1)` is `or · 2ⁿ⁻¹`.

### 3.3 Returning values up the tree instead of collecting them

`dfs` **returns** the sum for its subtree, and each call adds its two children's results. No shared list or counter is needed.

### 3.4 Bitmask enumeration (in the test)

```java
for (int mask = 0; mask < (1 << a.length); mask++) {
    int x = 0;
    for (int i = 0; i < a.length; i++) if ((mask >> i & 1) == 1) x ^= a[i];
    brute += x;
}
```

Each `mask` from 0 to 2ⁿ − 1 is one subset: bit i says whether `a[i]` is in it. `mask >> i & 1` reads bit i. This is a third way to enumerate subsets, without recursion.

### 3.5 `rnd.ints(count, from, to).toArray()`

It produces `count` random ints in [from, to) as a stream and collects them into an `int[]`.

---

## 4. Approach 1: Pick / not pick recursion (the file's main solution)

### Code ([E02_SumOfAllSubsetXorTotals.java:32-37](E02_SumOfAllSubsetXorTotals.java#L32-L37))

```java
static int subsetXORSum(int[] nums) { return dfs(nums, 0, 0); }

static int dfs(int[] a, int i, int xor) {
    if (i == a.length) return xor;
    return dfs(a, i + 1, xor ^ a[i]) + dfs(a, i + 1, xor);   // pick + not pick
}
```

### Line by line

| Line | What it does |
|---|---|
| `dfs(nums, 0, 0)` | start before the first element with an XOR of 0 (the empty subset) |
| `i == a.length` | every element has been decided: this leaf is one subset |
| the first call | include `a[i]` |
| the second call | leave it out |

- **Time O(2ⁿ).**
- **Space O(n)** recursion depth.

---

## 5. Approach 2: The OR trick

### Code ([E02_SumOfAllSubsetXorTotals.java:39-43](E02_SumOfAllSubsetXorTotals.java#L39-L43))

```java
static int trick(int[] a) {
    int or = 0;
    for (int x : a) or |= x;
    return or << (a.length - 1);
}
```

| Line | What it does |
|---|---|
| the loop | every bit that appears in at least one element |
| `<< (n − 1)` | each such bit appears in 2ⁿ⁻¹ subset XORs |

- **Time O(n), space O(1).**

---

## 6. Comparing the approaches

| | Pick / not pick | Bitmask loop | OR trick |
|---|---|---|---|
| Time | O(2ⁿ) | O(2ⁿ · n) | **O(n)** |
| Space | O(n) stack | O(1) | O(1) |

The file checks all three against each other on 500 random arrays of up to 12 elements.

---

## 7. Edge cases and common mistakes

**Edge cases:** a single element (the answer is that element), all elements equal, n = 12.

**Mistakes:**
1. Forgetting the empty subset (it contributes 0, so the sum isn't affected, but the count of subsets is).
2. Using `+` instead of `^` while building the subset value.
3. In the trick, multiplying by 2ⁿ instead of 2ⁿ⁻¹.

---

## 8. Interview questions and answers

**Q1. Why does pick / not pick visit every subset exactly once?**
> Each leaf corresponds to one sequence of n include/exclude decisions, and each sequence is exactly one subset.

**Q2. Explain the trick.**
> For any bit set in some element x, toggling x in or out of a subset flips that bit of the XOR, which pairs the subsets so that exactly half of them have the bit. The sum is the OR (the set of bits that appear) times 2ⁿ⁻¹.

### Follow-up questions

**F1. Return the sum of all subset sums instead.**
> Each element appears in 2ⁿ⁻¹ subsets, so the answer is (sum of the elements) · 2ⁿ⁻¹.

**F2. Count the subsets whose XOR equals k.**
> Pick / not pick with memoization on (i, xorSoFar), which is a DP over XOR values.

**F3. Subsets (LC 78).** → [M01](../03-Medium/M01_Subsets.md)
> The same enumeration, collecting the subsets themselves.

---

## 9. Running the file

```bash
java E02_SumOfAllSubsetXorTotals.java      # 4/4 tests passed (including 500 random arrays: dfs == bitmask == trick)
```

**One-line summary:** *"Recurse with two branches per element (XOR it in, or skip it) and sum the XOR at each leaf, or use the shortcut (OR of all elements) · 2ⁿ⁻¹."*
