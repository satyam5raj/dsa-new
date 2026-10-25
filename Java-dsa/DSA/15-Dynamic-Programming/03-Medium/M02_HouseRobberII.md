# M02 · House Robber II

| | |
|---|---|
| **LeetCode** | [#213 House Robber II](https://leetcode.com/problems/house-robber-ii/) |
| **Difficulty** | Medium |
| **Pattern** | Break a **circle** into two **lines** and reuse the linear DP |
| **Code** | [M02_HouseRobberII.java](M02_HouseRobberII.java) |
| **Run** | `java M02_HouseRobberII.java` |

The linear version is [M01 House Robber](M01_HouseRobber.md); the DP recipe is in [E01](../02-Easy/E01_ClimbingStairs.md#3-java-concepts-used-in-this-problem-and-the-dp-recipe).

---

## 1. The problem

The same as House Robber, but the houses form a **circle**: the first and last houses are neighbours.

| nums | answer | why |
|---|---|---|
| [2, 3, 2] | 3 | houses 0 and 2 are now adjacent |
| [1, 2, 3, 1] | 4 | 1 + 3 |
| [1, 2, 3] | 3 | |
| [7] | 7 | |

---

## 2. How to think about it

### The problem with a circle
The linear DP needs a start and an end. But the only new rule is "not both house 0 and house n − 1". So every valid plan either:
- **skips house n − 1**, which makes it a valid plan on the **line** 0..n − 2; or
- **skips house 0**, which makes it a valid plan on the **line** 1..n − 1.

(A plan that skips both is counted in either case.) So:

```
answer = max(rob(0..n−2), rob(1..n−1))         and for n == 1: nums[0]
```

This "cut the cycle by fixing one element's choice" trick comes up again and again: circular arrays, maximum circular subarray sum, and so on.

### Dry run: [2, 3, 2]

| line | houses | linear best |
|---|---|---|
| 0..1 | [2, 3] | 3 |
| 1..2 | [3, 2] | 3 |

Answer = max(3, 3) = **3**.

### Dry run: [1, 2, 3, 1]

| line | houses | linear best |
|---|---|---|
| 0..2 | [1, 2, 3] | 1 + 3 = 4 |
| 1..3 | [2, 3, 1] | 3 (or 2 + 1) = 3 |

Answer = **4**.

---

## 3. Java concepts used in this problem

### 3.1 Reusing a helper on a range

```java
static int robLine(int[] a, int from, int to) { ... }
```

Instead of copying sub-arrays, the helper takes the range's bounds. Both calls share the same array with no extra memory.

### 3.2 The single-house special case

With n = 1, both ranges would be empty (0..−1 and 1..0), giving 0. But a single house has no neighbour, so it can be robbed: `if (nums.length == 1) return nums[0];`.

### 3.3 Circular neighbours with `%` (in the referee)

`(i + 1) % n` is the next house around the circle, so house n − 1's neighbour is house 0. The `n > 1` guard stops a single house from being its own neighbour.

---

## 4. Approach 1: Every circular subset (the referee)

### Code ([M02_HouseRobberII.java:48-60](M02_HouseRobberII.java#L48-L60))

```java
static int brute(int[] a) {
    int n = a.length, best = 0;
    for (int mask = 0; mask < 1 << n; mask++) {
        boolean ok = true;
        for (int i = 0; i < n && ok; i++)
            if ((mask >> i & 1) == 1 && (mask >> ((i + 1) % n) & 1) == 1 && n > 1) ok = false;   // circular neighbour
        if (!ok) continue;
        int s = 0;
        for (int i = 0; i < n; i++) if ((mask >> i & 1) == 1) s += a[i];
        best = Math.max(best, s);
    }
    return best;
}
```

- **Time O(n · 2ⁿ).**

---

## 5. Approach 2: Two linear passes (the file's main solution)

### Code ([M02_HouseRobberII.java:33-46](M02_HouseRobberII.java#L33-L46))

```java
static int rob(int[] nums) {
    if (nums.length == 1) return nums[0];
    return Math.max(robLine(nums, 0, nums.length - 2), robLine(nums, 1, nums.length - 1));
}

static int robLine(int[] a, int from, int to) {      // M01 on a[from..to]
    int prev2 = 0, prev1 = 0;
    for (int i = from; i <= to; i++) {
        int cur = Math.max(prev1, prev2 + a[i]);
        prev2 = prev1;
        prev1 = cur;
    }
    return prev1;
}
```

### Line by line

| Line | What it does |
|---|---|
| `nums.length == 1` | one house, no conflict |
| the first `robLine` | exclude the last house |
| the second `robLine` | exclude the first house |
| `robLine` | exactly [M01](M01_HouseRobber.md#5-approach-2-take-or-skip-with-two-variables-the-files-main-solution) on a range |

### Complexity
- **Time O(n)** (two passes), **space O(1).**

---

## 6. Comparing the approaches

| | Every circular subset | Two lines |
|---|---|---|
| Time | O(n · 2ⁿ) | **O(n)** |

The file checks the two-line version against the brute force on 500 random arrays of 1–14 houses.

---

## 7. Edge cases and common mistakes

**Edge cases:** one house, two houses (the larger: they're adjacent both ways), three houses (only one can be robbed).

**Mistakes:**
1. Forgetting the n = 1 case.
2. Running the linear DP on the whole array once (it may rob both ends).
3. Excluding both ends in a single pass (that misses plans that use one of them).

---

## 8. Interview questions and answers

**Q1. Why does splitting into two lines lose nothing?**
> Any valid circular plan can't rob both house 0 and house n − 1, so it's valid on at least one of the two lines. And every plan on either line is valid on the circle.

**Q2. What other problems use this trick?**
> Maximum Sum Circular Subarray (the answer is either a normal subarray or the total minus a minimum subarray), and circular scheduling problems.

### Follow-up questions

**F1. House Robber III (a tree).** → [Trees M13](../../07-Trees/03-Medium/M13_HouseRobberIII.md)

**F2. Maximum Sum Circular Subarray (LC 918).**
> max(Kadane's maximum, total − Kadane's minimum), unless every number is negative.

**F3. Pizza With 3n Slices (LC 1388).**
> A circular "pick n non-adjacent slices" problem: the same split into two lines, with a 2-D DP on (position, slices taken).

---

## 9. Running the file

```bash
java M02_HouseRobberII.java      # 5/5 tests passed (including 500 random vs every circular subset)
```

**One-line summary:** *"A circle forbids robbing both ends, so take the better of the linear House Robber on houses 0..n − 2 and on houses 1..n − 1 (with n = 1 as a special case)."*
