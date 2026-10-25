# M01 · House Robber

| | |
|---|---|
| **LeetCode** | [#198 House Robber](https://leetcode.com/problems/house-robber/) |
| **Difficulty** | Medium |
| **Pattern** | 1-D DP: **take it or skip it** |
| **Code** | [M01_HouseRobber.java](M01_HouseRobber.java) |
| **Run** | `java M01_HouseRobber.java` |

The DP recipe is explained in [E01](../02-Easy/E01_ClimbingStairs.md#3-java-concepts-used-in-this-problem-and-the-dp-recipe).

---

## 1. The problem

Houses in a row hold some money. Robbing two **adjacent** houses triggers the alarm. What's the maximum you can rob?

| nums | answer | robbed |
|---|---|---|
| [1, 2, 3, 1] | 4 | 1 + 3 |
| [2, 7, 9, 3, 1] | 12 | 2 + 9 + 1 |
| [2, 3, 2] | 4 | 2 + 2 |
| [5] | 5 | |

---

## 2. How to think about it

### Brute force
Every subset with no two adjacent houses: O(2ⁿ).

### Greedy fails
"Take the biggest" on [2, 3, 2] takes 3 and then can't take either 2: total 3. But 2 + 2 = 4.

### Decide house i
Either:
- **skip it**: the best is whatever the first i − 1 houses give, dp[i − 1];
- **rob it**: house i − 1 is then forbidden, so dp[i − 2] + a[i].

| step | for this problem |
|---|---|
| **State** | dp[i] = the most money from houses 0..i |
| **Transition** | dp[i] = max(dp[i − 1], dp[i − 2] + a[i]) |
| **Base** | dp[−1] = dp[−2] = 0 (no houses, no money) |
| **Answer** | dp[n − 1] |

Only the two previous values are read, so O(1) space.

### Dry run: [2, 7, 9, 3, 1]

| house | value | skip (prev1) | rob (prev2 + value) | best | (prev2, prev1) after |
|---|---|---|---|---|---|
| start | | | | | (0, 0) |
| 0 | 2 | 0 | 0 + 2 | 2 | (0, 2) |
| 1 | 7 | 2 | 0 + 7 | 7 | (2, 7) |
| 2 | 9 | 7 | 2 + 9 | 11 | (7, 11) |
| 3 | 3 | 11 | 7 + 3 | 11 | (11, 11) |
| 4 | 1 | 11 | 11 + 1 | **12** | (11, 12) |

---

## 3. Java concepts used in this problem

### 3.1 The enhanced `for` with rolling variables

```java
for (int x : nums) {
    int cur = Math.max(prev1, prev2 + x);
    prev2 = prev1;
    prev1 = cur;
}
```

Because dp[i] only depends on the value x and the two previous results, there's no need for indices at all.

### 3.2 Representing "no two adjacent" with bits (in the referee)

```java
if ((mask & (mask >> 1)) != 0) continue;
```

Shifting the mask right by one lines each bit up with its neighbour. If any bit is set in both, two adjacent houses were chosen.

### 3.3 `1 << a.length` in a loop condition

`mask < 1 << a.length` parses as `mask < (1 << a.length)`, because shifts bind tighter than comparisons.

---

## 4. Approach 1: Every valid subset (the referee)

### Code ([M01_HouseRobber.java:49-58](M01_HouseRobber.java#L49-L58))

```java
static int brute(int[] a) {                           // every subset with no two adjacent
    int best = 0;
    for (int mask = 0; mask < 1 << a.length; mask++) {
        if ((mask & (mask >> 1)) != 0) continue;       // two adjacent bits set
        int s = 0;
        for (int i = 0; i < a.length; i++) if ((mask >> i & 1) == 1) s += a[i];
        best = Math.max(best, s);
    }
    return best;
}
```

- **Time O(n · 2ⁿ).** The test uses up to 15 houses.

---

## 5. Approach 2: Take or skip with two variables (the file's main solution)

### Code ([M01_HouseRobber.java:39-47](M01_HouseRobber.java#L39-L47))

```java
static int rob(int[] nums) {
    int prev2 = 0, prev1 = 0;                          // best up to i-2, best up to i-1
    for (int x : nums) {
        int cur = Math.max(prev1, prev2 + x);          // skip vs rob
        prev2 = prev1;
        prev1 = cur;
    }
    return prev1;
}
```

### Line by line

| Line | What it does |
|---|---|
| `prev2, prev1` | dp[i − 2] and dp[i − 1], starting as "no houses" |
| `cur` | the better of skipping and robbing house i |
| the shift | move the window |
| `return prev1` | the best over all houses |

### Complexity
- **Time O(n), space O(1).**

---

## 6. Comparing the approaches

| | Every subset | Greedy (largest first) | DP |
|---|---|---|---|
| Correct | yes | **no** ([2, 3, 2]) | yes |
| Time | O(n · 2ⁿ) | O(n log n) | **O(n)** |

The file checks the DP against the subset enumeration on 500 random arrays.

---

## 7. Edge cases and common mistakes

**Edge cases:** one house, two houses (the larger), all zeros, alternating large and small.

**Mistakes:**
1. Thinking the answer is "the sum of the even indices or the odd indices" (fails on [2, 1, 1, 2]: the best is 2 + 2 = 4, using indices 0 and 3).
2. Shifting `prev1` before saving it into `prev2`.
3. Using greedy.

---

## 8. Interview questions and answers

**Q1. Why is the transition correct?**
> In the best plan for houses 0..i, house i is either robbed (then i − 1 isn't, and the rest is the best plan for 0..i − 2) or not robbed (then it's the best plan for 0..i − 1).

**Q2. Can you recover which houses were robbed?**
> Keep the full dp array, then walk back from the end: if dp[i] == dp[i − 1], house i was skipped; otherwise it was robbed, so jump to i − 2.

### Follow-up questions

**F1. Houses in a circle.** → [M02](M02_HouseRobberII.md)
> Split into two lines that each exclude one end.

**F2. Houses in a binary tree.** → [Trees M13 House Robber III](../../07-Trees/03-Medium/M13_HouseRobberIII.md)
> Each node returns a pair: (best if robbed, best if not robbed).

**F3. Delete and Earn (LC 740).**
> Turn it into House Robber over the values: points[v] = v · count(v), and taking v forbids v − 1 and v + 1.

---

## 9. Running the file

```bash
java M01_HouseRobber.java      # 5/5 tests passed (including 500 random vs every valid subset)
```

**One-line summary:** *"For each house, the best so far is max(skip it: dp[i − 1], rob it: dp[i − 2] + a[i]); keep only the last two values."*
