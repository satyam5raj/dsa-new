# M05 · Jump Game

| | |
|---|---|
| **LeetCode** | [#55 Jump Game](https://leetcode.com/problems/jump-game/) |
| **Difficulty** | Medium |
| **Pattern** | A DP that **collapses into greedy** (track the farthest reachable index) |
| **Code** | [M05_JumpGame.java](M05_JumpGame.java) |
| **Run** | `java M05_JumpGame.java` |

The DP recipe is explained in [E01](../02-Easy/E01_ClimbingStairs.md#3-java-concepts-used-in-this-problem-and-the-dp-recipe).

---

## 1. The problem

`nums[i]` is the maximum jump length from index i. Starting at index 0, can you reach the last index?

| nums | answer | why |
|---|---|---|
| [2, 3, 1, 1, 4] | true | 0 → 1 → 4 |
| [3, 2, 1, 0, 4] | false | every path lands on index 3, whose jump is 0 |
| [0] | true | already at the end |
| [0, 1] | false | |

---

## 2. How to think about it

### The DP version (O(n²))

| step | for this problem |
|---|---|
| **State** | good[i] = can the last index be reached starting from i? |
| **Transition** | good[i] = any good[j] for j in i + 1 .. i + nums[i] |
| **Base** | good[n − 1] = true |
| **Order** | right to left |
| **Answer** | good[0] |

### From DP to greedy
The set of indices reachable from the start is always a **prefix** 0..far: if you can reach j, you can reach every index before it (any jump that passes over an index could have stopped there instead). So a single number describes the whole state:

```
far = max over reachable i of (i + nums[i])
```

Scan left to right. If i > far, index i can't be reached, so you're stuck: return false.

This happens with many DP problems: once the state collapses to "a single best value", the DP **is** a greedy.

### Dry run: the greedy on [3, 2, 1, 0, 4]

| i | nums[i] | i ≤ far? | far after |
|---|---|---|---|
| 0 | 3 | yes (0 ≤ 0) | 3 |
| 1 | 2 | yes | 3 |
| 2 | 1 | yes | 3 |
| 3 | 0 | yes | 3 |
| 4 | | **4 > 3** | → **false** |

### Dry run: the greedy on [2, 3, 1, 1, 4]
i = 0: far = 2. i = 1: far = max(2, 4) = 4 ≥ 4 → **true** (it returns early).

### Dry run: the DP on [3, 2, 1, 0, 4]

| i | reaches j in | good[i] |
|---|---|---|
| 4 | | true (base) |
| 3 | (none: nums[3] = 0) | false |
| 2 | 3 | false |
| 1 | 2, 3 | false |
| 0 | 1, 2, 3 | **false** |

---

## 3. Java concepts used in this problem

### 3.1 Early returns in a scan

```java
if (i > far) return false;
far = Math.max(far, i + nums[i]);
if (far >= nums.length - 1) return true;
```

The loop stops as soon as the answer is known in either direction.

### 3.2 Stopping an inner loop with a condition

```java
for (int j = i + 1; j <= Math.min(n - 1, i + nums[i]) && !good[i]; j++) good[i] = good[j];
```

`&& !good[i]` in the loop condition stops scanning once one good successor is found. `Math.min(n − 1, …)` keeps j inside the array.

---

## 4. Approach 1: DP from the right (the referee)

### Code ([M05_JumpGame.java:46-53](M05_JumpGame.java#L46-L53))

```java
static boolean canJumpDP(int[] nums) {
    int n = nums.length;
    boolean[] good = new boolean[n];
    good[n - 1] = true;
    for (int i = n - 2; i >= 0; i--)
        for (int j = i + 1; j <= Math.min(n - 1, i + nums[i]) && !good[i]; j++) good[i] = good[j];
    return good[0];
}
```

- **Time O(n²)** in the worst case, **space O(n).**

---

## 5. Approach 2: Greedy farthest reach (the file's main solution)

### Code ([M05_JumpGame.java:36-44](M05_JumpGame.java#L36-L44))

```java
static boolean canJump(int[] nums) {
    int far = 0;
    for (int i = 0; i < nums.length; i++) {
        if (i > far) return false;                    // index i can't be reached
        far = Math.max(far, i + nums[i]);
        if (far >= nums.length - 1) return true;
    }
    return true;
}
```

### Line by line

| Line | What it does |
|---|---|
| `far = 0` | only index 0 is reachable at the start |
| `i > far` | there's a gap: nothing reaches i, so nothing beyond it either |
| `far = max(...)` | from i we can reach up to i + nums[i] |
| `far >= n − 1` | the end is reachable |

### Complexity
- **Time O(n), space O(1).**

---

## 6. Comparing the approaches

| | DP | Greedy |
|---|---|---|
| Time | O(n²) | **O(n)** |
| Space | O(n) | O(1) |
| Why it's correct | the definition | the reachable set is a prefix |

The file checks the greedy against the DP on 2,000 random arrays with jumps 0–3.

---

## 7. Edge cases and common mistakes

**Edge cases:** a single element (true), a 0 at the start with more elements (false), zeros that can be jumped over.

**Mistakes:**
1. Updating `far` before checking `i > far` (it lets an unreachable index extend the reach).
2. Treating `nums[i]` as an exact jump instead of a maximum.
3. Doing BFS or DFS over all jumps (correct but O(n²)).

---

## 8. Interview questions and answers

**Q1. Why is the reachable set always a prefix?**
> If index j is reachable, some jump from an earlier reachable index i covers j, and that same jump could have stopped at any index between i and j. So every index up to j is reachable.

**Q2. When does a DP collapse into a greedy?**
> When the DP's state can be summarised by a single best value (here, the farthest reach) that never needs to be "undone".

### Follow-up questions

**F1. The minimum number of jumps.** → [M06 Jump Game II](M06_JumpGameII.md)

**F2. Jump Game III (LC 1306): jump exactly ±nums[i].**
> BFS or DFS over indices, since reachability is no longer a prefix.

**F3. Jump Game IV.** → [Graph H08](../../09-Graph/04-Hard/H08_JumpGameIV.md)
> BFS with "teleports" between equal values.

---

## 9. Running the file

```bash
java M05_JumpGame.java      # 5/5 tests passed (including 2000 random: greedy == DP)
```

**One-line summary:** *"Scan left to right keeping the farthest reachable index; if some index lies beyond it you're stuck, and once it covers the last index you can reach the end."*
