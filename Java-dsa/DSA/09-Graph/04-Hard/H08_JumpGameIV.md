# H08 · Jump Game IV

| | |
|---|---|
| **LeetCode** | [#1345 Jump Game IV](https://leetcode.com/problems/jump-game-iv/) |
| **Difficulty** | Hard |
| **Pattern** | BFS on indices + "use each value's teleport group **once**" (clear it) |
| **Code** | [H08_JumpGameIV.java](H08_JumpGameIV.java) |
| **Run** | `java H08_JumpGameIV.java` |

BFS basics are explained in [E01](../02-Easy/E01_FindIfPathExists.md#34-bfs-breadth-first-search-with-a-queue).

---

## 1. The problem

From index i you can jump to i + 1, i − 1, or **any** j with `arr[j] == arr[i]`. What's the fewest number of jumps from index 0 to the last index?

```
[100, −23, −23, 404, 100, 23, 23, 23, 3, 404]  →  3     (0 → 4 → 3 → 9)
```

| arr | answer |
|---|---|
| the example | 3 |
| [7] | 0 |
| [7,6,9,6,9,6,9,7] | 1 (index 0 teleports straight to the last 7) |

**Constraints:** up to 5 · 10⁴ elements.

---

## 2. How to think about it

### BFS (each jump costs 1)
The neighbours of i: i − 1, i + 1, and every index with the same value (precomputed in a map value → list of indices).

### The trap
An array of 50,000 sevens. Every index has 49,999 "same value" neighbours, so scanning them from every index is O(n²) = 2.5 · 10⁹.

### The fix: clear each group after its first use
Once **any** index with value v is processed, **all** the indices with value v are enqueued (or already visited). Scanning that list again from another index can never find anything new. So clear it:

```
for j in sameValue[arr[i]]:  enqueue it if unvisited
sameValue[arr[i]].clear()      ← each group is scanned exactly once
```

Total work: O(n).

### Dry run: the example (from the real run)

| after jump | newly reached indices |
|---|---|
| 1 | 4 (the other 100), 1 (i + 1) |
| 2 | 3 (from 4: i − 1), 5 (from 4: i + 1), 2 (from 1: i + 1, or the −23 group) |
| 3 | from 3 (value 404): the 404 group contains **9**, the last index → **3** |

---

## 3. Java concepts used in this problem

### 3.1 Reusing the group list for the ±1 neighbours

```java
List<Integer> group = sameValue.get(arr[i]);
group.add(i - 1);                              // reuse the list to scan the 2 neighbours too
group.add(i + 1);
for (int j : group) { ... }
group.clear();
```

A neat trick: appending i − 1 and i + 1 to the group means one loop handles all the neighbour types. Since the list is cleared right after, the extra entries do no harm. (Out-of-range j values are filtered by the bounds check.)

### 3.2 `List.clear()` makes later scans O(1)

After `clear()`, the next index with the same value finds an empty list (plus its own 2 neighbours). That's what turns O(n²) into O(n).

### 3.3 Checking the target at discovery time

```java
if (j == n - 1) return jumps;
```

Returning as soon as the last index is **discovered** (instead of when it's polled) saves one level of work. That's safe in BFS, because the level number is already known.

---

## 4. Approach 1: Plain BFS without clearing (the referee)

### Code ([H08_JumpGameIV.java:68-82](H08_JumpGameIV.java#L68-L82))

```java
static int plainBfs(int[] arr) {
    int n = arr.length;
    int[] d = new int[n];
    Arrays.fill(d, -1);
    d[0] = 0;
    Queue<Integer> q = new ArrayDeque<>(List.of(0));
    while (!q.isEmpty()) {
        int i = q.poll();
        for (int j = 0; j < n; j++) {
            boolean edge = j == i - 1 || j == i + 1 || (j != i && arr[j] == arr[i]);
            if (edge && d[j] == -1) { d[j] = d[i] + 1; q.add(j); }
        }
    }
    return d[n - 1];
}
```

It checks every j as a possible neighbour of every i: **O(n²)**. Correct, and fine for n ≤ 20.

---

## 5. Approach 2: BFS with group clearing (the file's main solution)

### Code ([H08_JumpGameIV.java:39-65](H08_JumpGameIV.java#L39-L65))

```java
static int minJumps(int[] arr) {
    int n = arr.length;
    if (n == 1) return 0;
    Map<Integer, List<Integer>> sameValue = new HashMap<>();
    for (int i = 0; i < n; i++) sameValue.computeIfAbsent(arr[i], k -> new ArrayList<>()).add(i);
    boolean[] seen = new boolean[n];
    seen[0] = true;
    Queue<Integer> q = new ArrayDeque<>(List.of(0));
    int jumps = 0;
    while (!q.isEmpty()) {
        jumps++;
        for (int k = q.size(); k > 0; k--) {
            int i = q.poll();
            List<Integer> group = sameValue.get(arr[i]);
            group.add(i - 1);                              // reuse the list to scan the 2 neighbours too
            group.add(i + 1);
            for (int j : group) {
                if (j < 0 || j >= n || seen[j]) continue;
                if (j == n - 1) return jumps;
                seen[j] = true;
                q.add(j);
            }
            group.clear();                                 // this value's teleports are used up
        }
    }
    return -1;
}
```

### Line by line

| Line | What it does |
|---|---|
| `n == 1` | already at the last index |
| `sameValue` | value → all the indices holding it |
| `jumps++` at the start of each level | the indices discovered in this level are `jumps` away |
| `group.add(i − 1); group.add(i + 1);` | the walking neighbours, folded into the same loop |
| the bounds/seen check | skip invalid or visited indices |
| `j == n − 1` | discovered the last index |
| `group.clear()` | this value's teleports (and these two walk entries) are done |

### Complexity
- **Time O(n):** each index is queued once; each value group is scanned once (plus 2 extra entries per poll).
- **Space O(n).**

The file runs 50,000 sevens with an 8 at the end in a few milliseconds (answer 2: teleport to index 49,998, then step).

---

## 6. Comparing the approaches

| | Plain BFS | BFS + clearing |
|---|---|---|
| Time | O(n²) | **O(n)** |
| 50,000 equal values | ~2.5 · 10⁹ steps | fast |

The file checks the clearing version against plain BFS on 1,000 random arrays (values 0..4, so groups are big).

---

## 7. Edge cases and common mistakes

**Edge cases:** n = 1 (0), the first and last values equal (1), all values distinct (n − 1 steps), all values equal.

**Mistakes:**
1. Rescanning a value's group from every index (O(n²)).
2. Forgetting the i − 1 jump (sometimes stepping back is optimal: 0 → 4 → **3** → 9 in the example).
3. Not marking indices as seen when they're queued.

---

## 8. Interview questions and answers

**Q1. Why is clearing a group safe?**
> When the first index with value v is processed, every other index with value v is queued in that same step (or was already visited). Later scans of the group could only find visited indices.

**Q2. Why BFS and not DP?**
> The jumps go both left and right (and teleport), so there's no left-to-right order to process the indices in; the graph has cycles. BFS handles that with uniform cost.

### Follow-up questions

**F1. Jump Game VI (DP + deque).** → [Queue M06](../../05-Queue/03-Medium/M06_JumpGameVI.md)

**F2. Jump Game III (LC 1306): can you reach any 0?**
> BFS/DFS over i ± arr[i].

**F3. Weighted teleports.**
> Dijkstra, with the same "relax a value group once" optimisation when all teleports in a group cost the same.

---

## 9. Running the file

```bash
java H08_JumpGameIV.java      # 5/5 tests passed (including 1,000 random arrays vs plain BFS and 50,000 equal values)
```

**One-line summary:** *"BFS over indices with i ± 1 and same-value teleports; clear each value's index list after its first use so every group is scanned once, making it O(n)."*
