# M11 · Partition Equal Subset Sum

| | |
|---|---|
| **LeetCode** | [#416 Partition Equal Subset Sum](https://leetcode.com/problems/partition-equal-subset-sum/) |
| **Difficulty** | Medium |
| **Pattern** | 0/1 knapsack, **boolean** ("can some subset reach this sum?") |
| **Code** | [M11_PartitionEqualSubsetSum.java](M11_PartitionEqualSubsetSum.java) |
| **Run** | `java M11_PartitionEqualSubsetSum.java` |

The DP recipe is explained in [E01](../02-Easy/E01_ClimbingStairs.md#3-java-concepts-used-in-this-problem-and-the-dp-recipe); the unbounded version is [M10](M10_CoinChangeII.md).

---

## 1. The problem

Can `nums` be split into two subsets with equal sums?

| nums | answer | split |
|---|---|---|
| [1, 5, 11, 5] | true | [1, 5, 5] and [11] |
| [1, 2, 3, 5] | false | the total 11 is odd |
| [2, 2] | true | |
| [1] | false | |

---

## 2. How to think about it

### The reduction
Two equal halves exist **iff** some subset sums to total / 2 (the rest is then the other half). If the total is odd, return false immediately.

### Subset sum (0/1 knapsack: each number used at most once)

| step | for this problem |
|---|---|
| **State** | can[s] = some subset of the numbers seen so far sums to s |
| **Transition** | for each x: for s = target **down to** x: can[s] \|= can[s − x] |
| **Base** | can[0] = true (the empty subset) |
| **Answer** | can[target] |

### Why the loop runs downward
Going down, `can[s − x]` hasn't been updated for this x yet, so it still means "reachable **without** x". Each number is used at most once. Going up would let x be used repeatedly: with x = 1, can[1] becomes true, then can[2] |= can[1] would use 1 twice. That's the unbounded version from [M10](M10_CoinChangeII.md).

### Dry run: [1, 5, 11, 5], target 11

| after x | reachable sums (≤ 11) |
|---|---|
| (start) | {0} |
| 1 | {0, 1} |
| 5 | {0, 1, 5, 6} |
| 11 | {0, 1, 5, 6, **11**} |
| 5 | {0, 1, 5, 6, 10, 11} |

can[11] is true.

### The bitset trick
Represent `can[]` as the bits of one big number. Adding x then becomes

```
bits |= bits << x
```

which processes 64 sums per machine word: up to about 64 × faster. Java has no fixed-size bitset with shifts that's as convenient as C++'s `std::bitset`, but `BigInteger`'s `shiftLeft` and `or` do the job in one line.

---

## 3. Java concepts used in this problem

### 3.1 `|=` on booleans

`can[s] |= can[s - x]` is `can[s] = can[s] | can[s - x]`: it becomes true if either was true.

### 3.2 `BigInteger` as a bitset

```java
BigInteger bits = BigInteger.ONE;                 // bit 0 set: sum 0 is reachable
for (int x : nums) bits = bits.or(bits.shiftLeft(x));
return bits.testBit(total / 2);
```

- `BigInteger` is immutable: `or` and `shiftLeft` return **new** values, so the result must be assigned back.
- `shiftLeft(x)` moves every reachable sum s to s + x.
- `testBit(k)` checks whether bit k is set.

### 3.3 `import java.math.BigInteger`

`BigInteger` lives in `java.math`, which `import java.util.*` doesn't cover, so the file imports it separately.

---

## 4. Approach 1: Every subset (the referee)

### Code ([M11_PartitionEqualSubsetSum.java:59-67](M11_PartitionEqualSubsetSum.java#L59-L67))

```java
static boolean brute(int[] a) {
    int total = Arrays.stream(a).sum();
    for (int mask = 0; mask < 1 << a.length; mask++) {
        int s = 0;
        for (int i = 0; i < a.length; i++) if ((mask >> i & 1) == 1) s += a[i];
        if (2 * s == total) return true;
    }
    return false;
}
```

`2 * s == total` avoids dividing (and handles odd totals automatically). **O(n · 2ⁿ).**

---

## 5. Approach 2: Boolean 0/1 knapsack (the file's main solution)

### Code ([M11_PartitionEqualSubsetSum.java:38-49](M11_PartitionEqualSubsetSum.java#L38-L49))

```java
static boolean canPartition(int[] nums) {
    int total = 0;
    for (int x : nums) total += x;
    if (total % 2 == 1) return false;
    int target = total / 2;
    boolean[] can = new boolean[target + 1];
    can[0] = true;
    for (int x : nums)
        for (int s = target; s >= x; s--)            // backwards: each number used once
            can[s] |= can[s - x];
    return can[target];
}
```

| Line | What it does |
|---|---|
| the odd check | equal halves are impossible |
| `can[0] = true` | the empty subset |
| the inner loop | going down, so each x is used at most once |
| `can[target]` | a subset with half the total exists |

- **Time O(n · sum), space O(sum).**

---

## 6. Approach 3: Bitset with `BigInteger`

### Code ([M11_PartitionEqualSubsetSum.java:51-57](M11_PartitionEqualSubsetSum.java#L51-L57))

```java
static boolean canPartitionBits(int[] nums) {
    int total = Arrays.stream(nums).sum();
    if (total % 2 == 1) return false;
    BigInteger bits = BigInteger.ONE;                 // bit s set = sum s reachable
    for (int x : nums) bits = bits.or(bits.shiftLeft(x));
    return bits.testBit(total / 2);
}
```

The same DP, 64 sums at a time. There's no "downward" issue here, because the whole shifted copy is computed from the **old** `bits` before the `or`.

- **Time O(n · sum / 64)** in principle (plus `BigInteger` overhead), **space O(sum)** bits.

---

## 7. Comparing the approaches

| | Every subset | Boolean DP | Bitset |
|---|---|---|---|
| Time | O(n · 2ⁿ) | O(n · sum) | O(n · sum / 64) |
| Space | O(1) | O(sum) | O(sum / 64) words |

The file checks both fast versions against the brute force on 1,000 random arrays.

---

## 8. Edge cases and common mistakes

**Edge cases:** an odd total, a single element, one element equal to half the total, all equal elements.

**Mistakes:**
1. Iterating s upward (numbers get reused).
2. Forgetting the odd-total check.
3. Trying greedy (sort and fill one side).

---

## 9. Interview questions and answers

**Q1. Why does the direction of the inner loop matter?**
> Downward, can[s − x] still reflects the subsets **without** x, so each number is counted once. Upward, it may already include x, allowing it to be reused.

**Q2. How do you make this faster?**
> The bitset version processes 64 sums per word. Also, sorting and stopping as soon as can[target] becomes true helps in practice.

### Follow-up questions

**F1. Partition to K Equal Sum Subsets.** → [Backtracking M12](../../14-Backtracking/03-Medium/M12_PartitionToKEqualSumSubsets.md)
> For k > 2, a single subset sum isn't enough; it needs backtracking or a bitmask DP.

**F2. Last Stone Weight II (LC 1049).**
> Minimise |sum(A) − sum(B)|: find the largest reachable sum ≤ total / 2 with this DP.

**F3. Target Sum.** → [M12](M12_TargetSum.md)
> The counting version of the same 0/1 knapsack.

---

## 10. Running the file

```bash
java M11_PartitionEqualSubsetSum.java      # 5/5 tests passed (including 1000 random vs all subsets, + bitset)
```

**One-line summary:** *"If the total is even, check whether some subset sums to total / 2 with a boolean 0/1 knapsack (can[s] |= can[s − x], s going down), or as bits |= bits << x."*
