# H01 · Burst Balloons

| | |
|---|---|
| **LeetCode** | [#312 Burst Balloons](https://leetcode.com/problems/burst-balloons/) |
| **Difficulty** | Hard |
| **Pattern** | **Interval DP** where k is the **last** balloon burst in the interval |
| **Code** | [H01_BurstBalloons.java](H01_BurstBalloons.java) |
| **Run** | `java H01_BurstBalloons.java` |

The interval-DP loop order is explained in [M24](../03-Medium/M24_PalindromicSubstringsDP.md#31-the-interval-dp-loop-order); a simpler split-based interval DP is [M27](../03-Medium/M27_MinimumCostTreeFromLeafValues.md).

---

## 1. The problem

Bursting balloon i earns nums[left] · nums[i] · nums[right], where left and right are its **current** neighbours (anything outside the array counts as 1). What's the maximum number of coins?

| nums | answer | best order |
|---|---|---|
| [3, 1, 5, 8] | 167 | burst 1, 5, 3, 8: 3·1·5 + 3·5·8 + 1·3·8 + 1·8·1 = 15 + 120 + 24 + 8 |
| [1, 5] | 10 | |
| [7] | 7 | |

---

## 2. How to think about it

### Brute force
Every burst order: O(n!).

### Why "the first balloon burst" doesn't split the problem
Bursting k first makes k − 1 and k + 1 neighbours, so the left and right parts still interact. They're not independent subproblems.

### Think of k as the **last** balloon burst in the open interval (l, r)
While everything else in (l, r) is being burst, k is still standing and acts as a **wall**: the left part (l, k) and the right part (k, r) never see each other. When k finally goes, its neighbours are exactly l and r.

| step | for this problem |
|---|---|
| **Padding** | a = [1, nums…, 1] |
| **State** | dp[l][r] = the most coins from bursting every balloon **strictly between** l and r |
| **Transition** | dp[l][r] = max over l < k < r of dp[l][k] + a[l] · a[k] · a[r] + dp[k][r] |
| **Base** | dp[l][l + 1] = 0 (nothing in between) |
| **Order** | by increasing gap r − l |
| **Answer** | dp[0][n + 1] |

"Choose the **last** action so that the rest splits cleanly" is the key move of interval DP.

### Dry run: [3, 1, 5, 8] → a = [1, 3, 1, 5, 8, 1] (from a real run)

| interval (l, r) | dp | best last k |
|---|---|---|
| (1, 3): between 3 and 5, only the 1 | 3·1·5 = 15 | 2 |
| (1, 4): between 3 and 8, the 1 and the 5 | dp[1][3] + 3·5·8 + 0 = 15 + 120 = 135 | 3 (the 5) |
| (0, 4): between 1 and 8, the 3, 1, 5 | 0 + 1·3·8 + dp[1][4] = 24 + 135 = 159 | 1 (the 3) |
| (0, 5): everything | dp[0][4] + 1·8·1 + 0 = 159 + 8 = **167** | 4 (the 8) |

Reading the "last" choices from the inside out gives the burst order 1, 5, 3, 8.

---

## 3. Java concepts used in this problem

### 3.1 Padding with `System.arraycopy`

```java
int[] a = new int[n + 2];
a[0] = a[n + 1] = 1;
System.arraycopy(nums, 0, a, 1, n);
```

The two 1s at the ends mean "outside the array counts as 1" without any special cases.

### 3.2 Looping by gap

```java
for (int gap = 2; gap <= n + 1; gap++)
    for (int l = 0; l + gap <= n + 1; l++) { int r = l + gap; ... }
```

dp[l][r] uses dp[l][k] and dp[k][r], which have smaller gaps, so they're already computed.

### 3.3 `List.remove(int)` in the referee

`rest.remove(i)` removes by **index** (the argument is an `int`), which is what "burst balloon i" means.

---

## 4. Approach 1: Every burst order (the referee)

### Code ([H01_BurstBalloons.java:58-69](H01_BurstBalloons.java#L58-L69))

```java
static int brute(List<Integer> b) {
    if (b.isEmpty()) return 0;
    int best = 0;
    for (int i = 0; i < b.size(); i++) {
        int left = i > 0 ? b.get(i - 1) : 1, right = i < b.size() - 1 ? b.get(i + 1) : 1;
        int coins = left * b.get(i) * right;
        List<Integer> rest = new ArrayList<>(b);
        rest.remove(i);
        best = Math.max(best, coins + brute(rest));
    }
    return best;
}
```

Try bursting each balloon next, and recurse on the rest. **O(n!).** The test keeps n ≤ 7.

---

## 5. Approach 2: Interval DP, last balloon (the file's main solution)

### Code ([H01_BurstBalloons.java:42-55](H01_BurstBalloons.java#L42-L55))

```java
static int maxCoins(int[] nums) {
    int n = nums.length;
    int[] a = new int[n + 2];
    a[0] = a[n + 1] = 1;
    System.arraycopy(nums, 0, a, 1, n);
    int[][] dp = new int[n + 2][n + 2];
    for (int gap = 2; gap <= n + 1; gap++)                  // r - l
        for (int l = 0; l + gap <= n + 1; l++) {
            int r = l + gap;
            for (int k = l + 1; k < r; k++)                 // k = the LAST balloon burst in (l, r)
                dp[l][r] = Math.max(dp[l][r], dp[l][k] + a[l] * a[k] * a[r] + dp[k][r]);
        }
    return dp[0][n + 1];
}
```

| Line | What it does |
|---|---|
| the padding | sentinel 1s at both ends |
| `gap` | intervals from short to long |
| `k` | the balloon burst last in (l, r) |
| the transition | burst the left side, burst the right side, then k with neighbours l and r |

### Complexity
- **Time O(n³), space O(n²).**

---

## 6. Comparing the approaches

| | Every order | Interval DP |
|---|---|---|
| Time | O(n!) | **O(n³)** |
| Space | O(n) stack (plus copies) | O(n²) |

The file checks the DP against all burst orders on 200 random arrays of up to 7 balloons (with zeros).

---

## 7. Edge cases and common mistakes

**Edge cases:** one balloon (its value), balloons worth 0, two balloons.

**Mistakes:**
1. Choosing the **first** balloon burst (the subproblems aren't independent).
2. Forgetting the padding (the edge balloons' neighbours).
3. Using closed intervals [l, r] and getting the neighbours wrong; the open interval (l, r) makes l and r the neighbours.

---

## 8. Interview questions and answers

**Q1. Why the last balloon?**
> If k is the last one burst in (l, r), it separates the two sides for the entire time they're being burst, so they're independent. And when k is burst, its neighbours are known: l and r.

**Q2. What's the general lesson?**
> In interval DP, pick the action that splits the interval into independent pieces. Sometimes that's the first action (a split point), and sometimes, as here, it's the last.

### Follow-up questions

**F1. Minimum Cost to Merge Stones (LC 1000).**
> Interval DP with an extra dimension for how many piles the interval has been merged into.

**F2. Remove Boxes (LC 546).**
> Interval DP with a third index counting the boxes of the same colour attached to the left.

**F3. Matrix chain multiplication.**
> The classic interval DP: choose the last multiplication.

---

## 9. Running the file

```bash
java H01_BurstBalloons.java      # 4/4 tests passed (including 200 random vs every burst order)
```

**One-line summary:** *"Pad with 1s, and let dp[l][r] be the best for the balloons strictly between l and r, choosing the last one burst, k: dp[l][r] = max of dp[l][k] + a[l]·a[k]·a[r] + dp[k][r], filled by increasing gap."*
