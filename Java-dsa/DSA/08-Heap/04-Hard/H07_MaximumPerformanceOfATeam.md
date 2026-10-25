# H07 · Maximum Performance of a Team

| | |
|---|---|
| **LeetCode** | [#1383 Maximum Performance of a Team](https://leetcode.com/problems/maximum-performance-of-a-team/) |
| **Difficulty** | Hard |
| **Pattern** | Sort by efficiency (descending) + a min-heap of speeds (keep the k fastest) |
| **Code** | [H07_MaximumPerformanceOfATeam.java](H07_MaximumPerformanceOfATeam.java) |
| **Run** | `java H07_MaximumPerformanceOfATeam.java` |

PriorityQueue basics are explained in [E01](../02-Easy/E01_KthLargestInStream.md#3-java-concepts-used-in-this-problem). This is the same skeleton as [H05 Hire K Workers](H05_MinimumCostToHireKWorkers.md).

---

## 1. The problem

Engineer i has `speed[i]` and `efficiency[i]`. Pick **at most k** engineers.

```
performance = (sum of their speeds) × (the MIN efficiency among them)
```

Return the maximum performance modulo 10⁹ + 7.

```
n = 6, speed [2, 10, 3, 1, 5, 8], efficiency [5, 4, 3, 9, 7, 2]
k = 2 → 60    (engineers 1 and 4: (10 + 5) × min(4, 7) = 60)
k = 3 → 68
k = 4 → 72
```

**Constraints:** `1 ≤ k ≤ n ≤ 10⁵`, speeds ≤ 10⁵, efficiencies ≤ 10⁸.

---

## 2. How to think about it

### Fix the bottleneck, optimise the sum
As in [H05](H05_MinimumCostToHireKWorkers.md): one factor is a min/max over the group, the other is a sum.

Fix the engineer with the **minimum efficiency** in the team (the "bottleneck"). Everyone else must have efficiency ≥ theirs. Sort by efficiency **descending**: when we reach engineer i, everyone before it qualifies. The best team with i as the bottleneck is:

```
i + the (k − 1) FASTEST engineers seen before it
```

→ Keep the **k largest speeds** in a **min-heap of size k** (top-k), with a running sum. After adding i: if the size exceeds k, pop the slowest. Candidate = sum × efficiency[i].

**If i itself gets popped** (it's the slowest), the candidate uses i's efficiency for a group that doesn't contain i, which is lower than that group's real minimum. So it's an **underestimate**, never a wrong maximum.

### The modulo trap
Compare the **real** products: they fit in a `long` (10⁵ × 10⁵ × 10⁸ = 10¹⁸ < 9.2 · 10¹⁸). Take `%` only at the very end. Taking it earlier would make the comparisons meaningless.

### Dry run: k = 2, sorted by efficiency (descending)

| (speed, eff) | heap of speeds | sum | candidate |
|---|---|---|---|
| (1, 9) | {1} | 1 | 9 |
| (5, 7) | {1, 5} | 6 | 42 |
| (2, 5) | push 2, pop 1 → {2, 5} | 7 | 35 |
| (10, 4) | push 10, pop 2 → {5, 10} | 15 | **60** |
| (3, 3) | push 3, pop 3 → {5, 10} | 15 | 45 |
| (8, 2) | push 8, pop 5 → {8, 10} | 18 | 36 |

Best **60** ✓

---

## 3. Java concepts used in this problem

### 3.1 Sorting indices in descending order

```java
Arrays.sort(order, (a, b) -> Integer.compare(efficiency[b], efficiency[a]));
```

Swapping a and b inside `compare` reverses the order.

### 3.2 `long` for the sum and the product

```java
long sum = 0, best = 0;
best = Math.max(best, sum * efficiency[i]);       // long × int → long
```

Since `sum` is a `long`, the multiplication happens in 64 bits. (If both were ints, the product would overflow **before** being assigned to a long.)

### 3.3 Modulo only at the end

```java
return (int) (best % 1_000_000_007L);
```

The maximum is found on the true values; only the final answer is reduced.

---

## 4. Approach 1: Every subset of at most k (the referee)

### Code ([H07_MaximumPerformanceOfATeam.java:61-71](H07_MaximumPerformanceOfATeam.java#L61-L71))

```java
static long brute(int n, int[] s, int[] e, int k) {
    long best = 0;
    for (int mask = 1; mask < (1 << n); mask++) {
        if (Integer.bitCount(mask) > k) continue;
        long sum = 0;
        int min = Integer.MAX_VALUE;
        for (int i = 0; i < n; i++) if ((mask >> i & 1) == 1) { sum += s[i]; min = Math.min(min, e[i]); }
        best = Math.max(best, sum * min);
    }
    return best;
}
```

**O(2ⁿ · n).** It works directly from the definition.

---

## 5. Approach 2: Sort by efficiency + min-heap of speeds (the file's solution)

### Code ([H07_MaximumPerformanceOfATeam.java:46-59](H07_MaximumPerformanceOfATeam.java#L46-L59))

```java
static int maxPerformance(int n, int[] speed, int[] efficiency, int k) {
    Integer[] order = new Integer[n];
    for (int i = 0; i < n; i++) order[i] = i;
    Arrays.sort(order, (a, b) -> Integer.compare(efficiency[b], efficiency[a]));   // efficiency desc
    PriorityQueue<Integer> speeds = new PriorityQueue<>();                          // min-heap: k fastest
    long sum = 0, best = 0;
    for (int i : order) {
        speeds.offer(speed[i]);
        sum += speed[i];
        if (speeds.size() > k) sum -= speeds.poll();       // drop the slowest
        best = Math.max(best, sum * efficiency[i]);        // i is the bottleneck
    }
    return (int) (best % 1_000_000_007L);                  // modulo only at the end
}
```

### Line by line

| Line | What it does |
|---|---|
| sort by efficiency descending | the current engineer has the lowest efficiency so far: the bottleneck |
| `speeds` (min-heap) | the k fastest engineers seen so far |
| `sum += speed[i]` | the running sum of the kept speeds |
| `if (speeds.size() > k) sum -= speeds.poll();` | keep at most k: drop the slowest |
| `best = Math.max(best, sum * efficiency[i])` | the team's performance with this bottleneck |

**"At most k"** is handled naturally: candidates are also computed while the heap holds fewer than k engineers.

### Complexity
- **Time O(n log n).**
- **Space O(n).**

---

## 6. Comparing the approaches

| | Every subset | Sort + heap |
|---|---|---|
| Time | O(2ⁿ · n) | **O(n log n)** |
| Space | O(1) | O(n) |

The file checks the heap version against every subset on 1,000 random teams.

---

## 7. Edge cases and common mistakes

**Edge cases:** k = 1 (the best single speed × efficiency), k = n, equal efficiencies, a fast but very inefficient engineer.

**Mistakes:**
1. Applying `%` during the comparisons.
2. An int product overflowing before being widened.
3. Sorting by efficiency ascending.
4. Requiring exactly k engineers (the problem says at most k).

---

## 8. Interview questions and answers

**Q1. Why sort by efficiency descending?**
> When processing engineer i, everyone already seen has efficiency ≥ i's, so i is the team's minimum, and the best teammates are simply the fastest of those seen.

**Q2. Why is it fine if the current engineer is popped?**
> Then the computed value uses i's efficiency for a group without i, which is at most that group's true performance. Its true value was already counted when that group's own bottleneck was processed.

### Follow-up questions

**F1. Exactly k engineers.**
> Only record the candidates once the heap holds exactly k.

**F2. Hire K Workers.** → [H05](H05_MinimumCostToHireKWorkers.md)
> Fix the max ratio, minimise the sum of qualities: the same sort + heap.

**F3. Maximum Subsequence Score (LC 2542).**
> The identical structure: (sum of nums1 over k indices) × (min of nums2 over them).

---

## 9. Running the file

```bash
java H07_MaximumPerformanceOfATeam.java      # 4/4 tests passed (including 1,000 random teams vs all subsets)
```

**One-line summary:** *"Sort by efficiency descending; keep the k fastest speeds in a min-heap with their sum; each engineer, as the bottleneck, gives sum × its efficiency; take the modulo only at the end."*
