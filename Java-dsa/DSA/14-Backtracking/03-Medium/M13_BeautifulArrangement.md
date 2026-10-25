# M13 · Beautiful Arrangement

| | |
|---|---|
| **LeetCode** | [#526 Beautiful Arrangement](https://leetcode.com/problems/beautiful-arrangement/) |
| **Difficulty** | Medium |
| **Pattern** | Permutations with a constraint checked **as each position is filled** |
| **Code** | [M13_BeautifulArrangement.java](M13_BeautifulArrangement.java) |
| **Run** | `java M13_BeautifulArrangement.java` |

Backtracking basics are explained in [E01](../02-Easy/E01_BinaryWatch.md#3-java-concepts-used-in-this-problem); the permutations template is in [M04](M04_Permutations.md).

---

## 1. The problem

Count the permutations `perm` of 1..n where, for every position i (1-based), `perm[i] % i == 0` or `i % perm[i] == 0`.

| n | answer | arrangements |
|---|---|---|
| 1 | 1 | [1] |
| 2 | 2 | [1, 2], [2, 1] |
| 3 | 3 | [1, 2, 3], [2, 1, 3], [3, 2, 1] |
| 15 | 24,679 | |

---

## 2. How to think about it

### Brute force
Generate all n! permutations and check each one at the end. For n = 15 that's 1.3 · 10¹² permutations. No.

### Prune while building
Fill position `pos` with an unused number v **only if** the divisibility rule holds for (v, pos). A bad prefix is cut immediately, so most of the n! tree is never entered.

```
bt(pos):
    if every position is filled: count++
    for v in 1..n:
        if !used[v] && (v % pos == 0 || pos % v == 0):  use v,  bt(next pos),  unuse v
```

### Most constrained first
Fill the positions from **n down to 1**. Large positions have the fewest valid numbers (position 15 only accepts 1, 3, 5 and 15), so placing them first cuts the tree even earlier. Position 1 accepts anything, so it's best left for last. Measured for n = 15:

| fill order | nodes visited |
|---|---|
| 1 → n | 747,961 |
| n → 1 | **102,376** |

Both give 24,679. This is the "most constrained variable first" heuristic from constraint solving.

### Dry run: n = 3, filling positions 3, 2, 1

```
pos 3: allowed v with v%3==0 or 3%v==0 → 1, 3
├─ v=1 → pos 2: allowed (unused) 2  (3 fails: 3%2 and 2%3 are both non-zero)
│   └─ v=2 → pos 1: 3 → count       arrangement (by position 1,2,3): [3, 2, 1]
└─ v=3 → pos 2: allowed 1, 2
    ├─ v=1 → pos 1: 2 → count       [2, 1, 3]
    └─ v=2 → pos 1: 1 → count       [1, 2, 3]
```

Total **3** ✓

---

## 3. Java concepts used in this problem

### 3.1 Counting by returning values

```java
count += bt(n, pos - 1, used);
...
return count;
```

Each call returns how many complete arrangements its subtree contains. The base case returns 1. No shared counter is needed.

### 3.2 A 1-indexed `boolean[]`

`new boolean[n + 1]` lets `used[v]` be indexed directly by the number v (1..n); index 0 is unused.

### 3.3 The negated condition with `continue`

```java
if (used[v] || (v % pos != 0 && pos % v != 0)) continue;
```

The rule "v % pos == 0 or pos % v == 0", negated with De Morgan's law, becomes "v % pos != 0 and pos % v != 0". So this skips v when it's used or when it fails both divisibility tests.

---

## 4. Approach 1: All permutations, checked at the end (the referee)

### Code ([M13_BeautifulArrangement.java:49-67](M13_BeautifulArrangement.java#L49-L67))

```java
static int brute(int n) {
    int[] p = new int[n];
    for (int i = 0; i < n; i++) p[i] = i + 1;
    return perm(p, 0);
}

static int perm(int[] p, int k) {
    if (k == p.length) {
        for (int i = 0; i < p.length; i++) if (p[i] % (i + 1) != 0 && (i + 1) % p[i] != 0) return 0;
        return 1;
    }
    int c = 0;
    for (int i = k; i < p.length; i++) {
        int t = p[k]; p[k] = p[i]; p[i] = t;
        c += perm(p, k + 1);
        t = p[k]; p[k] = p[i]; p[i] = t;
    }
    return c;
}
```

The swap-based permutation generator from [M04](M04_Permutations.md#5-approach-2-swap-in-place), with the rule checked only at the leaves (positions are `i + 1` because the array is 0-indexed).

- **Time O(n · n!).** The test uses it for n ≤ 9.

---

## 5. Approach 2: Prune at each position, most constrained first (the file's main solution)

### Code ([M13_BeautifulArrangement.java:33-46](M13_BeautifulArrangement.java#L33-L46))

```java
static int countArrangement(int n) { return bt(n, n, new boolean[n + 1]); }

// fill positions n, n-1, ..., 1 (most constrained first)
static int bt(int n, int pos, boolean[] used) {
    if (pos == 0) return 1;
    int count = 0;
    for (int v = 1; v <= n; v++) {
        if (used[v] || (v % pos != 0 && pos % v != 0)) continue;   // rule fails: prune now
        used[v] = true;
        count += bt(n, pos - 1, used);
        used[v] = false;
    }
    return count;
}
```

### Line by line

| Line | What it does |
|---|---|
| `bt(n, n, ...)` | start by filling position n |
| `pos == 0` | positions n..1 are all filled: one valid arrangement |
| the `continue` | the number is taken, or the rule fails for this position |
| use / recurse / unuse | the permutation template |

### Complexity
- **Time O(number of valid partial arrangements)**: 102,376 nodes for n = 15, versus 1.3 · 10¹² permutations.
- **Space O(n).**

---

## 6. Comparing the approaches

| | Check at the end | Prune while building (1 → n) | Prune, n → 1 |
|---|---|---|---|
| Nodes for n = 15 | ~10¹² | 747,961 | **102,376** |

The file checks the fast version against the brute force for n = 1..9.

---

## 7. Edge cases and common mistakes

**Edge cases:** n = 1, n = 15 (LeetCode's limit).

**Mistakes:**
1. Checking the rule only at the leaves (correct but far too slow).
2. Using `&&` where `||` belongs in the rule (or forgetting to negate correctly).
3. 0-based positions (the rule is defined on 1-based positions; 0 would divide by zero).

---

## 8. Interview questions and answers

**Q1. Why fill from position n down?**
> High positions have few valid numbers, so placing them first makes bad branches fail near the root. Position 1 accepts every number, so it never prunes anything and is best filled last.

**Q2. Could you memoize?**
> Yes: the state is (position, set of used numbers), which is a bitmask. dp[mask] = the number of ways to fill the first popcount(mask) positions using exactly the numbers in mask. That's O(2ⁿ · n).

### Follow-up questions

**F1. Beautiful Arrangement II (LC 667).**
> A construction problem (no search): alternate low and high numbers to create exactly k distinct differences.

**F2. Return the arrangements, not just the count.**
> Keep a `path` array and record a copy at each leaf.

**F3. N-Queens.** → [H01](../04-Hard/H01_NQueens.md)
> Another "place one item per position with constraints checked immediately".

---

## 9. Running the file

```bash
java M13_BeautifulArrangement.java      # 4/4 tests passed (including n = 15 → 24679 and n = 1..9 vs all n! permutations)
```

**One-line summary:** *"Fill positions from n down to 1, only placing an unused number that divides or is divided by the position, and count the complete fillings."*
