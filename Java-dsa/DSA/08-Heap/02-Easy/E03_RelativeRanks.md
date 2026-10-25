# E03 · Relative Ranks ⭐

| | |
|---|---|
| **LeetCode** | [#506 Relative Ranks](https://leetcode.com/problems/relative-ranks/) |
| **Difficulty** | Easy |
| **Pattern** | A heap of **indices** ordered by value (remember where each value came from) |
| **Code** | [E03_RelativeRanks.java](E03_RelativeRanks.java) |
| **Run** | `java E03_RelativeRanks.java` |

⭐ = not in `leetcode.md`, added for a key technique. PriorityQueue basics are explained in [E01](E01_KthLargestInStream.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

`score` holds unique values. The highest score gets "Gold Medal", the 2nd "Silver Medal", the 3rd "Bronze Medal", and the rest get their place number as a string. Return the answers in the **original** order.

| score | answer |
|---|---|
| `[5,4,3,2,1]` | `["Gold Medal","Silver Medal","Bronze Medal","4","5"]` |
| `[10,3,8,9,4]` | `["Gold Medal","5","Bronze Medal","Silver Medal","4"]` |
| `[1]` | `["Gold Medal"]` |

---

## 2. How to think about it

### The twist
Sorting the scores loses their **positions**, but the answer must be written back in the original order.

### Heap (or sort) the indices, not the values
Push the **indices** 0..n−1 into a heap ordered by `score[index]`, descending. Polling then gives the athletes from best to worst, and each polled index tells us **where** to write the rank.

```
[10, 3, 8, 9, 4]
poll order of indices: 0 (10), 3 (9), 2 (8), 4 (4), 1 (3)
result[0] = Gold, result[3] = Silver, result[2] = Bronze, result[4] = "4", result[1] = "5"
```

"A heap or sort of **indices** with a comparator on the **values**" is used constantly: K Closest Points ([M02](../03-Medium/M02_KClosestPointsToOrigin.md)), IPO ([H03](../04-Hard/H03_IPO.md)), Hire K Workers ([H05](../04-Hard/H05_MinimumCostToHireKWorkers.md)), …

---

## 3. Java concepts used in this problem

### 3.1 A comparator that looks things up in another array

```java
PriorityQueue<Integer> heap = new PriorityQueue<>((i, j) -> Integer.compare(score[j], score[i]));
```

The heap stores indices, but compares them by their **scores**. Swapping i and j in the `compare` call (`score[j], score[i]`) gives **descending** order. The lambda can read `score` because it's an effectively final local variable (a parameter that's never reassigned).

### 3.2 `String.valueOf(int)`

Converts a place number to a String ("4"). `"" + place` also works, but `valueOf` states the intent.

### 3.3 A `for` loop with an unusual condition

```java
for (int place = 1; !heap.isEmpty(); place++)
```

The counter `place` and the stopping condition are unrelated variables, which is perfectly legal: it counts up while the heap still has athletes.

### 3.4 Sorting boxed indices

The alternative needs an `Integer[]` (not `int[]`), because `Arrays.sort` only accepts a comparator for **object** arrays.

---

## 4. Approach 1: Sort the indices

```java
static String[] findRelativeRanksSort(int[] score) {
    int n = score.length;
    Integer[] idx = new Integer[n];
    for (int i = 0; i < n; i++) idx[i] = i;
    Arrays.sort(idx, (i, j) -> Integer.compare(score[j], score[i]));   // best first
    String[] medals = {"Gold Medal", "Silver Medal", "Bronze Medal"};
    String[] result = new String[n];
    for (int place = 0; place < n; place++)
        result[idx[place]] = place < 3 ? medals[place] : String.valueOf(place + 1);
    return result;
}
```

The same idea, with a sort instead of a heap: **O(n log n)**. In practice this is what you'd write; the heap version shows the pattern.

---

## 5. Approach 2: A max-heap of indices (the file's solution)

### Code ([E03_RelativeRanks.java:36-46](E03_RelativeRanks.java#L36-L46))

```java
static String[] findRelativeRanks(int[] score) {
    PriorityQueue<Integer> heap = new PriorityQueue<>((i, j) -> Integer.compare(score[j], score[i]));   // max by score
    for (int i = 0; i < score.length; i++) heap.offer(i);
    String[] result = new String[score.length];
    String[] medals = {"Gold Medal", "Silver Medal", "Bronze Medal"};
    for (int place = 1; !heap.isEmpty(); place++) {
        int i = heap.poll();                                 // the next best athlete's INDEX
        result[i] = place <= 3 ? medals[place - 1] : String.valueOf(place);
    }
    return result;
}
```

### Line by line

| Line | What it does |
|---|---|
| the comparator | the index with the **higher** score comes out first |
| `heap.offer(i)` | push every index |
| `medals` | the names for places 1..3 |
| `int i = heap.poll();` | the index of the best athlete not yet ranked |
| `result[i] = …` | write the rank at that athlete's **original** position |

### Dry run: `[10, 3, 8, 9, 4]`

| place | polled index | score | result[index] |
|---|---|---|---|
| 1 | 0 | 10 | Gold Medal |
| 2 | 3 | 9 | Silver Medal |
| 3 | 2 | 8 | Bronze Medal |
| 4 | 4 | 4 | "4" |
| 5 | 1 | 3 | "5" |

result = `[Gold Medal, 5, Bronze Medal, Silver Medal, 4]` ✓

### Complexity
- **Time O(n log n).**
- **Space O(n).**

---

## 6. Comparing the approaches

| | Sort the indices | Heap of indices |
|---|---|---|
| Time | O(n log n) | O(n log n) |
| Space | O(n) | O(n) |
| When it matters | the whole order is needed | only the top few are needed (poll k times: O(n + k log n) with heapify) |

---

## 7. Edge cases and common mistakes

**Edge cases:** n = 1, 2 or 3 (only medals), already sorted input.

**Mistakes:**
1. Sorting the scores themselves and losing the positions.
2. An off-by-one between `place` and the medal index.
3. Using `int[]` with `Arrays.sort(arr, comparator)` (it doesn't compile; it needs `Integer[]`).

---

## 8. Interview questions and answers

**Q1. Why store indices in the heap?**
> The answer is indexed by the original position. An index lets us read the score (for ordering) and also know where to write the result.

**Q2. What if the scores weren't unique?**
> Ties would need a rule (the same rank for equal scores). Track the previous score and keep the same place number while equal.

### Follow-up questions

**F1. Rank Transform of an Array (LC 1331).**
> Sort the distinct values and map value → rank with a HashMap (coordinate compression, as in [Trees H05](../../07-Trees/04-Hard/H05_CountOfSmallerNumbersAfterSelf.md)).

**F2. Only the top 3 are needed.**
> One pass keeping the three largest (O(n)), or poll the heap 3 times.

**F3. K Closest Points to Origin.** → [M02](../03-Medium/M02_KClosestPointsToOrigin.md)
> The same "order items by a computed key" with a size-k heap.

---

## 9. Running the file

```bash
java E03_RelativeRanks.java      # 3/3 tests passed
```

**One-line summary:** *"Put the indices in a heap ordered by score (descending); each poll gives the next place, written at that index."*
