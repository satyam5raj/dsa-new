# E02 · Last Stone Weight ⭐

| | |
|---|---|
| **LeetCode** | [#1046 Last Stone Weight](https://leetcode.com/problems/last-stone-weight/) |
| **Difficulty** | Easy |
| **Pattern** | Simulation with a **max**-heap ("repeatedly take the two largest") |
| **Code** | [E02_LastStoneWeight.java](E02_LastStoneWeight.java) |
| **Run** | `java E02_LastStoneWeight.java` |

⭐ = not in `leetcode.md`, added for a key technique. PriorityQueue basics are explained in [E01](E01_KthLargestInStream.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Each turn, smash the two **heaviest** stones, x ≤ y:
- x == y → both are destroyed
- x != y → a stone of weight y − x remains

Return the last stone's weight, or 0 if none remain.

| stones | answer |
|---|---|
| `[2,7,4,1,8,1]` | 1 |
| `[1]` | 1 |
| `[3,3]` | 0 |

**Constraints:** up to 30 stones, weights 1..1000.

---

## 2. How to think about it

### Brute force
Sort the array every turn and take the last two: O(n² log n).

### A max-heap
A max-heap gives "the largest" in O(log n), **and** lets us put the leftover back in O(log n):

```
while the heap has ≥ 2 stones:
    y = poll(), x = poll()          (y ≥ x)
    if y > x: offer(y − x)
return heap empty ? 0 : peek()
```

**The general signal:** whenever a problem says "repeatedly take the largest/smallest and put something back", think **heap**. Other examples: Huffman coding, and connecting ropes at minimum cost.

```
8, 7 → 1     heap {4, 2, 1, 1, 1}
4, 2 → 2     heap {2, 1, 1, 1}
2, 1 → 1     heap {1, 1, 1}
1, 1 → 0     heap {1}             → 1
```

---

## 3. Java concepts used in this problem

### 3.1 A max-heap: `Comparator.reverseOrder()`

```java
PriorityQueue<Integer> heap = new PriorityQueue<>(Comparator.reverseOrder());
```

It reverses the natural order, so the **largest** Integer comes out first.

### 3.2 Two polls in one declaration

```java
int y = heap.poll(), x = heap.poll();
```

The initialisers run left to right, so y is the heaviest and x the second heaviest (y ≥ x).

### 3.3 `List.remove(int index)` in the brute force

`s.remove(s.size() - 1)` removes **by index** (the last element) and returns it: here, the largest after sorting.

---

## 4. Approach 1: Re-sort every turn

### Code ([E02_LastStoneWeight.java:47-56](E02_LastStoneWeight.java#L47-L56))

```java
static int brute(int[] stones) {
    List<Integer> s = new ArrayList<>();
    for (int x : stones) s.add(x);
    while (s.size() > 1) {
        Collections.sort(s);
        int y = s.remove(s.size() - 1), x = s.remove(s.size() - 1);
        if (y > x) s.add(y - x);
    }
    return s.isEmpty() ? 0 : s.get(0);
}
```

| Line | What it does |
|---|---|
| `Collections.sort(s)` | the largest two are now at the end |
| the two removes | take them out |
| `if (y > x) s.add(y - x)` | the leftover goes back |

Up to n turns of O(n log n) sorting: **O(n² log n)**.

---

## 5. Approach 2: Max-heap simulation (the file's solution)

### Code ([E02_LastStoneWeight.java:37-45](E02_LastStoneWeight.java#L37-L45))

```java
static int lastStoneWeight(int[] stones) {
    PriorityQueue<Integer> heap = new PriorityQueue<>(Comparator.reverseOrder());   // max-heap
    for (int s : stones) heap.offer(s);
    while (heap.size() > 1) {
        int y = heap.poll(), x = heap.poll();        // the two heaviest, y >= x
        if (y > x) heap.offer(y - x);
    }
    return heap.isEmpty() ? 0 : heap.peek();
}
```

### Line by line

| Line | What it does |
|---|---|
| the max-heap | the heaviest stone at the root |
| the `offer` loop | all the stones go in |
| `while (heap.size() > 1)` | smash while there are at least two |
| `y = poll(), x = poll()` | the two heaviest |
| `if (y > x) heap.offer(y − x)` | a remaining fragment (equal stones vanish) |
| `heap.isEmpty() ? 0 : heap.peek()` | no stone, or the last one |

### Dry run: `[2, 7, 4, 1, 8, 1]` (internal arrays from the real run)

| turn | y, x | leftover | heap after |
|---|---|---|---|
| start | | | [8, 7, 4, 1, 2, 1] |
| 1 | 8, 7 | 1 | [4, 2, 1, 1, 1] |
| 2 | 4, 2 | 2 | [2, 1, 1, 1] |
| 3 | 2, 1 | 1 | [1, 1, 1] |
| 4 | 1, 1 | none | [1] |

**Return 1** ✓

### Dry run: `[3, 3]`
3, 3 → both destroyed → the heap is empty → **0** ✓

### Complexity
- **Time O(n log n):** at most n − 1 turns, each O(log n).
- **Space O(n).**

---

## 6. Comparing the approaches

| | Re-sort every turn | Max-heap |
|---|---|---|
| Time | O(n² log n) | **O(n log n)** |
| Space | O(n) | O(n) |

The file checks the heap version against re-sorting on 500 random inputs.

---

## 7. Edge cases and common mistakes

**Edge cases:** a single stone, all stones equal (an even count → 0, odd → that weight), two stones.

**Mistakes:**
1. Using a default (min) heap.
2. Offering 0 when y == x (harmless here, but it changes the count; the file skips it).
3. `peek()` on an empty heap unboxed into an `int` (a NullPointerException).

---

## 8. Interview questions and answers

**Q1. Why is a heap the right structure?**
> Each turn needs the current two largest, and the set changes (a new fragment). A heap supports exactly "take the max" and "insert" in O(log n).

**Q2. Could you do better than O(n log n)?**
> With weights ≤ 1000, a counting array of size 1001 and a pointer moving down from the top gives O(n + W). For general weights, the heap is the natural choice.

### Follow-up questions

**F1. Last Stone Weight II (LC 1049): you choose any two stones each turn.**
> It becomes partitioning the stones into two groups with sums as close as possible: a subset-sum DP.

**F2. Minimum cost to connect ropes (LC 1167).**
> A **min**-heap: always join the two shortest ropes and push the sum back (Huffman's idea).

**F3. The k-th largest in a stream.** → [E01](E01_KthLargestInStream.md)

---

## 9. Running the file

```bash
java E02_LastStoneWeight.java      # 4/4 tests passed (including 500 random inputs vs re-sorting)
```

**One-line summary:** *"A max-heap: poll the two heaviest, push back the difference if it's non-zero, and repeat until at most one stone remains."*
