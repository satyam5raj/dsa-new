# M06 · Jump Game II

| | |
|---|---|
| **LeetCode** | [#45 Jump Game II](https://leetcode.com/problems/jump-game-ii/) |
| **Difficulty** | Medium |
| **Pattern** | Min-steps DP → an implicit **BFS by levels** (greedy windows) |
| **Code** | [M06_JumpGameII.java](M06_JumpGameII.java) |
| **Run** | `java M06_JumpGameII.java` |

The reachability version is [M05 Jump Game](M05_JumpGame.md); the DP recipe is in [E01](../02-Easy/E01_ClimbingStairs.md#3-java-concepts-used-in-this-problem-and-the-dp-recipe).

---

## 1. The problem

The same jumps as Jump Game, and the end is guaranteed to be reachable. What's the **minimum** number of jumps?

| nums | answer | path |
|---|---|---|
| [2, 3, 1, 1, 4] | 2 | 0 → 1 → 4 |
| [2, 3, 0, 1, 4] | 2 | 0 → 1 → 4 |
| [0] | 0 | already there |
| [1, 1, 1, 1] | 3 | |

---

## 2. How to think about it

### The DP version (O(n²))

| step | for this problem |
|---|---|
| **State** | dp[i] = the minimum number of jumps to reach i |
| **Transition** | for each reachable i: dp[j] = min(dp[j], dp[i] + 1) for j in i + 1 .. i + nums[i] |
| **Base** | dp[0] = 0, everything else ∞ |
| **Answer** | dp[n − 1] |

### BFS levels
The indices reachable with **exactly k** jumps (and not fewer) form a contiguous **window** [start, end]. The next window runs from end + 1 to the farthest i + nums[i] over the current window. Count the windows until one covers n − 1.

```
jumps = 0, end = 0, far = 0
for i in 0 .. n−2:
    far = max(far, i + nums[i])
    if i == end:                      finished this level
        jumps++, end = far
```

It's BFS on an implicit graph where each level is an interval, done in O(n) with no queue.

### Dry run: [2, 3, 1, 1, 4]

| i | nums[i] | far | i == end? | jumps | end |
|---|---|---|---|---|---|
| start | | 0 | | 0 | 0 |
| 0 | 2 | 2 | yes | 1 | 2 |
| 1 | 3 | 4 | no | 1 | 2 |
| 2 | 1 | 4 | yes | 2 | 4 |
| 3 | 1 | 4 | no | 2 | 4 |

The loop stops before the last index; the answer is **2**. Level 0 is {0}, level 1 is {1, 2}, and level 2 is {3, 4}.

### Dry run: [2, 3, 0, 1, 4]
i = 0: far = 2, end reached → jumps 1, end 2. i = 1: far = 4. i = 2: far = 4, end reached → jumps 2, end 4. i = 3: far = 4. Answer **2**.

---

## 3. Java concepts used in this problem

### 3.1 Looping to `n − 2`

`for (int i = 0; i < nums.length - 1; i++)` stops before the last index. If the last index were processed and happened to equal `end`, it would count an extra jump that's never needed.

### 3.2 `Integer.MAX_VALUE` as infinity, with a guard

```java
Arrays.fill(dp, Integer.MAX_VALUE);
if (dp[i] == Integer.MAX_VALUE) continue;
```

The guard prevents computing `MAX_VALUE + 1`, which would overflow to a negative number and look like a very good answer.

### 3.3 Generating only valid tests (in the test)

`if (jumpDP(a) == Integer.MAX_VALUE) continue;` skips random arrays whose end is unreachable, because the problem guarantees reachability.

---

## 4. Approach 1: DP relaxation (the referee)

### Code ([M06_JumpGameII.java:51-61](M06_JumpGameII.java#L51-L61))

```java
static int jumpDP(int[] nums) {
    int n = nums.length;
    int[] dp = new int[n];
    Arrays.fill(dp, Integer.MAX_VALUE);
    dp[0] = 0;
    for (int i = 0; i < n; i++) {
        if (dp[i] == Integer.MAX_VALUE) continue;
        for (int j = i + 1; j <= Math.min(n - 1, i + nums[i]); j++) dp[j] = Math.min(dp[j], dp[i] + 1);
    }
    return dp[n - 1];
}
```

Every reachable index improves its successors. Processing left to right works because jumps only go forward.

- **Time O(n²), space O(n).**

---

## 5. Approach 2: BFS by windows (the file's main solution)

### Code ([M06_JumpGameII.java:39-49](M06_JumpGameII.java#L39-L49))

```java
static int jump(int[] nums) {
    int jumps = 0, end = 0, far = 0;
    for (int i = 0; i < nums.length - 1; i++) {
        far = Math.max(far, i + nums[i]);
        if (i == end) {                               // the current level is exhausted -> take a jump
            jumps++;
            end = far;
        }
    }
    return jumps;
}
```

### Line by line

| Line | What it does |
|---|---|
| `end` | the last index of the current BFS level |
| `far` | the farthest index reachable from any index seen so far (the next level's end) |
| `i == end` | the whole current level has been scanned: one more jump reaches up to `far` |

### Complexity
- **Time O(n), space O(1).**

---

## 6. Comparing the approaches

| | DP | BFS windows |
|---|---|---|
| Time | O(n²) | **O(n)** |
| Space | O(n) | O(1) |

The file checks the window version against the DP on 1,000 random reachable arrays.

---

## 7. Edge cases and common mistakes

**Edge cases:** one element (0 jumps), every jump 1 (n − 1 jumps), a first jump that reaches the end at once.

**Mistakes:**
1. Looping through the last index (an off-by-one extra jump).
2. Jumping greedily to the index with the largest nums[i] (that isn't the same as maximising i + nums[i]).
3. Overflow with `MAX_VALUE + 1` in the DP.

---

## 8. Interview questions and answers

**Q1. Why is this BFS?**
> Each jump count defines a level of indices. Within a level, every index is reachable with the same number of jumps, and the next level is everything newly reachable from it. Because levels are intervals, the next level's end is just the maximum of i + nums[i].

**Q2. Why does the window greedy give the minimum?**
> BFS visits levels in increasing order of jump count, so the first level that contains n − 1 gives the minimum.

### Follow-up questions

**F1. Jump Game VI (score with a window of k).** → [Queue M06](../../05-Queue/03-Medium/M06_JumpGameVI.md)
> DP with a sliding-window maximum (monotonic deque).

**F2. Jump Game IV.** → [Graph H08](../../09-Graph/04-Hard/H08_JumpGameIV.md)
> Explicit BFS, because jumps can go backwards and teleport.

**F3. Video Stitching / Minimum Taps to Water a Garden (LC 1024, 1326).**
> The same "interval levels" greedy over ranges.

---

## 9. Running the file

```bash
java M06_JumpGameII.java      # 5/5 tests passed (including 1000 random reachable: BFS levels == DP)
```

**One-line summary:** *"Treat the indices reachable with k jumps as one interval: scan it while tracking the farthest reach, and when the scan hits the interval's end, take a jump and extend the end to that farthest reach."*
