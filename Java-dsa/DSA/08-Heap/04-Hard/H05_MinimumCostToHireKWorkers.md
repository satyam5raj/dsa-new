# H05 · Minimum Cost to Hire K Workers

| | |
|---|---|
| **LeetCode** | [#857 Minimum Cost to Hire K Workers](https://leetcode.com/problems/minimum-cost-to-hire-k-workers/) |
| **Difficulty** | Hard |
| **Pattern** | Sort by ratio + a max-heap of qualities (keep the k smallest sum) |
| **Code** | [H05_MinimumCostToHireKWorkers.java](H05_MinimumCostToHireKWorkers.java) |
| **Run** | `java H05_MinimumCostToHireKWorkers.java` |

PriorityQueue basics are explained in [E01](../02-Easy/E01_KthLargestInStream.md#3-java-concepts-used-in-this-problem). The "sort by one key, heap by another" shape was introduced in [H03 IPO](H03_IPO.md).

---

## 1. The problem

Worker i has `quality[i]` and a minimum wage `wage[i]`. Hire **exactly k** workers so that:
- the pay is **proportional to quality** within the group
- everyone gets at least their minimum wage

What's the minimum total cost?

| quality | wage | k | answer |
|---|---|---|---|
| [10,20,5] | [70,50,30] | 2 | 105.0 (workers 0 and 2: 70 + 35) |
| [3,1,10,10,1] | [4,8,2,2,7] | 3 | 30.66667 |

---

## 2. How to think about it

### The math
Proportional pay means there's one **rate r** (money per unit of quality) for the whole group. Worker i is satisfied iff

```
r · quality[i] ≥ wage[i]   ⇔   r ≥ wage[i] / quality[i]   (worker i's "ratio")
```

So r must be at least the **largest ratio in the group**, and paying more is pointless:

```
r = the max ratio in the group,      cost = r × (sum of the qualities in the group)
```

### The search
Sort the workers by ratio. Fix the **"captain"**, the worker with the max ratio in the group: everyone else must come from **earlier** in the sorted order (a smaller ratio). For that captain, cost = ratio_captain × (quality sum), so pick the k workers (captain included) with the **smallest qualities** among those seen so far.

→ Sweep in ratio order, keeping the k smallest qualities in a **max-heap of size k** (top-k smallest, like [K Closest Points](../03-Medium/M02_KClosestPointsToOrigin.md)) with their running sum. Whenever the heap holds k: `cost = ratio_current × sum`.

**What if the current worker gets popped right away?** Then the cost computed with its ratio belongs to a group whose real max ratio is smaller, so it's an **overestimate** of a group that was already considered. Harmless.

### Dry run: quality [10, 20, 5], wage [70, 50, 30], k = 2 (from the real run)

| worker | ratio | heap (qualities) | sum | cost |
|---|---|---|---|---|
| w1 | 2.5 | {20} | 20 | (only 1) |
| w2 | 6.0 | {20, 5} | 25 | 6.0 × 25 = 150 |
| w0 | 7.0 | push 10, pop 20 → {10, 5} | 15 | 7.0 × 15 = **105** |

---

## 3. Java concepts used in this problem

### 3.1 Sorting indices by a `double` key

```java
Arrays.sort(order, Comparator.comparingDouble(i -> (double) wage[i] / quality[i]));
```

The `(double)` cast is essential: `wage[i] / quality[i]` on ints would truncate (70/20 = 3, not 3.5).

### 3.2 `long sum` and a `double` result

The quality sum stays an exact `long`; multiplying by the double ratio gives the cost. `Double.MAX_VALUE` is the initial "infinity".

### 3.3 Comparing doubles in tests

```java
Math.abs(a - b) < 1e-6
```

Floating-point results can differ in the last bits depending on the order of the operations, so the test compares within a tolerance instead of with `==`. The examples are formatted with `String.format("%.5f", x)`.

---

## 4. Approach 1: Every group of k (the referee)

### Code ([H05_MinimumCostToHireKWorkers.java:64-75](H05_MinimumCostToHireKWorkers.java#L64-L75))

```java
static double brute(int[] q, int[] w, int k) {
    int n = q.length;
    double best = Double.MAX_VALUE;
    for (int mask = 0; mask < (1 << n); mask++) {
        if (Integer.bitCount(mask) != k) continue;
        double rate = 0;
        long sum = 0;
        for (int i = 0; i < n; i++) if ((mask >> i & 1) == 1) { rate = Math.max(rate, (double) w[i] / q[i]); sum += q[i]; }
        best = Math.min(best, rate * sum);
    }
    return best;
}
```

For each group of exactly k: rate = its max ratio, cost = rate × total quality. **O(2ⁿ · n).**

---

## 5. Approach 2: Try every captain, with a sort of the qualities

```java
static double mincostCaptains(int[] quality, int[] wage, int k) {
    int n = quality.length;
    double best = Double.MAX_VALUE;
    for (int c = 0; c < n; c++) {
        double r = (double) wage[c] / quality[c];
        List<Integer> qs = new ArrayList<>();
        for (int i = 0; i < n; i++) if (i != c && (double) wage[i] / quality[i] <= r) qs.add(quality[i]);
        if (qs.size() < k - 1) continue;
        Collections.sort(qs);
        long sum = quality[c];
        for (int i = 0; i < k - 1; i++) sum += qs.get(i);
        best = Math.min(best, r * sum);
    }
    return best;
}
```

For each captain: everyone with a ratio ≤ the captain's is eligible; take the k − 1 smallest qualities. **O(n² log n)**. It's the same idea as the heap version, without sharing work between captains.

---

## 6. Approach 3: Sweep by ratio + max-heap of qualities (the file's solution)

### Code ([H05_MinimumCostToHireKWorkers.java:46-61](H05_MinimumCostToHireKWorkers.java#L46-L61))

```java
static double mincostToHireWorkers(int[] quality, int[] wage, int k) {
    int n = quality.length;
    Integer[] order = new Integer[n];
    for (int i = 0; i < n; i++) order[i] = i;
    Arrays.sort(order, Comparator.comparingDouble(i -> (double) wage[i] / quality[i]));   // by ratio
    PriorityQueue<Integer> maxQ = new PriorityQueue<>(Comparator.reverseOrder());       // qualities kept
    long sum = 0;
    double best = Double.MAX_VALUE;
    for (int i : order) {
        maxQ.offer(quality[i]);
        sum += quality[i];
        if (maxQ.size() > k) sum -= maxQ.poll();          // drop the largest quality
        if (maxQ.size() == k) best = Math.min(best, sum * ((double) wage[i] / quality[i]));
    }
    return best;
}
```

### Line by line

| Line | What it does |
|---|---|
| sort by ratio | the current worker always has the largest ratio so far: the captain |
| `maxQ` | the qualities of the k cheapest-to-include workers so far |
| `sum += quality[i]` | the running total of the kept qualities |
| `if (maxQ.size() > k) sum -= maxQ.poll();` | drop the largest quality, which costs the most |
| `if (maxQ.size() == k)` | a full group → its cost at the captain's rate |

### Complexity
- **Time O(n log n).**
- **Space O(n).**

---

## 7. Comparing the approaches

| | Every group | Every captain | Sweep + heap |
|---|---|---|---|
| Time | O(2ⁿ · n) | O(n² log n) | **O(n log n)** |
| Space | O(1) | O(n) | O(n) |

The file checks the sweep against every group on 1,000 random cases (within 1e-6).

---

## 8. Edge cases and common mistakes

**Edge cases:** k = 1 (the cheapest single minimum wage), k = n (everyone, at the max ratio), equal ratios.

**Mistakes:**
1. Integer division when computing the ratios.
2. Using a min-heap (that drops the smallest quality, the opposite of what we want).
3. Computing the cost before the heap has k workers.
4. Sorting by wage or quality instead of the ratio.

---

## 9. Interview questions and answers

**Q1. Why is the rate the maximum ratio in the group?**
> Every worker needs r ≥ their ratio, so r ≥ the max ratio. Any larger r only increases the cost, so the optimum is exactly the max.

**Q2. Why does keeping the k smallest qualities work?**
> With the captain fixed, the cost is the captain's ratio × the sum of the qualities, and every earlier worker is eligible. Minimising the sum means choosing the smallest qualities, which is a top-k-smallest problem: a max-heap of size k.

### Follow-up questions

**F1. Maximum Performance of a Team.** → [H07](H07_MaximumPerformanceOfATeam.md)
> The same skeleton: fix the min efficiency (the bottleneck), maximise the sum of speeds.

**F2. Return the chosen workers.**
> Store `{quality, index}` in the heap and snapshot the heap's contents when the best cost is updated.

**F3. Pay must be integers.**
> Round each pay up: the cost function changes, and the sweep needs care with rounding.

---

## 10. Running the file

```bash
java H05_MinimumCostToHireKWorkers.java      # 3/3 tests passed (including 1,000 random cases vs all groups)
```

**One-line summary:** *"Sort by wage/quality; sweep, keeping the k smallest qualities in a max-heap with their sum; each full heap gives a cost of the current ratio × sum."*
