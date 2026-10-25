# M12 · Target Sum

| | |
|---|---|
| **LeetCode** | [#494 Target Sum](https://leetcode.com/problems/target-sum/) |
| **Difficulty** | Medium |
| **Pattern** | Algebra turns the +/− signs into **0/1 knapsack counting** |
| **Code** | [M12_TargetSum.java](M12_TargetSum.java) |
| **Run** | `java M12_TargetSum.java` |

The DP recipe is explained in [E01](../02-Easy/E01_ClimbingStairs.md#3-java-concepts-used-in-this-problem-and-the-dp-recipe); the boolean version of this knapsack is [M11](M11_PartitionEqualSubsetSum.md).

---

## 1. The problem

Put a `+` or a `−` before every number. How many sign assignments make the expression equal `target`?

| nums | target | answer |
|---|---|---|
| [1, 1, 1, 1, 1] | 3 | 5 |
| [1] | 1 | 1 |
| [0, 0, 1] | 1 | 4 (each 0 can be +0 or −0) |
| [1] | 2 | 0 |

---

## 2. How to think about it

### Brute force and memo
- Brute force: 2ⁿ sign choices.
- Memo on (index, running sum): O(n · range of sums). It works, but there's a neater way.

### The algebra
Let P be the numbers with `+` and N the numbers with `−`:

```
sum(P) − sum(N) = target
sum(P) + sum(N) = total
⇒ 2 · sum(P) = total + target
⇒ sum(P) = (total + target) / 2
```

So the question becomes: **how many subsets sum to (total + target) / 2?** If total + target is odd, or |target| > total, the answer is 0.

### Counting subsets (0/1 knapsack counting)
```
ways[0] = 1
for each x:  for s = goal down to x:  ways[s] += ways[s − x]
```

The same downward loop as [M11](M11_PartitionEqualSubsetSum.md), with `+=` instead of `|=`.

**Zeros are handled naturally.** Each 0 doubles the count (+0 and −0 are different assignments), and `ways[s] += ways[s − 0]` doubles every entry.

### Dry run: [1, 1, 1, 1, 1], target 3
goal = (5 + 3) / 2 = 4.

| after x | ways[0..4] |
|---|---|
| (start) | 1, 0, 0, 0, 0 |
| 1 | 1, 1, 0, 0, 0 |
| 1 | 1, 2, 1, 0, 0 |
| 1 | 1, 3, 3, 1, 0 |
| 1 | 1, 4, 6, 4, 1 |
| 1 | 1, 5, 10, 10, **5** |

These are binomial coefficients: choosing which 4 of the 5 ones get a `+` gives C(5, 4) = **5**.

### Dry run: [0, 0, 1], target 1
total = 1, goal = 1.

| after x | ways[0..1] |
|---|---|
| (start) | 1, 0 |
| 0 | 2, 0 |
| 0 | 4, 0 |
| 1 | 4, **4** |

---

## 3. Java concepts used in this problem

### 3.1 Guarding the reduction

```java
if (Math.abs(target) > total || (total + target) % 2 != 0) return 0;
```

- If |target| > total, no assignment can reach it, and the goal could be negative (an invalid array size).
- If total + target is odd, sum(P) would not be an integer.

`(total + target) % 2 != 0` is safe for negative values too: in Java, `−3 % 2` is −1, which is still `!= 0`.

### 3.2 A loop that includes `s = x` when x = 0

`for (int s = goal; s >= x; s--)` with x = 0 runs down to s = 0, where `ways[0] += ways[0]` doubles it. That's exactly the "+0 and −0" doubling.

### 3.3 Two-branch recursion (in the referee)

`brute(a, i + 1, sum + a[i], target) + brute(a, i + 1, sum - a[i], target)`: one branch per sign.

---

## 4. Approach 1: Try every sign (the referee)

### Code ([M12_TargetSum.java:50-53](M12_TargetSum.java#L50-L53))

```java
static int brute(int[] a, int i, int sum, int target) {
    if (i == a.length) return sum == target ? 1 : 0;
    return brute(a, i + 1, sum + a[i], target) + brute(a, i + 1, sum - a[i], target);
}
```

- **Time O(2ⁿ).** Memoizing on (i, sum) would make it O(n · total).

---

## 5. Approach 2: Subset counting (the file's main solution)

### Code ([M12_TargetSum.java:38-48](M12_TargetSum.java#L38-L48))

```java
static int findTargetSumWays(int[] nums, int target) {
    int total = 0;
    for (int x : nums) total += x;
    if (Math.abs(target) > total || (total + target) % 2 != 0) return 0;
    int goal = (total + target) / 2;
    int[] ways = new int[goal + 1];
    ways[0] = 1;
    for (int x : nums)
        for (int s = goal; s >= x; s--) ways[s] += ways[s - x];    // backwards: 0/1
    return ways[goal];
}
```

### Line by line

| Line | What it does |
|---|---|
| `total` | the sum of all numbers |
| the guard | impossible targets |
| `goal` | the sum the `+` numbers must have |
| `ways[0] = 1` | the empty subset |
| the inner loop | 0/1 counting, going down |

### Complexity
- **Time O(n · goal), space O(goal).**

---

## 6. Comparing the approaches

| | Every sign | Memo on (i, sum) | Subset counting |
|---|---|---|---|
| Time | O(2ⁿ) | O(n · total) | O(n · goal) |
| Space | O(n) | O(n · total) | **O(goal)** |

The file checks the subset-count version against all 2ⁿ sign choices on 1,000 random arrays (with zeros) and targets from −10 to 10.

---

## 7. Edge cases and common mistakes

**Edge cases:** zeros in the input, a negative target, |target| > total, a single number.

**Mistakes:**
1. Ignoring zeros in the reduction (they still double the count, which the loop handles).
2. Not checking |target| > total (a negative goal crashes the array allocation).
3. Iterating s upward (counts reuse).

---

## 8. Interview questions and answers

**Q1. Derive the reduction.**
> Adding the two equations sum(P) − sum(N) = target and sum(P) + sum(N) = total gives sum(P) = (total + target) / 2. So each valid sign assignment corresponds to exactly one subset P with that sum.

**Q2. Why do zeros double the answer?**
> +0 and −0 give the same sum but count as different assignments. In the subset view, each 0 can be in P or not without changing the sum.

### Follow-up questions

**F1. Partition Equal Subset Sum.** → [M11](M11_PartitionEqualSubsetSum.md)
> The boolean version with target 0, essentially.

**F2. Return the assignments.**
> Backtracking over the signs (exponential output).

**F3. Ones and Zeroes (LC 474).**
> 0/1 knapsack with two capacities (zeros and ones), iterated downward in both.

---

## 9. Running the file

```bash
java M12_TargetSum.java      # 5/5 tests passed (including 1000 random vs all 2^n signs)
```

**One-line summary:** *"The + numbers must sum to (total + target) / 2, so count subsets with that sum using 0/1 knapsack counting (ways[s] += ways[s − x], s going down), after checking parity and |target| ≤ total."*
