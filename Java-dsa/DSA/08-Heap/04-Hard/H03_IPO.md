# H03 · IPO

| | |
|---|---|
| **LeetCode** | [#502 IPO](https://leetcode.com/problems/ipo/) |
| **Difficulty** | Hard |
| **Pattern** | Sort + heap: "unlock" by one key (capital), "choose" by another (profit) |
| **Code** | [H03_IPO.java](H03_IPO.java) |
| **Run** | `java H03_IPO.java` |

PriorityQueue basics are explained in [E01](../02-Easy/E01_KthLargestInStream.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

You have capital w and may finish **at most k** projects. Project i needs `capital[i]` to **start** (it isn't spent) and adds `profits[i]` to your capital. Maximise the final capital.

```
k = 2, w = 0, profits [1, 2, 3], capital [0, 1, 1]  →  4
  w = 0: only project 0 is affordable → w = 1
  w = 1: projects 1 and 2 are affordable → take profit 3 → w = 4
```

| k | w | profits | capital | answer |
|---|---|---|---|---|
| 2 | 0 | [1,2,3] | [0,1,1] | 4 |
| 3 | 0 | [1,2,3] | [0,1,2] | 6 |
| 1 | 0 | [5] | [1] | 0 (nothing affordable) |

---

## 2. How to think about it

### The greedy
At each step, among the projects you can **afford right now**, take the most **profitable**. Profit only ever increases the capital, so taking the biggest profit now never closes a door: it can only unlock more projects.

### Two structures
- the projects **sorted by the capital they need** (a pointer walks through them)
- a **max-heap of the profits** of the projects unlocked so far

```
repeat k times:
    move every project with capital ≤ w from the sorted list into the heap
    if the heap is empty → nothing is affordable: stop early
    w += heap.poll()
```

Each project enters and leaves the heap at most once: O(n log n).

**This shape (sort by A, sweep, heap by B) appears in many hard greedy problems:** Course Schedule III ([H08](H08_CourseScheduleIII.md)), Maximum Performance ([H07](H07_MaximumPerformanceOfATeam.md)), Hire K Workers ([H05](H05_MinimumCostToHireKWorkers.md)), Meeting Rooms ([M07](../03-Medium/M07_MeetingRoomsII.md)).

### Dry run: k = 3, w = 0, profits [1, 2, 3], capital [0, 1, 2]
```
unlock cap ≤ 0: {1} → take 1, w = 1
unlock cap ≤ 1: {2} → take 2, w = 3
unlock cap ≤ 3: {3} → take 3, w = 6
```

---

## 3. Java concepts used in this problem

### 3.1 Sorting indices by another array: `Integer[]` + a comparator

```java
Integer[] byCapital = new Integer[n];
for (int i = 0; i < n; i++) byCapital[i] = i;
Arrays.sort(byCapital, Comparator.comparingInt(i -> capital[i]));
```

`Arrays.sort` with a comparator needs an **object** array, hence `Integer[]`. The indices end up ordered by the capital they need, and both `capital[]` and `profits[]` can then be read through the same index.

### 3.2 Post-increment inside an expression

```java
affordable.offer(profits[byCapital[next++]]);
```

It uses `byCapital[next]`, then increments `next`: the "unlock pointer" moves forward.

### 3.3 Brute force with backtracking (the referee)

`brute` tries every affordable unused project as the next one (marking it used, recursing with k − 1, then unmarking). Exponential, so it's only for n ≤ 6.

---

## 4. Approach 1: Try every order (the referee)

### Code ([H03_IPO.java:60-71](H03_IPO.java#L60-L71))

```java
static int brute(int k, int w, int[] profits, int[] capital, boolean[] used) {
    int best = w;
    if (k == 0) return w;
    for (int i = 0; i < profits.length; i++) {
        if (!used[i] && capital[i] <= w) {
            used[i] = true;
            best = Math.max(best, brute(k - 1, w + profits[i], profits, capital, used));
            used[i] = false;
        }
    }
    return best;
}
```

| Line | What it does |
|---|---|
| `best = w` | taking no more projects is always an option |
| the loop | every affordable, unused project as the next choice |
| `used[i] = true / false` | choose, recurse, un-choose |

**O(n! / (n − k)!)**.

---

## 5. Approach 2: Scan for the best affordable project each step

```java
static int ipoScan(int k, int w, int[] profits, int[] capital) {
    boolean[] used = new boolean[profits.length];
    for (int step = 0; step < k; step++) {
        int best = -1;
        for (int i = 0; i < profits.length; i++)
            if (!used[i] && capital[i] <= w && (best < 0 || profits[i] > profits[best])) best = i;
        if (best < 0) break;
        used[best] = true;
        w += profits[best];
    }
    return w;
}
```

The same greedy without the data structures: **O(k · n)**. It's correct, but too slow for n = k = 10⁵.

---

## 6. Approach 3: Sorted unlock pointer + max-heap of profits (the file's solution)

### Code ([H03_IPO.java:44-57](H03_IPO.java#L44-L57))

```java
static int findMaximizedCapital(int k, int w, int[] profits, int[] capital) {
    int n = profits.length;
    Integer[] byCapital = new Integer[n];
    for (int i = 0; i < n; i++) byCapital[i] = i;
    Arrays.sort(byCapital, Comparator.comparingInt(i -> capital[i]));
    PriorityQueue<Integer> affordable = new PriorityQueue<>(Comparator.reverseOrder());   // profits
    int next = 0;
    for (int step = 0; step < k; step++) {
        while (next < n && capital[byCapital[next]] <= w) affordable.offer(profits[byCapital[next++]]);
        if (affordable.isEmpty()) break;                    // can't afford anything more
        w += affordable.poll();                             // the best project we can start
    }
    return w;
}
```

### Line by line

| Line | What it does |
|---|---|
| `byCapital` | the project indices, cheapest to start first |
| `affordable` | a max-heap of the profits of the unlocked projects |
| the `while` | unlock every project whose capital requirement is now met |
| `if (affordable.isEmpty()) break;` | no affordable project → more steps can't help |
| `w += affordable.poll()` | do the most profitable affordable project |

Projects stay in the heap across steps: once affordable, always affordable (w never decreases).

### Dry run: k = 2, w = 0, profits [1, 2, 3], capital [0, 1, 1]

byCapital = [0, 1, 2] (capitals 0, 1, 1)

| step | unlock (capital ≤ w) | heap | poll | w |
|---|---|---|---|---|
| 1 | project 0 (cap 0 ≤ 0) | {1} | 1 | 1 |
| 2 | projects 1, 2 (cap 1 ≤ 1) | {3, 2} | 3 | **4** |

### Complexity
- **Time O(n log n + k log n).**
- **Space O(n).**

---

## 7. Comparing the approaches

| | Every order | Scan each step | Sort + heap |
|---|---|---|---|
| Time | exponential | O(k · n) | **O(n log n)** |
| Space | O(n) | O(n) | O(n) |

The file checks the heap version against every order on 1,000 random small cases.

---

## 8. Edge cases and common mistakes

**Edge cases:** nothing affordable at the start, k larger than n, zero-profit projects, all projects free (capital 0).

**Mistakes:**
1. Choosing by the **lowest capital** instead of the highest profit.
2. Re-sorting or rebuilding the heap at every step.
3. Not stopping when the heap is empty (polling null → a NullPointerException on unboxing).
4. Subtracting the capital (it isn't spent).

---

## 9. Interview questions and answers

**Q1. Why is the greedy optimal?**
> Every affordable project stays affordable, and taking a larger profit now leaves you with at least as much capital as any other choice, so the set of options later is at least as large. An exchange argument shows that swapping in the larger profit never hurts.

**Q2. What's the pattern?**
> Sort by the "unlock" key, sweep a pointer to make items available, and use a heap ordered by the "choose" key to pick the best available item.

### Follow-up questions

**F1. Projects cost their capital (it's spent).**
> It becomes a knapsack-like problem; the greedy no longer works.

**F2. Return the order of the chosen projects.**
> Store `{profit, index}` in the heap and record the indices as they're polled.

**F3. Maximum Performance of a Team.** → [H07](H07_MaximumPerformanceOfATeam.md)
> Sort by one key (efficiency), and keep a heap over the other (speed).

---

## 10. Running the file

```bash
java H03_IPO.java      # 4/4 tests passed (including 1,000 random cases vs all orders)
```

**One-line summary:** *"Sort the projects by required capital; each step, unlock everything affordable into a max-heap of profits and take the top one, k times."*
