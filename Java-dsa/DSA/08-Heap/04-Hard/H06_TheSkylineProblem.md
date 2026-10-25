# H06 · The Skyline Problem

| | |
|---|---|
| **LeetCode** | [#218 The Skyline Problem](https://leetcode.com/problems/the-skyline-problem/) |
| **Difficulty** | Hard |
| **Pattern** | A sweep line over x + a max-heap of "active" heights (with lazy deletion) |
| **Code** | [H06_TheSkylineProblem.java](H06_TheSkylineProblem.java) |
| **Run** | `java H06_TheSkylineProblem.java` |

PriorityQueue basics are explained in [E01](../02-Easy/E01_KthLargestInStream.md#3-java-concepts-used-in-this-problem). Lazy deletion was introduced in [H04](H04_SlidingWindowMedian.md).

---

## 1. The problem

Buildings are `[left, right, height]`. Return the skyline's **key points** `[x, y]` wherever the outline's height **changes**: from left to right, no two consecutive points with the same y, and the last point has y = 0.

```
[[2,9,10],[3,7,15],[5,12,12],[15,20,10],[19,24,8]]
→ [[2,10],[3,15],[7,12],[12,0],[15,10],[20,8],[24,0]]
```

| buildings | skyline |
|---|---|
| the example | as above |
| `[[0,2,3],[2,5,3]]` | `[[0,3],[5,0]]` (equal heights merge) |

---

## 2. How to think about it

### A sweep line
Move a vertical line from left to right. The outline's height at x is the **maximum height among the buildings covering x**. It can only change at a building's left or right edge, so only look at those x values (the "events").

### The algorithm
```
collect every distinct edge x, sorted
sort the buildings by left; a pointer adds buildings as the sweep reaches them
max-heap of {height, right} for the buildings started so far
at each x:
    push every building with left == x
    LAZY DELETION: pop from the top while top.right ≤ x   (that building has ended)
        (ended buildings that aren't on top don't matter until they surface)
    h = heap empty ? 0 : top.height
    if h != the previous height → key point [x, h]
```

### Dry run (from the real run)

| x | pushed | popped | heap after (height, right) | h | key point |
|---|---|---|---|---|---|
| 2 | (10, 9) | | (10, 9) | 10 | **[2, 10]** |
| 3 | (15, 7) | | (15, 7) (10, 9) | 15 | **[3, 15]** |
| 5 | (12, 12) | | (15, 7) (10, 9) (12, 12) | 15 | |
| 7 | | (15, 7) | (12, 12) (10, 9) | 12 | **[7, 12]** |
| 9 | | — ((10, 9) has ended, but it's buried under 12) | (12, 12) (10, 9) | 12 | |
| 12 | | (12, 12), (10, 9) | — | 0 | **[12, 0]** |
| 15 | (10, 20) | | (10, 20) | 10 | **[15, 10]** |
| 19 | (8, 24) | | (10, 20) (8, 24) | 10 | |
| 20 | | (10, 20) | (8, 24) | 8 | **[20, 8]** |
| 24 | | (8, 24) | — | 0 | **[24, 0]** |

At x = 9, building (10, 9) has ended but stays in the heap. It's harmless, because the taller (12, 12) is on top. It gets removed at x = 12, when it surfaces.

---

## 3. Java concepts used in this problem

### 3.1 `TreeSet<Integer>` for sorted, distinct events

```java
TreeSet<Integer> xs = new TreeSet<>();
for (int[] b : buildings) { xs.add(b[0]); xs.add(b[1]); }
for (int x : xs) ...                       // iterates in increasing order
```

It removes duplicates and keeps the x coordinates sorted.

### 3.2 A max-heap of `int[]` pairs

```java
new PriorityQueue<int[]>((a, b) -> Integer.compare(b[0], a[0]))   // the tallest first
```

Each entry stores the height (for ordering) and the right edge (to know when it expires).

### 3.3 Half-open coverage

A building covers [left, right): at x = right it no longer counts. That's why expired entries are popped with `top.right <= x`, and it's also why adjacent equal-height buildings merge (`[[0,2,3],[2,5,3]]`: at x = 2 the first one is gone but the second gives the same height 3, so there's no key point).

---

## 4. Approach 1: Brute force over the events

### Code ([H06_TheSkylineProblem.java:71-82](H06_TheSkylineProblem.java#L71-L82))

```java
static List<List<Integer>> brute(int[][] buildings) {
    TreeSet<Integer> xs = new TreeSet<>();
    for (int[] b : buildings) { xs.add(b[0]); xs.add(b[1]); }
    List<List<Integer>> out = new ArrayList<>();
    int prev = 0;
    for (int x : xs) {
        int h = 0;
        for (int[] b : buildings) if (b[0] <= x && x < b[1]) h = Math.max(h, b[2]);
        if (h != prev) { out.add(List.of(x, h)); prev = h; }
    }
    return out;
}
```

| Line | What it does |
|---|---|
| `xs` | the only places where the height can change |
| the inner loop | the max height of the buildings covering x (half-open) |
| `h != prev` | a change → a key point |

**O(n²)** (2n events × n buildings). Correct and simple: the file's referee.

---

## 5. Approach 2: Sweep + max-heap with lazy deletion (the file's solution)

### Code ([H06_TheSkylineProblem.java:47-68](H06_TheSkylineProblem.java#L47-L68))

```java
static List<List<Integer>> getSkyline(int[][] buildings) {
    TreeSet<Integer> xs = new TreeSet<>();
    for (int[] b : buildings) { xs.add(b[0]); xs.add(b[1]); }
    int[][] byLeft = buildings.clone();
    Arrays.sort(byLeft, Comparator.comparingInt(b -> b[0]));
    PriorityQueue<int[]> heap = new PriorityQueue<>((a, b) -> Integer.compare(b[0], a[0]));   // {height, right}, max height
    List<List<Integer>> result = new ArrayList<>();
    int i = 0, prevHeight = 0;
    for (int x : xs) {
        while (i < byLeft.length && byLeft[i][0] == x) {           // buildings starting here
            heap.offer(new int[]{byLeft[i][2], byLeft[i][1]});
            i++;
        }
        while (!heap.isEmpty() && heap.peek()[1] <= x) heap.poll();  // lazily drop finished tallest
        int h = heap.isEmpty() ? 0 : heap.peek()[0];
        if (h != prevHeight) {
            result.add(List.of(x, h));
            prevHeight = h;
        }
    }
    return result;
}
```

### Line by line

| Line | What it does |
|---|---|
| `xs` | the sorted event coordinates |
| `byLeft` | the buildings in the order they start |
| the first `while` | add every building that starts at x |
| the second `while` | remove expired buildings, but only from the top |
| `h = …` | the current outline height (0 if no building is active) |
| `h != prevHeight` | record a key point only when the height changes |

**Why lazy deletion is enough:** only the **top** of the heap determines h. An expired building buried below a taller live one can't affect h, and it's removed as soon as it reaches the top.

### Complexity
- **Time O(n log n):** 2n events; each building is pushed and popped once.
- **Space O(n).**

---

## 6. Comparing the approaches

| | Brute force | Sweep + heap |
|---|---|---|
| Time | O(n²) | **O(n log n)** |
| Space | O(n) | O(n) |

The file checks the sweep against the brute force on 1,000 random cities.

---

## 7. Edge cases and common mistakes

**Edge cases:** adjacent buildings with equal heights (merge), one building inside a taller one (no key points for it), buildings sharing a left edge, gaps between buildings (height 0).

**Mistakes:**
1. Treating the right edge as inclusive (`<` instead of `<=` when expiring).
2. Emitting a key point at every event, even without a height change.
3. Removing expired buildings from the middle of the heap (O(n) each; unnecessary).
4. Processing ends before starts at the same x, without care (it can create a spurious drop to a lower height).

---

## 8. Interview questions and answers

**Q1. Why can the outline only change at building edges?**
> Between two consecutive edges, the set of covering buildings is fixed, so the max height is constant.

**Q2. What's the classic alternative implementation?**
> Turn each building into two events, (left, −height) and (right, +height), sort them, and keep a `TreeMap<height, count>` of the active heights (removing on end events). The max key is the current height. It's also O(n log n).

### Follow-up questions

**F1. The total area under the skyline.**
> Sum (next x − x) × h over consecutive key points.

**F2. Divide and conquer.**
> Build the skylines of each half, then merge two skylines like merging sorted lists (O(n log n)).

**F3. Rectangle Area II (LC 850).**
> A sweep line with coordinate compression and a segment tree of covered lengths.

---

## 9. Running the file

```bash
java H06_TheSkylineProblem.java      # 3/3 tests passed (including 1,000 random cities vs brute force)
```

**One-line summary:** *"Sweep over the sorted edge x's, pushing buildings into a max-heap of (height, right) as they start, popping expired tops lazily, and emitting [x, h] whenever the top height changes."*
