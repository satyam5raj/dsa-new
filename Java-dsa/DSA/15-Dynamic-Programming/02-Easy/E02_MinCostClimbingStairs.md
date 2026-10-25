# E02 · Min Cost Climbing Stairs ⭐

| | |
|---|---|
| **LeetCode** | [#746 Min Cost Climbing Stairs](https://leetcode.com/problems/min-cost-climbing-stairs/) |
| **Difficulty** | Easy |
| **Pattern** | 1-D DP with **min** instead of **+** (from counting to optimisation) |
| **Code** | [E02_MinCostClimbingStairs.java](E02_MinCostClimbingStairs.java) |
| **Run** | `java E02_MinCostClimbingStairs.java` |

⭐ = not in `leetcode.md`, added for a key technique. The DP recipe is explained in [E01](E01_ClimbingStairs.md#3-java-concepts-used-in-this-problem-and-the-dp-recipe).

---

## 1. The problem

`cost[i]` is paid when you step **on** stair i. You may start on stair 0 or stair 1. Each move climbs 1 or 2 stairs. The top is just past the last stair. What's the minimum total cost?

| cost | answer | how |
|---|---|---|
| [10, 15, 20] | 15 | start on stair 1 (pay 15), jump 2 to the top |
| [1,100,1,1,1,100,1,1,100,1] | 6 | step only on the 1s |
| [0, 0] | 0 | |

---

## 2. How to think about it

The same shape as [Climbing Stairs](E01_ClimbingStairs.md), but we **minimise** instead of **count**: the last move came from i − 1 or from i − 2, and we take the cheaper.

| step | for this problem |
|---|---|
| **State** | dp[i] = the minimum cost to **arrive** at position i (not yet paying cost[i]) |
| **Transition** | dp[i] = min(dp[i − 1] + cost[i − 1], dp[i − 2] + cost[i − 2]) |
| **Base** | dp[0] = dp[1] = 0 (you may start on either for free) |
| **Answer** | dp[n] (the top, one past the last stair) |

### The state definition is a choice
"Cost to **arrive** at i" (pay when you leave a stair) and "cost to **stand on** i, including cost[i]" both work, as long as the base cases and the answer cell match the definition. The file's referee uses the second one; see section 4.

### Dry run: [10, 15, 20]

| i | dp[i] |
|---|---|
| 0 | 0 |
| 1 | 0 |
| 2 | min(dp[1] + 15, dp[0] + 10) = min(15, 10) = 10 |
| 3 | min(dp[2] + 20, dp[1] + 15) = min(30, 15) = **15** |

### Dry run: [1,100,1,1,1,100,1,1,100,1] (from a real run)

| i | 0 | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 | 10 |
|---|---|---|---|---|---|---|---|---|---|---|---|
| dp[i] | 0 | 0 | 1 | 2 | 2 | 3 | 3 | 4 | 4 | 5 | **6** |

---

## 3. Java concepts used in this problem

### 3.1 `Math.min` as the transition

```java
int cur = Math.min(prev1 + cost[i - 1], prev2 + cost[i - 2]);
```

Counting problems combine the choices with `+`; optimisation problems combine them with `min` or `max`. That's the only structural difference from E01.

### 3.2 A loop that runs to `cost.length` (inclusive)

The answer is position n, one **past** the last index, so the loop is `i <= cost.length`, while the costs read are `cost[i − 1]` and `cost[i − 2]`, which are always in range.

### 3.3 Top-down recursion with two starting points (the referee)

```java
Math.min(brute(c, 0), brute(c, 1))
```

The referee's state is "the cost from standing on i to the top", so the answer is the better of the two allowed starts.

---

## 4. Approach 1: Recursion from each stair (the referee)

### Code ([E02_MinCostClimbingStairs.java:43-46](E02_MinCostClimbingStairs.java#L43-L46))

```java
static int brute(int[] cost, int i) {                // min cost from standing on i (paying it) to the top
    if (i >= cost.length) return 0;
    return cost[i] + Math.min(brute(cost, i + 1), brute(cost, i + 2));
}
```

| Line | What it does |
|---|---|
| `i >= cost.length` | at (or past) the top: nothing more to pay |
| the return | pay for this stair, then take the cheaper of the two next moves |

It uses the other state definition (stand on i and pay cost[i]) and runs in **O(2ⁿ)**. Memoizing it on i would make it O(n).

---

## 5. Approach 2: Bottom-up with two variables (the file's main solution)

### Code ([E02_MinCostClimbingStairs.java:33-41](E02_MinCostClimbingStairs.java#L33-L41))

```java
static int minCostClimbingStairs(int[] cost) {
    int prev2 = 0, prev1 = 0;                         // dp[i-2], dp[i-1]
    for (int i = 2; i <= cost.length; i++) {
        int cur = Math.min(prev1 + cost[i - 1], prev2 + cost[i - 2]);
        prev2 = prev1;
        prev1 = cur;
    }
    return prev1;
}
```

### Line by line

| Line | What it does |
|---|---|
| `prev2 = prev1 = 0` | dp[0] = dp[1] = 0 |
| `cur` | arrive at i from i − 1 (paying cost[i − 1]) or from i − 2 (paying cost[i − 2]) |
| the shift | slide the window |
| `return prev1` | dp[n] |

### Complexity
- **Time O(n), space O(1).**

---

## 6. Comparing the approaches

| | Recursion | Two variables |
|---|---|---|
| State | cost from standing on i | cost to arrive at i |
| Time | O(2ⁿ) | **O(n)** |
| Space | O(n) stack | O(1) |

The file checks the fast version against the recursion on 500 random cost arrays of length 2–16.

---

## 7. Edge cases and common mistakes

**Edge cases:** exactly two stairs (the answer is the cheaper one), all costs zero, one very expensive stair.

**Mistakes:**
1. Returning dp[n − 1] (the last stair) instead of dp[n] (the top).
2. Mixing the two state definitions (for example "arrive at" transitions with "stand on" base cases).
3. Forgetting that you may start on stair 1 for free.

---

## 8. Interview questions and answers

**Q1. How is this different from Climbing Stairs?**
> The same recurrence shape, but the two predecessors are combined with `min` (and each adds a cost) instead of `+`. Counting becomes optimisation.

**Q2. Why are both base cases 0?**
> You can start on stair 0 or stair 1 without paying anything to get there.

### Follow-up questions

**F1. Print the cheapest path.**
> Keep the full dp array and walk back from n, choosing whichever predecessor gave the minimum.

**F2. Moves of 1, 2 or 3.**
> Take the minimum over three predecessors.

**F3. House Robber.** → [M01](../03-Medium/M01_HouseRobber.md)
> Another "take or skip" 1-D DP, maximising instead.

---

## 9. Running the file

```bash
java E02_MinCostClimbingStairs.java      # 4/4 tests passed (including 500 random vs recursion)
```

**One-line summary:** *"dp[i] = the minimum cost to arrive at i = min(dp[i − 1] + cost[i − 1], dp[i − 2] + cost[i − 2]), with dp[0] = dp[1] = 0 and the answer at dp[n]."*
