# M05 · Furthest Building You Can Reach

| | |
|---|---|
| **LeetCode** | [#1642 Furthest Building You Can Reach](https://leetcode.com/problems/furthest-building-you-can-reach/) |
| **Difficulty** | Medium |
| **Pattern** | Greedy with a heap: "use the ladders on the k biggest climbs so far" |
| **Code** | [M05_FurthestBuildingYouCanReach.java](M05_FurthestBuildingYouCanReach.java) |
| **Run** | `java M05_FurthestBuildingYouCanReach.java` |

PriorityQueue basics are explained in [E01](../02-Easy/E01_KthLargestInStream.md#3-java-concepts-used-in-this-problem).

---

## 1. The problem

Walk from building 0 to the right. Going **down** or staying level is free. Going **up** by d costs either **d bricks** or **one ladder** (of any height). Given `bricks` and `ladders`, what's the furthest building index you can reach?

```
heights [4, 2, 7, 6, 9, 14, 12], bricks 5, ladders 1 → 4
  4→2 free,  2→7 (+5) bricks,  7→6 free,  6→9 (+3) ladder,  9→14 (+5) nothing left
```

| heights | bricks | ladders | answer |
|---|---|---|---|
| `[4,2,7,6,9,14,12]` | 5 | 1 | 4 |
| `[4,12,2,7,3,18,20,3,19]` | 10 | 2 | 7 |
| `[14,3,19,3]` | 17 | 0 | 3 |

---

## 2. How to think about it

### The dilemma
When you meet a climb, you don't know the future: should this climb get a ladder or bricks?

### Decide in hindsight
Ladders are most valuable on the **biggest** climbs (a ladder saves exactly the climb's height in bricks). So at every point, the ladders should cover the **largest climbs seen so far**, and bricks pay for the rest.

Keep the climbs that currently "own" a ladder in a **min-heap**:

```
for each climb d > 0:
    give it a ladder:  heap.offer(d)
    if the heap now holds more climbs than there are ladders:
        take back the SMALLEST laddered climb and pay for it with bricks instead
        bricks −= heap.poll()
        if bricks < 0 → we can't pass this building: answer = i
```

At every moment, the ladders cover the k largest climbs seen so far: the cheapest possible brick usage. (It's top-k again: [E01](../02-Easy/E01_KthLargestInStream.md).)

### Dry run: `[4,2,7,6,9,14,12]`, bricks 5, ladders 1

```
0→1: down
1→2: +5 → heap {5}          (1 ladder: ok)
2→3: down
3→4: +3 → heap {3, 5}       > 1 ladder → bricks pay 3 → bricks 2, heap {5}
4→5: +5 → heap {5, 5}       → bricks pay 5 → bricks −3 < 0 → stop at 4
```

---

## 3. Java concepts used in this problem

### 3.1 A min-heap as a "best k so far" set

`laddered` always holds at most `ladders` climbs: the largest ones seen. Its root is the **smallest** of them, which is the one to hand over to bricks when a bigger climb appears.

### 3.2 Returning from inside the loop

`if (bricks < 0) return i;`: building i is the last one reached, because the climb from i to i + 1 can't be paid for.

### 3.3 Brute force with recursion (the referee)

`dfs(h, i, bricks, ladders)` tries both options at each climb: bricks (if affordable) and a ladder (if any remain), taking the furthest result. It's exponential in the number of climbs, but fine for ≤ 12 buildings. (Backtracking gets its own topic: [14-Backtracking](../../14-Backtracking/README.md).)

---

## 4. Approach 1: Try every choice (the referee)

### Code ([M05_FurthestBuildingYouCanReach.java:60-72](M05_FurthestBuildingYouCanReach.java#L60-L72))

```java
static int brute(int[] h, int bricks, int ladders) {
    return dfs(h, 0, bricks, ladders);
}

static int dfs(int[] h, int i, int bricks, int ladders) {
    if (i == h.length - 1) return i;
    int climb = h[i + 1] - h[i];
    if (climb <= 0) return dfs(h, i + 1, bricks, ladders);
    int best = i;
    if (bricks >= climb) best = Math.max(best, dfs(h, i + 1, bricks - climb, ladders));
    if (ladders > 0) best = Math.max(best, dfs(h, i + 1, bricks, ladders - 1));
    return best;
}
```

| Line | What it does |
|---|---|
| `i == h.length − 1` | reached the last building |
| `climb <= 0` | free: move on |
| `best = i` | if neither option works, we stop here |
| the two recursive options | bricks, or a ladder |

**O(2^climbs).**

---

## 5. Approach 2: Binary search the answer

```java
static int furthestBinarySearch(int[] h, int bricks, int ladders) {
    int lo = 0, hi = h.length - 1;
    while (lo < hi) {
        int mid = (lo + hi + 1) / 2;                       // can we reach building mid?
        if (canReach(h, mid, bricks, ladders)) lo = mid; else hi = mid - 1;
    }
    return lo;
}

static boolean canReach(int[] h, int target, int bricks, int ladders) {
    List<Integer> climbs = new ArrayList<>();
    for (int i = 0; i < target; i++) if (h[i + 1] > h[i]) climbs.add(h[i + 1] - h[i]);
    Collections.sort(climbs);
    long need = 0;
    for (int i = 0; i < climbs.size() - ladders; i++) need += climbs.get(i);   // bricks pay for the smallest
    return need <= bricks;
}
```

Reachability is monotonic (if you can reach building m, you can reach every earlier one). To check a target, give the ladders to the largest climbs and pay bricks for the rest. **O(n log² n).** It's a nice alternative, but the heap does it in one pass.

---

## 6. Approach 3: Min-heap of laddered climbs (the file's solution)

### Code ([M05_FurthestBuildingYouCanReach.java:45-57](M05_FurthestBuildingYouCanReach.java#L45-L57))

```java
static int furthestBuilding(int[] heights, int bricks, int ladders) {
    PriorityQueue<Integer> laddered = new PriorityQueue<>();      // climbs covered by ladders
    for (int i = 0; i < heights.length - 1; i++) {
        int climb = heights[i + 1] - heights[i];
        if (climb <= 0) continue;                                 // free
        laddered.offer(climb);
        if (laddered.size() > ladders) {
            bricks -= laddered.poll();                            // smallest climb -> bricks
            if (bricks < 0) return i;
        }
    }
    return heights.length - 1;
}
```

### Line by line

| Line | What it does |
|---|---|
| `climb <= 0 → continue` | going down or level costs nothing |
| `laddered.offer(climb)` | tentatively give this climb a ladder |
| `laddered.size() > ladders` | not enough ladders for all of them |
| `bricks -= laddered.poll()` | the smallest laddered climb is paid with bricks instead |
| `if (bricks < 0) return i;` | even the optimal assignment can't pay for this step |
| `return heights.length − 1` | we made it to the end |

### Dry run: `[4, 12, 2, 7, 3, 18, 20, 3, 19]`, bricks 10, ladders 2

| step i → i+1 | climb | heap after offer | over 2? | bricks | heap |
|---|---|---|---|---|---|
| 0 → 1 | 8 | {8} | | 10 | {8} |
| 1 → 2 | down | | | 10 | {8} |
| 2 → 3 | 5 | {5, 8} | | 10 | {5, 8} |
| 3 → 4 | down | | | | |
| 4 → 5 | 15 | {5, 8, 15} | poll 5 | 5 | {8, 15} |
| 5 → 6 | 2 | {2, 8, 15} | poll 2 | 3 | {8, 15} |
| 6 → 7 | down | | | | |
| 7 → 8 | 16 | {8, 15, 16} | poll 8 | **−5** → return 7 | |

**Return 7** ✓

### Complexity
- **Time O(n log L)** (L = ladders; the heap never exceeds L + 1).
- **Space O(L).**

---

## 7. Comparing the approaches

| | Try every choice | Binary search | Min-heap |
|---|---|---|---|
| Time | O(2^n) | O(n log² n) | **O(n log L)** |
| Space | O(n) | O(n) | O(L) |

The file checks the heap version against trying every choice on 2,000 random cases.

---

## 8. Edge cases and common mistakes

**Edge cases:** no ladders (pure bricks), no bricks (only the ladders), all descending (reach the end), a single building.

**Mistakes:**
1. Greedily using bricks first and ladders only when the bricks run out (wastes ladders on small climbs).
2. Using a max-heap (you'd hand the **largest** climb to bricks).
3. Returning i + 1 instead of i when the bricks go negative.

---

## 9. Interview questions and answers

**Q1. Why is the greedy optimal?**
> For any prefix of the walk, the minimum bricks needed is the sum of all climbs except the L largest (those get the ladders). The heap maintains exactly that assignment, so if it fails, no assignment could succeed.

**Q2. Why a min-heap of size L?**
> It holds the L largest climbs so far. When a new climb pushes the size past L, the smallest of them is the right one to switch to bricks.

### Follow-up questions

**F1. Ladders have limited heights.**
> The greedy breaks. Model it as an assignment problem, or a DP when the sizes are small.

**F2. Return which climbs used ladders.**
> Store `{climb, index}` in the heap; whatever remains at the end used ladders.

**F3. Similar "regret" greedy: Course Schedule III.** → [H08](../04-Hard/H08_CourseScheduleIII.md)
> Take each course; when over the deadline, drop the longest one taken so far (a max-heap).

---

## 10. Running the file

```bash
java M05_FurthestBuildingYouCanReach.java      # 4/4 tests passed (including 2,000 random cases vs trying all choices)
```

**One-line summary:** *"Give every climb a ladder in a min-heap; when there are more climbs than ladders, pay bricks for the smallest one; stop when the bricks go negative."*
