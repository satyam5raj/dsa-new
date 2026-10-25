# M14 · Matchsticks to Square

| | |
|---|---|
| **LeetCode** | [#473 Matchsticks to Square](https://leetcode.com/problems/matchsticks-to-square/) |
| **Difficulty** | Medium |
| **Pattern** | [M12](M12_PartitionToKEqualSumSubsets.md) with k = 4: four **sides** as buckets, longest sticks first |
| **Code** | [M14_MatchsticksToSquare.java](M14_MatchsticksToSquare.java) |
| **Run** | `java M14_MatchsticksToSquare.java` |

Backtracking basics are explained in [E01](../02-Easy/E01_BinaryWatch.md#3-java-concepts-used-in-this-problem); the bucket idea and its pruning are explained in [M12](M12_PartitionToKEqualSumSubsets.md).

---

## 1. The problem

Use every matchstick exactly once (no breaking them) to form a square.

| matchsticks | answer | why |
|---|---|---|
| [1, 1, 2, 2, 2] | true | sides 2, 2, 2 and 1 + 1 |
| [3, 3, 3, 3, 4] | false | total 16 gives side 4, but the 3s can't pair up to make 4 |
| [5,5,5,5,4,4,4,4,3,3,3,3] | true | each side is 5 + 4 + 3 |

---

## 2. How to think about it

A square has 4 sides of equal length, so this is **Partition to K Equal Sum Subsets with k = 4**. The same checks and pruning apply:
- if `total % 4 != 0`, or some stick is longer than total / 4, return false;
- sort **descending** (long sticks have the fewest places to go);
- try each stick on each side, but skip a side whose current length equals a side already tried for this stick (**symmetry**).

### Dry run: [1, 1, 2, 2, 2], side 2 (sorted: 2, 2, 2, 1, 1)

| stick | sides before | placed on | sides after |
|---|---|---|---|
| 2 | [0, 0, 0, 0] | side 0 (sides 1–3 would be symmetric anyway) | [2, 0, 0, 0] |
| 2 | [2, 0, 0, 0] | side 1 (side 0 would be 4) | [2, 2, 0, 0] |
| 2 | [2, 2, 0, 0] | side 2 | [2, 2, 2, 0] |
| 1 | [2, 2, 2, 0] | side 3 | [2, 2, 2, 1] |
| 1 | [2, 2, 2, 1] | side 3 | [2, 2, 2, 2] → **true** |

### Dry run: [3, 3, 3, 3, 4], side 4 (sorted: 4, 3, 3, 3, 3)
4 goes on side 0 and fills it. The 3s go on sides 1, 2, 3. The last 3 fits nowhere (every side would exceed 4). Backing up doesn't help: no side can hold two 3s, and there are four 3s for three free sides. Result: **false**.

---

## 3. Java concepts used in this problem

### 3.1 Reversing a sorted `int[]` in place

```java
Arrays.sort(a);
for (int i = 0, j = a.length - 1; i < j; i++, j--) { int t = a[i]; a[i] = a[j]; a[j] = t; }
```

`Arrays.sort` on an `int[]` only sorts ascending (a comparator needs an object array). Sorting and then reversing with two pointers gives descending order without boxing. Compare with [M12](M12_PartitionToKEqualSumSubsets.md#31-sorting-ints-in-descending-order), which boxes into `Integer[]` and uses `Collections.reverseOrder()`.

### 3.2 Symmetry with a short scan instead of a set

```java
boolean seen = false;
for (int p = 0; p < s; p++) if (sides[p] == sides[s]) { seen = true; break; }
if (seen) continue;
```

With only 4 sides, scanning the earlier sides is simpler and faster than a `HashSet`. It skips side s if an earlier side has the same length (and was therefore already tried for this stick).

### 3.3 Base-4 counting (in the referee)

```java
for (int code = 0; code < total; code++) {       // total = 4^n
    int c = code;
    for (int i = 0; i < n; i++) { s[c % 4] += a[i]; c /= 4; }
}
```

Each `code` from 0 to 4ⁿ − 1, written in base 4, assigns every stick to one of the 4 sides: digit i (`c % 4`, then `c /= 4`) is stick i's side.

---

## 4. Approach 1: Try all 4ⁿ assignments (the referee)

### Code ([M14_MatchsticksToSquare.java:55-65](M14_MatchsticksToSquare.java#L55-L65))

```java
static boolean brute(int[] a) {
    int n = a.length, total = 1;
    for (int i = 0; i < n; i++) total *= 4;
    for (int code = 0; code < total; code++) {
        int[] s = new int[4];
        int c = code;
        for (int i = 0; i < n; i++) { s[c % 4] += a[i]; c /= 4; }
        if (s[0] == s[1] && s[1] == s[2] && s[2] == s[3] && s[0] > 0) return true;
    }
    return false;
}
```

`s[0] > 0` rules out the degenerate "all sides 0" case. **O(4ⁿ · n)**, so the test keeps n ≤ 8.

---

## 5. Approach 2: Four buckets with pruning (the file's main solution)

### Code ([M14_MatchsticksToSquare.java:29-52](M14_MatchsticksToSquare.java#L29-L52))

```java
static boolean makesquare(int[] matchsticks) {
    int total = Arrays.stream(matchsticks).sum();
    if (total % 4 != 0) return false;
    int side = total / 4;
    int[] a = matchsticks.clone();
    Arrays.sort(a);
    for (int i = 0, j = a.length - 1; i < j; i++, j--) { int t = a[i]; a[i] = a[j]; a[j] = t; }   // descending
    if (a[0] > side) return false;
    return place(a, 0, new int[4], side);
}

static boolean place(int[] a, int i, int[] sides, int target) {
    if (i == a.length) return true;
    for (int s = 0; s < 4; s++) {
        if (sides[s] + a[i] > target) continue;
        boolean seen = false;
        for (int p = 0; p < s; p++) if (sides[p] == sides[s]) { seen = true; break; }
        if (seen) continue;                                   // symmetric to an earlier side
        sides[s] += a[i];
        if (place(a, i + 1, sides, target)) return true;
        sides[s] -= a[i];
    }
    return false;
}
```

### Line by line

| Line | What it does |
|---|---|
| `total % 4` | a square needs 4 equal sides |
| sort + reverse | the longest sticks first |
| `a[0] > side` | the longest stick doesn't fit on any side |
| `i == a.length` | every stick is placed and no side exceeded the target, so all four are exactly `side` |
| the overflow check | this stick doesn't fit on side s |
| the symmetry scan | an earlier side with the same length was already tried |
| place / recurse / undo | backtrack; stop at the first success |

### Complexity
- **Time O(4ⁿ)** in the worst case, heavily pruned in practice.
- **Space O(n)** recursion.

---

## 6. Comparing the approaches

| | All 4ⁿ assignments | Pruned buckets |
|---|---|---|
| Time | O(4ⁿ · n) always | far less |
| Stops at the first solution? | yes | yes |

The file checks the pruned version against all 4ⁿ assignments on 1,000 random sets of 4–8 sticks.

---

## 7. Edge cases and common mistakes

**Edge cases:** fewer than 4 sticks (the total can't be split into 4 positive sides unless… it can't; `total % 4` or the side checks fail), all sticks equal and a multiple of 4 in number, one very long stick.

**Mistakes:**
1. Forgetting to undo `sides[s] += a[i]`.
2. Sorting ascending (much slower).
3. Using `long` vs `int`: the total of 15 sticks of up to 10⁸ can exceed `int`. (LeetCode's limits keep it within `int`, but it's worth mentioning.)

---

## 8. Interview questions and answers

**Q1. How is this related to Partition to K Equal Sum Subsets?**
> It's the same problem with k = 4: each side is a bucket that must sum to total / 4.

**Q2. Why does the symmetry check compare with earlier sides only?**
> If an earlier side has the same length, putting the stick there was already tried (it came first in the loop), so trying this side would explore an identical subtree.

### Follow-up questions

**F1. Bitmask DP version.**
> The [M12 referee](M12_PartitionToKEqualSumSubsets.md#4-approach-1-bitmask-dp-the-referee) with k = 4: O(2ⁿ · n), good for n ≤ 15.

**F2. Form a rectangle instead.**
> Two pairs of equal sides: try the possible values of one side length and check that the sticks split into 2 sides of length a and 2 of length b.

**F3. Form an equilateral triangle.**
> k = 3, the same algorithm.

---

## 9. Running the file

```bash
java M14_MatchsticksToSquare.java      # 4/4 tests passed (including 1,000 random stick sets vs all 4^n assignments)
```

**One-line summary:** *"Partition into 4 equal-sum buckets: check total % 4 and the longest stick, sort descending, and place each stick on a side that stays ≤ total / 4, skipping sides equal in length to one already tried."*
