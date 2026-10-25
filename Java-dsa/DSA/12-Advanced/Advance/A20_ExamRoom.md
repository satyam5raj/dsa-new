# A20 · Exam Room ⭐

| | |
|---|---|
| **LeetCode** | [#855 Exam Room](https://leetcode.com/problems/exam-room/) |
| **Difficulty** | Medium |
| **Mix of structures** | **TreeSet** of taken seats + **max-heap of gaps** with **lazy deletion** |
| **Code** | [A20_ExamRoom.java](A20_ExamRoom.java) |
| **Run** | `java A20_ExamRoom.java` |

⭐ = not in `leetcode.md`, added because it combines an ordered set with a heap of intervals.

---

## 1. The problem

There are `n` seats in a row, numbered `0 … n−1`.
- `seat()`: a student sits in the seat that **maximises the distance to the closest other student**; on a tie, the **lowest** seat number. In an empty room, that's seat 0.
- `leave(p)`: the student in seat `p` leaves.

```
ExamRoom(10)
seat() → 0
seat() → 9      (as far as possible from 0)
seat() → 4      (seats 4 and 5 are both 4 away from the nearest student; 4 is lower)
seat() → 2      (gaps 0..4 and 4..9 both give distance 2; seat 2 < seat 6)
leave(4)
seat() → 5      (the gap 2..9 now gives seat 5, distance 3)
```

---

## 2. How to think about it

### Think in gaps
Between two neighbouring students at `a` and `b`, the best seat is the middle, `(a + b) / 2`, at distance `(b − a) / 2`. The ends are special:
- before the **first** student (at `b`), the best seat is **0**, at distance `b`;
- after the **last** student (at `a`), the best seat is **n − 1**, at distance `n − 1 − a`.

Use sentinels so every gap looks the same: `a = −1` means "the left wall" and `b = n` means "the right wall".

### Approach 1: TreeSet + scan
Keep taken seats in a `TreeSet` (sorted). `seat()` walks all neighbouring pairs and picks the best gap: **O(k)** for k students. That's simple and is LeetCode's official solution.

### Approach 2: TreeSet + heap of gaps (main)
Keep every gap in a **max-heap** ordered by (distance desc, seat asc); the top is the answer.
- **seat:** pop the best gap `(a, b)`, sit at s, and push the two new gaps `(a, s)` and `(s, b)`.
- **leave(p):** the gaps `(lo, p)` and `(p, hi)` merge into `(lo, hi)`. Removing the two old gaps from a heap would be O(n), so **don't**: push `(lo, hi)` and let the old ones go stale.

**Lazy deletion:** when a gap reaches the top, check it against the TreeSet. `(a, b)` is still real only if `a` is still taken (or is the wall) **and** `b` is the very next taken seat after `a`. Otherwise, discard it.

### Dry run: n = 10 (gap → seat, distance)
| Call | Heap work | Seats |
|---|---|---|
| `seat()` | top (−1,10) → seat 0. Push (0,10). ((−1,0) has no empty seat) | {0} |
| `seat()` | top (0,10) → seat 9, distance 9. Push (0,9) | {0, 9} |
| `seat()` | top (0,9) → seat 4, distance 4. Push (0,4), (4,9) | {0, 4, 9} |
| `seat()` | (0,4) → seat 2, dist 2 vs (4,9) → seat 6, dist 2. Tie → lower seat: **2**. Push (0,2), (2,4) | {0, 2, 4, 9} |
| `leave(4)` | push (2,9) → seat 5, dist 3. (2,4) and (4,9) are stale now | {0, 2, 9} |
| `seat()` | top (2,9): valid (next after 2 is 9) → **5** | {0, 2, 5, 9} |

---

## 3. Java concepts used in this problem

### 3.1 TreeSet neighbours
`seats.lower(p)` / `seats.higher(p)` give the nearest taken seats on each side (or `null`); `first()` and `last()` give the extremes. All are O(log n).

### 3.2 A heap whose comparator calls methods
```java
gaps = new PriorityQueue<>((x, y) -> {
    int dx = distance(x), dy = distance(y);
    return dx != dy ? Integer.compare(dy, dx) : Integer.compare(seatIn(x), seatIn(y));
});
```
The ordering is derived from each gap's `{a, b}` by helper methods. Since `a`, `b` and `n` never change, an entry's position in the heap stays correct.

### 3.3 Validity check with `Integer` and `null`
```java
Integer next = g[0] == -1 ? (seats.isEmpty() ? null : seats.first()) : seats.higher(g[0]);
return (next == null ? n : next) == g[1];
```
`next == null` means there's no student after `a`, which is the right wall. Mixing `Integer` and `int` in `==` unboxes, which is safe because `null` was handled first.

### 3.4 Constructor work in a lambda
The comparator lambda calls `distance` and `seatIn`, which read the field `n`. It's assigned in the constructor before the heap is used, so this is fine.

---

## 4. The referee: try every empty seat

### Code ([A20_ExamRoom.java:45-61](A20_ExamRoom.java#L45-L61))
```java
static class BruteExamRoom {
    private final boolean[] taken;
    BruteExamRoom(int n) { taken = new boolean[n]; }

    public int seat() {
        int n = taken.length, best = -1, bestDist = -1;
        for (int s = 0; s < n; s++) {
            if (taken[s]) continue;
            int d = Integer.MAX_VALUE;
            for (int o = 0; o < n; o++) if (taken[o]) d = Math.min(d, Math.abs(o - s));
            if (d > bestDist) { bestDist = d; best = s; }               // strict > keeps the lowest seat on ties
        }
        taken[best] = true;
        return best;
    }
    public void leave(int p) { taken[p] = false; }
}
```

This follows the definition literally: for each empty seat, the distance to the nearest student, keeping the best. In an empty room every distance is `MAX_VALUE`, so seat 0 wins. **O(n²)** per seat.

---

## 5. Approach 1: TreeSet, scan the gaps

### Code ([A20_ExamRoom.java:64-84](A20_ExamRoom.java#L64-L84))
```java
static class ScanExamRoom {
    private final int n;
    private final TreeSet<Integer> seats = new TreeSet<>();
    ScanExamRoom(int n) { this.n = n; }

    public int seat() {
        int best = 0;
        if (!seats.isEmpty()) {
            int bestDist = seats.first();                               // sit at 0: distance to the first student
            Integer prev = null;
            for (int s : seats) {
                if (prev != null && (s - prev) / 2 > bestDist) { bestDist = (s - prev) / 2; best = prev + bestDist; }
                prev = s;
            }
            if (n - 1 - seats.last() > bestDist) best = n - 1;           // sit at the far end
        }
        seats.add(best);
        return best;
    }
    public void leave(int p) { seats.remove(p); }
}
```

| Line | What it does |
|---|---|
| `best = 0`, `bestDist = first()` | candidate: seat 0 |
| the loop | each gap's middle; strict `>` keeps the earlier (lower) seat on ties |
| the last check | candidate: seat n − 1, checked last because it's the highest number |

**seat O(k), leave O(log k).**

---

## 6. Approach 2: TreeSet + heap of gaps (main solution)

### Code ([A20_ExamRoom.java:87-133](A20_ExamRoom.java#L87-L133))
```java
static class ExamRoom {
    private final int n;
    private final TreeSet<Integer> seats = new TreeSet<>();
    private final PriorityQueue<int[]> gaps;                            // {a, b}: a = -1 left wall, b = n right wall

    ExamRoom(int n) {
        this.n = n;
        gaps = new PriorityQueue<>((x, y) -> {
            int dx = distance(x), dy = distance(y);
            return dx != dy ? Integer.compare(dy, dx) : Integer.compare(seatIn(x), seatIn(y));
        });
        gaps.add(new int[]{-1, n});
    }

    private int seatIn(int[] g) { return g[0] == -1 ? 0 : g[1] == n ? n - 1 : (g[0] + g[1]) / 2; }

    private int distance(int[] g) {
        if (g[0] == -1 && g[1] == n) return n;                          // empty room: the only gap
        if (g[0] == -1) return g[1];                                    // sit at 0, nearest student at b
        if (g[1] == n) return n - 1 - g[0];                             // sit at n-1, nearest student at a
        return (g[1] - g[0]) / 2;
    }

    private boolean valid(int[] g) {
        if (g[0] != -1 && !seats.contains(g[0])) return false;
        Integer next = g[0] == -1 ? (seats.isEmpty() ? null : seats.first()) : seats.higher(g[0]);
        return (next == null ? n : next) == g[1];                       // b must be the very next student after a
    }

    private void push(int a, int b) { if (b - a > 1) gaps.add(new int[]{a, b}); }   // only gaps with an empty seat

    public int seat() {
        while (!valid(gaps.peek())) gaps.poll();                        // stale gaps from earlier leaves
        int[] g = gaps.poll();
        int s = seatIn(g);
        seats.add(s);
        push(g[0], s);
        push(s, g[1]);
        return s;
    }

    public void leave(int p) {
        seats.remove(p);
        Integer lo = seats.lower(p), hi = seats.higher(p);
        push(lo == null ? -1 : lo, hi == null ? n : hi);                // the merged gap; the two old ones go stale
    }
}
```

### Line by line
| Line | What it does |
|---|---|
| start with `(−1, n)` | an empty room is one big gap |
| `seatIn` | the best seat inside a gap (0 or n − 1 at the walls) |
| `distance` | how far that seat is from the nearest student |
| `valid` | `a` still taken (or the wall), and `b` is the next taken seat after `a` |
| `push` only if `b − a > 1` | a gap like (3, 4) has no empty seat |
| `seat`: the `while` | discard stale gaps |
| `seat`: split | sitting at s splits (a, b) into (a, s) and (s, b) |
| `leave` | push the merged gap between p's neighbours |

### Why stale gaps are harmless
A stale gap fails `valid` as soon as it reaches the top. A gap can also become valid **again**: after someone leaves, an old gap might describe the room correctly once more. That's fine, because it then describes a real gap.

### Complexity
- Each `seat` pushes 2 gaps and each `leave` pushes 1, so the total pops are bounded by the total pushes: **amortised O(log n)** per operation.
- Space: O(number of operations) for the heap.

---

## 7. Comparing the approaches

| | seat | leave |
|---|---|---|
| Brute force | O(n²) | O(1) |
| TreeSet scan | O(k) | O(log k) |
| **TreeSet + heap** | amortised O(log n) | O(log n) |

The file runs 60,000 random seat/leave operations and checks all three return the same seats.

---

## 8. Edge cases and common mistakes

**Edge cases:** n = 1, an empty room (seat 0), the first student leaving (the left wall gap reopens), ties between a middle seat and an end seat, a full room (LeetCode never calls `seat` then).

**Mistakes:**
1. Treating the ends like middle gaps: before the first student the best seat is 0, not the midpoint.
2. Breaking ties toward the higher seat.
3. Removing gaps from the heap with `remove(...)` (O(n)) instead of lazy deletion.
4. A validity check that only tests whether both ends are taken; they must also be **adjacent** in the TreeSet.
5. Pushing gaps with no empty seat (`b − a = 1`).

---

## 9. Interview questions and answers

**Q1. What does the heap store?**
> Gaps between neighbouring students (with walls as sentinels), ordered by the distance the best seat inside would get, then by that seat's number.

**Q2. How do you handle `leave` without searching the heap?**
> Push the merged gap and leave the two old gaps in the heap. When an old gap reaches the top, the TreeSet shows its ends are no longer neighbours, so it's discarded.

**Q3. When would you choose the simple scan?**
> When n or the number of students is small, or `seat` is rare. It's easier to get right in an interview, and you can mention the heap upgrade.

### Follow-up questions

**F1. "Seats in a 2-D room."**
> Distances become 2-D (e.g. Manhattan), and gaps aren't intervals any more; use a spatial approach or brute force over empty seats.

**F2. "Students prefer seats near the front."**
> Change the tie-break (or the distance function) in the comparator; the structure stays the same.

**F3. "Similar problems?"**
> Seat Reservation Manager (a min-heap of free seats) and Maximize Distance to Closest Person (the one-shot version of this).

---

## 10. Running the file

```bash
java A20_ExamRoom.java     # 4/4 tests passed (LeetCode example, one seat, re-opened wall gap, 60,000 random ops)
```

**One-line summary:** *"Keep taken seats in a TreeSet and all gaps (with walls as −1 and n) in a max-heap by (distance desc, seat asc). seat pops the best still-valid gap and splits it; leave pushes the merged gap, and stale gaps are discarded when they surface."*
